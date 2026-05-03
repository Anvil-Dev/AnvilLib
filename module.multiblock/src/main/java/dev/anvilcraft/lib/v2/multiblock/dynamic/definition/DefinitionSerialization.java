package dev.anvilcraft.lib.v2.multiblock.dynamic.definition;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectMaps;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2CharArrayMap;
import it.unimi.dsi.fastutil.objects.Object2CharMap;
import net.minecraft.core.Vec3i;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

record DefinitionSerialization(String[][] grid, Char2ObjectMap<BlockStatePredicate> mapping) {
    static final MapCodec<DefinitionSerialization> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        Codec.STRING
            .listOf()
            .listOf()
            .xmap(DefinitionSerialization::toArray, DefinitionSerialization::toList)
            .fieldOf("grid")
            .forGetter(DefinitionSerialization::grid),
        Codec.unboundedMap(CodecUtil.CHAR, BlockStatePredicate.CODEC)
            .xmap(DefinitionSerialization::toC2OMap, Function.identity())
            .fieldOf("mapping")
            .forGetter(DefinitionSerialization::mapping)
    ).apply(inst, DefinitionSerialization::new));
    static final StreamCodec<RegistryFriendlyByteBuf, DefinitionSerialization> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8
            .apply(ByteBufCodecs.list())
            .apply(ByteBufCodecs.list())
            .map(DefinitionSerialization::toArray, DefinitionSerialization::toList),
        DefinitionSerialization::grid,
        ByteBufCodecs.<RegistryFriendlyByteBuf, Character, BlockStatePredicate, Map<Character, BlockStatePredicate>>map(
            HashMap::new,
            StreamCodecUtil.CHAR,
            BlockStatePredicate.STREAM_CODEC
        ).map(DefinitionSerialization::toC2OMap, Function.identity()),
        DefinitionSerialization::mapping,
        DefinitionSerialization::new
    );

    private Vec3i findControllerPos() {
        for (int y = 0; y < grid.length; y++) {
            String[] layer = grid[y];
            for (int z = 0; z < layer.length; z++) {
                String xs = layer[z];
                for (int x = 0; x < xs.length(); x++) {
                    if (xs.charAt(x) == '0') return new Vec3i(x, y, z);
                }
            }
        }
        return Vec3i.ZERO;
    }

    MultiblockDefinition toDefinition() {
        Vec3i offset = this.findControllerPos();
        ImmutableMap.Builder<Vec3i, BlockStatePredicate> definition = ImmutableMap.builder();
        String[][] grid = this.grid;
        for (int y = 0; y < grid.length; y++) {
            String[] layer = grid[y];
            for (int z = 0; z < layer.length; z++) {
                String xs = layer[z];
                for (int x = 0; x < xs.length(); x++) {
                    char key = xs.charAt(x);
                    if (key == ' ') continue;
                    BlockStatePredicate predicate = this.mapping.get(key);
                    if (predicate == null) throw new IllegalArgumentException("Undefined key '" + key + "' found");
                    Vec3i localPos = new Vec3i(x, y, z).subtract(offset);
                    definition.put(localPos, predicate);
                }
            }
        }
        return new MultiblockDefinition(definition.build());
    }

    static DefinitionSerialization fromDefinition(MultiblockDefinition definition) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        char currentKey = 'A';
        Object2CharMap<Vec3i> keyMapping = new Object2CharArrayMap<>();
        BiMap<Character, BlockStatePredicate> mapping = HashBiMap.create();
        for (Map.Entry<Vec3i, BlockStatePredicate> entry : definition.definition().entrySet()) {
            Vec3i localPos = entry.getKey();
            if (localPos.getX() < minX) minX = localPos.getX();
            if (localPos.getY() < minY) minY = localPos.getY();
            if (localPos.getZ() < minZ) minZ = localPos.getZ();
            if (localPos.getX() > maxX) maxX = localPos.getX();
            if (localPos.getY() > maxY) maxY = localPos.getY();
            if (localPos.getZ() > maxZ) maxZ = localPos.getZ();

            BlockStatePredicate predicate = entry.getValue();
            if (localPos.equals(Vec3i.ZERO)) {
                mapping.put('0', predicate);
            }
            if (!mapping.containsValue(predicate)) {
                mapping.put(currentKey, predicate);
                currentKey = DefinitionSerialization.findNewKey(currentKey);
            }
            keyMapping.put(localPos, mapping.inverse().get(predicate).charValue());
        }
        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;
        String[][] grid = new String[sizeY][sizeZ];
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                char[] xs = new char[sizeX];
                Arrays.fill(xs, ' ');
                for (Vec3i localPos : keyMapping.keySet()) {
                    if (localPos.getY() != y || localPos.getZ() != z) continue;
                    xs[localPos.getX() - minX] = keyMapping.getChar(localPos);
                }
                grid[y - minY][z - minZ] = String.valueOf(xs);
            }
        }
        return new DefinitionSerialization(grid, DefinitionSerialization.toC2OMap(mapping));
    }

    private static char findNewKey(char currentKey) {
        if (currentKey == 'Z') { // 大写字母用完
            currentKey = 'a';
        } else if (currentKey == 'z') { // 小写字母用完
            currentKey = '1';
        } else if (currentKey == '9') { // 数字用完
            currentKey = ']';
        } else if (currentKey == '`') {
            currentKey = '{';
        } else if (currentKey == '~') {
            currentKey = ':';
        } else if (currentKey == '@') {
            currentKey = '#';
        } else if (currentKey == '/') { // 除[和\的符号用完
            currentKey = '一';
        } else if (currentKey == '龥') { // 基本汉字用完?!
            throw new IllegalStateException("The number of characters has reached the maximum limit. How did you get here?");
        } else {
            currentKey++;
        }
        return currentKey;
    }

    private static List<List<String>> toList(String[][] grid) {
        ImmutableList.Builder<List<String>> builder = ImmutableList.builder();
        for (String[] layers : grid) {
            builder.add(layers == null ? List.of() : List.of(layers));
        }
        return builder.build();
    }

    private static String[][] toArray(List<List<String>> grid) {
        int size = grid.size();
        String[][] result = new String[size][];
        for (int i = 0; i < size; i++) {
            result[i] = grid.get(i).toArray(String[]::new);
        }
        return result;
    }

    private static Char2ObjectMap<BlockStatePredicate> toC2OMap(Map<Character, BlockStatePredicate> map) {
        return Char2ObjectMaps.unmodifiable(new Char2ObjectOpenHashMap<>(map));
    }
}
