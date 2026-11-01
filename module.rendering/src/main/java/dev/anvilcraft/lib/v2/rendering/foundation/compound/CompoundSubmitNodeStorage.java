package dev.anvilcraft.lib.v2.rendering.foundation.compound;

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.jspecify.annotations.Nullable;



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

}
