package dev.anvilcraft.lib.v2.rendering.mixins.preview;

import com.llamalad7.mixinextras.sugar.Local;
import dev.anvilcraft.lib.v2.rendering.preview.gui.StructureGuiItemOpacity;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(GuiRenderer.class)
public abstract class StructureGuiItemRendererMixin {
    @ModifyArgs(method = "submitBlitFromItemAtlas", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/render/state/BlitRenderState;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2f;IIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;Lnet/minecraft/client/gui/navigation/ScreenRectangle;)V"))
    private void anvillib$fadeItem(Args args, @Local(argsOnly = true) GuiItemRenderState item) {
        args.set(11, StructureGuiItemOpacity.premultipliedColor(((StructureGuiItemOpacity) (Object) item).anvillib$opacity()));
    }
}
