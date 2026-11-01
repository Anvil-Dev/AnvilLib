package dev.anvilcraft.lib.v2.rendering.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.commands.RenderPass;
import dev.anvilcraft.lib.v2.rendering.internal.ItemStackRenderStateInternals;
import dev.anvilcraft.lib.v2.rendering.sdf.SdfGraphics;
import dev.anvilcraft.lib.v2.rendering.state.LibGuiElementRenderState;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import net.minecraft.client.renderer.StagedVertexBuffer;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Shadow private StagedVertexBuffer.Draw previousDraw;
    @Shadow @Final private List<GuiRenderer.Draw> draws;
    @Unique private GuiElementRenderState anvillib$previousState;
    @Unique private final Map<GuiRenderer.Draw, LibGuiElementRenderState> anvillib$states = new HashMap<>();

    @Inject(method = "addElementsToMeshes", at = @At("HEAD"))
    private void anvillib$flushSdf(GuiRenderState.TraverseRange range, CallbackInfo ci) {
        SdfGraphics.flush();
    }

    @Inject(method = "addElementToMesh", at = @At("HEAD"))
    private void anvillib$splitCustomState(GuiElementRenderState state, CallbackInfo ci) {
        if (state != this.anvillib$previousState &&
            (state instanceof LibGuiElementRenderState || this.anvillib$previousState instanceof LibGuiElementRenderState)) {
            this.previousDraw = null;
        }
        this.anvillib$previousState = state;
    }

    @Inject(method = "addElementToMesh", at = @At("TAIL"))
    private void anvillib$rememberCustomState(GuiElementRenderState state, CallbackInfo ci) {
        if (state instanceof LibGuiElementRenderState custom) this.anvillib$states.put(this.draws.getLast(), custom);
    }

    @Inject(method = "executeDraw", at = @At("HEAD"))
    private void anvillib$beforeDraw(GuiRenderer.Draw draw, RenderPass pass, CallbackInfo ci) {
        var state = this.anvillib$states.get(draw);
        if (state != null) state.executeDrawBeforeSetPipeline(pass);
    }

    @Inject(method = "executeDraw", at = @At(value = "INVOKE",
        target = "Lcom/mojang/renderpearl/api/commands/RenderPass;setPipeline(Lcom/mojang/renderpearl/api/pipeline/CompiledRenderPipeline;)V", shift = At.Shift.AFTER))
    private void anvillib$afterPipeline(GuiRenderer.Draw draw, RenderPass pass, CallbackInfo ci) {
        var state = this.anvillib$states.get(draw);
        if (state != null) state.executeDrawAfterSetPipeline(pass);
    }

    @ModifyArg(
        method = "submitBlitFromItemAtlas",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/state/gui/BlitRenderState;<init>(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2fc;IIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;Lnet/minecraft/client/gui/navigation/ScreenRectangle;)V"
        ),
        index = 0
    )
    public RenderPipeline modifyPipeline(
        RenderPipeline pipeline,
        @Local(argsOnly = true, index = 1) GuiItemRenderState itemState
    ) {
        boolean transparencyEnforced = ItemStackRenderStateInternals.isTransparencyEnforced(itemState.itemStackRenderState());
        if (transparencyEnforced) {
            return RenderPipelines.GUI_TEXTURED;
        }
        return pipeline;
    }

    @ModifyArg(
        method = "submitBlitFromItemAtlas",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/state/gui/BlitRenderState;<init>(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2fc;IIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;Lnet/minecraft/client/gui/navigation/ScreenRectangle;)V"
        ),
        index = 11
    )
    public int modifyAlpha(
        int color,
        @Local(argsOnly = true, index = 1) GuiItemRenderState itemState
    ) {
        boolean transparencyEnforced = ItemStackRenderStateInternals.isTransparencyEnforced(itemState.itemStackRenderState());
        if (transparencyEnforced) {
            float newAlpha = ItemStackRenderStateInternals.getAlpha(itemState.itemStackRenderState());
            return ARGB.color(newAlpha, color);
        }
        return color;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void anvillib$clearStates(CallbackInfo ci) {
        this.anvillib$states.clear();
        this.anvillib$previousState = null;
    }
}
