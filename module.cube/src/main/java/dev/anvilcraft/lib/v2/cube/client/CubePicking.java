package dev.anvilcraft.lib.v2.cube.client;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class CubePicking {
    public static final double MAX_RANGE = 64;
    private static final ThreadLocal<Context> CONTEXT = ThreadLocal.withInitial(Context::new);
    private CubePicking() { }

    /** 只包围准星射线调用，普通碰撞、服务端和投射物射线不读取客户端模型。 */
    public static HitResult pick(Entity entity, double range, float partialTick, boolean fluids) {
        if (!(entity.level() instanceof ClientLevel level) || !CubeSelection.active() || range > MAX_RANGE) {
            return entity.pick(range, partialTick, fluids);
        }
        Context context = CONTEXT.get();
        if (context.level != null) return entity.pick(range, partialTick, fluids);
        CubeSelection.beginFrame(level, partialTick);
        context.level = level;
        context.partialTick = partialTick;
        try {
            HitResult vanilla = entity.pick(range, partialTick, fluids);
            if (!(vanilla instanceof BlockHitResult block) || CubeSelection.searchRadius() == 0) return vanilla;
            Vec3 start = entity.getEyePosition(partialTick);
            Vec3 end = start.add(entity.getViewVector(partialTick).scale(range));
            context.nearest = block;
            context.distance = start.distanceToSqr(block.getLocation());
            context.visited.clear();
            // 只扫描射线邻近格，且同一锚点每帧最多检查一次，不遍历世界实体或全部方块。
            BlockGetter.traverseBlocks(start, block.getLocation(), context, (query, cell) -> {
                int radius = CubeSelection.searchRadius();
                BlockPos.MutableBlockPos anchor = query.anchor;
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            anchor.set(cell.getX() + x, cell.getY() + y, cell.getZ() + z);
                            if (!query.visited.add(anchor.asLong())) continue;
                            BlockState state = level.getBlockState(anchor);
                            if (!CubeSelection.extendsBlock(state)) continue;
                            CubeSelection.Target target = CubeSelection.target(level, anchor, state, partialTick);
                            if (target == null) continue;
                            BlockHitResult hit = target.clip(anchor, start, end);
                            if (hit == null) continue;
                            double distance = start.distanceToSqr(hit.getLocation());
                            if (distance < query.distance) { query.distance = distance; query.nearest = hit; }
                        }
                    }
                }
                return null;
            }, query -> null);
            return context.nearest;
        } finally {
            context.level = null;
            context.nearest = null;
            context.visited.clear();
        }
    }

    public static CubeSelection.@Nullable Target target(BlockGetter getter, BlockPos pos, BlockState state) {
        Context context = CONTEXT.get();
        if (getter != context.level || context.level == null) return null;
        return CubeSelection.target(context.level, pos, state, context.partialTick);
    }

    private static final class Context {
        private @Nullable ClientLevel level;
        private float partialTick;
        private final LongOpenHashSet visited = new LongOpenHashSet();
        private final BlockPos.MutableBlockPos anchor = new BlockPos.MutableBlockPos();
        private @Nullable BlockHitResult nearest;
        private double distance;
    }
}
