package dev.anvilcraft.lib.v2.multiblock.dynamic.definition;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public record MultiblockDefinition(@Unmodifiable Map<Vec3i, BlockStatePredicate> definition) {
    public static final MapCodec<MultiblockDefinition> CODEC = DefinitionSerialization.CODEC.xmap(
        DefinitionSerialization::toDefinition,
        DefinitionSerialization::fromDefinition
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, MultiblockDefinition> STREAM_CODEC = DefinitionSerialization.STREAM_CODEC.map(
        DefinitionSerialization::toDefinition,
        DefinitionSerialization::fromDefinition
    );

    public MultiblockDefinition(Map<Vec3i, BlockStatePredicate> definition) {
        this.definition = definition;
    }
    
    public static Builder builder() {
        return new Builder();
    }

    public static SeriaBuilder seriaBuilder() {
        return new SeriaBuilder();
    }

    public @Unmodifiable Map<BlockPos, BlockStatePredicate> toGlobal(BlockPos centerPos) {
        ImmutableMap.Builder<BlockPos, BlockStatePredicate> global = ImmutableMap.builder();
        for (Map.Entry<Vec3i, BlockStatePredicate> entry : this.definition.entrySet()) {
            global.put(centerPos.immutable().offset(entry.getKey()), entry.getValue());
        }
        return global.build();
    }

    public boolean isController(LevelAccessor level, BlockState state, @Nullable BlockEntity entity) {
        return this.definition.get(Vec3i.ZERO).test(level, state, entity);
    }

    public static class Builder {
        private final ImmutableMap.Builder<Vec3i, BlockStatePredicate> definition = ImmutableMap.builder();

        public Builder() {
        }

        public Builder add(Vec3i localPos, BlockStatePredicate.Builder predicate) {
            this.definition.put(localPos, predicate.build());
            return this;
        }

        public Builder add(Vec3i localPos, Block block) {
            return this.add(localPos, BlockStatePredicate.builder().of(block));
        }

        public Builder add(Vec3i localPos, CompoundTag tag) {
            return this.add(localPos, BlockStatePredicate.builder().nbt(tag));
        }

        public Builder add(Vec3i localPos, Block block, CompoundTag tag) {
            return this.add(localPos, BlockStatePredicate.builder().of(block).nbt(tag));
        }

        public Builder addController(BlockStatePredicate.Builder predicate) {
            this.definition.put(Vec3i.ZERO, predicate.build());
            return this;
        }

        public Builder addController(Block block) {
            return this.addController(BlockStatePredicate.builder().of(block));
        }

        public Builder addController(CompoundTag tag) {
            return this.addController(BlockStatePredicate.builder().nbt(tag));
        }

        public Builder addController(Block block, CompoundTag tag) {
            return this.addController(BlockStatePredicate.builder().of(block).nbt(tag));
        }

        public MultiblockDefinition build() {
            return new MultiblockDefinition(this.definition.build());
        }
    }
    
    public static class SeriaBuilder {
        private final List<String[]> grid = new ArrayList<>();
        private final Char2ObjectMap<BlockStatePredicate> mapping = new Char2ObjectOpenHashMap<>();
        
        public SeriaBuilder() {
        }
        
        public SeriaBuilder layer(String... layer) {
            this.grid.add(layer);
            return this;
        }

        public SeriaBuilder map(char key, BlockStatePredicate.Builder predicate) {
            this.mapping.put(key, predicate.build());
            return this;
        }

        public SeriaBuilder map(char key, Block block) {
            return this.map(key, BlockStatePredicate.builder().of(block));
        }

        public SeriaBuilder map(char key, CompoundTag tag) {
            return this.map(key, BlockStatePredicate.builder().nbt(tag));
        }

        public SeriaBuilder map(char key, Block block, CompoundTag tag) {
            return this.map(key, BlockStatePredicate.builder().of(block).nbt(tag));
        }

        public SeriaBuilder mapController(BlockStatePredicate.Builder predicate) {
            return this.map('0', predicate);
        }

        public SeriaBuilder mapController(Block block) {
            return this.mapController(BlockStatePredicate.builder().of(block));
        }

        public SeriaBuilder mapController(CompoundTag tag) {
            return this.mapController(BlockStatePredicate.builder().nbt(tag));
        }

        public SeriaBuilder mapController(Block block, CompoundTag tag) {
            return this.mapController(BlockStatePredicate.builder().of(block).nbt(tag));
        }

        public MultiblockDefinition build() {
            int size = this.grid.size();
            String[][] result = new String[size][this.grid.getFirst().length];
            for (int i = 0; i < size; i++) {
                result[i] = this.grid.get(i);
            }
            return new DefinitionSerialization(result, this.mapping).toDefinition();
        }
    }
}
