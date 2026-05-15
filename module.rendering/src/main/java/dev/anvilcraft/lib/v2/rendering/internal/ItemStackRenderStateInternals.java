package dev.anvilcraft.lib.v2.rendering.internal;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NullUnmarked;

@ApiStatus.Internal
@NullUnmarked
public class ItemStackRenderStateInternals {

    public static Extension of(ItemStackRenderState thiz) {
        return (Extension) thiz;
    }

    public static void setAlpha(ItemStackRenderState thiz, float value) {
        of(thiz).anvillib_rendering$setAlpha(value);
    }

    public static float getAlpha(ItemStackRenderState thiz) {
        return of(thiz).anvillib_rendering$getAlpha();
    }

    public static boolean isTransparencyEnforced(ItemStackRenderState thiz) {
        return of(thiz).anvillib_rendering$isTransparencyEnforced();
    }

    @ApiStatus.Internal
    public interface Extension {
        void anvillib_rendering$setAlpha(float value);

        float anvillib_rendering$getAlpha();

        boolean anvillib_rendering$isTransparencyEnforced();
    }
}
