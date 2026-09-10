package dev.anvilcraft.lib.v2.rendering.foundation.buffers;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class EmptyBufferSource extends MultiBufferSource.BufferSource {

    public static final EmptyBufferSource INSTANCE = new EmptyBufferSource();

    protected EmptyBufferSource() {
        super(null, null);
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return EmptyVertexConsumer.INSTANCE;
    }

    @Override
    public void endBatch() {

    }

    @Override
    public void endBatch(RenderType type) {

    }

    @Override
    public void endLastBatch() {

    }
}
