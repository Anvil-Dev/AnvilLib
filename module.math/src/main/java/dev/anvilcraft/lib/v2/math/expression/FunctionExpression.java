package dev.anvilcraft.lib.v2.math.expression;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.math.init.LibRegistries;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import dev.anvilcraft.lib.v2.math.expression.function.ConstantFunction;
import dev.anvilcraft.lib.v2.math.expression.function.IFunction;

import java.util.List;

/**
 * 一次函数调用，是表达式树里唯一的节点类型。
 *
 * <p>内联写法有三种：常量函数写成数字，能用 flat 文本表达的写成文本（{@code x*2}、{@code 2x}、
 * {@code $(cost)*2}），其余写成 {@code function}/{@code arguments} 对象。三种形式读回来都是同一个
 * 调用树，写出去时按同一优先级选择。</p>
 *
 * @param function  被调用的函数，可能内联定义，也可能引用
 *                  {@link LibRegistries#FUNCTION_KEY} 中的条目
 * @param arguments 各参数表达式
 */
public record FunctionExpression(Holder<IFunction> function, List<IExpression> arguments) implements IExpression {
    /**
     * 对象形式：{@code {"function": …, "arguments": […]}}。
     */
    public static final MapCodec<FunctionExpression> MAP_CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        IFunction.HOLDER_CODEC
            .fieldOf("function")
            .forGetter(FunctionExpression::function),
        IExpression.CODEC
            .listOf()
            .optionalFieldOf("arguments", List.of())
            .forGetter(FunctionExpression::arguments)
    ).apply(ins, FunctionExpression::new));
    /**
     * 完整编解码：数字、flat 文本与对象形式都接受，写回时按同样的优先级选择。
     *
     * <p>flat 文本这一支要够到函数注册表，因此它只接受
     * {@link net.minecraft.resources.RegistryOps}；数字与对象形式在别的动态操作上也能用。</p>
     *
     * <p>延后构造，避免与 {@link IExpression} 的静态初始化互相牵扯。</p>
     */
    public static final Codec<FunctionExpression> CODEC = Codec.lazyInitialized(
        () -> Codec.either(ConstantFunction.CODEC, FunctionExpression.FLAT_OR_OBJECT_CODEC).xmap(
            either -> either.map(value -> ConstantFunction.of(value).call(), call -> call),
            call -> ConstantFunction.value(call)
                    .map(Either::<Double, FunctionExpression>left)
                    .orElseGet(() -> Either.right(call))
        )
    );
    /**
     * flat 文本与对象形式的二选一：能写成文本就写文本，否则退回对象。
     */
    private static final Codec<FunctionExpression> FLAT_OR_OBJECT_CODEC = Codec.xor(
        FlatExpressionParser.codec(),
        FunctionExpression.MAP_CODEC.codec()
    ).xmap(
        either -> either.map(call -> call, call -> call),
        Either::left
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, FunctionExpression> STREAM_CODEC = StreamCodec.composite(
        IFunction.HOLDER_STREAM_CODEC,
        FunctionExpression::function,
        IExpression.STREAM_CODEC.apply(ByteBufCodecs.list()),
        FunctionExpression::arguments,
        FunctionExpression::new
    );

    public FunctionExpression {
        arguments = List.copyOf(arguments);
    }

    /**
     * 内联一个函数定义并调用它。
     */
    public static FunctionExpression of(IFunction function, IExpression... arguments) {
        return new FunctionExpression(Holder.direct(function), List.of(arguments));
    }

    /**
     * 引用一个函数并调用它。
     */
    public static FunctionExpression of(Holder<IFunction> function, IExpression... arguments) {
        return new FunctionExpression(function, List.of(arguments));
    }

    @Override
    public double evaluate(NumberArguments inputs) {
        List<Double> evaluated = this.arguments.stream().map(argument -> argument.evaluate(inputs)).toList();
        return this.function.value().apply(evaluated, inputs);
    }
}
