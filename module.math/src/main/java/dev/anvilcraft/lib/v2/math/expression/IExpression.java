package dev.anvilcraft.lib.v2.math.expression;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.anvilcraft.lib.v2.math.expression.function.ConstantFunction;
import dev.anvilcraft.lib.v2.math.expression.function.IFunction;
import dev.anvilcraft.lib.v2.math.expression.function.Parameter;
import dev.anvilcraft.lib.v2.math.init.LibRegistries;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

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
     * <p>读的时候三支都试；写的时候优先写最简的一支：常量写数字，能用 flat 文本表达的写文本，
     * 剩下的一律写对象形式。文本这一支要够到函数注册表，因此它只接受
     * {@link net.minecraft.resources.RegistryOps}。</p>
     */
    Codec<IExpression> CODEC = Codec.lazyInitialized(() -> Codec.either(
        ConstantFunction.CODEC,
        IExpression.FLAT_OR_OBJECT_CODEC
    ).xmap(
        either -> either.map(value -> ConstantFunction.of(value).call(), expression -> expression),
        expression -> expression instanceof FunctionExpression call
            ? ConstantFunction.value(call).<Either<Double, IExpression>>map(Either::left)
                .orElseGet(() -> Either.right(expression))
            : Either.right(expression)
    ));
    StreamCodec<RegistryFriendlyByteBuf, IExpression> STREAM_CODEC = IExpression.defer(
        () -> ByteBufCodecs.fromCodecWithRegistries(IExpression.CODEC).cast()
    );
    Codec<List<IExpression>> LIST_CODEC = IExpression.CODEC.listOf();

    /**
     * flat 文本与对象形式的二选一：先试文本，写不出来再退回对象。
     *
     * <p>不能直接用 {@link Codec#xor}：xor 的编码器只认定一支，文本写不出来时会直接失败，
     * 退回不了对象。</p>
     */
    Codec<IExpression> FLAT_OR_OBJECT_CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<IExpression, T>> decode(DynamicOps<T> ops, T input) {
            DataResult<Pair<IExpression, T>> flat = FlatExpressionParser.codec().decode(ops, input);
            return flat.result().isPresent()
                ? flat
                : FunctionExpression.CODEC.decode(ops, input).map(pair -> pair.mapFirst(call -> call));
        }

        @Override
        public <T> DataResult<T> encode(IExpression input, DynamicOps<T> ops, T prefix) {
            DataResult<T> flat = FlatExpressionParser.codec().encodeStart(ops, input);
            return flat.result().isPresent()
                ? flat
                : FunctionExpression.MAP_CODEC.codec().encodeStart(ops, (FunctionExpression) input);
        }
    };

    /**
     * 计算表达式的值。除零、负数开方、下标越界、名字未绑定等情况不会抛出异常。
     *
     * @param inputs 传入值，{@code input} 函数按下标引用，{@code named} 函数按名字引用
     */
    double evaluate(Arguments inputs);

    /**
     * 计算表达式的值，传入值只按下标引用。
     */
    default double evaluate(double... inputs) {
        return this.evaluate(Arguments.of(inputs));
    }

    /**
     * 计算表达式的值并四舍五入为整数。
     */
    default int evaluateInt(Arguments inputs) {
        return (int) Math.round(this.evaluate(inputs));
    }

    /**
     * 计算表达式的值并四舍五入为整数，传入值只按下标引用。
     */
    default int evaluateInt(double... inputs) {
        return this.evaluateInt(Arguments.of(inputs));
    }

    /**
     * 按名字直接取值的实参，取值结果由调用上下文决定。
     *
     * <p>它不算“一段可求值的表达式”：{@link Spread} 取到的是变参列表本身，因此只有变参形参接得住，
     * 别处求值一律报错，而不是悄悄取一个数字。</p>
     */
    sealed interface Reference extends IExpression permits Reference.Named, Reference.Spread {
        /**
         * 取值的名字。
         */
        String name();

        /**
         * {@code $(name)}：取一个数字，绑到变参名上时取列表里的最大值。
         */
        record Named(String name) implements Reference {
            @Override
            public double evaluate(Arguments inputs) {
                return inputs.value(this.name);
            }
        }

        /**
         * {@code $(name...)}：取整个变参列表本身，只能交给变参形参。
         */
        record Spread(String name) implements Reference {
            @Override
            public double evaluate(Arguments inputs) {
                throw new IllegalStateException(
                    "$(" + this.name + "...) is a list and can only be passed to a variadic parameter"
                );
            }
        }
    }

    /**
     * 按名字直接取值的一次引用，名字带 {@code ...} 时取整个变参列表。
     */
    static Reference ref(String name) {
        return name.endsWith(Parameter.VARIADIC_SUFFIX)
            ? new Reference.Spread(name.substring(0, name.length() - Parameter.VARIADIC_SUFFIX.length()))
            : new Reference.Named(name);
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
