package dev.anvilcraft.lib.v2.recipe.predicate.function;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.recipe.cache.TagCache;
import dev.anvilcraft.lib.v2.recipe.init.recipe.LibPredicateFunctionTypes;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.Objects;

public record SaveComponentToTag<T>(
    DataComponentType<T> component,
    Identifier path
) implements IPredicateFunction<ItemStackTemplate> {
    @Override
    public ItemStackTemplate apply(InWorldRecipeContext context, ItemStackTemplate stack) {
        RegistryOps<Tag> ops = context.getNbtRegistryOps();
        T object = stack.get(this.component);
        DataResult<Tag> result = Objects.requireNonNull(this.component.codec()).encodeStart(ops, object);
        TagCache cache = context.computeIfAbsent(TagCache.TAG_CACHE);
        cache.computeIfAbsent(this.path, key -> result.getOrThrow());
        return stack;
    }

    @Override
    public IPredicateFunction.Type<?> getType() {
        return LibPredicateFunctionTypes.SAVE_COMPONENT_TO_TAG.get();
    }

    public static class Type implements IPredicateFunction.Type<SaveComponentToTag<?>> {
        public static final MapCodec<SaveComponentToTag<?>> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DataComponentType.CODEC.fieldOf("component").forGetter(SaveComponentToTag::component),
            Identifier.CODEC.fieldOf("path").forGetter(SaveComponentToTag::path)
        ).apply(instance, SaveComponentToTag::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, SaveComponentToTag<?>> STREAM_CODEC = StreamCodec.composite(
            DataComponentType.STREAM_CODEC,
            SaveComponentToTag::component,
            Identifier.STREAM_CODEC,
            SaveComponentToTag::path,
            SaveComponentToTag::new
        );

        @Override
        public MapCodec<SaveComponentToTag<?>> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SaveComponentToTag<?>> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
