package dev.anvilcraft.lib.v2.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Performs the actual shell-by-shell spherical block removal, driven by
 * {@link ServerTickEvent.Post}. Each tick removes at most {@code maxBreakPerTick}
 * blocks from the current shell until the explosion radius is exhausted,
 * then self-unregisters.
 *
 * <p>Shell points are generated via the
 * <a href="https://extremelearning.com.au/evenly-distributing-points-on-a-sphere/">Fibonacci sphere</a>
 * (golden-angle spiral) algorithm and de-duplicated to discrete
 * {@link BlockPos}.
 */
class ExplosionSession {
    private final ServerLevel level;
    private final BlockPos center;
    private final int maxRadius;
    private final int maxBreakPerTick;
    private final boolean dropItems;

    private int currentRadius;
    private @Nullable List<BlockPos> currentShell;
    private int shellCursor;
    private boolean finished;

    ExplosionSession(ServerLevel level, BlockPos center, int maxRadius, int maxBreakPerTick, boolean dropItems) {
        this.level = level;
        this.center = center;
        this.maxRadius = maxRadius;
        this.maxBreakPerTick = maxBreakPerTick;
        this.dropItems = dropItems;
    }

    // ---- lifecycle ----

    void start() {
        this.currentRadius = 0;
        this.currentShell = null;
        this.shellCursor = 0;
        this.finished = false;
        NeoForge.EVENT_BUS.register(this);
    }

    private void stop() {
        NeoForge.EVENT_BUS.unregister(this);
    }

    // ---- tick processing ----

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (this.finished) {
            this.stop();
            return;
        }

        int removed = 0;
        while (removed < this.maxBreakPerTick && this.currentRadius <= this.maxRadius) {
            if (this.currentShell == null) {
                this.currentShell = this.generateShell(this.currentRadius);
                this.shellCursor = 0;
                if (this.currentShell.isEmpty()) {
                    this.currentRadius++;
                    continue;
                }
            }

            while (this.shellCursor < this.currentShell.size() && removed < this.maxBreakPerTick) {
                BlockPos target = this.currentShell.get(this.shellCursor);
                this.shellCursor++;

                if (!this.level.isLoaded(target)) continue;
                if (this.level.getBlockState(target).isAir()) continue;

                ExplosionSession.destroyBlock(this.level, target, this.dropItems);
                removed++;
            }

            if (this.shellCursor >= this.currentShell.size()) {
                this.currentRadius++;
                this.currentShell = null;
            }
        }

        if (this.currentRadius > this.maxRadius) {
            this.finished = true;
            this.stop();
        }
    }

    // ---- Fibonacci sphere generation ----

    /**
     * Generate uniformly-distributed block positions for the spherical shell at
     * integer radius {@code r} (distance ∈ (r-1, r]).
     *
     * <p>A single Fibonacci sphere samples only the exact surface; rounding to
     * {@link BlockPos} leaves interior gaps.  We therefore generate points on
     * multiple concentric sub-spheres within the shell, then deduplicate.
     */
    private List<BlockPos> generateShell(int r) {
        if (r == 0) {
            return List.of(this.center);
        }

        // Sub-radii evenly distributed through the shell thickness:
        // one right at the outer surface, the rest stepped inward.
        // 4 layers guarantees that any integer lattice point in the shell
        // is within ~0.87 (√3/2) of at least one sample point.
        double[] subRadii = {
            r,
            r - 0.333,
            r - 0.667
        };

        // Surface area of the outer bounding sphere determines sample density.
        double surfaceArea = 4.0 * Math.PI * r * r;
        int basePoints = Math.max((int) Math.ceil(surfaceArea), 1);

        LinkedHashSet<BlockPos> blocks = new LinkedHashSet<>((int) (basePoints * subRadii.length / 0.75) + 1);

        for (double radius : subRadii) {
            this.generateFibonacciPoints(radius, basePoints, blocks);
        }

        return new ArrayList<>(blocks);
    }

    /**
     * Generate Fibonacci-sphere points at the given {@code radius}, convert to
     * {@link BlockPos}, and insert into {@code sink}.
     */
    private void generateFibonacciPoints(double radius, int numPoints, LinkedHashSet<BlockPos> sink) {
        double phi = (1.0 + Math.sqrt(5.0)) / 2.0; // golden ratio

        for (int i = 0; i < numPoints; i++) {
            double y = 1.0 - (2.0 * i) / (numPoints - 1);    // y ∈ [+1, -1]
            double radiusAtY = Math.sqrt(1.0 - y * y);
            double theta = 2.0 * Math.PI * i / phi;

            double x = Math.cos(theta) * radiusAtY;
            double z = Math.sin(theta) * radiusAtY;

            int bx = this.center.getX() + (int) Math.round(x * radius);
            int by = this.center.getY() + (int) Math.round(y * radius);
            int bz = this.center.getZ() + (int) Math.round(z * radius);

            sink.add(new BlockPos(bx, by, bz));
        }
    }

    public static void destroyBlock(ServerLevel level, BlockPos pos, boolean dropResources) {
        BlockState blockState = level.getBlockState(pos);
        if (blockState.isAir()) {
            return;
        }
        FluidState fluidState = level.getFluidState(pos);
        if (dropResources) {
            BlockEntity blockEntity = blockState.hasBlockEntity() ? level.getBlockEntity(pos) : null;
            Block.dropResources(blockState, level, pos, blockEntity, null, ItemStack.EMPTY);
        }
        boolean destroyed = level.setBlock(pos, fluidState.createLegacyBlock(), Block.UPDATE_ALL, 512);
        if (destroyed) {
            level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(null, blockState));
        }
    }
}
