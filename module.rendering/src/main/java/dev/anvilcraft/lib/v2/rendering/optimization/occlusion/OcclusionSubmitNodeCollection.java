package dev.anvilcraft.lib.v2.rendering.optimization.occlusion;

import dev.anvilcraft.lib.v2.rendering.mixins.accessors.CustomFeatureRendererStorageAccess;
import dev.anvilcraft.lib.v2.rendering.mixins.accessors.ModelFeatureRendererStorageAccess;
import dev.anvilcraft.lib.v2.rendering.mixins.accessors.ModelPartFeatureRendererStorageAccess;
import dev.anvilcraft.lib.v2.rendering.mixins.accessors.NameTagFeatureRendererStorageAccess;
import dev.anvilcraft.lib.v2.rendering.mixins.accessors.SubmitNodeCollectionAccess;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OcclusionSubmitNodeCollection extends SubmitNodeCollection {
    private final OcclusionCuller culler;
    private final SubmitNodeCollection original;

    private OcclusionKey currentKey = null;

    @ApiStatus.Internal
    public OcclusionSubmitNodeCollection(
        SubmitNodeStorage submitNodeStorage,
        OcclusionCuller culler,
        SubmitNodeCollection original
    ) {
        super(submitNodeStorage);
        this.culler = culler;
        this.original = original;
    }

    public void beginOcclusionRecord(OcclusionKey occlusionKey) {
        if (this.currentKey != null){
            throw new IllegalStateException("occlusion record begun without terminating previous record.");
        }
        this.clear();
        this.endFrame();
        this.currentKey = occlusionKey;
    }

    public void endOcclusionRecord() {
        if (this.wasUsed()) {
            this.submitFeatureToCuller();
        }
        this.currentKey = null;
    }


    private void submitFeatureToCuller() {
        List<Object> collectedFeatures = new ArrayList<>();
        if (this.currentKey == null) {
            return;
        }

        this.submitFeatureList(
            this.getShadowSubmits(),
            this.original.getShadowSubmits(),
            collectedFeatures
        );
        this.submitFeatureList(
            this.getFlameSubmits(),
            this.original.getFlameSubmits(),
            collectedFeatures
        );
        this.submitFeatureList(
            this.getTextSubmits(),
            this.original.getTextSubmits(),
            collectedFeatures
        );
        this.submitFeatureList(
            this.getLeashSubmits(),
            this.original.getLeashSubmits(),
            collectedFeatures
        );
        this.submitFeatureList(
            this.getMovingBlockSubmits(),
            this.original.getMovingBlockSubmits(),
            collectedFeatures
        );
        this.submitFeatureList(
            this.getBlockModelSubmits(),
            this.original.getBlockModelSubmits(),
            collectedFeatures
        );
        this.submitFeatureList(
            this.getMultiLayerBlockModelSubmits(),
            this.original.getMultiLayerBlockModelSubmits(),
            collectedFeatures
        );
        this.submitFeatureList(
            this.getBreakingBlockModelSubmits(),
            this.original.getBreakingBlockModelSubmits(),
            collectedFeatures
        );
        this.submitFeatureList(this.getItemSubmits(), this.original.getItemSubmits(), collectedFeatures);
        this.submitFeatureList(
            this.getParticleGroupRenderers(),
            this.original.getParticleGroupRenderers(),
            collectedFeatures
        );

        NameTagFeatureRendererStorageAccess nameTagSubmits = (NameTagFeatureRendererStorageAccess) this.getNameTagSubmits();
        NameTagFeatureRendererStorageAccess originalNameTagSubmits = (NameTagFeatureRendererStorageAccess) this.original.getNameTagSubmits();

        this.submitFeatureList(
            nameTagSubmits.alrGetNameTagSubmitsNormal(),
            originalNameTagSubmits.alrGetNameTagSubmitsNormal(),
            collectedFeatures
        );
        this.submitFeatureList(
            nameTagSubmits.alrGetNameTagSubmitsSeethrough(),
            originalNameTagSubmits.alrGetNameTagSubmitsSeethrough(),
            collectedFeatures
        );

        ModelFeatureRendererStorageAccess modelSubmits = (ModelFeatureRendererStorageAccess) this.getModelSubmits();
        ModelFeatureRendererStorageAccess originalModelSubmits = (ModelFeatureRendererStorageAccess) this.original.getModelSubmits();

        this.submitFeatureMap(
            modelSubmits.alrGetSolidModelSubmits(),
            originalModelSubmits.alrGetSolidModelSubmits(),
            collectedFeatures
        );
        this.submitFeatureList(
            modelSubmits.alrGetTranslucentModelSubmits(),
            originalModelSubmits.alrGetTranslucentModelSubmits(),
            collectedFeatures
        );

        ModelPartFeatureRendererStorageAccess modelPartSubmits = (ModelPartFeatureRendererStorageAccess) this.getModelPartSubmits();
        ModelPartFeatureRendererStorageAccess originalModelPartSubmits = (ModelPartFeatureRendererStorageAccess) this.original.getModelPartSubmits();

        this.submitFeatureMap(
            modelPartSubmits.alrGetSolidModelPartSubmits(),
            originalModelPartSubmits.alrGetSolidModelPartSubmits(),
            collectedFeatures
        );
        this.submitFeatureMap(
            modelPartSubmits.alrGetTranslucentModelPartSubmits(),
            originalModelPartSubmits.alrGetTranslucentModelPartSubmits(),
            collectedFeatures
        );


        CustomFeatureRendererStorageAccess customGeometrySubmits = (CustomFeatureRendererStorageAccess) this.getCustomGeometrySubmits();
        CustomFeatureRendererStorageAccess originalCustomGeometrySubmits = (CustomFeatureRendererStorageAccess) this.original.getCustomGeometrySubmits();

        this.submitFeatureMap(
            customGeometrySubmits.alrGetTranslucentCustomGeometrySubmits(),
            originalCustomGeometrySubmits.alrGetTranslucentCustomGeometrySubmits(),
            collectedFeatures
        );
        this.submitFeatureMap(
            customGeometrySubmits.alrGetSolidCustomGeometrySubmits(),
            originalCustomGeometrySubmits.alrGetSolidCustomGeometrySubmits(),
            collectedFeatures
        );

        ((SubmitNodeCollectionAccess) this.original).alrSetWasUsed(true);

        this.culler.submitFeatureKey(this.currentKey, collectedFeatures);
    }

    private <T> void submitFeatureList(List<T> features, List<T> target, List<Object> collectedFeatures) {
        collectedFeatures.addAll(features);
        target.addAll(features);
    }

    private <T> void submitFeatureMap(
        Map<RenderType, List<T>> src,
        Map<RenderType, List<T>> dst,
        List<Object> collectedFeatures
    ) {
        for (Map.Entry<RenderType, List<T>> entry : src.entrySet()) {
            dst.computeIfAbsent(entry.getKey(), _ -> new ArrayList<>()).addAll(entry.getValue());
            collectedFeatures.addAll(entry.getValue());
        }
    }

}
