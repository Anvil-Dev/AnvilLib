package dev.anvilcraft.lib.v2.rendering.foundation.compound;

import dev.anvilcraft.lib.v2.rendering.foundation.ForwardingSubmitNodeCollection;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;
import java.util.function.Consumer;

@ApiStatus.Internal
public class CompoundSubmitNodeCollection extends ForwardingSubmitNodeCollection {
    private final SubmitNodeCollection collection1;
    private final SubmitNodeCollection collection2;
    private final @Nullable DirtyTracked tracker;

    public CompoundSubmitNodeCollection(SubmitNodeStorage storage, SubmitNodeCollection first, SubmitNodeCollection second, @Nullable DirtyTracked tracker) {
        super(first);
        this.collection1 = first;
        this.collection2 = second;
        this.tracker = tracker;
    }

    @Override
    protected void forward(Consumer<OrderedSubmitNodeCollector> submit) {
        if (this.tracker != null) this.tracker.markDirty();
        submit.accept(this.collection1);
        submit.accept(this.collection2);
    }
}
