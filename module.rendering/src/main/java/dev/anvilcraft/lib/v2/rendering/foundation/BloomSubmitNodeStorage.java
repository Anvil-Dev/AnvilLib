package dev.anvilcraft.lib.v2.rendering.foundation;

import dev.anvilcraft.lib.v2.rendering.extension.ALRRenderTypeExtension;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.rendertype.RenderType;
import java.util.function.Consumer;

public class BloomSubmitNodeStorage extends SubmitNodeStorage {
    private final SubmitNodeCollector delegate;
    private final Int2ObjectAVLTreeMap<SubmitNodeCollection> orders = new Int2ObjectAVLTreeMap<>();

    public BloomSubmitNodeStorage(SubmitNodeCollector delegate) {
        this.delegate = delegate;
    }

    @Override
    public SubmitNodeCollection order(int order) {
        return this.orders.computeIfAbsent(order, ignored -> {
            SubmitNodeCollection collection = (SubmitNodeCollection) this.delegate.order(order);
            return new ForwardingSubmitNodeCollection(collection) {
                @Override
                protected void forward(Consumer<OrderedSubmitNodeCollector> submit) {
                    submit.accept(collection);
                }

                @Override
                protected RenderType transformRenderType(RenderType renderType) {
                    return ALRRenderTypeExtension.copyWithBloom(renderType);
                }
            };
        });
    }

    public static BloomSubmitNodeStorage wrap(SubmitNodeCollector delegate) {
        return new BloomSubmitNodeStorage(delegate);
    }
}
