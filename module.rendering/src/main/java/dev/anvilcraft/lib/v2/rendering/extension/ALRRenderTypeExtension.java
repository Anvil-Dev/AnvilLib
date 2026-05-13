package dev.anvilcraft.lib.v2.rendering.extension;

import net.minecraft.client.renderer.rendertype.RenderType;

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

    static RenderType copyWithBloom(RenderType that) {
        RenderType newValue = RenderType.create(that.name, that.state);
        setRenderingBloomed(newValue, true);
        return newValue;
    }
}
