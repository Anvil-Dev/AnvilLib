package dev.anvilcraft.lib.v2.recipe.outcome.function;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.recipe.cache.TagCache;
import dev.anvilcraft.lib.v2.recipe.init.recipe.LibOutcomeFunctionTypes;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public record ApplyTagToComponent<T>(
    DataComponentType<T> component,
    Identifier path
) implements IOutcomeFunction<ItemStack> {
    @Override
    public ItemStack apply(InWorldRecipeContext context, ItemStack stack) {
        TagCache cache = context.computeIfAbsent(TagCache.TAG_CACHE);
        Tag tag = cache.getTag(this.path);
        if (tag == null) return stack;
        RegistryOps<Tag> ops = context.getNbtRegistryOps();
        DataResult<Pair<T, Tag>> decode = Objects.requireNonNull(this.component.codec()).decode(ops, tag);
        T object = decode.getOrThrow().getFirst();
        stack.set(this.component, object);
        return stack;
    }

    @Override
    public IOutcomeFunction.Type<?> getType() {
        return LibOutcomeFunctionTypes.APPLY_TAG_TO_COMPONENT.get();
    }

    public static class Type implements IOutcomeFunction.Type<ApplyTagToComponent<?>> {
        public static final MapCodec<ApplyTagToComponent<?>> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DataComponentType.CODEC.fieldOf("component").forGetter(ApplyTagToComponent::component),
            Identifier.CODEC.fieldOf("path").forGetter(ApplyTagToComponent::path)
        ).apply(instance, ApplyTagToComponent::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ApplyTagToComponent<?>> STREAM_CODEC = StreamCodec.composite(
            DataComponentType.STREAM_CODEC,
            ApplyTagToComponent::component,
            Identifier.STREAM_CODEC,
            ApplyTagToComponent::path,
            ApplyTagToComponent::new
        );

        @Override
        public MapCodec<ApplyTagToComponent<?>> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ApplyTagToComponent<?>> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
