package dev.anvilcraft.lib.v2.rendering.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.rendering.state.LibGuiElementRenderState;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(GuiRenderer.class)
public class GuiRendererMixin {
    @Unique
    private GuiElementRenderState anvillib$renderState = null;
    @Unique
    private Map<GuiRenderer.MeshToDraw, GuiElementRenderState> anvillib$renderStatesByMeshToDraw = new HashMap<>();
    @Unique
    private Map<GuiRenderer.Draw, GuiElementRenderState> anvillib$renderStatesByDraw = new HashMap<>();

    @Inject(
        method = "addElementsToMeshes",
        at = @At(
            value = "HEAD"
        )
    )
    private void addElementsToMeshes(GuiRenderState.TraverseRange traverseRange, CallbackInfo ci) {
        this.anvillib$renderState = null;
    }

    @Inject(
        method = "addElementToMesh",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;getBufferBuilder(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)Lcom/mojang/blaze3d/vertex/BufferBuilder;"
        )
    )
    private void addElementToMesh(GuiElementRenderState renderState, CallbackInfo ci) {
        this.anvillib$renderState = renderState;
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
    private <E> boolean recordDraws(List<E> instance, E e, Operation<Boolean> original, @Local GuiRenderer.MeshToDraw meshToDraw) {
        this.anvillib$renderStatesByMeshToDraw.computeIfPresent(
            meshToDraw,
            (k, v) -> this.anvillib$renderStatesByDraw.put((GuiRenderer.Draw) e, v)
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
        GpuBuffer buffer,
        VertexFormat.IndexType indexType,
        CallbackInfo ci
    ) {
        this.anvillib$renderStatesByDraw.computeIfPresent(
            draw, (k, v) -> {
                if (v instanceof LibGuiElementRenderState state) {
                    state.bufferSlices().forEach(renderPass::setUniform);
                }
                return v;
            }
        );
    }

    @Inject(
        method = "render",
        at = @At(value = "INVOKE", target = "Ljava/util/List;clear()V", ordinal = 0)
    )
    public void render(GpuBufferSlice bufferSlice, CallbackInfo ci) {
        this.anvillib$renderStatesByDraw.clear();
        this.anvillib$renderStatesByMeshToDraw.clear();
    }
}
