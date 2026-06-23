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
import java.util.Collections;
import java.util.List;

/**
 * Performs the actual block removal in a spherical explosion, driven by
 * {@link ServerTickEvent.Post}. Each tick removes at most {@code maxBreakPerTick}
 * blocks from inside to outside until the explosion radius is exhausted,
 * then self-unregisters.
 *
 * <p>Uses dynamic layer-by-layer generation to avoid memory issues with large radii.
 * Blocks are processed in distance-sorted order to maintain the visual effect of
 * an expanding explosion while ensuring complete coverage.
 */
class ExplosionSession {
    private final ServerLevel level;
    private final BlockPos center;
    private final int maxRadius;
    private final int maxBreakPerTick;
    private final boolean dropItems;

    // Current processing state
    private int currentLayer; // Current distance layer (0 to maxRadius)
    private @Nullable List<BlockPos> currentLayerBlocks; // Blocks in current layer
    private int layerIndex; // Index within current layer
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
        this.currentLayer = 0;
        this.currentLayerBlocks = null;
        this.layerIndex = 0;
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
        
        // Process blocks layer by layer to avoid memory issues with large radii
        while (removed < this.maxBreakPerTick && this.currentLayer <= this.maxRadius) {
            // Generate current layer if needed
            if (this.currentLayerBlocks == null || this.layerIndex >= this.currentLayerBlocks.size()) {
                this.currentLayerBlocks = this.generateLayerBlocks(this.currentLayer);
                this.layerIndex = 0;
                
                // Move to next layer if current one is empty
                if (this.currentLayerBlocks.isEmpty()) {
                    this.currentLayer++;
                    continue;
                }
            }
            
            // Process blocks in current layer
            while (this.layerIndex < this.currentLayerBlocks.size() && removed < this.maxBreakPerTick) {
                BlockPos target = this.currentLayerBlocks.get(this.layerIndex);
                this.layerIndex++;

                if (!this.level.isLoaded(target)) continue;
                if (this.level.getBlockState(target).isAir()) continue;

                if (ExplosionSession.destroyBlock(this.level, target, this.dropItems)) {
                    removed++;
                }
            }
            
            // Move to next layer when current one is done
            if (this.layerIndex >= this.currentLayerBlocks.size()) {
                this.currentLayer++;
                this.currentLayerBlocks = null; // Allow GC to collect
            }
        }

        if (this.currentLayer > this.maxRadius) {
            this.finished = true;
            this.stop();
        }
    }

    // ---- Block enumeration ----

    /**
     * Generate all block positions at a specific distance layer from center.
     * This approach avoids storing all blocks in memory at once for large radii.
     * 
     * @param layer The distance layer (Manhattan distance approximation for efficiency)
     * @return List of BlockPos at this layer, sorted for consistent ordering
     */
    private List<BlockPos> generateLayerBlocks(int layer) {
        List<BlockPos> blocks = new ArrayList<>();
        
        if (layer == 0) {
            // Center point
            blocks.add(this.center);
            return blocks;
        }
        
        // For each layer, we process a "shell" at approximately that distance
        // We use integer bounds to efficiently find blocks in the shell
        int r = layer;
        int rSquared = r * r;
        int innerRSquared = (r - 1) * (r - 1);
        
        // Iterate through bounding box of this shell
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    int distSquared = x * x + y * y + z * z;
                    
                    // Only include blocks in this shell layer
                    if (distSquared > innerRSquared && distSquared <= rSquared) {
                        blocks.add(new BlockPos(
                            this.center.getX() + x,
                            this.center.getY() + y,
                            this.center.getZ() + z
                        ));
                    }
                }
            }
        }

        Collections.shuffle(blocks);

        return blocks;
    }

    public static boolean destroyBlock(ServerLevel level, BlockPos pos, boolean dropResources) {
        if(!level.isLoaded(pos)) {
            return false;
        }
        BlockState blockState = level.getBlockState(pos);
        if (blockState.isAir()) {
            return false;
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
        return true;
    }
}
