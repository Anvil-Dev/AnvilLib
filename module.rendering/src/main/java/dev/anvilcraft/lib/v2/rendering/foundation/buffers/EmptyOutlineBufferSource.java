package dev.anvilcraft.lib.v2.rendering.foundation.buffers;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;

public class EmptyOutlineBufferSource extends OutlineBufferSource {

    public static final EmptyOutlineBufferSource INSTANCE = new EmptyOutlineBufferSource();

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return EmptyVertexConsumer.INSTANCE;
    }
}
