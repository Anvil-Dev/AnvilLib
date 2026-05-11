package dev.anvilcraft.lib.v2.rendering.cachedber.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.rendering.cachedber.renderer.CachedBlockEntityRenderDispatcher;
import dev.anvilcraft.lib.v2.rendering.cachedber.renderer.CachedBlockEntityRenderState;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.EmptyBufferSource;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.EmptyOutlineBufferSource;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.FullyBufferedBufferSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;

class RebuildTask implements Runnable {
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
                minecraft.gameRenderer.getMainCamera()
            );

            if (renderState != null) {
                CachedBlockEntityRenderDispatcher.INSTANCE.submit(
                    renderState,
                    poseStack,
                    submitNodeStorage,
                    minecraft.gameRenderer.getGameRenderState().levelRenderState.cameraRenderState
                );
            }

            poseStack.popPose();
        }

        FeatureRenderDispatcher dispatcher = new FeatureRenderDispatcher(
            submitNodeStorage,
            minecraft.getModelManager(),
            bufferSource,
            minecraft.getAtlasManager(),
            EmptyOutlineBufferSource.INSTANCE,
            EmptyBufferSource.INSTANCE,
            minecraft.font,
            minecraft.gameRenderer.getGameRenderState()
        );
        dispatcher.renderAllFeatures();
        dispatcher.endFrame();

        owner.setEmpty(bufferSource.isEmpty());
        bufferSource.upload(owner);
        owner.replaceMeshData(bufferSource.getMeshSorts(), bufferSource.getIndexCountMap());
        owner.setLastRebuildTask(null);
    }

    void cancel() {
        cancelled = true;
    }
}
