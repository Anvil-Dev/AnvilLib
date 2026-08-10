package dev.anvilcraft.lib.v2.rendering.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.rendering.internal.ItemStackRenderStateInternals;
import dev.anvilcraft.lib.v2.rendering.sdf.SdfGraphics;
import dev.anvilcraft.lib.v2.rendering.state.LibGuiElementRenderState;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(GuiRenderer.class)
@ApiStatus.Internal
public class GuiRendererMixin {
    @Unique
    private GuiElementRenderState anvillib$renderState = null;
    @Unique
    private final Map<GuiRenderer.MeshToDraw, GuiElementRenderState> anvillib$renderStatesByMeshToDraw = new HashMap<>();
    @Unique
    private final Map<GuiRenderer.Draw, GuiElementRenderState> anvillib$renderStatesByDraw = new HashMap<>();

    @Inject(
        method = "addElementsToMeshes",
        at = @At(
            value = "HEAD"
        )
    )
    private void addElementsToMeshes(GuiRenderState.TraverseRange range, CallbackInfo ci) {
        this.anvillib$renderState = null;

        SdfGraphics.flush();
    }

    @Inject(
        method = "addElementToMesh",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;getBufferBuilder(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)Lcom/mojang/blaze3d/vertex/BufferBuilder;"
        )
    )
    private void addElementToMesh(GuiElementRenderState elementState, CallbackInfo ci) {
        this.anvillib$renderState = elementState;
    }


    @WrapOperation(
        method = "recordMesh",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z"
        )
    )
    private <E> boolean recordMesh(List<E> instance, E e, Operation<Boolean> original) {
        this.anvillib$renderStatesByMeshToDraw.put((GuiRenderer.MeshToDraw) e, this.anvillib$renderState);
        return original.call(instance, e);
    }

    @WrapOperation(
        method = "recordDraws",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z"
        )
    )
    private <E> boolean recordDraws(
        List<E> instance,
        E e,
        Operation<Boolean> original,
        @Local(name = "meshToDraw") GuiRenderer.MeshToDraw meshToDraw
    ) {
        this.anvillib$renderStatesByMeshToDraw.computeIfPresent(
            meshToDraw,
            (_, v) -> this.anvillib$renderStatesByDraw.put((GuiRenderer.Draw) e, v)
        );
        return original.call(instance, e);
    }

    @Inject(
        method = "executeDraw",
        at = @At(
            value = "HEAD"
        )
    )
    private void executeDraw(
        GuiRenderer.Draw draw,
        RenderPass renderPass,
        GpuBuffer indexBuffer,
        VertexFormat.IndexType indexType,
        CallbackInfo ci
    ) {
        this.anvillib$renderStatesByDraw.computeIfPresent(
            draw, (_, v) -> {
                if (v instanceof LibGuiElementRenderState state) {
                    state.executeDrawBeforeSetPipeline(renderPass);
                }
                return v;
            }
        );
    }

    @Inject(
        method = "executeDraw",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline;getVertexFormatMode()Lcom/mojang/blaze3d/vertex/VertexFormat$Mode;"
        )
    )
    private void executeDrawAfterSetPipline(
        GuiRenderer.Draw draw,
        RenderPass renderPass,
        GpuBuffer indexBuffer,
        VertexFormat.IndexType indexType,
        CallbackInfo ci
    ) {
        this.anvillib$renderStatesByDraw.computeIfPresent(
            draw, (_, v) -> {
                if (v instanceof LibGuiElementRenderState state) {
                    state.executeDrawAfterSetPipeline(renderPass);
                }
                return v;
            }
        );
    }

    @ModifyArg(
        method = "submitBlitFromItemAtlas",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/state/gui/BlitRenderState;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2f;IIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;Lnet/minecraft/client/gui/navigation/ScreenRectangle;)V"
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
            target = "Lnet/minecraft/client/renderer/state/gui/BlitRenderState;<init>(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;Lorg/joml/Matrix3x2f;IIIIFFFFILnet/minecraft/client/gui/navigation/ScreenRectangle;Lnet/minecraft/client/gui/navigation/ScreenRectangle;)V"
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

    @Inject(
        method = "render",
        at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V", ordinal = 0)
    )
    public void render(GpuBufferSlice fogBuffer, CallbackInfo ci) {
        this.anvillib$renderStatesByDraw.clear();
        this.anvillib$renderStatesByMeshToDraw.clear();
    }
}
