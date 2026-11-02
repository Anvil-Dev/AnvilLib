package dev.anvilcraft.lib.v2.recipe.predicate.entity;

import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

/** 两种实体条件共用的范围和原版谓词构建入口。 */
public abstract class EntityConditionBuilder<B extends EntityConditionBuilder<B>> {
    protected Vec3 offset = Vec3.ZERO;
    protected Vec3 range = new Vec3(1, 1, 1);
    protected EntityPredicate predicate = EntityPredicate.Builder.entity().build();

    @SuppressWarnings("unchecked")
    private B self() {
        return (B) this;
    }

    public B offset(Vec3 offset) {
        this.offset = offset;
        return this.self();
    }

    public B offset(double x, double y, double z) {
        return this.offset(new Vec3(x, y, z));
    }

    public B range(Vec3 range) {
        this.range = range;
        return this.self();
    }

    public B range(double x, double y, double z) {
        return this.range(new Vec3(x, y, z));
    }

    public B range(double size) {
        return this.range(size, size, size);
    }

    public B predicate(EntityPredicate predicate) {
        this.predicate = predicate;
        return this.self();
    }

    public B predicate(Consumer<EntityPredicate.Builder> consumer) {
        EntityPredicate.Builder builder = EntityPredicate.Builder.entity();
        consumer.accept(builder);
        return this.predicate(builder.build());
    }

    public B of(EntityType<?> type) {
        return this.predicate(EntityPredicate.Builder.entity().of(BuiltInRegistries.ENTITY_TYPE, type).build());
    }

    public B of(HolderGetter<EntityType<?>> lookup, TagKey<EntityType<?>> tag) {
        return this.predicate(EntityPredicate.Builder.entity().of(lookup, tag).build());
    }
}
