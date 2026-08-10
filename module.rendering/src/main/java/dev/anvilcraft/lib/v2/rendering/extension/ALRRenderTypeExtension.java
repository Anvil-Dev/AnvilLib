package dev.anvilcraft.lib.v2.rendering.extension;

import net.minecraft.client.renderer.RenderType;

public interface ALRRenderTypeExtension {
    boolean anvillib_rendering$bloomRendering();

    void anvillib_rendering$setBloomRendering(boolean value);

    static ALRRenderTypeExtension of(RenderType thiz) {
        return ((ALRRenderTypeExtension) thiz);
    }

    static void setRenderingBloomed(RenderType thiz, boolean value) {
        of(thiz).anvillib_rendering$setBloomRendering(value);
    }

    static boolean isRenderingBloomed(RenderType thiz) {
        return of(thiz).anvillib_rendering$bloomRendering();
    }

    /**
     * Creates a structurally identical copy of {@code that} marked as bloom-rendered.
     * <p>
     * Ported from 26.1: {@code RenderType.create(that.name, that.state)} (26.1 took a {@code RenderState})
     * becomes {@code RenderType.create(name, format, mode, bufferSize, state)} on 1.21.1; the
     * {@code CompositeState} is exposed via the access transformer (see META-INF/accesstransformer.cfg).
     */
    static RenderType copyWithBloom(RenderType that) {
        if (that instanceof RenderType.CompositeRenderType composite) {
            RenderType newValue = RenderType.create(that.name, that.format(), that.mode(), that.bufferSize(), composite.state());
            setRenderingBloomed(newValue, true);
            return newValue;
        }
        return that;
    }
}
