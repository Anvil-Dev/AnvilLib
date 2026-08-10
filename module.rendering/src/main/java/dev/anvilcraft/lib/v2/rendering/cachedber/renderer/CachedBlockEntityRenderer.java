package dev.anvilcraft.lib.v2.rendering.cachedber.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Single-phase renderer interface for the cached BER pipeline.
 * <p>
 * Ported from 26.1: the original two-phase {@code extractRenderState}/{@code submit} model (driven by
 * the 26.1 {@code SubmitNodeCollector} feature system) does not exist on 1.21.1, where block entities
 * render through the single {@code BlockEntityRenderDispatcher.render(...)} stage. Renderers therefore
 * draw directly into the {@link MultiBufferSource} handed out by {@link dev.anvilcraft.lib.v2.rendering.foundation.buffers.FullyBufferedBufferSource}.
 *
 * @param <T> the block entity type
 */
public interface CachedBlockEntityRenderer<T extends BlockEntity> {
    /**
     * Draws the block entity into {@code bufferSource}. Called once per compile of the containing
     * chunk with a {@code partialTick} of 0 and a pose already translated to the block entity's
     * position relative to the chunk origin.
     */
    void render(T blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay);
}
