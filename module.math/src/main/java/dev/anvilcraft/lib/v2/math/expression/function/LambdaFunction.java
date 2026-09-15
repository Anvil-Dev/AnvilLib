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

/**
 * lambda：把一段表达式当作值传给别的函数。
 *
 * <p>flat 文本里写成 {@code x -> $(x)*2}，多个形参写 {@code (a, b) -> $(a)+$(b)}；形参名以 {@code ...}
 * 结尾是变参，规则与 {@link CustomFunction} 一致。lambda 只能被别的函数调用，不能出现在顶层做求值，
 * 因为它自己没有实参可绑。</p>
 *
 * <p>函数体沿用求值处的传入值，因此 lambda 能读到外层正在求值的那些名字（闭包）；形参绑定只覆盖
 * 同名的传入值。</p>
 *
 * @param parameters 形参声明
 * @param body       函数体，用 {@code $(name)} 引用形参
 */
public record LambdaFunction(Parameters parameters, IExpression body) implements IFunction {
    public static final MapCodec<LambdaFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Codec.STRING
            .listOf()
            .fieldOf("parameters")
            .forGetter(LambdaFunction::declarations),
        IExpression.CODEC
            .fieldOf("body")
            .forGetter(LambdaFunction::body)
    ).apply(ins, LambdaFunction::of));
    public static final StreamCodec<RegistryFriendlyByteBuf, LambdaFunction> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
        LambdaFunction::declarations,
        IExpression.STREAM_CODEC,
        LambdaFunction::body,
        LambdaFunction::of
    );

    /**
     * 按声明文本创建，{@code "x..."} 是变参。
     */
    public static LambdaFunction of(List<String> declarations, IExpression body) {
        return new LambdaFunction(Parameters.parse(declarations), body);
    }

    /**
     * 按形参名创建，全部当作固定形参。
     */
    public static LambdaFunction named(List<String> names, IExpression body) {
        return new LambdaFunction(Parameters.of(names), body);
    }

    /**
     * 形参声明文本，变参带 {@code ...}，用于编解码与报错。
     */
    public List<String> declarations() {
        return this.parameters.declarations();
    }

    /**
     * 单形参 lambda 的一次调用。
     */
    public static FunctionExpression call(String parameter, IExpression body) {
        return FunctionExpression.of(LambdaFunction.named(List.of(parameter), body));
    }

    @Override
    public double apply(List<IExpression> arguments, Arguments inputs) {
        // 实参在调用点上下文里求值，函数体再换成形参绑定：这样函数体既能读到形参，也能读到外层的名字
        return this.body.evaluate(IFunction.bind(arguments, inputs, this.parameters).bound());
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
        return LibFunctionTypes.LAMBDA.get();
    }

    public static class Type implements IFunction.Type<LambdaFunction> {
        @Override
        public MapCodec<LambdaFunction> codec() {
            return LambdaFunction.MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, LambdaFunction> streamCodec() {
            return LambdaFunction.STREAM_CODEC;
        }

        @Override
        public String getSerializedName() {
            return "lambda";
        }
    }
}
