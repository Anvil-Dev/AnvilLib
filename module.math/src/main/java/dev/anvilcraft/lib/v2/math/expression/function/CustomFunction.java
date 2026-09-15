package dev.anvilcraft.lib.v2.math.expression.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.math.expression.Arguments;
import dev.anvilcraft.lib.v2.math.expression.IExpression;
import dev.anvilcraft.lib.v2.math.init.LibFunctionTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * 数据包自定义函数：声明若干形参，并用一段表达式作为函数体。
 *
 * <p>函数体里用 {@code $(name)} 引用形参，调用时各形参按声明顺序绑定到实参的求值结果，
 * 因此调用点不必再提供同名的传入值。</p>
 *
 * <p>形参名以 {@code ...} 结尾是变参，例如 {@code ["a", "x..."]}：{@code a} 绑第一个实参，{@code x}
 * 绑住剩下的全部。变参在函数体里取不到单个值，直接用 {@code $(x)} 得到的是整串实参里的最大值，
 * 要逐个处理得把它交给 {@code forEach} 之类的函数；函数体自己拆不开变参。</p>
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
 * @param parameters 形参声明，以 {@code ...} 结尾的是变参
 * @param body       函数体表达式，用 {@code $(name)} 引用形参
 */
public record CustomFunction(Parameters parameters, IExpression body) implements IFunction {
    public static final MapCodec<CustomFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Codec.STRING
            .listOf()
            .fieldOf("parameters")
            .forGetter(CustomFunction::declarations),
        IExpression.CODEC
            .fieldOf("body")
            .forGetter(CustomFunction::body)
    ).apply(ins, CustomFunction::of));
    public static final StreamCodec<RegistryFriendlyByteBuf, CustomFunction> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
        CustomFunction::declarations,
        IExpression.STREAM_CODEC,
        CustomFunction::body,
        CustomFunction::of
    );

    /**
     * 按声明文本创建，{@code "x..."} 是变参。
     */
    public static CustomFunction of(List<String> declarations, IExpression body) {
        return new CustomFunction(Parameters.parse(declarations), body);
    }

    /**
     * 按形参名创建，全部当作固定形参。
     */
    public static CustomFunction named(List<String> names, IExpression body) {
        return new CustomFunction(Parameters.of(names), body);
    }

    /**
     * 形参声明文本，变参带 {@code ...}，用于编解码。
     */
    public List<String> declarations() {
        return this.parameters.declarations();
    }

    @Override
    public double apply(List<IExpression> arguments, Arguments inputs) {
        // 实参在调用点上下文里求值，函数体再换成形参绑定：这样函数体既能读到形参，也能读到调用点的名字
        IFunction.Call call = IFunction.bind(arguments, inputs, this.parameters);
        return this.guarded("parameters " + this.declarations(), () -> this.body.evaluate(call.bound()));
    }

    /**
     * 形参声明，函数体正是靠它来解析 {@code $(name)}。
     */
    @Override
    public Parameters parameters() {
        return this.parameters;
    }

    @Override
    public IFunction.Type<? extends IFunction> type() {
        return IFunction.typeOf(LibFunctionTypes.CUSTOM.getKey());
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
