package dev.anvilcraft.lib.v2.rendering.bloom;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

/**
 * Callback that re-renders part of the scene into the bloom input target.
 * <p>
 * Ported from 26.1: the original received a {@code SubmitNodeCollector} and relied on
 * {@code FeatureRenderDispatcher} to flush the re-rendered features; neither exists on 1.21.1,
 * so the callback now receives a {@link MultiBufferSource.BufferSource} and is expected to submit
 * geometry (typically with {@code ALRRenderTypeExtension.copyWithBloom} marked render types) which
 * {@code BloomPostEffect} flushes while the bloom input target is bound.
 */
public interface BloomRenderCallback {
    void render(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack);
}
