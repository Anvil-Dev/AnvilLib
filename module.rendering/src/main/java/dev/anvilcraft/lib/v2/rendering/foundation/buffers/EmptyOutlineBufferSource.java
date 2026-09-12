package dev.anvilcraft.lib.v2.rendering.foundation.buffers;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderType;

public class EmptyOutlineBufferSource extends OutlineBufferSource {

    public static final EmptyOutlineBufferSource INSTANCE = new EmptyOutlineBufferSource();

    private EmptyOutlineBufferSource() { super(EmptyBufferSource.INSTANCE); }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return EmptyVertexConsumer.INSTANCE;
    }
}
