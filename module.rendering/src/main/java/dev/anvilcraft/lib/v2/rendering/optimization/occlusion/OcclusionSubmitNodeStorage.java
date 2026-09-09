package dev.anvilcraft.lib.v2.rendering.optimization.occlusion;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.NonNull;

@ApiStatus.Internal
public class OcclusionSubmitNodeStorage extends SubmitNodeStorage {
    private final Int2ObjectAVLTreeMap<SubmitNodeCollection> submitsPerOrder = new Int2ObjectAVLTreeMap<>();
    private final OcclusionCuller culler;
    private final SubmitNodeCollector original;

    public OcclusionSubmitNodeStorage(OcclusionCuller culler, SubmitNodeCollector original) {
        this.culler = culler;
        this.original = original;
    }

    @Override
    @NonNull
    public SubmitNodeCollection order(int order) {
        SubmitNodeCollection originalCollection = (SubmitNodeCollection) original.order(order);
        return this.submitsPerOrder.computeIfAbsent(
            order,
            _ -> new OcclusionSubmitNodeCollection(this, this.culler, originalCollection)
        );
    }

    public void beginOcclusionRecord(OcclusionKey occlusionKey) {
        this.beginOcclusionRecord(occlusionKey, 0);
    }

    public void endOcclusionRecord() {
        this.endOcclusionRecord(0);
    }

    public void beginOcclusionRecord(OcclusionKey occlusionKey, int order) {
        SubmitNodeCollection collection = this.order(order);
        ((OcclusionSubmitNodeCollection) collection).beginOcclusionRecord(occlusionKey);
    }

    public void endOcclusionRecord(int order) {
        SubmitNodeCollection collection = this.order(order);
        ((OcclusionSubmitNodeCollection) collection).endOcclusionRecord();
    }
}
