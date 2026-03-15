package dev.anvilcraft.lib.v2.multiblock.definition;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.recipe.component.BlockStatePredicate;
import dev.anvilcraft.lib.v2.recipe.util.CodecUtil;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectMaps;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record DefinitionSerializer(@Unmodifiable String[][] structure, @Unmodifiable Char2ObjectMap<BlockStatePredicate> definitions) {
    public static final MapCodec<DefinitionSerializer> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Codec.STRING
            .listOf()
            .listOf()
            .xmap(DefinitionSerializer::toArrays, DefinitionSerializer::toLists)
            .fieldOf("structure")
            .forGetter(DefinitionSerializer::structure),
        Codec.unboundedMap(CodecUtil.CHAR_CODEC, BlockStatePredicate.CODEC)
            .xmap(DefinitionSerializer::toC2OMap, Function.identity())
            .fieldOf("definitions")
            .forGetter(DefinitionSerializer::definitions)
    ).apply(ins, DefinitionSerializer::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, DefinitionSerializer> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8
            .apply(ByteBufCodecs.list())
            .apply(ByteBufCodecs.list())
            .map(DefinitionSerializer::toArrays, DefinitionSerializer::toLists),
        DefinitionSerializer::structure,
        ByteBufCodecs.<RegistryFriendlyByteBuf, Character, BlockStatePredicate, Map<Character, BlockStatePredicate>>map(
            HashMap::new,
            CodecUtil.CHAR_STREAM_CODEC,
            BlockStatePredicate.STREAM_CODEC
        ).map(DefinitionSerializer::toC2OMap, Function.identity()),
        DefinitionSerializer::definitions,
        DefinitionSerializer::new
    );

    MultiblockDefinition toDefinition() {
        Vec3i controllerOffset = this.getControllerOffset();
        int sizeX = this.structure[0][0].length();
        int sizeZ = this.structure[0].length;
        int sizeXZ = sizeX * sizeZ;
        int sizeY = this.structure.length;
        int sizeXYZ = sizeXZ * sizeY;
        ImmutableList.Builder<MultiblockPosInfo> structure = ImmutableList.builder();
        int cursor = 0;
        while (cursor < sizeXYZ) {
            int indexX = cursor % sizeX;
            int indexY = cursor / sizeXZ;
            int indexZ = (cursor % sizeXZ) / sizeX;

            char key = this.getChar(indexX, indexY, indexZ);
            if (this.isEmpty(key)) {
                cursor++;
                continue;
            }

            // 找到非空位置，计算世界坐标
            int worldX = indexX - controllerOffset.getX();
            int worldY = sizeY - 1 - indexY - controllerOffset.getY();
            int worldZ = indexZ - controllerOffset.getZ();
            cursor++;
            structure.add(new MultiblockPosInfo(
                key,
                new BlockPos(worldX, worldY, worldZ),
                new BlockPos(indexX, indexY, indexZ)
            ));
        }
        return new MultiblockDefinition(structure.build(), this.definitions, sizeX, sizeY, sizeZ);
    }

    static DefinitionSerializer fromDefinition(MultiblockDefinition definition) {
        String[][] structure = new String[definition.sizeY()][definition.sizeZ()];
        for (int y = 0; y < definition.sizeY(); y++) {
            for (int z = 0; z < definition.sizeZ(); z++) {
                char[] xs = new char[definition.sizeX()];
                Arrays.fill(xs, ' ');
                for (MultiblockPosInfo pos : definition.structure()) {
                    if (pos.pos().getY() != y || pos.pos().getZ() != z) continue;
                    xs[pos.pos().getX()] = pos.key();
                }
                structure[y][z] = new String(xs);
            }
        }
        return new DefinitionSerializer(structure, definition.definitions());
    }

    public char getChar(int x, int y, int z) {
        return this.structure[y][z].charAt(x);
    }

    public boolean isEmpty(char key) {
        return key == Character.MIN_VALUE;
    }

    private Vec3i getControllerOffset() {
        for (int y = 0; y < this.structure.length; y++) {
            String[] xz = this.structure[y];
            for (int z = 0; z < xz.length; z++) {
                String xs = xz[z];
                for (int x = 0; x < xs.length(); x++) {
                    if (xs.charAt(x) != MultiblockDefinition.CONTROLLER) continue;
                    return new Vec3i(x, y, z);
                }
            }
        }
        throw new IllegalStateException("Unexpected no controller in structure");
    }

    static @Unmodifiable String[][] toArrays(List<List<String>> lists) {
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
