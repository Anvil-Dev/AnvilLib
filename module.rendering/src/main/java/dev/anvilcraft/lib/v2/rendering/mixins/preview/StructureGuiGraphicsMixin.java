package dev.anvilcraft.lib.v2.rendering.mixins.preview;

import dev.anvilcraft.lib.v2.rendering.preview.gui.StructureGuiSession;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** 将只读浮窗的过渡透明度应用于原生界面的背景、文字及控件。 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class StructureGuiGraphicsMixin {
    @ModifyVariable(method = "innerBlit(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Lcom/mojang/renderpearl/api/textures/GpuSampler;IIIIFFFFI)V",
        at = @At("HEAD"), argsOnly = true, ordinal = 4)
    private int anvillib$textureOpacity(int color) { return StructureGuiSession.tint(color); }

    @ModifyVariable(method = "innerFill", at = @At("HEAD"), argsOnly = true, ordinal = 4)
    private int anvillib$fillOpacity(int color) { return StructureGuiSession.tint(color); }

    @ModifyVariable(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
        at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private int anvillib$textOpacity(int color) { return StructureGuiSession.tint(color); }
    @ModifyVariable(method = "innerFill", at = @At("HEAD"), argsOnly = true)
    private @org.jspecify.annotations.Nullable Integer anvillib$gradientOpacity(@org.jspecify.annotations.Nullable Integer color) {
        return color == null ? null : StructureGuiSession.tint(color);
    }

    @ModifyVariable(method = "innerTiledBlit", at = @At("HEAD"), argsOnly = true, ordinal = 6)
    private int anvillib$tiledOpacity(int color) {
        return StructureGuiSession.tint(color);
    }
}
