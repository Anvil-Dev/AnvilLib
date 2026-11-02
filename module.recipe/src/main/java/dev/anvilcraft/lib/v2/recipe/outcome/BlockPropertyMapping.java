package dev.anvilcraft.lib.v2.recipe.outcome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;
import java.util.Objects;

/**
 * 从源方块的一个属性读取值，并可选地转换为目标属性的值。
 * 空转换表表示按序列化名称直接复制；非空表中未列出的值不改变目标状态。
 */
public record BlockPropertyMapping(String source, Map<String, String> values) {
    public static final Codec<BlockPropertyMapping> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("source").forGetter(BlockPropertyMapping::source),
        Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("values", Map.of())
            .forGetter(BlockPropertyMapping::values)
    ).apply(instance, BlockPropertyMapping::new));

    public BlockPropertyMapping {
        if (Objects.requireNonNull(source).isBlank()) throw new IllegalArgumentException("Source property must not be blank");
        values = Map.copyOf(values);
    }

    public BlockPropertyMapping(String source) {
        this(source, Map.of());
    }

    /** 全部映射均读取传入的源快照，不读取其他映射的结果。 */
    public BlockState apply(BlockState input, BlockState output, String target) {
        Property<?> property = input.getBlock().getStateDefinition().getProperty(this.source);
        if (property == null) return output;
        String sourceValue = name(input, property);
        String value = this.values.isEmpty() ? sourceValue : this.values.get(sourceValue);
        return value == null ? output : set(output, target, value);
    }

    static <T extends Comparable<T>> String name(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    static BlockState set(BlockState state, String name, String value) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        return property == null ? state : set(state, property, value);
    }

    private static <T extends Comparable<T>> BlockState set(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(parsed -> state.setValue(property, parsed)).orElse(state);
    }
}
