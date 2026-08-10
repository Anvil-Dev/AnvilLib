package dev.anvilcraft.lib.v2.rendering.mixins;

import dev.anvilcraft.lib.v2.rendering.extension.ALRRenderTypeExtension;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RenderType.class)
@ApiStatus.Internal
public class RenderTypeMixin implements ALRRenderTypeExtension {

    @Unique
    private boolean anvillib_rendering$bloomRendering = false;

    public boolean anvillib_rendering$bloomRendering() {
        return anvillib_rendering$bloomRendering;
    }

    public void anvillib_rendering$setBloomRendering(boolean value) {
        this.anvillib_rendering$bloomRendering = value;
    }
}
