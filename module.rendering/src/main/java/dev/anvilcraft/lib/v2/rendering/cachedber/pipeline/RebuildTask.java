package dev.anvilcraft.lib.v2.rendering.cachedber.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.rendering.cachedber.renderer.CachedBlockEntityRenderDispatcher;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.FullyBufferedBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;

/**
 * Compiles all block entities of a {@link CachedRenderingChunk} into a {@link FullyBufferedBufferSource}
 * and uploads the resulting meshes to the chunk's {@link com.mojang.blaze3d.vertex.VertexBuffer}s.
 * <p>
 * Ported from 26.1: the 26.1 feature pipeline ({@code SubmitNodeStorage} + {@code FeatureRenderDispatcher})
 * is replaced by a direct dispatch through {@link CachedBlockEntityRenderDispatcher} into the
 * fully-buffered source, matching the single-stage 1.21.1 BER model.
 */
@ApiStatus.Internal
public class RebuildTask implements Runnable {
    private final CachedRenderingChunk owner;
    private boolean cancelled = false;

    public RebuildTask(CachedRenderingChunk owner) {
        this.owner = owner;
    }

    @Override
    public void run() {
        owner.setLastRebuildTask(this);
        PoseStack poseStack = new PoseStack();
        owner.setEmpty(true);
        FullyBufferedBufferSource bufferSource = new FullyBufferedBufferSource();
        Minecraft minecraft = Minecraft.getInstance();
        for (BlockEntity be : new ArrayList<>(owner.getBlockEntities())) {
            if (cancelled) {
                bufferSource.close();
                return;
            }

            poseStack.pushPose();
            BlockPos pos = be.getBlockPos();
            ChunkPos chunkPos = owner.getChunkPos();
            poseStack.translate(
                pos.getX() - chunkPos.getMinBlockX(),
                pos.getY(),
                pos.getZ() - chunkPos.getMinBlockZ()
            );

            CachedBlockEntityRenderDispatcher.INSTANCE.render(
                be,
                0,
                poseStack,
                bufferSource
            );

            poseStack.popPose();
        }

        owner.setEmpty(bufferSource.isEmpty());
        bufferSource.upload(owner);
        owner.replaceMeshData(bufferSource.getMeshSorts(), bufferSource.getIndexCountMap());
        owner.setLastRebuildTask(null);
    }

    void cancel() {
        cancelled = true;
    }
}
