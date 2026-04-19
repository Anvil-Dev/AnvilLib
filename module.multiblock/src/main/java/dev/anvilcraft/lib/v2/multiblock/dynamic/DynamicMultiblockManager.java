package dev.anvilcraft.lib.v2.multiblock.dynamic;

import dev.anvilcraft.lib.v2.multiblock.AnvilLibMultiblock;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.ControllerRecord;
import dev.anvilcraft.lib.v2.multiblock.dynamic.controller.IController;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.multiblock.init.LibRegistries;
import dev.anvilcraft.lib.v2.multiblock.network.MultiblockFormPacket;
import dev.anvilcraft.lib.v2.multiblock.network.MultiblockUnformPacket;
import dev.anvilcraft.lib.v2.util.Util;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
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
    private static final String TAG_LIST = "multiblocks";
    private static final String SAVED_DATA_NAME = AnvilLibMultiblock.of("multiblocks")
        .toString()
        .replace(':', '_');
    private static final Map<Level, DynamicMultiblockManager> CLIENT_SIDE = new WeakHashMap<>();

    private final Map<Long, MultiblockState> multiblocks = new HashMap<>();
    private int tickCounterUnformed = 0;
    private int tickCounterFormed = 0;

    /**
     * 获取指定世界的 DynamicMultiblockManager 实例。
     *
     * <p>在服务器端，管理器作为持久化的 {@link SavedData} 存储；在客户端使用内存缓存。
     *
     * @param level 世界实例
     * @return 对应世界的管理器
     */
    public static DynamicMultiblockManager get(Level level) {
        if (!(level instanceof ServerLevel serverside)) return CLIENT_SIDE.computeIfAbsent(level, a -> new DynamicMultiblockManager());
        return serverside.getDataStorage()
            .computeIfAbsent(new Factory<>(DynamicMultiblockManager::new, DynamicMultiblockManager::load), SAVED_DATA_NAME);
    }

    /**
     * 获取指定位置（控制器位置）注册的多方块状态。
     *
     * @param pos 控制器方块位置
     * @return 对应的 {@link MultiblockState}，若不存在则返回 {@code null}
     */
    public @Nullable MultiblockState getAt(BlockPos pos) {
        return this.multiblocks.get(pos.asLong());
    }

    /**
     * 将新的多方块状态注册到管理器中并标记数据已变更以便保存。
     *
     * @param state 待注册的多方块状态，控制器位置由 {@link MultiblockState#getControllerPos()} 提供
     */
    public void add(MultiblockState state) {
        this.multiblocks.put(state.getControllerPos().asLong(), state);
        this.setDirty();
    }

    /**
     * 按控制器位置移除多方块状态并将其加入最近移除队列，返回被移除的状态。
     *
     * @param pos 控制器位置
     * @return 被移除的 {@link MultiblockState}；若未注册则返回 {@code null}
     */
    public @Nullable MultiblockState removeAt(BlockPos pos) {
        MultiblockState removed = this.multiblocks.remove(pos.asLong());
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
        return this.multiblocks.containsKey(pos.asLong());
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
        if (level.isClientSide) return;

        BlockPos controllerPos = cur.getControllerPos();
        BlockState state = level.getBlockState(controllerPos);
        if (cur.getDefinition().value().isController(level, state, level.getBlockEntity(controllerPos))) {
            IController controller = ControllerRecord.get(
                state.getBlock(),
                cur.getDefinitionKey().location()
            );
            if (formed) {
                controller.onFormed(level, cur);
            } else {
                controller.onUnformed(level, cur);
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

    public static void onPlace(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;
        DynamicMultiblockManager manager = DynamicMultiblockManager.get(level);

        // 尝试通过所有定义匹配，这里以放置点为中心
        HolderLookup.Provider registries = level.registryAccess();
        var definitions = registries.lookupOrThrow(LibRegistries.DEFINITIONS_KEY);
        var entries = definitions.listElements().toList();
        for (Holder.Reference<MultiblockDefinition> holder : entries) {
            MultiblockDefinition definition = holder.value();
            if (!definition.isController(level, state, null)) continue;
            MultiblockState mstate = new MultiblockState(pos.immutable(), holder);
            manager.add(mstate);
            manager.checkMultiblockFormed(level, mstate);
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
            manager.multiblocks.remove(pos.asLong());
            manager.setDirty();
            return;
        }

        for (MultiblockState state : manager.multiblocks.values()) {
            if (!state.isFormed()) continue;
            MultiblockDefinition def = state.getDefinition().value();
            Map<BlockPos, BlockStatePredicate> global = def.toGlobal(state.getControllerPos());
            if (global.containsKey(pos)) {
                // 非控制器方块被破坏，将多方块标记为未形成（会通知控制器并广播）
                manager.updateFormed(level, state, false);
            }
        }
    }

    /**
     * 周期性检测最近被移除及所有注册的多方块，尝试重新形成（如果条件满足）。
     *
     * <p>此方法由定时调度器在服务器端调用。为降低开销，仅在计数器达到配置的间隔时执行实际检测。
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

        if (checkUnformed) {
            for (MultiblockState state : manager.multiblocks.values()) {
                if (!checkFormed && state.isFormed()) continue;
                manager.checkMultiblockFormed(level, state);
            }
        } else {
            for (MultiblockState state : manager.multiblocks.values()) {
                if (!state.isFormed()) continue;
                manager.checkMultiblockFormed(level, state);
            }
        }
    }

    private void checkMultiblockFormed(Level level, MultiblockState state) {
        if (level.isClientSide) return;
        MultiblockDefinition def = state.getDefinition().value();
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

    /**
     * 从持久化数据中加载 DynamicMultiblockManager 的状态。
     *
     * <p>此方法在 {@link SavedData} 恢复时被调用，负责从 NBT 中反序列化已注册的多方块控制器及最近移除队列。
     *
     * @param tag       存储在磁盘/数据包中的 NBT 标签
     * @param registries 注册表访问提供者（用于解析定义引用）
     * @return 恢复后的管理器实例
     */
    private static DynamicMultiblockManager load(CompoundTag tag, HolderLookup.Provider registries) {
        DynamicMultiblockManager manager = new DynamicMultiblockManager();
        if (tag.contains(TAG_LIST)) {
            ListTag list = tag.getList(TAG_LIST, Tag.TAG_COMPOUND);
            for (Tag value : list) {
                MultiblockState mb = MultiblockState.fromTag(Util.cast(value), registries);
                manager.multiblocks.put(mb.getControllerPos().asLong(), mb);
            }
        }
        return manager;
    }

    /**
     * 将当前管理器状态序列化为 NBT，用于持久化保存。
     *
     * @param tag        传入的 NBT 容器，将在此方法中被填充并返回
     * @param registries 注册表访问提供者（用于序列化定义引用）
     * @return 填充后的 NBT 容器
     */
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (MultiblockState state : this.multiblocks.values()) {
            list.add(state.toTag(registries));
        }
        tag.put(TAG_LIST, list);
        return tag;
    }
}
