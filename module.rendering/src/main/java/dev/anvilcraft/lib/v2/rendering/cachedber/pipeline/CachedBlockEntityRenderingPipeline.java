package dev.anvilcraft.lib.v2.rendering.cachedber.pipeline;

import dev.anvilcraft.lib.v2.rendering.cachedber.renderer.CachedBlockEntityRenderDispatcher;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

/**
 * @author ZhuRuoLing
 */
@SuppressWarnings("unused")
public class CachedBlockEntityRenderingPipeline {
    @Nullable
    private static CachedBlockEntityRenderingPipeline instance;
    @Getter
    private final ClientLevel level;
    private final Queue<Runnable> pendingCompiles = new ArrayDeque<>();
    private final Queue<Runnable> pendingUploads = new ArrayDeque<>();
    private final Map<ChunkPos, CachedRegion> regions = new HashMap<>();
    @Getter
    private boolean valid = true;

    public CachedRegion getRenderRegion(ChunkPos chunkPos) {
        if (regions.containsKey(chunkPos)) {
            return regions.get(chunkPos);
        }
        CachedRegion region = new CachedRegion(chunkPos, this);
        regions.put(chunkPos, region);
        return region;
    }

    public CachedBlockEntityRenderingPipeline(ClientLevel level) {
        this.level = level;
    }

    public void runTasks() {
        while (!pendingCompiles.isEmpty() && valid) {
            pendingCompiles.poll().run();
        }
        while (!pendingUploads.isEmpty() && valid) {
            pendingUploads.poll().run();
        }
    }

    /**
     * Updates the rendering pipeline instance with a new level context.
     *
     * @param level The new ClientLevel instance that the rendering pipeline should be updated to use.
     */
    public static void updateLevel(@Nullable ClientLevel level) {
        if (instance != null) {
            instance.releaseBuffers();
        }
        if (level == null) {
            instance = null;
            return;
        }
        instance = new CachedBlockEntityRenderingPipeline(level);
    }

    /**
     * Notifies the pipeline that a {@link BlockEntity} has been removed.
     * This method will be automatically called when a {@link BlockEntity} has been removed.
     *
     * @param be The removed {@link BlockEntity}
     */
    public void blockRemoved(BlockEntity be) {
        IBlockEntityRendererExtension<?> renderer = Minecraft.getInstance()
            .getBlockEntityRenderDispatcher()
            .getRenderer(be);
        if (renderer == null) return;
        ChunkPos chunkPos = ChunkPos.containing(be.getBlockPos());
        getRenderRegion(chunkPos).blockRemoved(be);
    }

    public void update(BlockEntity be) {
        update(be, false);
    }

    /**
     * Notifies the pipeline that a {@link BlockEntity} has been updated and the cache should be rebuilt.
     *
     * @param be The updated {@link BlockEntity}
     */
    public void update(BlockEntity be, boolean forced) {
        if (!CachedBlockEntityRenderDispatcher.INSTANCE.hasRenderer(be)) return;
        ChunkPos chunkPos = ChunkPos.containing(be.getBlockPos());
        getRenderRegion(chunkPos).update(be, forced);
    }

    public void submitUploadTask(Runnable task) {
        pendingUploads.add(task);
    }

    public void submitCompileTask(Runnable task) {
        pendingCompiles.add(task);
    }

    /**
     * Releases all buffers in use and mark current pipeline instance as invalid.
     */
    public void releaseBuffers() {
        regions.values().forEach(CachedRegion::releaseBuffers);
        valid = false;
    }

    public void render() {
        regions.values().forEach(CachedRegion::render);
    }

    /**
     * Retrieves the current instance of the CacheableBERenderingPipeline.
     *
     * @return The current instance of the CacheableBERenderingPipeline,
     * or null if there has no {@link ClientLevel} in current {@link Minecraft} client.
     */
    @Nullable
    public static CachedBlockEntityRenderingPipeline getInstance() {
        return instance;
    }


    public void forcedUpdate(BlockPos pos) {
        getRenderRegion(ChunkPos.containing(pos)).forcedUpdate();
    }
}