package dev.anvilcraft.lib.v2.rendering.mixins.preview;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.anvilcraft.lib.v2.rendering.preview.gui.StructureGuiSession;
import net.minecraft.client.renderer.RenderStateShard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 不透明物品材质在浮窗淡入淡出期间也参与透明混合。 */
@Mixin(RenderStateShard.class)
public abstract class StructureGuiRenderStateMixin {
    @Inject(method = "setupRenderState", at = @At("TAIL"))
    private void anvillib$blendPreview(CallbackInfo ci) {
        if (StructureGuiSession.isRendering() && StructureGuiSession.opacity() < 1) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        }
    }
}
