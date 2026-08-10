package dev.anvilcraft.lib.v2.rendering.mixins;

import dev.anvilcraft.lib.v2.rendering.internal.ItemStackRenderStateInternals;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemStackRenderState.class)
@ApiStatus.Internal
public class ItemStackRenderStateMixin implements ItemStackRenderStateInternals.Extension {
    @Unique
    private float anvillib_rendering$alpha = 1f;
    @Unique
    private boolean anvillib_rendering$transparencyEnforced = false;

    @Override
    public void anvillib_rendering$setAlpha(float value) {
        this.anvillib_rendering$alpha = value;
        this.anvillib_rendering$transparencyEnforced = true;
    }

    @Override
    public float anvillib_rendering$getAlpha() {
        return this.anvillib_rendering$alpha;
    }

    @Override
    public boolean anvillib_rendering$isTransparencyEnforced() {
        return anvillib_rendering$transparencyEnforced;
    }
}
