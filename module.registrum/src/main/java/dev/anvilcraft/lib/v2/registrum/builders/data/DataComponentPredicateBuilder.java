package dev.anvilcraft.lib.v2.registrum.builders.data;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.builders.AbstractBuilder;
import dev.anvilcraft.lib.v2.registrum.builders.BuilderCallback;
import dev.anvilcraft.lib.v2.registrum.util.entry.data.DataComponentPredicateEntry;
import net.minecraft.core.component.predicates.DataComponentPredicate;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jspecify.annotations.Nullable;

public class DataComponentPredicateBuilder<E extends DataComponentPredicate, P>
    extends AbstractBuilder<DataComponentPredicate.Type<?>, DataComponentPredicate.Type<E>, P, DataComponentPredicateBuilder<E, P>> {
    @Nullable Codec<E> codec;

    public DataComponentPredicateBuilder(
        AbstractRegistrum<?> owner,
        P parent,
        String name,
        BuilderCallback callback
    ) {
        super(owner, parent, name, callback, Registries.DATA_COMPONENT_PREDICATE_TYPE);
    }

    public DataComponentPredicateBuilder<E, P> codec(Codec<E> codec) {
        this.codec = codec;
        return this;
    }

    @Override
    protected DataComponentPredicate.Type<E> createEntry() {
        if (this.codec == null) {
            throw new IllegalStateException("Codec is not set");
        }
        return new DataComponentPredicate.ConcreteType<>(this.codec);
    }

    @Override
    protected DataComponentPredicateEntry<E> createEntryWrapper(
        DeferredHolder<DataComponentPredicate.Type<?>, DataComponentPredicate.Type<E>> delegate
    ) {
        return new DataComponentPredicateEntry<>(this.getOwner(), delegate);
    }

    @Override
    public DataComponentPredicateEntry<E> register() {
        return (DataComponentPredicateEntry<E>) super.register();
    }
}
