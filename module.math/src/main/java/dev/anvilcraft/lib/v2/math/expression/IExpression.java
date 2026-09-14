package dev.anvilcraft.lib.v2.math.expression;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.math.init.LibRegistries;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import dev.anvilcraft.lib.v2.math.expression.function.ConstantFunction;
import dev.anvilcraft.lib.v2.math.expression.function.IFunction;

import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * 表达式树节点：一次函数调用。
 *
 * <p>树里只有这一种节点。字面量数字是 {@link ConstantFunction} 的零参调用，
 * {@code x}/{@code y}/{@code z} 与 {@code $(name)} 是 {@code input}/{@code named} 函数的零参调用，
 * 四则运算与乘方是 {@code builtin} 函数的调用，函数体则是 {@code custom} 函数的调用。</p>
 *
 * <p>可以直接内联写出的形式有三种：一个数字、一段形如 {@code x*2}、{@code 2x}、{@code $(cost)*2}
 * 的 flat 表达式文本，以及 {@code function}/{@code arguments} 对象。文本会被解析成函数调用树，
 * 其中按名引用的函数来自
 * {@link LibRegistries#FUNCTION_KEY}。</p>
 */
public interface IExpression {
    /**
     * 完整编解码：数字、flat 文本与函数调用对象。
     *
     * <p>解析与回写 flat 文本要够到函数注册表，因此只接受
     * {@link net.minecraft.resources.RegistryOps}。</p>
     */
    Codec<IExpression> CODEC = Codec.lazyInitialized(() -> FunctionExpression.CODEC.xmap(
        call -> call,
        expression -> (FunctionExpression) expression
    ));
    StreamCodec<RegistryFriendlyByteBuf, IExpression> STREAM_CODEC = IExpression.defer(
        () -> ByteBufCodecs.fromCodecWithRegistries(IExpression.CODEC).cast()
    );
    Codec<List<IExpression>> LIST_CODEC = IExpression.CODEC.listOf();

    /**
     * 计算表达式的值。除零、负数开方、下标越界、名字未绑定等情况不会抛出异常。
     *
     * @param inputs 传入值，{@code input} 函数按下标引用，{@code named} 函数按名字引用
     */
    double evaluate(NumberArguments inputs);

    /**
     * 计算表达式的值，传入值只按下标引用。
     */
    default double evaluate(double... inputs) {
        return this.evaluate(NumberArguments.of(inputs));
    }

    /**
     * 计算表达式的值并四舍五入为整数。
     */
    default int evaluateInt(NumberArguments inputs) {
        return (int) Math.round(this.evaluate(inputs));
    }

    /**
     * 计算表达式的值并四舍五入为整数，传入值只按下标引用。
     */
    default int evaluateInt(double... inputs) {
        return this.evaluateInt(NumberArguments.of(inputs));
    }

    /**
     * 内联一个数字，也就是一次常量函数调用。
     */
    static FunctionExpression of(double value) {
        return ConstantFunction.of(value).call();
    }

    /**
     * 解析一段 flat 表达式文本。
     *
     * @param functions 函数注册表，用于解析文本里按名引用的函数
     * @param source    flat 表达式文本
     * @throws IllegalArgumentException 文本不合法时抛出
     */
    static IExpression of(HolderGetter<IFunction> functions, String source) {
        return FlatExpressionParser.parseValue(source, functions);
    }

    /**
     * 内联一次函数调用。
     */
    static FunctionExpression of(IFunction function, IExpression... arguments) {
        return FunctionExpression.of(function, arguments);
    }

    /**
     * 把网络编解码器的构造推迟到第一次使用时，避免静态初始化顺序问题。
     */
    private static <B extends ByteBuf, V> StreamCodec<B, V> defer(Supplier<StreamCodec<B, V>> source) {
        return new StreamCodec<>() {
            private @Nullable StreamCodec<B, V> delegate;

            @Override
            public V decode(B buffer) {
                if (this.delegate == null) this.delegate = source.get();
                return this.delegate.decode(buffer);
            }

            @Override
            public void encode(B buffer, V value) {
                if (this.delegate == null) this.delegate = source.get();
                this.delegate.encode(buffer, value);
            }
        };
    }
}
