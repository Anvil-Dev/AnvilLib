package dev.anvilcraft.lib.v2.rendering.cachedber.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.rendering.cachedber.renderer.CachedBlockEntityRenderDispatcher;
import dev.anvilcraft.lib.v2.rendering.cachedber.renderer.CachedBlockEntityRenderState;
import dev.anvilcraft.lib.v2.rendering.foundation.FeatureGeometryCapture;
import dev.anvilcraft.lib.v2.rendering.foundation.FeatureRendering;
import net.minecraft.client.renderer.RenderBuffers;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.FullyBufferedBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;

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
        SubmitNodeStorage submitNodeStorage = new SubmitNodeStorage();
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

            CachedBlockEntityRenderState renderState = CachedBlockEntityRenderDispatcher.INSTANCE.tryExtractRenderState(
                be,
                0,
                minecraft.gameRenderer.mainCamera()
            );

            if (renderState != null) {
                CachedBlockEntityRenderDispatcher.INSTANCE.submit(
                    renderState,
                    poseStack,
                    submitNodeStorage,
                    minecraft.gameRenderer.gameRenderState().levelRenderState.cameraRenderState
                );
            }

            poseStack.popPose();
        }

        try (RenderBuffers buffers = new RenderBuffers(1);
             FeatureRenderDispatcher dispatcher = FeatureRendering.dispatcher(buffers);
             FeatureGeometryCapture capture = new FeatureGeometryCapture(bufferSource::getBuffer);
             var frame = dispatcher.prepareFrame(submitNodeStorage)) {
            // Preparing runs every registered feature renderer into the persistent chunk buffers.
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
