package dev.anvilcraft.lib.v2.multiblock.definition;

import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.recipe.component.BlockStatePredicate;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectMaps;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;

/**
 * 多方块定义类
 */
@Getter
@Accessors(fluent = true, chain = false)
public class MultiblockDefinition {
    public static final char CONTROLLER = '0';
    public static final MapCodec<MultiblockDefinition> CODEC = DefinitionSerializer.CODEC.xmap(
        DefinitionSerializer::toDefinition,
        DefinitionSerializer::fromDefinition
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, MultiblockDefinition> STREAM_CODEC = DefinitionSerializer.STREAM_CODEC.map(
        DefinitionSerializer::toDefinition,
        DefinitionSerializer::fromDefinition
    );
    private final @Unmodifiable List<MultiblockPosInfo> structure;
    private final @Unmodifiable Char2ObjectMap<BlockStatePredicate> definitions;
    private final int sizeX, sizeY, sizeZ;

    /**
     * 构建多方块定义类
     *
     * @param structure 结构
     * @param definitions 定义，记录了字符与方块状态的对应情况。
     */
    MultiblockDefinition(
        @Unmodifiable List<MultiblockPosInfo> structure,
        @Unmodifiable Char2ObjectMap<BlockStatePredicate> definitions,
        int sizeX,
        int sizeY,
        int sizeZ
    ) {
        this.structure = structure;
        this.definitions = definitions;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean test(LevelAccessor level, Vec3i localPos, BlockState state, @Nullable BlockEntity entity) {
        return this.getState(localPos).test(level, state, entity);
    }

    public BlockStatePredicate getState(Vec3i localPos) {
        return this.definitions.get(this.getChar(localPos));
    }

    public char getChar(Vec3i localPos) {
        return this.getChar(localPos.getX(), localPos.getY(), localPos.getZ());
    }

    public char getChar(int x, int y, int z) {
        for (MultiblockPosInfo pos : this.structure) {
            if (pos.pos().getX() == x && pos.pos().getY() == y && pos.pos().getZ() == z) return pos.key();
        }
        return Character.MIN_VALUE;
    }

    public boolean isEmpty(char key) {
        return key == Character.MIN_VALUE;
    }

    public boolean isEmpty(Vec3i localPos) {
        return this.isEmpty(this.getChar(localPos));
    }

    public boolean isEmpty(int x, int y, int z) {
        return this.isEmpty(this.getChar(x, y, z));
    }

    public static class Builder {
        private final List<List<String>> structure = new ArrayList<>();
        private final Char2ObjectMap<BlockStatePredicate> definitions = new Char2ObjectOpenHashMap<>();

        public Builder() {
        }

        public Builder layer(String... layer) {
            this.structure.add(List.of(layer));
            return this;
        }

        public Builder defineController(BlockStatePredicate.Builder state) {
            this.definitions.put(MultiblockDefinition.CONTROLLER, state.build());
            return this;
        }

        public Builder define(char key, BlockStatePredicate.Builder state) {
            this.definitions.put(key, state.build());
            return this;
        }

        public void validate(String dmbId) {
            if (!this.definitions.containsKey(MultiblockDefinition.CONTROLLER)) {
                throw new IllegalArgumentException(
                    "Unlinked controller key %s in DMB id %s"
                        .formatted(MultiblockDefinition.CONTROLLER, dmbId)
                );
            }

            int sizeX = -1;
            int sizeZ = -1;
            boolean checkedController = false;
            for (int indexY = 0; indexY < this.structure.size(); indexY++) {
                List<String> layer = this.structure.get(indexY);
                if (sizeZ == -1) sizeZ = layer.size();
                if (sizeZ != layer.size()) {
                    throw new IllegalArgumentException(
                        "Inconsistent z-width was found in layer %d, DMB id %s. Need %d, found %d"
                            .formatted(indexY, dmbId, sizeZ, layer.size())
                    );
                }

                for (String xs : layer) {
                    for (int indexX = 0; indexX < xs.length(); indexX++) {
                        char key = xs.charAt(indexX);
                        if (sizeX == -1) sizeX = xs.length();
                        if (sizeX != xs.length()) {
                            throw new IllegalArgumentException(
                                "Inconsistent x-width was found in layer %d, DMB id %s. Need %d, found %d"
                                    .formatted(indexY, dmbId, sizeX, xs.length())
                            );
                        }
                        if (key == MultiblockDefinition.CONTROLLER) {
                            if (!checkedController) {
                                checkedController = true;
                                continue;
                            } else {
                                throw new IllegalArgumentException(
                                    "Multiple center key (%s) found in DMB id %s"
                                        .formatted(MultiblockDefinition.CONTROLLER, dmbId)
                                );
                            }
                        }
                        if (!this.definitions.containsKey(key)) {
                            throw new IllegalArgumentException(
                                "Unlinked key %s in DMB id %s"
                                    .formatted(key, dmbId)
                            );
                        }
                    }
                }
            }

            if (!checkedController) {
                throw new IllegalArgumentException(
                    "Undefined controller key (%s) in DMB id %s"
                        .formatted(MultiblockDefinition.CONTROLLER, dmbId)
                );
            }
        }

        public Holder.Reference<MultiblockDefinition> register(
            BootstrapContext<MultiblockDefinition> context,
            ResourceKey<MultiblockDefinition> key
        ) {
            this.validate(key.location().toString());
            return context.register(key, this.build());
        }

        public Holder.Reference<MultiblockDefinition> register(
            BootstrapContext<MultiblockDefinition> context,
            ResourceKey<MultiblockDefinition> key,
            Lifecycle registryLifecycle
        ) {
            this.validate(key.location().toString());
            return context.register(key, this.build(), registryLifecycle);
        }

        public Holder<MultiblockDefinition> create() {
            this.validate("<unregistered>");
            return Holder.direct(this.build());
        }

        public MultiblockDefinition build() {
            return new DefinitionSerializer(
                DefinitionSerializer.toArrays(this.structure),
                Char2ObjectMaps.unmodifiable(this.definitions)
            ).toDefinition();
        }
    }
}
