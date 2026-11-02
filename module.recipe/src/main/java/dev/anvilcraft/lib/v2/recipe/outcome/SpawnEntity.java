package dev.anvilcraft.lib.v2.recipe.outcome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.recipe.init.recipe.LibRecipeOutcomeTypes;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** 在资源提交成功后生成实体，NBT 不能覆盖配方的实体类型、位置及唯一标识。 */
public record SpawnEntity(EntityType<?> entity, CompoundTag nbt, Vec3 offset, NumberProvider count, NumberProvider chance)
    implements IRecipeOutcome<SpawnEntity> {
    public SpawnEntity {
        Objects.requireNonNull(entity);
        if (entity == EntityType.PLAYER) throw new IllegalArgumentException("Recipes cannot spawn players");
        nbt = nbt == null ? new CompoundTag() : nbt.copy();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Type getType() {
        return LibRecipeOutcomeTypes.SPAWN_ENTITY.get();
    }

    @Override
    public void accept(InWorldRecipeContext context) {
        context.assertPlanning();
        Vec3 pos = context.getPos().add(this.offset);
        int amount = context.getInt(this.count, 0, 99);
        List<Entity> prepared = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            CompoundTag tag = this.nbt.copy();
            sanitize(tag);
            tag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(this.entity).toString());
            Entity result = EntityType.loadEntityRecursive(this.entity, tag, context.getLevel(), EntitySpawnReason.TRIGGERED, value -> {
                if (value instanceof Player) throw new IllegalArgumentException("Recipes cannot spawn player passengers");
                value.setUUID(UUID.randomUUID());
                value.snapTo(pos.x, pos.y, pos.z, value.getYRot(), value.getXRot());
                return value;
            });
            if (result == null) throw new IllegalArgumentException("Cannot create recipe entity: " + this.entity);
            prepared.add(result);
        }
        context.afterCommit(() -> {
            for (Entity result : prepared) {
                if (this.nbt.isEmpty() && result instanceof Mob mob) {
                    EventHooks.finalizeMobSpawn(mob, context.getLevel(), context.getLevel().getCurrentDifficultyAt(BlockPos.containing(pos)),
                        EntitySpawnReason.TRIGGERED, null);
                }
                if (!context.getLevel().tryAddFreshEntityWithPassengers(result)) {
                    throw new IllegalStateException("Recipe entity could not be added to the world");
                }
            }
        });
    }

    private static void sanitize(CompoundTag tag) {
        tag.remove("UUID");
        tag.remove("uuid");
        tag.remove("Pos");
        tag.remove("pos");
        for (Tag passenger : tag.getListOrEmpty("Passengers")) {
            if (passenger instanceof CompoundTag compound) sanitize(compound);
        }
        for (Tag passenger : tag.getListOrEmpty("passengers")) {
            if (passenger instanceof CompoundTag compound) sanitize(compound);
        }
    }

    public static class Type implements IRecipeOutcome.Type<SpawnEntity> {
        private static final MapCodec<SpawnEntity> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("entity").forGetter(SpawnEntity::entity),
            CompoundTag.CODEC.optionalFieldOf("nbt", new CompoundTag()).forGetter(SpawnEntity::nbt),
            Vec3.CODEC.optionalFieldOf("offset", Vec3.ZERO).forGetter(SpawnEntity::offset),
            CodecUtil.NUMBER_PROVIDER.optionalFieldOf("count", ConstantValue.exactly(1)).forGetter(SpawnEntity::count),
            CodecUtil.NUMBER_PROVIDER.optionalFieldOf("chance", ConstantValue.exactly(1)).forGetter(SpawnEntity::chance)
        ).apply(instance, SpawnEntity::new));

        @Override
        public MapCodec<SpawnEntity> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SpawnEntity> streamCodec() {
            return StreamCodecUtil.codec2Stream(CODEC.codec());
        }
    }

    public static class Builder {
        private EntityType<?> entity;
        private CompoundTag nbt = new CompoundTag();
        private Vec3 offset = Vec3.ZERO;
        private NumberProvider count = ConstantValue.exactly(1);
        private NumberProvider chance = ConstantValue.exactly(1);

        public Builder entity(EntityType<?> entity) {
            this.entity = entity;
            return this;
        }

        public Builder nbt(CompoundTag nbt) {
            this.nbt = nbt;
            return this;
        }

        public Builder offset(Vec3 offset) {
            this.offset = offset;
            return this;
        }

        public Builder offset(double x, double y, double z) {
            return this.offset(new Vec3(x, y, z));
        }

        public Builder count(NumberProvider count) {
            this.count = count;
            return this;
        }

        public Builder count(int count) {
            return this.count(ConstantValue.exactly(count));
        }

        public Builder chance(NumberProvider chance) {
            this.chance = chance;
            return this;
        }

        public Builder chance(float chance) {
            return this.chance(ConstantValue.exactly(chance));
        }

        public SpawnEntity build() {
            return new SpawnEntity(this.entity, this.nbt, this.offset, this.count, this.chance);
        }
    }
}
