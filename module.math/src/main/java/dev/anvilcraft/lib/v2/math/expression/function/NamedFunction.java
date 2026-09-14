package dev.anvilcraft.lib.v2.math.expression.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import dev.anvilcraft.lib.v2.math.expression.FunctionExpression;
import dev.anvilcraft.lib.v2.math.expression.NumberArguments;
import dev.anvilcraft.lib.v2.math.init.LibFunctionTypes;

import java.util.List;

/**
 * 具名传入值函数，按名字引用 {@link NumberArguments} 中的传入值。
 *
 * <p>flat 表达式里写 {@code $(name)}，函数体内用参数名引用调用点提供的值时通常走这条路径。</p>
 *
 * @param name 传入值名字
 */
public record NamedFunction(String name) implements IFunction {
    public static final MapCodec<NamedFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Codec.STRING
            .fieldOf("name")
            .forGetter(NamedFunction::name)
    ).apply(ins, NamedFunction::of));
    public static final StreamCodec<RegistryFriendlyByteBuf, NamedFunction> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,
        NamedFunction::name,
        NamedFunction::of
    );

    /**
     * 创建一个按名字取值的传入值函数。
     */
    public static NamedFunction of(String name) {
        return new NamedFunction(name);
    }

    /**
     * 该具名传入值的一次零参调用。
     */
    public static FunctionExpression call(String name) {
        return FunctionExpression.of(NamedFunction.of(name));
    }

    @Override
    public double apply(List<Double> arguments, NumberArguments inputs) {
        return inputs.value(this.name);
    }

    @Override
    public IFunction.Type<? extends IFunction> type() {
        return LibFunctionTypes.NAMED.get();
    }

    public static class Type implements IFunction.Type<NamedFunction> {
        @Override
        public MapCodec<NamedFunction> codec() {
            return NamedFunction.MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, NamedFunction> streamCodec() {
            return NamedFunction.STREAM_CODEC;
        }

        @Override
        public String getSerializedName() {
            return "named";
        }
    }
}
