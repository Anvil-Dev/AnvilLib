package dev.anvilcraft.lib.v2.cube.client;

import dev.anvilcraft.lib.v2.cube.client.model.ModelSelection;
import dev.anvilcraft.lib.v2.cube.client.model.ModelSelections;
import dev.anvilcraft.lib.v2.cube.geometry.OutlineCache;
import dev.anvilcraft.lib.v2.cube.geometry.SelectionGeometry;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/** 资源包控制选取与描边模式；客户端注册接口提供默认启用范围及动态几何。 */
public final class CubeSelection {
    public static final int MAX_PARTS = 32;
    private static final Set<String> NAMESPACES = ConcurrentHashMap.newKeySet();
    private static final Set<Block> EXCLUDED = ConcurrentHashMap.newKeySet();
    private static final Map<ResourceLocation, Predicate<BlockState>> TARGET_EXCLUSIONS = new ConcurrentHashMap<>();
    private static final Map<Block, Dynamic> DYNAMIC = new ConcurrentHashMap<>();
    private static final OutlineCache OUTLINES = new OutlineCache();
    private static final Long2ObjectOpenHashMap<Target> FRAME = new Long2ObjectOpenHashMap<>();
    private static CubeSelectionRules rules = CubeSelectionRules.EMPTY;
    private static Map<BlockState, ModelSelection> models = Map.of();
    private static Map<BlockState, Integer> extended = Map.of();
    private static @Nullable ClientLevel frameLevel;
    private static int radius;
    private static boolean pickingActive;
    private static float partialTick;
    private static long geometryBytes;
    private static int unsupportedStates;

    private CubeSelection() { }

    public static void enableNamespace(String namespace) {
        ResourceLocation.fromNamespaceAndPath(namespace, "cube_selection");
        NAMESPACES.add(namespace);
    }

    public static void exclude(Block block) { EXCLUDED.add(block); }

    /**
     * 注册运行时目标排除规则；同一 id 再次注册会替换旧规则，不同 id 的规则互不覆盖。
     * 任意规则返回 true 时，target 返回 null，精确拾取与默认模型高亮均回退原版。
     *
     * <p>规则不影响模型烘焙和 modelParts，可读取接入方当前的资源包配置。
     * 每次目标查询都会在帧缓存之前检查规则，因此修改配置或注销规则后即可恢复已加载的模型。
     * 回调应保持轻量、无副作用，不应捕获世界或方块实体等会跨存档保留的对象。
     *
     * @param id 由调用模组自己的命名空间限定的规则标识
     * @param exclusion 返回 true 表示该状态不使用模型目标
     */
    public static void registerTargetExclusion(ResourceLocation id, Predicate<BlockState> exclusion) {
        TARGET_EXCLUSIONS.put(id, exclusion);
    }

    /**
     * 注销指定运行时规则；不会清除其他规则或 exclude(Block) 设置的永久排除。
     *
     * @return 是否存在并移除了该规则
     */
    public static boolean unregisterTargetExclusion(ResourceLocation id) {
        return TARGET_EXCLUSIONS.remove(id) != null;
    }

    /** 检查永久排除及当前运行时规则；不表示该状态一定存在可用的模型几何。 */
    public static boolean isTargetExcluded(BlockState state) {
        if (EXCLUDED.contains(state.getBlock())) return true;
        for (Predicate<BlockState> exclusion : TARGET_EXCLUSIONS.values()) {
            if (exclusion.test(state)) return true;
        }
        return false;
    }

    /** 部件变换须与 BER 一致；最大包围盒用于寻找越过锚点格的机械臂等部件。 */
    public static void registerDynamic(Block block, AABB maximumBounds, boolean includeStaticModel, BlockSelectionProvider provider) {
        if (!supportedBounds(block.defaultBlockState().hasOffsetFunction() ? maximumBounds.inflate(0.5) : maximumBounds)) {
            throw new IllegalArgumentException("Dynamic bounds including model offset must lie in [-2, 3]");
        }
        DYNAMIC.put(block, new Dynamic(maximumBounds, includeStaticModel, provider));
        if (mode(block, rules) == CubeSelectionRules.Mode.MODEL) {
            pickingActive = true;
            radius = Math.max(radius, reach(maximumBounds));
        }
    }

    /** 是否启用模型几何；outline 模式仍需烘焙并可由自定义描边器读取。 */
    public static boolean isEnabled(Block block) {
        return isEnabled(block, rules);
    }

    static boolean isEnabled(Block block, CubeSelectionRules selectionRules) {
        return mode(block, selectionRules) != CubeSelectionRules.Mode.VANILLA;
    }

    private static CubeSelectionRules.Mode mode(Block block, CubeSelectionRules selectionRules) {
        if (EXCLUDED.contains(block)) {
            return CubeSelectionRules.Mode.VANILLA;
        }
        var id = BuiltInRegistries.BLOCK.getKey(block);
        CubeSelectionRules.Mode fallback = NAMESPACES.contains(id.getNamespace()) || DYNAMIC.containsKey(block)
            ? CubeSelectionRules.Mode.MODEL : CubeSelectionRules.Mode.VANILLA;
        return selectionRules.mode(id, fallback);
    }

    public static boolean supportedBounds(AABB bounds) {
        return Double.isFinite(bounds.minX) && Double.isFinite(bounds.minY) && Double.isFinite(bounds.minZ)
            && Double.isFinite(bounds.maxX) && Double.isFinite(bounds.maxY) && Double.isFinite(bounds.maxZ)
            && bounds.minX >= -2 && bounds.minY >= -2 && bounds.minZ >= -2
            && bounds.maxX <= 3 && bounds.maxY <= 3 && bounds.maxZ <= 3;
    }

    static void install(ModelSelections.Snapshot snapshot, CubeSelectionRules selectionRules) {
        rules = selectionRules;
        install(snapshot);
    }

    public static void install(ModelSelections.Snapshot snapshot) {
        OUTLINES.clear();
        FRAME.clear();
        frameLevel = null;
        models = snapshot.states();
        geometryBytes = snapshot.geometryBytes();
        unsupportedStates = snapshot.unsupportedStates();
        Map<BlockState, Integer> overhang = new IdentityHashMap<>();
        radius = 0;
        pickingActive = false;
        models.forEach((state, selection) -> {
            if (mode(state.getBlock(), rules) != CubeSelectionRules.Mode.MODEL) {
                return;
            }
            pickingActive = true;
            int reach = reach(selection.bounds());
            // 原版模型偏移由位置决定，不可烘入共享几何。
            if (state.hasOffsetFunction()) reach = Math.min(2, reach + 1);
            if (reach > 0) overhang.put(state, reach);
        });
        extended = overhang;
        for (int value : extended.values()) radius = Math.max(radius, value);
        DYNAMIC.forEach((block, dynamic) -> {
            if (mode(block, rules) == CubeSelectionRules.Mode.MODEL) {
                pickingActive = true;
                radius = Math.max(radius, reach(dynamic.bounds));
            }
        });
    }

    public static void beginFrame(ClientLevel level, float fraction) {
        FRAME.clear();
        frameLevel = level;
        partialTick = fraction;
    }

    public static void clearWorld() {
        FRAME.clear();
        frameLevel = null;
        OUTLINES.clear();
    }

    public static OutlineCache outlines() { return OUTLINES; }
    public static int searchRadius() { return radius; }
    /** 当前快照是否需要执行模型精确拾取；仅描边的几何不会启动邻格补扫。 */
    public static boolean active() { return pickingActive; }

    static @Nullable ModelSelection modelForState(BlockState state) { return models.get(state); }

    /** 返回当前资源包的静态部件，遵守原版位置随机种子，供 BER 适配器复用。 */
    public static @Nullable List<SelectionPart> modelParts(BlockState state, BlockPos pos) {
        ModelSelection model = models.get(state);
        if (model == null || EXCLUDED.contains(state.getBlock())) return null;
        List<SelectionPart> parts = new ArrayList<>();
        model.collect(RandomSource.create(state.getSeed(pos)), parts);
        return parts.size() > MAX_PARTS ? null : List.copyOf(parts);
    }

    static float frameFraction(ClientLevel level, float fallback) { return frameLevel == level ? partialTick : fallback; }

    public static boolean extendsBlock(BlockState state) {
        if (mode(state.getBlock(), rules) != CubeSelectionRules.Mode.MODEL) {
            return false;
        }
        Dynamic dynamic = DYNAMIC.get(state.getBlock());
        return extended.containsKey(state) || dynamic != null && reach(dynamic.bounds) > 0;
    }

    /** 精确拾取入口；仅描边模式保留方块原有射线选取。 */
    public static @Nullable Target target(ClientLevel level, BlockPos pos, BlockState state, float fraction) {
        if (mode(state.getBlock(), rules) != CubeSelectionRules.Mode.MODEL) {
            return null;
        }
        return outlineTarget(level, pos, state, fraction);
    }

    /** 描边与预览共用的几何入口；运行时排除规则仍同时约束选取和默认描边。 */
    public static @Nullable Target outlineTarget(ClientLevel level, BlockPos pos, BlockState state, float fraction) {
        if (!isEnabled(state.getBlock()) || isTargetExcluded(state)) return null;
        if (frameLevel != level || fraction != partialTick) beginFrame(level, fraction);
        Target cached = FRAME.get(pos.asLong());
        if (cached != null && cached.state == state) return cached;
        ModelSelection model = modelForState(state);
        Dynamic dynamic = DYNAMIC.get(state.getBlock());
        if (model == null && dynamic == null) return null;
        List<SelectionPart> parts = new ArrayList<>();
        if (model != null && (dynamic == null || dynamic.includeStatic)) {
            model.collect(RandomSource.create(state.getSeed(pos)), parts);
        }
        if (dynamic != null) {
            List<SelectionPart> moving = dynamic.provider.parts(level, pos.immutable(), state, fraction);
            AABB allowed = dynamic.bounds.inflate(1.0E-5);
            for (SelectionPart part : moving) {
                AABB bounds = part.bounds();
                if (!allowed.contains(bounds.minX, bounds.minY, bounds.minZ)
                    || !allowed.contains(bounds.maxX, bounds.maxY, bounds.maxZ)) return null;
            }
            parts.addAll(moving);
        }
        if (parts.size() > MAX_PARTS) return null;
        int shapes = 0;
        AABB bounds = null;
        for (SelectionPart part : parts) {
            shapes += part.geometry().shapes().size();
            bounds = bounds == null ? part.bounds() : bounds.minmax(part.bounds());
        }
        if (shapes > SelectionGeometry.MAX_SHAPES) return null;
        Target result = new Target(state, List.copyOf(parts), state.getOffset(pos), bounds);
        if (FRAME.size() < 256) FRAME.put(pos.asLong(), result);
        return result;
    }

    public static Statistics statistics() {
        return new Statistics(models.size(), geometryBytes, unsupportedStates, OUTLINES.statistics());
    }

    private static int reach(AABB bounds) {
        double distance = Math.max(Math.max(Math.max(-bounds.minX, -bounds.minY), -bounds.minZ),
            Math.max(Math.max(bounds.maxX - 1, bounds.maxY - 1), bounds.maxZ - 1));
        return Math.clamp((int) Math.ceil(distance - 1.0E-7), 0, 2);
    }

    public record Target(BlockState state, List<SelectionPart> parts, Vec3 offset, @Nullable AABB bounds) {
        public @Nullable BlockHitResult clip(BlockPos pos, Vec3 start, Vec3 end) {
            Vec3 origin = Vec3.atLowerCornerOf(pos).add(this.offset);
            Vec3 localStart = start.subtract(origin), localEnd = end.subtract(origin);
            SelectionGeometry.RayHit best = null;
            for (SelectionPart part : this.parts) {
                SelectionGeometry.RayHit hit = part.clip(localStart, localEnd);
                if (hit != null && (best == null || hit.fraction() < best.fraction())) best = hit;
            }
            if (best == null) return null;
            Vec3 normal = best.normal();
            return new BlockHitResult(start.lerp(end, best.fraction()), Direction.getApproximateNearest(normal.x, normal.y, normal.z),
                pos.immutable(), best.inside());
        }
    }

    public record Statistics(int states, long geometryBytes, int unsupportedStates, OutlineCache.Statistics outlines) { }
    private record Dynamic(AABB bounds, boolean includeStatic, BlockSelectionProvider provider) { }
}
