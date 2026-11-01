package dev.anvilcraft.lib.v2.rendering.foundation.buffers;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class EmptyBufferSource {

    public static final EmptyBufferSource INSTANCE = new EmptyBufferSource();

    protected EmptyBufferSource() {
    }

    public VertexConsumer getBuffer(RenderType renderType) {
        return EmptyVertexConsumer.INSTANCE;
    }

    public void endBatch() {

    }

    public void endBatch(RenderType type) {

    }

    public void endLastBatch() {

    }
}
