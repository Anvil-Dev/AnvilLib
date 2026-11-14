package dev.anvilcraft.lib.v2.rendering.mixins.preview;

import com.llamalad7.mixinextras.sugar.Local;
import dev.anvilcraft.lib.v2.rendering.preview.gui.StructureGuiItemOpacity;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.pip.OversizedItemRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(PictureInPictureRenderer.class)
public abstract class StructureGuiOversizedItemMixin {
    @ModifyArgs(method = "blitTexture", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/state/gui/BlitRenderState;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2f;IIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;Lnet/minecraft/client/gui/navigation/ScreenRectangle;)V"))
    private void anvillib$fadeOversizedItem(Args args, @Local(argsOnly = true) PictureInPictureRenderState state) {
        if (state instanceof OversizedItemRenderState item) {
            args.set(11, StructureGuiItemOpacity.premultipliedColor(
                ((StructureGuiItemOpacity) (Object) item.guiItemRenderState()).anvillib$opacity()));
        }
    }
}
