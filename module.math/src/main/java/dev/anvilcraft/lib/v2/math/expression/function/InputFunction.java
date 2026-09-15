package dev.anvilcraft.lib.v2.math.expression.function;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.math.expression.Arguments;
import dev.anvilcraft.lib.v2.math.expression.FunctionExpression;
import dev.anvilcraft.lib.v2.math.expression.IExpression;
import dev.anvilcraft.lib.v2.math.init.LibFunctionTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

import java.util.List;

/**
 * 传入值函数，按下标引用 {@link Arguments} 中的传入值。
 *
 * <p>flat 表达式里的 {@code x}/{@code y}/{@code z} 与 {@code x0}… 都解析为对它的调用。</p>
 *
 * @param index 传入值下标，越界时取 0
 */
public record InputFunction(int index) implements IFunction {
    public static final MapCodec<InputFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        ExtraCodecs.NON_NEGATIVE_INT
            .fieldOf("index")
            .forGetter(InputFunction::index)
    ).apply(ins, InputFunction::of));
    public static final StreamCodec<RegistryFriendlyByteBuf, InputFunction> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        InputFunction::index,
        InputFunction::of
    );

    /**
     * 创建一个按下标取值的传入值函数。
     */
    public static InputFunction of(int index) {
        return new InputFunction(index);
    }

    /**
     * 该传入值的一次零参调用。
     */
    public static FunctionExpression call(int index) {
        return FunctionExpression.of(InputFunction.of(index));
    }

    @Override
    public double apply(List<IExpression> arguments, Arguments inputs) {
        return inputs.value(this.index);
    }

    @Override
    public IFunction.Type<? extends IFunction> type() {
        return IFunction.typeOf(LibFunctionTypes.INPUT.getKey());
    }

    public static class Type implements IFunction.Type<InputFunction> {
        @Override
        public MapCodec<InputFunction> codec() {
            return InputFunction.MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, InputFunction> streamCodec() {
            return InputFunction.STREAM_CODEC;
        }

        @Override
        public String getSerializedName() {
            return "input";
        }
    }
}
