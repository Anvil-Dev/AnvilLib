package dev.anvilcraft.lib.v2.recipe.predicate.entity;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.recipe.cache.EntityCache;
import dev.anvilcraft.lib.v2.recipe.init.recipe.LibRecipePredicateTypes;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/** 只读的实体存在条件，不预留或消耗匹配实体。 */
public record HasEntity(Vec3 offset, Vec3 range, EntityPredicate predicate, MinMaxBounds.Ints count)
    implements IRecipePredicate<HasEntity> {
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean test(InWorldRecipeContext context) {
        return this.count.matches(context.computeIfAbsent(EntityCache.ENTITY_CACHE)
            .find(this.offset, this.range, this.predicate, false).size());
    }

    @Override
    public Type getType() {
        return LibRecipePredicateTypes.HAS_ENTITY.get();
    }

    public static class Type implements IRecipePredicate.Type<HasEntity> {
        private static final MapCodec<HasEntity> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Vec3.CODEC.optionalFieldOf("offset", Vec3.ZERO).forGetter(HasEntity::offset),
            Vec3.CODEC.optionalFieldOf("range", new Vec3(1, 1, 1)).forGetter(HasEntity::range),
            EntityPredicate.CODEC.fieldOf("predicate").forGetter(HasEntity::predicate),
            MinMaxBounds.Ints.CODEC.optionalFieldOf("count", MinMaxBounds.Ints.atLeast(1)).forGetter(HasEntity::count)
        ).apply(instance, HasEntity::new));

        @Override
        public MapCodec<HasEntity> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HasEntity> streamCodec() {
            return StreamCodecUtil.codec2Stream(CODEC.codec());
        }
    }

    public static class Builder extends EntityConditionBuilder<Builder> {
        private MinMaxBounds.Ints count = MinMaxBounds.Ints.atLeast(1);

        public Builder count(MinMaxBounds.Ints count) {
            this.count = count;
            return this;
        }

        public Builder count(int count) {
            return this.count(MinMaxBounds.Ints.atLeast(count));
        }

        public HasEntity build() {
            return new HasEntity(this.offset, this.range, this.predicate, this.count);
        }
    }
}
