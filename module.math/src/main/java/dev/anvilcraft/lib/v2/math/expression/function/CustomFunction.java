package dev.anvilcraft.lib.v2.math.expression.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import dev.anvilcraft.lib.v2.math.expression.IExpression;
import dev.anvilcraft.lib.v2.math.expression.NumberArguments;
import dev.anvilcraft.lib.v2.math.init.LibFunctionTypes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据包自定义函数：声明若干参数，并用一段表达式作为函数体。
 *
 * <p>函数体里用 {@code $(name)} 引用参数，调用时各参数按声明顺序绑定到实参的求值结果，
 * 因此调用点不必再提供同名的传入值。</p>
 *
 * <pre>{@code
 * // data/<namespace>/anvillib/function/triple.json
 * {
 *   "type": "anvillib:custom",
 *   "parameters": ["value"],
 *   "body": "$(value)*3"
 * }
 * }</pre>
 *
 * @param parameters 参数名，按声明顺序绑定实参
 * @param body       函数体表达式，用 {@code $(name)} 引用参数
 */
public record CustomFunction(List<String> parameters, IExpression body) implements IFunction {
    public static final MapCodec<CustomFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Codec.STRING
            .listOf()
            .fieldOf("parameters")
            .forGetter(CustomFunction::parameters),
        IExpression.CODEC
            .fieldOf("body")
            .forGetter(CustomFunction::body)
    ).apply(ins, CustomFunction::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, CustomFunction> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
        CustomFunction::parameters,
        IExpression.STREAM_CODEC,
        CustomFunction::body,
        CustomFunction::new
    );

    public CustomFunction {
        parameters = List.copyOf(parameters);
        Set<String> seen = new HashSet<>(parameters.size());
        for (String parameter : parameters) {
            if (parameter.isEmpty()) {
                throw new IllegalArgumentException("Custom function parameter name cannot be empty");
            }
            if (!seen.add(parameter)) {
                throw new IllegalArgumentException("Duplicate custom function parameter '" + parameter + "'");
            }
        }
    }

    /**
     * 创建一个自定义函数。
     */
    public static CustomFunction of(List<String> parameters, IExpression body) {
        return new CustomFunction(parameters, body);
    }

    @Override
    public double apply(List<Double> arguments, NumberArguments inputs) {
        NumberArguments bound = inputs;
        for (int index = 0; index < this.parameters.size() && index < arguments.size(); index++) {
            bound = bound.with(this.parameters.get(index), arguments.get(index));
        }
        return this.body.evaluate(bound);
    }

    @Override
    public IFunction.Type<? extends IFunction> type() {
        return LibFunctionTypes.CUSTOM.get();
    }

    public static class Type implements IFunction.Type<CustomFunction> {
        @Override
        public MapCodec<CustomFunction> codec() {
            return CustomFunction.MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CustomFunction> streamCodec() {
            return CustomFunction.STREAM_CODEC;
        }

        @Override
        public String getSerializedName() {
            return "custom";
        }
    }
}
