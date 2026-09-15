package dev.anvilcraft.lib.v2.math.expression.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.math.expression.Arguments;
import dev.anvilcraft.lib.v2.math.expression.FunctionExpression;
import dev.anvilcraft.lib.v2.math.expression.IExpression;
import dev.anvilcraft.lib.v2.math.init.LibFunctionTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.Optional;

/**
 * 常量函数：不接收参数，求值恒为固定数字。
 *
 * <p>表达式树里的字面量数字就是它的零参调用，因此 {@code 2} 与
 * {@code {"function":{"type":"anvillib:constant","value":2}}} 是同一个表达式。</p>
 *
 * @param value 常量值
 */
public record ConstantFunction(double value) implements IFunction {
    /**
     * 常量在表达式里的内联形式就是一个数字。
     */
    public static final Codec<Double> CODEC = Codec.DOUBLE;
    public static final MapCodec<ConstantFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        ConstantFunction.CODEC
            .fieldOf("value")
            .forGetter(ConstantFunction::value)
    ).apply(ins, ConstantFunction::of));
    public static final StreamCodec<RegistryFriendlyByteBuf, ConstantFunction> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.DOUBLE,
        ConstantFunction::value,
        ConstantFunction::of
    );

    /**
     * 创建一个常量函数。
     */
    public static ConstantFunction of(double value) {
        return new ConstantFunction(value);
    }

    /**
     * 该常量的一次零参调用，也就是表达式树里的一个字面量。
     */
    public FunctionExpression call() {
        return FunctionExpression.of(this);
    }

    /**
     * 如果这次调用就是一次常量求值，返回它的值。
     */
    public static Optional<Double> value(FunctionExpression expression) {
        if (!expression.arguments().isEmpty()) return Optional.empty();
        if (!(expression.function().value() instanceof ConstantFunction(double value1))) return Optional.empty();
        return Optional.of(value1);
    }

    @Override
    public double apply(List<IExpression> arguments, Arguments inputs) {
        return this.value;
    }

    @Override
    public IFunction.Type<? extends IFunction> type() {
        return IFunction.typeOf(LibFunctionTypes.CONSTANT.getKey());
    }

    public static class Type implements IFunction.Type<ConstantFunction> {
        @Override
        public MapCodec<ConstantFunction> codec() {
            return ConstantFunction.MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ConstantFunction> streamCodec() {
            return ConstantFunction.STREAM_CODEC;
        }

        @Override
        public String getSerializedName() {
            return "constant";
        }
    }
}
