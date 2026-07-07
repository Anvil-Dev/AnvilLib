package dev.anvilcraft.lib.v2.multiblock.dynamic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.multiblock.AnvilLibMultiblock;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.ControllerRecord;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.IController;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.multiblock.dynamic.event.DynamicMultiblockEvent;
import dev.anvilcraft.lib.v2.multiblock.init.LibRegistries;
import dev.anvilcraft.lib.v2.multiblock.network.MultiblockFormPacket;
import dev.anvilcraft.lib.v2.multiblock.network.MultiblockUnformPacket;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.UnknownNullability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/**
 * 管理世界中的动态多方块（dynamic multiblock）实例。
 *
 * <p>此类负责：
 * <ul>
 *   <li>在方块放置/破坏时创建或取消多方块。</li>
 *   <li>保存/加载当前已注册的多方块控制器状态。</li>
 *   <li>定期检测最近移除的多方块能否重新形成。</li>
 * </ul>
 *
 * <p>此数据在服务器上以 {@link SavedData} 形式持久化；客户端侧使用缓存实例。
 */
public class DynamicMultiblockManager extends SavedData {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicMultiblockManager.class);
    private static final Map<Level, DynamicMultiblockManager> CLIENT_SIDE = new WeakHashMap<>();
    public static final Codec<DynamicMultiblockManager> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        MultiblockState.CODEC.codec()
            .listOf()
            .fieldOf("multiblocks")
            .forGetter(DynamicMultiblockManager::getStates)
    ).apply(inst, DynamicMultiblockManager::new));
    public static final SavedDataType<DynamicMultiblockManager> TYPE = new SavedDataType<>(
        AnvilLibMultiblock.of("multiblocks"),
        DynamicMultiblockManager::new,
        DynamicMultiblockManager.CODEC,
        null
    );

    private final Map<BlockPos, MultiblockState> multiblocks = new HashMap<>();
    private int tickCounterUnformed = 0;
    private int tickCounterFormed = 0;

    /** 异步检测线程池（惰性初始化）。 */
    private static volatile @UnknownNullability ExecutorService asyncExecutor;

    /** 当前正在异步检测中的控制器位置集合，用于去重。 */
    private final Set<BlockPos> pendingChecks = ConcurrentHashMap.newKeySet();

    private DynamicMultiblockManager() {
    }

    private DynamicMultiblockManager(List<MultiblockState> states) {
        for (MultiblockState state : states) {
            this.multiblocks.put(state.getControllerPos().immutable(), state);
        }
    }

    /**
     * 获取指定世界的 DynamicMultiblockManager 实例。
     *
     * <p>在服务器端，管理器作为持久化的 {@link SavedData} 存储；在客户端使用内存缓存。
     *
     * @param level 世界实例
     * @return 对应世界的管理器
     */
    public static DynamicMultiblockManager get(Level level) {
        if (!(level instanceof ServerLevel serverside)) return CLIENT_SIDE.computeIfAbsent(level, _ -> new DynamicMultiblockManager());
        return serverside.getDataStorage().computeIfAbsent(DynamicMultiblockManager.TYPE);
    }

    /**
     * 获取指定位置（控制器位置）注册的多方块状态。
     *
     * @param pos 控制器方块位置
     * @return 对应的 {@link MultiblockState}，若不存在则返回 {@code null}
     */
    public @Nullable MultiblockState getAt(BlockPos pos) {
        return this.multiblocks.get(pos.immutable());
    }

    /**
     * 将新的多方块状态注册到管理器中并标记数据已变更以便保存。
     *
     * @param state 待注册的多方块状态，控制器位置由 {@link MultiblockState#getControllerPos()} 提供
     */
    public void add(MultiblockState state) {
        this.multiblocks.put(state.getControllerPos().immutable(), state);
        this.setDirty();
    }

    /**
     * 按控制器位置移除多方块状态并将其加入最近移除队列，返回被移除的状态。
     *
     * @param pos 控制器位置
     * @return 被移除的 {@link MultiblockState}；若未注册则返回 {@code null}
     */
    public @Nullable MultiblockState removeAt(BlockPos pos) {
        MultiblockState removed = this.multiblocks.remove(pos.immutable());
        if (removed != null) this.setDirty();
        return removed;
    }

    /**
     * 判断给定位置是否注册为多方块的控制器。
     *
     * @param pos 控制器位置
     * @return 若存在注册则返回 {@code true}
     */
    public boolean containsAt(BlockPos pos) {
        return this.multiblocks.containsKey(pos.immutable());
    }

    /**
     * 更新多方块的形成状态（formed/unformed）。
     *
     * <p>当 formed 状态变化时：
     * <ul>
     *   <li>如果控制器仍然有效则回调控制器的 {@code onFormed} / {@code onUnformed}。</li>
     *   <li>在客户端广播对应的 Form/Unform 包。</li>
     *   <li>在取消形成时将状态加入最近移除队列以便稍后重试。</li>
     * </ul>
     *
     * @param level 当前世界实例（服务器端）
     * @param cur   待更新的多方块状态
     * @param formed 目标形成状态
     */
    public void updateFormed(Level level, MultiblockState cur, boolean formed) {
        if (cur.isFormed() == formed) return;
        cur.setFormed(formed);
        if (level.isClientSide()) return;

        BlockPos controllerPos = cur.getControllerPos();
        BlockState state = level.getBlockState(controllerPos);
        if (cur.getDefinition(level.registryAccess()).value().isController(level, state, level.getBlockEntity(controllerPos))) {
            IController controller;
            try {
                controller = ControllerRecord.get(
                    state.getBlock(),
                    cur.getDefinitionKey().identifier()
                );
            } catch (IllegalArgumentException e) {
                LOGGER.error(e.getLocalizedMessage(), e);
                throw e;
            }
            if (formed) {
                DynamicMultiblockEvent.Form event = new DynamicMultiblockEvent.Form(level, controller, cur);
                NeoForge.EVENT_BUS.post(event);
                if (!event.isCanceled()) {
                    controller.onFormed(level, cur);
                } else {
                    cur.setFormed(false);
                    return;
                }
            } else {
                DynamicMultiblockEvent.Unform event = new DynamicMultiblockEvent.Unform(level, controller, cur);
                NeoForge.EVENT_BUS.post(event);
                if (!event.isCanceled()) {
                    controller.onUnformed(level, cur);
                } else {
                    cur.setFormed(true);
                    return;
                }
            }
        }

        List<ServerPlayer> players = ((ServerLevel) level).players();
        if (formed) {
            for (ServerPlayer player : players) {
                PacketDistributor.sendToPlayer(player, new MultiblockFormPacket(cur));
            }
        } else {
            for (ServerPlayer player : players) {
                PacketDistributor.sendToPlayer(player, new MultiblockUnformPacket(cur));
            }
        }

        this.setDirty();
    }

    public static void onPlace(ServerLevel level, BlockPos pos, BlockState state) {
        DynamicMultiblockManager manager = DynamicMultiblockManager.get(level);

        // 尝试通过所有定义匹配，这里以放置点为中心
        HolderLookup.Provider registries = level.registryAccess();
        var definitions = registries.lookupOrThrow(LibRegistries.DEFINITIONS_KEY);
        var entries = definitions.listElements().toList();
        for (Holder.Reference<MultiblockDefinition> holder : entries) {
            MultiblockDefinition definition = holder.value();
            BlockPos correctedPos;
            BlockState correctedState;
            try {
                correctedPos = ControllerRecord.get(state.getBlock(), holder.key().identifier())
                    .correctPos(level, pos, state);
                correctedState = level.getBlockState(correctedPos);
            } catch (IllegalArgumentException ignored) {
                correctedPos = pos;
                correctedState = state;
            }
            if (!definition.isController(level, correctedState, null)) continue;
            MultiblockState mstate = new MultiblockState(correctedPos.immutable(), holder.key());
            manager.add(mstate);
            // onPlace 时同步检测（立即反馈），不走异步
            manager.checkMultiblockFormedSync(level, mstate);
        }
    }

    /**
     * 当世界中某个方块被破坏时调用的处理逻辑。
     *
     * <p>服务器端调用：
     * <ul>
     *   <li>若破坏位置为多方块的控制器，则取消形成（若已形成）并移除此多方块的记录。</li>
     *   <li>否则若破坏的是已形成多方块的一部分，则将该多方块标记为未形成（触发控制器的 {@code onUnformed} 并广播）。</li>
     * </ul>
     *
     * @param level 世界实例（仅在服务器端生效）
     * @param pos   被破坏方块的位置
     */
    public static void onBreak(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        DynamicMultiblockManager manager = DynamicMultiblockManager.get(level);

        MultiblockState controllerState = manager.getAt(pos);
        if (controllerState != null) {
            if (controllerState.isFormed()) {
                manager.updateFormed(level, controllerState, false);
            }
            manager.multiblocks.remove(pos.immutable());
            manager.setDirty();
            return;
        }

        for (MultiblockState state : manager.multiblocks.values()) {
            if (!state.isFormed()) continue;
            MultiblockDefinition def = state.getDefinition(level.registryAccess()).value();
            Map<BlockPos, BlockStatePredicate> global = def.toGlobal(state.getControllerPos());
            if (global.containsKey(pos)) {
                // 非控制器方块被破坏，将多方块标记为未形成（会通知控制器并广播）
                manager.updateFormed(level, state, false);
            }
        }
    }

    // ======================== 异步周期检测 ========================

    /**
     * 获取或创建异步检测线程池。
     */
    private static ExecutorService getOrCreateExecutor() {
        if (asyncExecutor == null) {
            synchronized (DynamicMultiblockManager.class) {
                if (asyncExecutor == null) {
                    int poolSize = Math.clamp(
                        Runtime.getRuntime().availableProcessors() - 1,
                        1,
                        AnvilLibMultiblock.CONFIG.asyncThreadPoolSize
                    );
                    asyncExecutor = Executors.newFixedThreadPool(poolSize, r -> {
                        Thread t = new Thread(r, "AnvilLib-MultiblockCheck");
                        t.setDaemon(true);
                        return t;
                    });
                }
            }
        }
        return asyncExecutor;
    }

    /**
     * 关闭异步线程池。应在服务器停止时调用。
     */
    public static void shutdownExecutor() {
        synchronized (DynamicMultiblockManager.class) {
            if (asyncExecutor != null) {
                asyncExecutor.shutdownNow();
                try {
                    // noinspection ResultOfMethodCallIgnored - 为剩余任务等待5秒，无需判断是否完成
                    asyncExecutor.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                asyncExecutor = null;
            }
        }
    }

    /**
     * 周期性检测所有注册的多方块，尝试重新形成（如果条件满足）。
     *
     * <p>此方法由定时调度器在服务器端每 tick 调用。在达到配置的间隔时，
     * 在主线程上构建轻量不可变快照，然后提交到工作线程池异步执行谓词测试，
     * 完成后将结果回调到主线程进行最终验证与状态更新。
     *
     * @param level 服务器世界实例或 {@code null}
     */
    public static void checkMultiblockFormed(@Nullable ServerLevel level) {
        if (level == null) return;
        DynamicMultiblockManager manager = DynamicMultiblockManager.get(level);

        boolean checkUnformed = ++manager.tickCounterUnformed
                                % AnvilLibMultiblock.CONFIG.unformedMultiblockCheckInterval == 0;
        boolean checkFormed = ++manager.tickCounterFormed
                              % AnvilLibMultiblock.CONFIG.formedMultiblockCheckInterval == 0;

        if (!checkUnformed && !checkFormed) return;

        // 收集本次需要检测的候选（受 maxChecksPerTick 限制）
        int maxChecks = AnvilLibMultiblock.CONFIG.maxChecksPerTick;
        List<MultiblockState> candidates = new ArrayList<>();
        if (checkUnformed) {
            for (MultiblockState state : manager.multiblocks.values()) {
                if (!checkFormed && state.isFormed()) continue;
                if (manager.pendingChecks.contains(state.getControllerPos())) continue;
                candidates.add(state);
                if (candidates.size() >= maxChecks) break;
            }
        } else {
            for (MultiblockState state : manager.multiblocks.values()) {
                if (!state.isFormed()) continue;
                if (manager.pendingChecks.contains(state.getControllerPos())) continue;
                candidates.add(state);
                if (candidates.size() >= maxChecks) break;
            }
        }

        if (candidates.isEmpty()) return;

        // 在主线程上为每个候选构建快照并提交异步任务
        ExecutorService executor = getOrCreateExecutor();
        for (MultiblockState mstate : candidates) {
            MultiblockCheckSnapshot snapshot = buildSnapshot(level, mstate);
            mstate.setSnapshot(snapshot);

            BlockPos pos = mstate.getControllerPos().immutable();
            manager.pendingChecks.add(pos);

            executor.submit(() -> {
                try {
                    boolean formed = snapshot.test();
                    // 将结果回调到主线程
                    level.getServer().execute(() -> {
                        manager.pendingChecks.remove(pos);
                        // 最终一致性验证：确认该 multiblock 仍然注册
                        MultiblockState current = manager.multiblocks.get(pos.immutable());
                        if (current == null) return;
                        manager.updateFormed(level, current, formed);
                    });
                } catch (Throwable t) {
                    LOGGER.error("Async multiblock check failed for pos {}", pos, t);
                    level.getServer().execute(() -> manager.pendingChecks.remove(pos.immutable()));
                }
            });
        }
    }

    /**
     * 在主线程上为指定多方块状态构建不可变快照。
     *
     * <p>快照包含每个检测位置的 {@link BlockState} 和（若谓词依赖 NBT 则）预序列化的
     * {@link CompoundTag}，使得工作线程无需访问 {@code Level}。
     *
     * @param level 服务器世界实例
     * @param state 待检测的多方块状态
     * @return 快照；若定义不存在则返回 {@code null}
     */
    private static MultiblockCheckSnapshot buildSnapshot(ServerLevel level, MultiblockState state) {
        MultiblockCheckSnapshot old = state.getSnapshot();
        MultiblockDefinition def = state.getDefinition(level.registryAccess()).value();
        Map<BlockPos, BlockStatePredicate> global = def.toGlobal(state.getControllerPos());
        Map<BlockPos, MultiblockCheckSnapshot.Entry> entries = new LinkedHashMap<>(global.size());
        for (Map.Entry<BlockPos, BlockStatePredicate> entry : global.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockStatePredicate predicate = entry.getValue();
            if (!level.isLoaded(pos)) {
                MultiblockCheckSnapshot.Entry snapshotEntry = old.entries().get(pos);
                if (snapshotEntry == null) {
                    snapshotEntry = new MultiblockCheckSnapshot.Entry(null, null, predicate);
                }
                entries.put(pos, snapshotEntry);
                continue;
            }
            BlockState blockState = level.getBlockState(pos);
            CompoundTag entityNbt = null;
            if (predicate.requiresBlockEntity()) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be != null) {
                    entityNbt = be.saveWithFullMetadata(level.registryAccess());
                }
            }
            entries.put(pos, new MultiblockCheckSnapshot.Entry(blockState, entityNbt, predicate));
        }
        return new MultiblockCheckSnapshot(state.getControllerPos(), entries);
    }

    /**
     * 同步检测多方块形成状态（用于 onPlace 等需要立即反馈的场景）。
     */
    private void checkMultiblockFormedSync(Level level, MultiblockState state) {
        if (level.isClientSide()) return;
        MultiblockDefinition def = state.getDefinition(level.registryAccess()).value();
        Map<BlockPos, BlockStatePredicate> global = def.toGlobal(state.getControllerPos());
        boolean ok = true;
        for (Map.Entry<BlockPos, BlockStatePredicate> entry : global.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!entry.getValue().test(level, level.getBlockState(pos), level.getBlockEntity(pos))) {
                ok = false;
                break;
            }
        }
        this.updateFormed(level, state, ok);
    }

    // ======================== 持久化 ========================

    private List<MultiblockState> getStates() {
        return List.copyOf(this.multiblocks.values());
    }
}
