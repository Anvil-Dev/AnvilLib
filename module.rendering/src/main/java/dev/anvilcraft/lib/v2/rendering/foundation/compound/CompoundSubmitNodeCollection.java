package dev.anvilcraft.lib.v2.rendering.foundation.compound;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.SubmitNodeStorageExtension;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class CompoundSubmitNodeCollection extends SubmitNodeCollection {
    private final SubmitNodeCollection collection1;
    private final SubmitNodeCollection collection2;
    @Nullable
    private final DirtyTracked tracker;

    public CompoundSubmitNodeCollection(SubmitNodeStorage submitNodeStorage, SubmitNodeCollection collection1, SubmitNodeCollection collection2, @Nullable DirtyTracked tracker) {
        super(submitNodeStorage);
        this.collection1 = collection1;
        this.collection2 = collection2;
        this.tracker = tracker;
    }

    private void markDirty() {
        if (this.tracker != null) {
            this.tracker.markDirty();
        }
    }

    @Override
    public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
        this.markDirty();
        this.collection1.submitShadow(poseStack, radius, pieces);
        this.collection2.submitShadow(poseStack, radius, pieces);
    }

    @Override
    public void submitNameTag(PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name, boolean seeThrough, int lightCoords, double distanceToCameraSq, CameraRenderState camera) {
        this.markDirty();
        this.collection1.submitNameTag(poseStack, nameTagAttachment, offset, name, seeThrough, lightCoords, distanceToCameraSq, camera);
        this.collection2.submitNameTag(poseStack, nameTagAttachment, offset, name, seeThrough, lightCoords, distanceToCameraSq, camera);
    }

    @Override
    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence string, boolean dropShadow, Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {
        this.markDirty();
        this.collection1.submitText(poseStack, x, y, string, dropShadow, displayMode, lightCoords, color, backgroundColor, outlineColor);
        this.collection2.submitText(poseStack, x, y, string, dropShadow, displayMode, lightCoords, color, backgroundColor, outlineColor);
    }

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
        this.markDirty();
        this.collection1.submitFlame(poseStack, renderState, rotation);
        this.collection2.submitFlame(poseStack, renderState, rotation);
    }

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
        this.markDirty();
        this.collection1.submitLeash(poseStack, leashState);
        this.collection2.submitLeash(poseStack, leashState);
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, @Nullable TextureAtlasSprite sprite, int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        this.markDirty();
        this.collection1.submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
        this.collection2.submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
    }

    @Override
    public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, @Nullable TextureAtlasSprite sprite, boolean sheeted, boolean hasFoil, int tintedColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay, int outlineColor) {
        this.markDirty();
        this.collection1.submitModelPart(modelPart, poseStack, renderType, lightCoords, overlayCoords, sprite, sheeted, hasFoil, tintedColor, crumblingOverlay, outlineColor);
        this.collection2.submitModelPart(modelPart, poseStack, renderType, lightCoords, overlayCoords, sprite, sheeted, hasFoil, tintedColor, crumblingOverlay, outlineColor);
    }

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState) {
        this.markDirty();
        this.collection1.submitMovingBlock(poseStack, movingBlockRenderState);
        this.collection2.submitMovingBlock(poseStack, movingBlockRenderState);
    }

    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> modelParts, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
        this.markDirty();
        this.collection1.submitBlockModel(poseStack, renderType, modelParts, tintLayers, lightCoords, overlayCoords, outlineColor);
        this.collection2.submitBlockModel(poseStack, renderType, modelParts, tintLayers, lightCoords, overlayCoords, outlineColor);
    }

    @Override
    public void submitMultiLayerBlockModel(PoseStack poseStack, List<BlockStateModelPart> modelParts, boolean translucent, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
        this.markDirty();
        this.collection1.submitMultiLayerBlockModel(poseStack, modelParts, translucent, tintLayers, lightCoords, overlayCoords, outlineColor);
        this.collection2.submitMultiLayerBlockModel(poseStack, modelParts, translucent, tintLayers, lightCoords, overlayCoords, outlineColor);
    }

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack, BlockStateModel model, long seed, int progress) {
        this.markDirty();
        this.collection1.submitBreakingBlockModel(poseStack, model, seed, progress);
        this.collection2.submitBreakingBlockModel(poseStack, model, seed, progress);
    }

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType) {
        this.markDirty();
        this.collection1.submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads, foilType);
        this.collection2.submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads, foilType);
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) {
        this.markDirty();
        this.collection1.submitCustomGeometry(poseStack, renderType, customGeometryRenderer);
        this.collection2.submitCustomGeometry(poseStack, renderType, customGeometryRenderer);
    }

    @Override
    public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer particleGroupRenderer) {
        this.markDirty();
        this.collection1.submitParticleGroup(particleGroupRenderer);
        this.collection2.submitParticleGroup(particleGroupRenderer);
    }

    // ===== getters: delegate to collection1 =====

    @Override
    public List<SubmitNodeStorage.ShadowSubmit> getShadowSubmits() {
        return this.collection1.getShadowSubmits();
    }

    @Override
    public List<SubmitNodeStorage.FlameSubmit> getFlameSubmits() {
        return this.collection1.getFlameSubmits();
    }

    @Override
    public NameTagFeatureRenderer.Storage getNameTagSubmits() {
        return this.collection1.getNameTagSubmits();
    }

    @Override
    public List<SubmitNodeStorage.TextSubmit> getTextSubmits() {
        return this.collection1.getTextSubmits();
    }

    @Override
    public List<SubmitNodeStorage.LeashSubmit> getLeashSubmits() {
        return this.collection1.getLeashSubmits();
    }

    @Override
    public List<SubmitNodeStorage.MovingBlockSubmit> getMovingBlockSubmits() {
        return this.collection1.getMovingBlockSubmits();
    }

    @Override
    public List<SubmitNodeStorage.BlockModelSubmit> getBlockModelSubmits() {
        return this.collection1.getBlockModelSubmits();
    }

    @Override
    public List<SubmitNodeStorageExtension.MultiLayerBlockModelSubmit> getMultiLayerBlockModelSubmits() {
        return this.collection1.getMultiLayerBlockModelSubmits();
    }

    @Override
    public List<SubmitNodeStorage.BreakingBlockModelSubmit> getBreakingBlockModelSubmits() {
        return this.collection1.getBreakingBlockModelSubmits();
    }

    @Override
    public ModelPartFeatureRenderer.Storage getModelPartSubmits() {
        return this.collection1.getModelPartSubmits();
    }

    @Override
    public List<SubmitNodeStorage.ItemSubmit> getItemSubmits() {
        return this.collection1.getItemSubmits();
    }

    @Override
    public List<SubmitNodeCollector.ParticleGroupRenderer> getParticleGroupRenderers() {
        return this.collection1.getParticleGroupRenderers();
    }

    @Override
    public ModelFeatureRenderer.Storage getModelSubmits() {
        return this.collection1.getModelSubmits();
    }

    @Override
    public CustomFeatureRenderer.Storage getCustomGeometrySubmits() {
        return this.collection1.getCustomGeometrySubmits();
    }

    @Override
    public boolean wasUsed() {
        return this.collection1.wasUsed();
    }

    @Override
    public void clear() {
        this.collection1.clear();
        this.collection2.clear();
    }

    @Override
    public void endFrame() {
        this.collection1.endFrame();
        this.collection2.endFrame();
    }
}
