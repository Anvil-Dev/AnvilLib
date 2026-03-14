package dev.anvilcraft.lib.v2.multiblock;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.recipe.component.BlockStatePredicate;
import dev.anvilcraft.lib.v2.recipe.util.CodecUtil;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectMaps;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 多方块定义类
 */
@Getter
@Accessors(fluent = true, chain = false)
public class MultiblockDefinition {
    public static final char CONTROLLER = '0';
    public static final MapCodec<MultiblockDefinition> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Codec.STRING
            .listOf()
            .listOf()
            .xmap(MultiblockDefinition::toArrays, MultiblockDefinition::toLists)
            .fieldOf("structure")
            .forGetter(MultiblockDefinition::structure),
        Codec.unboundedMap(CodecUtil.CHAR_CODEC, BlockStatePredicate.CODEC)
            .xmap(MultiblockDefinition::toC2OMap, Function.identity())
            .fieldOf("definitions")
            .forGetter(MultiblockDefinition::definitions)
    ).apply(ins, MultiblockDefinition::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, MultiblockDefinition> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8
            .apply(ByteBufCodecs.list())
            .apply(ByteBufCodecs.list())
            .map(MultiblockDefinition::toArrays, MultiblockDefinition::toLists),
        MultiblockDefinition::structure,
        ByteBufCodecs.<RegistryFriendlyByteBuf, Character, BlockStatePredicate, Map<Character, BlockStatePredicate>>map(
            HashMap::new,
            CodecUtil.CHAR_STREAM_CODEC,
            BlockStatePredicate.STREAM_CODEC
        ).map(MultiblockDefinition::toC2OMap, Function.identity()),
        MultiblockDefinition::definitions,
        MultiblockDefinition::new
    );
    private final @Unmodifiable String[][] structure;
    private final @Unmodifiable Char2ObjectMap<BlockStatePredicate> definitions;
    @Getter(AccessLevel.NONE)
    private Vec3i controllerOffset;
    private Iterable<BlockPos> localPoses;

    /**
     * 构建多方块定义类
     *
     * @param structure 结构，按 {@code String(x)[reversed y][z]} 的顺序定义
     * @param definitions 定义，记录了字符与方块状态的对应情况。
     */
    private MultiblockDefinition(
        @Unmodifiable String[][] structure,
        @Unmodifiable Char2ObjectMap<BlockStatePredicate> definitions
    ) {
        this.structure = structure;
        this.definitions = definitions;
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
        return this.structure[y][z].charAt(x);
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

    public Vec3i getControllerOffset() {
        if (this.controllerOffset != null) return this.controllerOffset;
        for (int y = 0; y < this.structure.length; y++) {
            String[] xz = this.structure[y];
            for (int z = 0; z < xz.length; z++) {
                String xs = xz[z];
                for (int x = 0; x < xs.length(); x++) {
                    if (xs.charAt(x) != MultiblockDefinition.CONTROLLER) continue;
                    return this.controllerOffset = new Vec3i(x, y, z);
                }
            }
        }
        throw new IllegalStateException("Unexpected no controller in structure");
    }

    public Iterable<BlockPos> getGlobalPoses(BlockPos controllerPos) {
        return () -> new AbstractIterator<>() {
            private final Iterator<BlockPos> localPoses = MultiblockDefinition.this.getLocalPoses().iterator();

            @Override
            protected @Nullable BlockPos computeNext() {
                BlockPos pos = this.localPoses.next();
                if (pos == null) return this.endOfData();
                return pos.offset(controllerPos);
            }
        };
    }

    public Iterable<BlockPos> getLocalPoses() {
        if (this.localPoses != null) return this.localPoses;
        Vec3i controllerOffset = this.getControllerOffset();
        int sizeX = this.structure[0][0].length();
        int sizeZ = this.structure[0].length;
        int sizeXZ = sizeX * sizeZ;
        int sizeY = this.structure.length;
        int sizeXYZ = sizeXZ * sizeY;
        return this.localPoses = () -> new AbstractIterator<>() {
            private final BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
            private int cursor = 0;

            @Override
            protected @Nullable BlockPos computeNext() {
                while (this.cursor < sizeXYZ) {
                    int indexX = this.cursor % sizeX;
                    int indexY = this.cursor / sizeXZ;
                    int indexZ = (this.cursor % sizeXZ) / sizeX;

                    if (MultiblockDefinition.this.isEmpty(indexX, indexY, indexZ)) {
                        this.cursor++;
                        continue;
                    }

                    // 找到非空位置，计算世界坐标
                    int worldX = indexX - controllerOffset.getX();
                    int worldY = sizeY - 1 - indexY - controllerOffset.getY();
                    int worldZ = indexZ - controllerOffset.getZ();
                    this.cursor++;
                    return this.mut.set(worldX, worldY, worldZ);
                }
                return this.endOfData();
            }
        };
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
            return new MultiblockDefinition(
                MultiblockDefinition.toArrays(this.structure),
                Char2ObjectMaps.unmodifiable(this.definitions)
            );
        }
    }

    // 工具方法

    private static @Unmodifiable String[][] toArrays(List<List<String>> lists) {
        int size = lists.size();
        String[][] result = new String[size][];
        for (int i = 0; i < size; i++) {
            result[i] = lists.get(i).toArray(String[]::new);
        }
        return result;
    }

    private static @Unmodifiable List<List<String>> toLists(String[][] arrays) {
        List<List<String>> result = new ArrayList<>();
        for (String[] array : arrays) {
            result.add(List.of(array));
        }
        return ImmutableList.copyOf(result);
    }

    private static @Unmodifiable Char2ObjectMap<BlockStatePredicate> toC2OMap(Map<Character, BlockStatePredicate> map) {
        return Char2ObjectMaps.unmodifiable(new Char2ObjectOpenHashMap<>(map));
    }
}
