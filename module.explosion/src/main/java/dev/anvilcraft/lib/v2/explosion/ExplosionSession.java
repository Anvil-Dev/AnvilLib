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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Performs the actual block removal in a spherical explosion, driven by
 * {@link ServerTickEvent.Post}. Each tick removes at most {@code maxBreakPerTick}
 * blocks from inside to outside until the explosion radius is exhausted,
 * then self-unregisters.
 *
 * <p>Uses dynamic layer-by-layer generation with virtual thread pre-computation
 * to avoid memory issues with large radii and improve performance through async processing.
 */
class ExplosionSession {
    // Shared virtual thread executor for async layer pre-computation
    private static final Executor VIRTUAL_EXECUTOR =
        Executors.newVirtualThreadPerTaskExecutor();
    
    private final ServerLevel level;
    private final BlockPos center;
    private final int maxRadius;
    private final int maxBreakPerTick;
    private final boolean dropItems;

    // Current processing state
    private int currentLayer; // Current distance layer (0 to maxRadius)
    private @Nullable List<BlockPos> currentLayerBlocks; // Blocks in current layer
    private int layerIndex; // Index within current layer
    
    // Async pre-computation for next layer
    private @Nullable CompletableFuture<List<BlockPos>> nextLayerFuture; // Future for next layer computation
    private int nextLayerToCompute; // Which layer is being pre-computed
    
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
        this.nextLayerFuture = null;
        this.nextLayerToCompute = -1;
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
                // Try to get pre-computed next layer
                if (this.nextLayerFuture != null && this.nextLayerToCompute == this.currentLayer) {
                    // Use pre-computed result from virtual thread
                    this.currentLayerBlocks = this.nextLayerFuture.join();
                    this.nextLayerFuture = null;
                    this.nextLayerToCompute = -1;
                } else {
                    // Fallback: compute synchronously if pre-computation wasn't ready
                    this.currentLayerBlocks = this.generateLayerBlocks(this.currentLayer);
                }
                
                this.layerIndex = 0;
                
                // Move to next layer if current one is empty
                if (this.currentLayerBlocks.isEmpty()) {
                    this.currentLayer++;
                    continue;
                }
                
                // Start pre-computing next layer in virtual thread
                this.startNextLayerPrecomputation();
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
     * Start pre-computing the next layer in a virtual thread.
     * This allows the expensive block enumeration to happen asynchronously
     * while we're still processing the current layer.
     */
    private void startNextLayerPrecomputation() {
        int nextLayer = this.currentLayer + 1;
        
        // Don't pre-compute beyond max radius
        if (nextLayer > this.maxRadius) {
            return;
        }
        
        // Only start if not already computing this layer
        if (this.nextLayerFuture != null && !this.nextLayerFuture.isDone()) {
            return;
        }
        
        // Start async computation using virtual thread executor
        this.nextLayerToCompute = nextLayer;
        this.nextLayerFuture = CompletableFuture.supplyAsync(
            () -> this.generateLayerBlocks(nextLayer),
            VIRTUAL_EXECUTOR
        );
    }

    /**
     * Generate all block positions at a specific distance layer from center.
     * This approach avoids storing all blocks in memory at once for large radii.
     * 
     * @param layer The distance layer (Euclidean distance shell)
     * @return List of BlockPos at this layer, shuffled for natural explosion pattern
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
        int rSquared = layer * layer;
        int innerRSquared = (layer - 1) * (layer - 1);
        
        // Iterate through bounding box of this shell
        for (int x = -layer; x <= layer; x++) {
            for (int y = -layer; y <= layer; y++) {
                for (int z = -layer; z <= layer; z++) {
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
