package dev.anvilcraft.lib.v2.recipe.cache;

import dev.anvilcraft.lib.v2.recipe.AnvilLibRecipe;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeData;
import dev.anvilcraft.lib.v2.recipe.util.Range;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 当前上下文的实体占用及待提交消耗，不持有跨世界、跨上下文的缓存。 */
public final class EntityCache {
    public static final InWorldRecipeData<EntityCache> ENTITY_CACHE =
        InWorldRecipeData.of(AnvilLibRecipe.of("entity_cache"), (context, key) -> new EntityCache(context));
    private final InWorldRecipeContext context;
    private final Set<Entity> reserved = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Entity, Consumption> pending = new LinkedHashMap<>();
    private final List<ItemEntity> drops = new ArrayList<>();
    private final List<Experience> experience = new ArrayList<>();
    private boolean registered;

    private record Consumption(float multiplier, boolean dropItems) {}
    private record Experience(Vec3 pos, int amount) {}

    private EntityCache(InWorldRecipeContext context) {
        this.context = context;
    }

    private ItemCache items() {
        ItemCache items = this.context.computeIfAbsent(ItemCache.ITEM_CACHE);
        items.connectEntities(this);
        return items;
    }

    public boolean isReserved(Entity entity) {
        return this.reserved.contains(entity);
    }

    public List<Entity> find(Vec3 offset, Vec3 range, EntityPredicate predicate, boolean ingredient) {
        this.context.assertPlanning();
        Vec3 origin = this.context.getPos().add(offset);
        ItemCache items = this.items();
        return this.context.getLevel().getEntities((Entity) null, Range.of(origin, range).toAABB(), entity ->
            entity.isAlive() && !this.reserved.contains(entity)
                && predicate.matches(this.context.getLevel(), origin, entity)
                && (!ingredient || !(entity instanceof Player) && items.canConsumeEntity(entity))
        ).stream().sorted(Comparator.comparingDouble((Entity entity) -> entity.distanceToSqr(origin))
            .thenComparing(Entity::getUUID)).toList();
    }

    public void reserve(List<Entity> entities) {
        this.context.assertPlanning();
        for (Entity entity : entities) {
            if (entity instanceof Player || !entity.isAlive() || this.reserved.contains(entity)
                || !this.items().canConsumeEntity(entity)) throw new IllegalStateException("Entity is already in use");
        }
        this.reserved.addAll(entities);
    }

    public void release(List<Entity> entities) {
        for (Entity entity : entities) {
            this.reserved.remove(entity);
            this.pending.remove(entity);
        }
    }

    public void consume(List<Entity> entities, float multiplier, boolean dropItems) {
        this.context.assertPlanning();
        for (Entity entity : entities) {
            if (!this.reserved.contains(entity) || this.pending.containsKey(entity)) {
                throw new IllegalStateException("Entity consumption must own a unique reservation");
            }
            this.pending.put(entity, new Consumption(multiplier, dropItems));
        }
        this.context.putAcceptor(AnvilLibRecipe.of("entity_validation"), -100, ignored -> this.validate());
        this.context.putAcceptor(ENTITY_CACHE.location(), 150, ignored -> this.commit());
        if (!this.registered) {
            this.registered = true;
            this.context.afterCommit(this::releaseRewards);
        }
    }

    private void validate() {
        for (Entity entity : this.pending.keySet()) {
            if (entity instanceof Player || !entity.isAlive() || entity.level() != this.context.getLevel()) {
                throw new IllegalStateException("Reserved entity is no longer available");
            }
        }
    }

    private void commit() {
        this.validate();
        ServerLevel level = this.context.getLevel();
        this.pending.forEach((entity, consumption) -> {
            boolean mobDrops = level.getGameRules().get(GameRules.MOB_DROPS);
            boolean allowItems = consumption.dropItems && (entity instanceof LivingEntity
                ? mobDrops : level.getGameRules().get(GameRules.ENTITY_DROPS));
            int reward = 0;
            if (entity instanceof LivingEntity living) {
                if (mobDrops && consumption.multiplier > 0 && living.shouldDropExperience() && !living.wasExperienceConsumed()) {
                    reward = (int) Math.min(Integer.MAX_VALUE,
                        Math.floor(living.getExperienceReward(level, null) * (double) consumption.multiplier));
                }
                living.skipDropExperience();
            }
            Vec3 pos = entity.position();
            try (EntityDropCapture scope = new EntityDropCapture(level)) {
                if (allowItems && entity instanceof LivingEntity living) {
                    living.dropAllDeathLoot(level, level.damageSources().generic());
                }
                entity.discard();
                if (allowItems) this.drops.addAll(scope.drops());
            }
            if (reward > 0) this.experience.add(new Experience(pos, reward));
        });
        this.pending.clear();
    }

    private void releaseRewards() {
        ServerLevel level = this.context.getLevel();
        this.drops.forEach(level::addFreshEntity);
        this.experience.forEach(reward -> ExperienceOrb.award(level, reward.pos, reward.amount));
        this.drops.clear();
        this.experience.clear();
    }
}
