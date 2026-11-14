package dev.anvilcraft.lib.v2.rendering.mixins.preview;

import dev.anvilcraft.lib.v2.rendering.preview.gui.StructureGuiItemOpacity;
import dev.anvilcraft.lib.v2.rendering.preview.gui.StructureGuiSession;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiItemRenderState.class)
public abstract class StructureGuiItemStateMixin implements StructureGuiItemOpacity {
    @Unique private float anvillib$opacity = 1;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void anvillib$captureOpacity(CallbackInfo ci) {
        this.anvillib$opacity = StructureGuiSession.opacity();
    }

    @Override
    public float anvillib$opacity() {
        return this.anvillib$opacity;
    }
}
