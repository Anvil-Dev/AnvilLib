package dev.anvilcraft.lib.v2.registrum.builders.recipe;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.builders.AbstractBuilder;
import dev.anvilcraft.lib.v2.registrum.builders.BuilderCallback;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import dev.anvilcraft.lib.v2.registrum.util.entry.recipe.RecipeSerializerEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;

public class RecipeSerializerBuilder<T extends Recipe<?>, P> extends AbstractBuilder<RecipeSerializer<?>, RecipeSerializer<T>, P, RecipeSerializerBuilder<T, P>> {
    private final MapCodec<T> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

    public RecipeSerializerBuilder(AbstractRegistrum<?> owner,
                                  P parent,
                                  String name,
                                  BuilderCallback callback,
                                  MapCodec<T> codec,
                                  StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
        super(owner, parent, name, callback, Registries.RECIPE_SERIALIZER);
        this.codec = codec;
        this.streamCodec = streamCodec;
    }

    @Override
    public RecipeSerializerEntry<T> register() {
        return (RecipeSerializerEntry<T>) super.register();
    }

    @Override
    protected RecipeSerializerEntry<T> createEntryWrapper(DeferredHolder<RecipeSerializer<?>, RecipeSerializer<T>> delegate) {
        return new RecipeSerializerEntry<>(getOwner(), delegate);
    }

    @Override
    protected RecipeSerializer<T> createEntry() {
        return new RecipeSerializer<>(codec, streamCodec);
    }
}
