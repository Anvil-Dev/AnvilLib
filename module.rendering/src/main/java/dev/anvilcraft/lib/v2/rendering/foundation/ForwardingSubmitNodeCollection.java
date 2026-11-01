package dev.anvilcraft.lib.v2.rendering.foundation;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.BlockModelFeatureRenderer;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.feature.GizmoFeatureRenderer;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.LeashFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.MovingBlockFeatureRenderer;
import net.minecraft.client.renderer.feature.QuadParticleFeatureRenderer;
import net.minecraft.client.renderer.feature.ShadowFeatureRenderer;
import net.minecraft.client.renderer.feature.ShapeOutlineFeatureRenderer;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.feature.phase.TranslucentFeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import java.util.function.Consumer;

/** Forwards the complete feature submission API, including mod-provided feature types. */
public abstract class ForwardingSubmitNodeCollection extends SubmitNodeCollection {
    private final SubmitNodeCollection primary;

    protected ForwardingSubmitNodeCollection(SubmitNodeCollection primary) {
        super(primary.shadows == primary.oitTranslucent, primary.seeThrough);
        this.primary = primary;
    }

    protected abstract void forward(Consumer<OrderedSubmitNodeCollector> submit);

    protected RenderType transformRenderType(RenderType renderType) {
        return renderType;
    }

    @Override
    public List<FeatureRenderPhase<?>> allPhases() {
        return this.primary.allPhases();
    }

    @Override
    public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
        this.forward(collector -> collector.submitShadow(poseStack, radius, pieces));
    }

    @Override
    public void submitNameTag(
        PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name, boolean seeThrough, int lightCoords, CameraRenderState camera
    ) {
        this.forward(collector -> collector.submitNameTag(poseStack, nameTagAttachment, offset, name, seeThrough, lightCoords, camera));
    }

    @Override
    public void submitText(
        PoseStack poseStack,
        float x,
        float y,
        FormattedCharSequence string,
        boolean dropShadow,
        Font.DisplayMode displayMode,
        int lightCoords,
        int color,
        int backgroundColor,
        int outlineColor
    ) {
        this.forward(collector -> collector.submitText(poseStack, x, y, string, dropShadow, displayMode, lightCoords, color, backgroundColor, outlineColor));
    }

    @Override
    public void submitTextBackground(PoseStack poseStack, float x0, float y0, float x1, float y1, int color, Font.DisplayMode displayMode, int lightCoords) {
        this.forward(collector -> collector.submitTextBackground(poseStack, x0, y0, x1, y1, color, displayMode, lightCoords));
    }

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
        this.forward(collector -> collector.submitFlame(poseStack, renderState, rotation));
    }

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
        this.forward(collector -> collector.submitLeash(poseStack, leashState));
    }

    @Override
    public <S> void submitModel(
        Model<? super S> model,
        S state,
        PoseStack poseStack,
        RenderType renderType,
        int lightCoords,
        int overlayCoords,
        int tintedColor,
        @Nullable UvMapping uvMapping,
        int outlineColor
    ) {
        this.forward(collector -> collector.submitModel(model, state, poseStack, this.transformRenderType(renderType), lightCoords, overlayCoords, tintedColor, uvMapping, outlineColor));
    }

    @Override
    public <S> void submitCrumblingOverlay(
        Model<? super S> model,
        S state,
        PoseStack poseStack,
        RenderType renderType,
        int lightCoords,
        int overlayCoords,
        int tintedColor,
        ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
    ) {
        this.forward(collector -> collector.submitCrumblingOverlay(model, state, poseStack, this.transformRenderType(renderType), lightCoords, overlayCoords, tintedColor, crumblingOverlay));
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState, int outlineColor) {
        this.forward(collector -> collector.submitMovingBlock(poseStack, movingBlockRenderState, outlineColor));
    }

    @Override
    public void submitBlockModel(
        PoseStack poseStack,
        RenderType renderType,
        List<BlockStateModelPart> modelParts,
        int[] tintLayers,
        int lightCoords,
        int overlayCoords,
        int outlineColor
    ) {
        this.forward(collector -> collector.submitBlockModel(poseStack, this.transformRenderType(renderType), modelParts, tintLayers, lightCoords, overlayCoords, outlineColor));
    }

    @Override
    public void submitMultiLayerBlockModel(
            PoseStack poseStack,
            List<BlockStateModelPart> modelParts,
            boolean translucent,
            int[] tintLayers,
            int lightCoords,
            int overlayCoords,
            int outlineColor
    ) {
        this.forward(collector -> collector.submitMultiLayerBlockModel(poseStack, modelParts, translucent, tintLayers, lightCoords, overlayCoords, outlineColor));
    }

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int progress, boolean isBlockTranslucent) {
        this.forward(collector -> collector.submitBreakingBlockModel(poseStack, parts, progress, isBlockTranslucent));
    }

    @Override
    public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color, float width, boolean afterTerrain) {
        this.forward(collector -> collector.submitShapeOutline(poseStack, shape, this.transformRenderType(renderType), color, width, afterTerrain));
    }

    @Override
    public void submitItem(
        PoseStack poseStack,
        ItemDisplayContext displayContext,
        int lightCoords,
        int overlayCoords,
        int outlineColor,
        int[] tintLayers,
        ItemQuads quads,
        ItemStackRenderState.FoilType foilType
    ) {
        this.forward(collector -> collector.submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads, foilType));
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) {
        this.forward(collector -> collector.submitCustomGeometry(poseStack, this.transformRenderType(renderType), customGeometryRenderer));
    }

    @Override
    public void submitQuadParticleGroup(QuadParticleRenderState particles) {
        this.forward(collector -> collector.submitQuadParticleGroup(particles));
    }

    @Override
    public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group group, CameraRenderState camera, boolean onTop) {
        this.forward(collector -> collector.submitGizmoPrimitives(group, camera, onTop));
    }

    @Override
    public <T extends net.minecraft.client.renderer.feature.submit.SubmitNode, S extends T> void submitSpecial(
        net.neoforged.neoforge.client.submit.RenderPhaseKey<T> phaseKey, S submitNode
    ) {
        this.forward(collector -> collector.submitSpecial(phaseKey, submitNode));
    }
}
