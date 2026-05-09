package dev.anvilcraft.lib.v2.rendering.foundation.compound;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
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
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class CompoundSubmitNodeStorage extends SubmitNodeStorage {
    private final Int2ObjectAVLTreeMap<CompoundSubmitNodeCollection> compoundSubmitsPerOrder = new Int2ObjectAVLTreeMap<>();

    private final SubmitNodeCollector delegate1;
    private final SubmitNodeCollector delegate2;
    @Nullable
    private final DirtyTracked tracker;

    public CompoundSubmitNodeStorage(SubmitNodeCollector a, SubmitNodeCollector b, @Nullable DirtyTracked tracker) {
        this.delegate1 = a;
        this.delegate2 = b;
        this.tracker = tracker;
    }

    public SubmitNodeCollection order(int order) {
        SubmitNodeCollection order1 = (SubmitNodeCollection) delegate1.order(order);
        SubmitNodeCollection order2 = (SubmitNodeCollection) delegate2.order(order);
        return this.compoundSubmitsPerOrder.computeIfAbsent(
            order,
            (ignored) -> new CompoundSubmitNodeCollection(this, order1, order2, tracker)
        );
    }

    public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
        this.order(0).submitShadow(poseStack, radius, pieces);
    }

    public void submitNameTag(PoseStack poseStack, @Nullable Vec3 nameTagAttachment, int offset, Component name, boolean seeThrough, int lightCoords, double distanceToCameraSq, CameraRenderState camera) {
        this.order(0).submitNameTag(poseStack, nameTagAttachment, offset, name, seeThrough, lightCoords, distanceToCameraSq, camera);
    }

    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence string, boolean dropShadow, Font.DisplayMode displayMode, int lightCoords, int color, int backgroundColor, int outlineColor) {
        this.order(0).submitText(poseStack, x, y, string, dropShadow, displayMode, lightCoords, color, backgroundColor, outlineColor);
    }

    public void submitFlame(PoseStack poseStack, EntityRenderState renderState, Quaternionf rotation) {
        this.order(0).submitFlame(poseStack, renderState, rotation);
    }

    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leashState) {
        this.order(0).submitLeash(poseStack, leashState);
    }

    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, int tintedColor, @Nullable TextureAtlasSprite sprite, int outlineColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay) {
        this.order(0).submitModel(model, state, poseStack, renderType, lightCoords, overlayCoords, tintedColor, sprite, outlineColor, crumblingOverlay);
    }

    public void submitModelPart(ModelPart modelPart, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords, @Nullable TextureAtlasSprite sprite, boolean sheeted, boolean hasFoil, int tintedColor, ModelFeatureRenderer.@Nullable CrumblingOverlay crumblingOverlay, int outlineColor) {
        this.order(0).submitModelPart(modelPart, poseStack, renderType, lightCoords, overlayCoords, sprite, sheeted, hasFoil, tintedColor, crumblingOverlay, outlineColor);
    }

    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState movingBlockRenderState) {
        this.order(0).submitMovingBlock(poseStack, movingBlockRenderState);
    }

    public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> modelParts, int[] tintLayers, int lightCoords, int overlayCoords, int outlineColor) {
        this.order(0).submitBlockModel(poseStack, renderType, modelParts, tintLayers, lightCoords, overlayCoords, outlineColor);
    }

    public void submitBreakingBlockModel(PoseStack poseStack, BlockStateModel model, long seed, int progress) {
        this.order(0).submitBreakingBlockModel(poseStack, model, seed, progress);
    }

    public void submitItem(PoseStack poseStack, ItemDisplayContext displayContext, int lightCoords, int overlayCoords, int outlineColor, int[] tintLayers, List<BakedQuad> quads, ItemStackRenderState.FoilType foilType) {
        this.order(0).submitItem(poseStack, displayContext, lightCoords, overlayCoords, outlineColor, tintLayers, quads, foilType);
    }

    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, SubmitNodeCollector.CustomGeometryRenderer customGeometryRenderer) {
        this.order(0).submitCustomGeometry(poseStack, renderType, customGeometryRenderer);
    }

    public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer particleGroupRenderer) {
        this.order(0).submitParticleGroup(particleGroupRenderer);
    }
}
