package dev.anvilcraft.lib.v2.explosion;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Performs the actual block removal in a spherical explosion, driven by
 * {@link ServerTickEvent.Post}. Each tick removes at most {@code maxBreakPerTick}
 * blocks from inside to outside until the explosion radius is exhausted,
 * then self-unregisters.
 *
 * <p>Uses dynamic layer-by-layer generation with virtual thread pre-computation
 * to avoid memory issues with large radii and improve performance through async processing.
 */
@ApiStatus.Internal
class ExplosionSession {
    // Shared virtual thread executor for async layer pre-computation
    private static final Executor THREAD_EXECUTOR = Executors.newWorkStealingPool();

    private final ServerLevel level;
    private final BlockPos center;
    private final int maxRadius;
    private final int maxBreakPerTick;
    private final boolean dropItems;
    private final int probabilityRadius; // Probability destruction radius
    private final int meltingRadius; // Melting radius (replace with air without drops)
    private final int effectiveMaxRadius; // Outermost radius to process (max of probabilityRadius and meltingRadius)
    private final @Nullable List<Predicate<Block>> excludedBlocks; // Blocks that cannot be destroyed by explosion
    private final @Nullable List<Predicate<Block>> frangibleBlocks; // Frangible blocks that are always destroyed within range
    private final @Nullable TriConsumer<ServerLevel, BlockPos, Entity> entityProcessor;

    private final int surfaceHeight;

    // Current processing state
    private int currentLayer; // Current distance layer (0 to effectiveMaxRadius)
    private @Nullable List<BlockPos> currentLayerBlocks; // Blocks in current layer
    private int layerIndex; // Index within current layer

    // Async pre-computation for next layer
    private @Nullable CompletableFuture<List<BlockPos>> nextLayerFuture; // Future for next layer computation
    private final Multimap<BlockPos, Entity> entityCache = MultimapBuilder.hashKeys().arrayListValues().build();
    private int nextLayerToCompute; // Which layer is being pre-computed

    private boolean finished;

    ExplosionSession(
        ServerLevel level,
        BlockPos center,
        int maxRadius,
        int maxBreakPerTick,
        boolean dropItems,
        int probabilityRadius,
        int meltingRadius,
        @Nullable List<Predicate<Block>> excludedBlocks,
        @Nullable List<Predicate<Block>> frangibleBlocks,
        @Nullable TriConsumer<ServerLevel, BlockPos, Entity> entityProcessor
    ) {
        this.level = level;
        this.center = center;
        this.maxRadius = maxRadius;
        this.maxBreakPerTick = maxBreakPerTick;
        this.dropItems = dropItems;
        this.probabilityRadius = probabilityRadius;
        this.meltingRadius = meltingRadius;
        this.effectiveMaxRadius = Math.max(probabilityRadius, meltingRadius);
        this.excludedBlocks = excludedBlocks;
        this.frangibleBlocks = frangibleBlocks;
        this.entityProcessor = entityProcessor;
        this.surfaceHeight = (int) (Math.ceil(maxRadius * 0.1) + 1);
    }

    // ---- lifecycle ----

    void start() {
        this.currentLayer = 0;
        this.currentLayerBlocks = null;
        this.layerIndex = 0;
        this.nextLayerFuture = null;
        this.nextLayerToCompute = -1;
        this.finished = false;
        this.entityCache.clear();
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

        this.createEntityCache();
        // Process blocks layer by layer to avoid memory issues with large radii
        while (removed < this.maxBreakPerTick && this.currentLayer <= this.effectiveMaxRadius) {
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

                if (this.entityProcessor != null) {
                    this.entityCache.get(target).forEach(entity -> this.entityProcessor.accept(this.level, target, entity));
                }
                BlockState blockState = this.level.getBlockState(target);
                if (blockState.isAir()) continue;
                // public int getHeight(Heightmap.Types type, int x, int z) {}
                int height = level.getHeight(Heightmap.Types.WORLD_SURFACE, target.getX(), target.getZ());
                boolean isSurface = target.getY() >= height - this.surfaceHeight;
                // Check if block is excluded from explosion
                if (this.isBlockExcluded(blockState.getBlock())) {
                    continue;
                }

                // Frangible blocks are always completely destroyed within range
                if (this.isBlockFrangible(blockState.getBlock())) {
                    if (ExplosionSession.destroyBlock(this.level, target, this.dropItems)) {
                        removed++;
                    }
                    continue;
                }

                double distance = Math.sqrt(target.distToCenterSqr(this.center.getX(), this.center.getY(), this.center.getZ()));

                // Determine action based on distance
                if (distance <= this.maxRadius) {
                    // Core explosion: always destroy
                    if (ExplosionSession.destroyBlock(this.level, target, this.dropItems)) {
                        removed++;
                    }
                } else {
                    if (distance <= this.probabilityRadius && isSurface) {
                        // Probability destruction: probability decreases from 80% at maxRadius to 0% at probabilityRadius
                        double probability = this.calculateProbability(distance, this.maxRadius, this.probabilityRadius);
                        if (Math.random() < probability) {
                            if (ExplosionSession.destroyBlock(this.level, target, this.dropItems)) {
                                removed++;
                                blockState = Blocks.AIR.defaultBlockState();
                            }
                        }
                    }
                    if (distance <= this.meltingRadius && isSurface && !blockState.isAir()) {
                        // Melting: probability decreases from 80% at probabilityRadius to 0% at meltingRadius
                        double probability = this.calculateProbability(distance, this.probabilityRadius, this.meltingRadius);
                        if (Math.random() < probability) {
                            if (ExplosionSession.meltBlock(this.level, target)) {
                                removed++;
                            }
                        }
                    }
                }
            }

            // Move to next layer when current one is done
            if (this.layerIndex >= this.currentLayerBlocks.size()) {
                this.currentLayer++;
                this.currentLayerBlocks = null; // Allow GC to collect
            }
        }

        if (this.currentLayer > this.effectiveMaxRadius) {
            this.finished = true;
            this.stop();
        }
    }

    // ---- Block enumeration ----

    /**
     * Start pre-computing the next layer in a virtual thread.
     * For large radii, splits the computation into multiple parallel sub-tasks.
     */
    private void startNextLayerPrecomputation() {
        int nextLayer = this.currentLayer + 1;

        // Don't pre-compute beyond effective max radius
        if (nextLayer > this.effectiveMaxRadius) {
            return;
        }

        // Only start if not already computing this layer
        if (this.nextLayerFuture != null && !this.nextLayerFuture.isDone()) {
            return;
        }

        // Use parallel computation for large layers to improve performance
        boolean useParallel = nextLayer >= 32; // Threshold for parallel processing

        this.nextLayerToCompute = nextLayer;
        this.nextLayerFuture = CompletableFuture.supplyAsync(() -> this.generateLayerBlocks(nextLayer, useParallel), THREAD_EXECUTOR);
    }

    /**
     * Generate all block positions at a specific distance layer from center.
     * This approach avoids storing all blocks in memory at once for large radii.
     *
     * @param layer The distance layer (Euclidean distance shell)
     * @return List of BlockPos at this layer, shuffled for natural explosion pattern
     */
    private List<BlockPos> generateLayerBlocks(int layer) {
        return this.generateLayerBlocks(layer, false);
    }

    /**
     * Generate all block positions at a specific distance layer from center.
     * Supports parallel computation for large radii.
     *
     * @param layer       The distance layer (Euclidean distance shell)
     * @param useParallel Whether to use parallel computation for this layer
     * @return List of BlockPos at this layer, shuffled for natural explosion pattern
     */
    private List<BlockPos> generateLayerBlocks(int layer, boolean useParallel) {
        if (layer == 0) {
            // Center point
            return List.of(this.center);
        }

        // For each layer, we process a "shell" at approximately that distance
        // We use integer bounds to efficiently find blocks in the shell
        int rSquared = layer * layer;
        int innerRSquared = (layer - 1) * (layer - 1);

        List<BlockPos> blocks;

        if (useParallel && layer >= 96) {
            // Parallel computation: split by X-axis slices
            blocks = this.generateLayerBlocksParallel(layer, rSquared, innerRSquared);
        } else {
            // Sequential computation for smaller layers
            blocks = this.generateLayerBlocksSequential(layer, rSquared, innerRSquared);
        }

        Collections.shuffle(blocks);
        return blocks;
    }

    /**
     * Sequential generation of layer blocks (for small radii).
     */
    private List<BlockPos> generateLayerBlocksSequential(int layer, int rSquared, int innerRSquared) {
        List<BlockPos> blocks = new ArrayList<>();

        // Iterate through bounding box of this shell
        for (int x = -layer; x <= layer; x++) {
            for (int y = -layer; y <= layer; y++) {
                for (int z = -layer; z <= layer; z++) {
                    int distSquared = x * x + y * y + z * z;

                    // Only include blocks in this shell layer
                    if (distSquared > innerRSquared && distSquared <= rSquared) {
                        blocks.add(new BlockPos(this.center.getX() + x, this.center.getY() + y, this.center.getZ() + z));
                    }
                }
            }
        }

        return blocks;
    }

    /**
     * Parallel generation of layer blocks (for large radii).
     * Splits the work along the X-axis and merges results.
     */
    private List<BlockPos> generateLayerBlocksParallel(int layer, int rSquared, int innerRSquared) {
        // Determine number of parallel tasks based on layer size
        int numTasks = Math.min(Runtime.getRuntime().availableProcessors(), layer / 8 + 1);
        int sliceSize = (layer * 2 + 1) / numTasks;

        // Create parallel tasks for each X-slice
        List<CompletableFuture<List<BlockPos>>> futures = new ArrayList<>(numTasks);

        for (int taskIdx = 0; taskIdx < numTasks; taskIdx++) {
            final int startX = -layer + taskIdx * sliceSize;
            final int endX = (taskIdx == numTasks - 1) ? layer : (startX + sliceSize - 1);

            CompletableFuture<List<BlockPos>> future = CompletableFuture.supplyAsync(
                () -> {
                    List<BlockPos> sliceBlocks = new ArrayList<>();

                    for (int x = startX; x <= endX; x++) {
                        for (int y = -layer; y <= layer; y++) {
                            for (int z = -layer; z <= layer; z++) {
                                int distSquared = x * x + y * y + z * z;

                                // Only include blocks in this shell layer
                                if (distSquared > innerRSquared && distSquared <= rSquared) {
                                    sliceBlocks.add(new BlockPos(this.center.getX() + x, this.center.getY() + y, this.center.getZ() + z));
                                }
                            }
                        }
                    }

                    return sliceBlocks;
                }, THREAD_EXECUTOR
            );

            futures.add(future);
        }

        // Wait for all tasks to complete and merge results
        return futures.stream().flatMap(f -> f.join().stream()).collect(Collectors.toList());
    }

    /**
     * Check if a block should be excluded from explosion.
     *
     * @param block The block to check
     * @return true if the block should not be destroyed/melted
     */
    private boolean isBlockExcluded(Block block) {
        if (this.excludedBlocks == null) {
            return false;
        }
        for (Predicate<Block> predicate : this.excludedBlocks) {
            if (predicate.test(block)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a block is frangible (always destroyed within range).
     *
     * @param block The block to check
     * @return true if the block is frangible
     */
    private boolean isBlockFrangible(Block block) {
        if (this.frangibleBlocks == null) {
            return false;
        }
        for (Predicate<Block> predicate : this.frangibleBlocks) {
            if (predicate.test(block)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculate probability based on distance.
     * Probability decreases linearly from 80% at innerRadius to 0% at outerRadius.
     *
     * @param distance    Distance from explosion center
     * @param innerRadius Inner boundary of the zone (80% probability)
     * @param outerRadius Outer boundary of the zone (0% probability)
     * @return Probability (0.0 to 0.8)
     */
    private double calculateProbability(double distance, int innerRadius, int outerRadius) {
        if (outerRadius <= innerRadius) return 0.0;
        if (distance <= innerRadius) {
            return 0.8; // 80% at inner boundary
        }
        if (distance >= outerRadius) {
            return 0.0; // 0% at outer boundary
        }

        // Linear interpolation: 80% at innerRadius, 0% at outerRadius
        double ratio = (distance - innerRadius) / (double) (outerRadius - innerRadius);
        return 0.8 * (1.0 - ratio); // Decreases from 0.8 to 0.0
    }

    /**
     * Melt a block by replacing it with another block from MELTING_CACHE.
     * Used for the melting radius effect.
     */
    public static boolean meltBlock(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockState blockState = level.getBlockState(pos);
        if (blockState.isAir()) {
            return false;
        }

        Block originalBlock = blockState.getBlock();
        Block targetBlock = AnvilLibExplosion.MELTING_CACHE.get(originalBlock);

        BlockState newState;
        if (targetBlock == null) {
            return false;
        }
        newState = targetBlock.defaultBlockState();

        boolean changed = level.setBlock(pos, newState, Block.UPDATE_CLIENTS, 512);
        if (changed) {
            level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(null, blockState));
        }
        return changed;
    }

    public static boolean destroyBlock(ServerLevel level, BlockPos pos, boolean dropResources) {
        if (!level.isLoaded(pos)) {
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
        boolean destroyed = level.setBlock(pos, fluidState.createLegacyBlock(), Block.UPDATE_CLIENTS, 512);
        if (destroyed) {
            level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(null, blockState));
        }
        return destroyed;
    }

    private void createEntityCache() {
        this.entityCache.clear();
        if (this.entityProcessor == null) {
            return;
        }
        this.level.getEntities().getAll().forEach(entity -> {
            if (entity.blockPosition().distSqr(this.center) > this.maxRadius * this.maxRadius) {
                return;
            }
            this.entityCache.put(entity.blockPosition(), entity);
        });
    }
}
