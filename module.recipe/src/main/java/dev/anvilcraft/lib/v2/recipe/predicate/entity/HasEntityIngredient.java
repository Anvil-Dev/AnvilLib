package dev.anvilcraft.lib.v2.recipe.predicate.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.recipe.cache.EntityCache;
import dev.anvilcraft.lib.v2.recipe.init.recipe.LibRecipePredicateTypes;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** 实体原料：匹配时仅占用，提交成功时才移除并产生可选奖励。 */
public record HasEntityIngredient(
    Vec3 offset, Vec3 range, EntityPredicate predicate, int count, float experienceMultiplier, boolean dropItems
) implements IRecipePredicate<HasEntityIngredient> {
    public HasEntityIngredient {
        if (count <= 0) throw new IllegalArgumentException("Entity ingredient count must be positive");
        if (!Float.isFinite(experienceMultiplier) || experienceMultiplier < 0) {
            throw new IllegalArgumentException("Experience multiplier must be finite and non-negative");
        }
    }

    private static final class Reservations {
        private final List<List<Entity>> stack = new ArrayList<>();
        private int accepted;
    }

    private Reservations reservations(InWorldRecipeContext context) {
        return context.computeByIdentity(this, Reservations::new);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean test(InWorldRecipeContext context) {
        return context.computeIfAbsent(EntityCache.ENTITY_CACHE).find(this.offset, this.range, this.predicate, true)
            .size() >= this.count;
    }

    @Override
    public void snapshot(InWorldRecipeContext context) {
        EntityCache cache = context.computeIfAbsent(EntityCache.ENTITY_CACHE);
        List<Entity> selected = cache.find(this.offset, this.range, this.predicate, true).stream().limit(this.count).toList();
        if (selected.size() != this.count) throw new IllegalStateException("Entity reservation no longer matches");
        cache.reserve(selected);
        this.reservations(context).stack.add(selected);
    }

    @Override
    public void rollback(InWorldRecipeContext context) {
        Reservations state = this.reservations(context);
        if (state.stack.isEmpty()) return;
        context.computeIfAbsent(EntityCache.ENTITY_CACHE).release(state.stack.removeLast());
        state.accepted = Math.min(state.accepted, state.stack.size());
    }

    @Override
    public void accept(InWorldRecipeContext context) {
        Reservations state = this.reservations(context);
        context.computeIfAbsent(EntityCache.ENTITY_CACHE).consume(state.stack.get(state.accepted++),
            this.experienceMultiplier, this.dropItems);
    }

    @Override
    public void clearStack(InWorldRecipeContext context) {
        Reservations state = this.reservations(context);
        state.stack.clear();
        state.accepted = 0;
    }

    @Override
    public Type getType() {
        return LibRecipePredicateTypes.HAS_ENTITY_INGREDIENT.get();
    }

    public static class Type implements IRecipePredicate.Type<HasEntityIngredient> {
        private static final Codec<Float> MULTIPLIER = Codec.FLOAT.validate(value ->
            Float.isFinite(value) && value >= 0 ? DataResult.success(value)
                : DataResult.error(() -> "Experience multiplier must be finite and non-negative"));
        private static final MapCodec<HasEntityIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Vec3.CODEC.optionalFieldOf("offset", Vec3.ZERO).forGetter(HasEntityIngredient::offset),
            Vec3.CODEC.optionalFieldOf("range", new Vec3(1, 1, 1)).forGetter(HasEntityIngredient::range),
            EntityPredicate.CODEC.fieldOf("predicate").forGetter(HasEntityIngredient::predicate),
            Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("count", 1).forGetter(HasEntityIngredient::count),
            MULTIPLIER.optionalFieldOf("experience_multiplier", 0f).forGetter(HasEntityIngredient::experienceMultiplier),
            Codec.BOOL.optionalFieldOf("drop_items", false).forGetter(HasEntityIngredient::dropItems)
        ).apply(instance, HasEntityIngredient::new));

        @Override
        public MapCodec<HasEntityIngredient> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HasEntityIngredient> streamCodec() {
            return StreamCodecUtil.codec2Stream(CODEC.codec());
        }

        @Override
        public boolean conflict() {
            return true;
        }
    }

    public static class Builder extends EntityConditionBuilder<Builder> {
        private int count = 1;
        private float experienceMultiplier;
        private boolean dropItems;

        public Builder count(int count) {
            this.count = count;
            return this;
        }

        public Builder experienceMultiplier(float multiplier) {
            this.experienceMultiplier = multiplier;
            return this;
        }

        public Builder dropItems(boolean dropItems) {
            this.dropItems = dropItems;
            return this;
        }

        public HasEntityIngredient build() {
            return new HasEntityIngredient(this.offset, this.range, this.predicate, this.count,
                this.experienceMultiplier, this.dropItems);
        }
    }
}
