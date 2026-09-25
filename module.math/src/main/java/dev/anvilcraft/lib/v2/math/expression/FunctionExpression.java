package dev.anvilcraft.lib.v2.math.expression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.math.expression.function.IFunction;
import dev.anvilcraft.lib.v2.math.init.LibRegistries;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

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
     * 对象形式 {@code {"function": …, "arguments": […]}}，写不出来的写法由 {@link IExpression#CODEC} 兜底。
     */
    public static final Codec<FunctionExpression> CODEC = Codec.lazyInitialized(FunctionExpression.MAP_CODEC::codec);
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
    public double evaluate(Arguments inputs) {
        // 实参交给函数自己求值：普通函数在调用点上下文里求，lambda 则换成自己的形参绑定再求函数体
        return this.function.value().apply(this.arguments, inputs);
    }}
