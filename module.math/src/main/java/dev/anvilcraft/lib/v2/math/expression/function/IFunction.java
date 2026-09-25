package dev.anvilcraft.lib.v2.math.expression.function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.math.expression.Arguments;
import dev.anvilcraft.lib.v2.math.expression.IExpression;
import dev.anvilcraft.lib.v2.math.init.LibRegistries;
import dev.anvilcraft.lib.v2.util.ISerializer;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * 可被表达式调用的函数定义。
 *
 * <p>函数定义是一个类型化对象：{@link #DIRECT_CODEC} 按 {@link #type()} 分发到对应的
 * {@link Type}，因此下游只要注册一个新的 {@link Type} 就能定义自己的函数类型；具名函数则放在
 * {@link LibRegistries#FUNCTION_KEY} 这个数据包注册表里，表达式按名引用。</p>
 */
public interface IFunction {
    /**
     * 内联形式的编解码：按 {@link #type()} 分发。
     */
    Codec<IFunction> DIRECT_CODEC = Codec.lazyInitialized(
        () -> LibRegistries.FUNCTION_TYPE.byNameCodec().dispatch(IFunction::type, Type::codec)
    );
    /**
     * 引用 {@link LibRegistries#FUNCTION_KEY} 里条目的编解码，也用于内联定义。
     */
    Codec<Holder<IFunction>> HOLDER_CODEC = RegistryFileCodec.create(
        LibRegistries.FUNCTION_KEY,
        IFunction.DIRECT_CODEC
    );
    StreamCodec<RegistryFriendlyByteBuf, Holder<IFunction>> HOLDER_STREAM_CODEC = ByteBufCodecs
        .holderRegistry(LibRegistries.FUNCTION_KEY);
    StreamCodec<RegistryFriendlyByteBuf, IFunction> STREAM_CODEC = ByteBufCodecs
        .registry(LibRegistries.FUNCTION_TYPE_KEY)
        .dispatch(IFunction::type, Type::streamCodec);
    /**
     * 优先写入数据包注册表中已有的函数引用，找不到可引用的条目时退化为内联的函数定义。
     *
     * <p>解码固定走第二个参数 {@link #HOLDER_CODEC}（{@code RegistryFileCodec}），因此需要
     * {@link RegistryOps} 才拿得到注册表；{@code Codec.of} 只把第一个参数当编码器用，所以这里只覆写
     * {@code encode}，不写不可达的 {@code decode}。</p>
     */
    Codec<IFunction> CODEC = Codec.of(
        new Encoder<>() {
            @Override
            public <T> DataResult<T> encode(IFunction input, DynamicOps<T> ops, T prefix) {
                HolderGetter<IFunction> getter = IFunction.getter(ops);
                if (getter instanceof HolderLookup.RegistryLookup<IFunction> lookup) {
                    Optional<Holder.Reference<IFunction>> reference = lookup.listElements()
                        .filter(ref -> input.equals(ref.value()))
                        .findFirst();
                    if (reference.isPresent()) {
                        return IFunction.HOLDER_CODEC.encode(reference.get(), ops, prefix);
                    }
                }
                return IFunction.HOLDER_CODEC.encode(Holder.direct(input), ops, prefix);
            }
        },
        IFunction.HOLDER_CODEC.map(Holder::value)
    );

    /**
     * 从动态操作里取出函数注册表，拿不到时返回 {@code null}。
     */
    static @Nullable HolderGetter<IFunction> getter(DynamicOps<?> ops) {
        if (!(ops instanceof RegistryOps<?> registryOps)) return null;
        return registryOps.getter(LibRegistries.FUNCTION_KEY).orElse(null);
    }

    /**
     * 计算本次调用的结果。
     *
     * <p>收到的是**未求值**的实参表达式，函数自己决定在什么上下文里求它们：普通函数先求值再把结果按
     * {@link #parameters()} 绑成名字，lambda 则把实参绑给自己的形参后再求值函数体。</p>
     *
     * <p>默认实现按 {@link #parameters()} 校验实参个数，在调用点上下文里求值全部实参，把结果按位置绑成
     * 名字后交给 {@link #apply(Call)}。没有形参声明、或者要自己控制实参求值时机的函数类型应当重写本方法。</p>
     *
     * @param arguments 实参表达式，顺序与 {@link #parameters()} 一致
     * @param inputs    调用点可访问的传入值
     * @throws IllegalArgumentException 实参个数与形参声明不符时抛出
     */
    default double apply(List<IExpression> arguments, Arguments inputs) {
        return this.apply(IFunction.bind(arguments, inputs, this.parameters()));
    }

    /**
     * 求值一次已经按位置绑定好形参的调用。
     *
     * @param call 本次调用的实参与形参绑定
     */
    default double apply(Call call) {
        return this.applyBound(call.values(), call.bound());
    }

    /**
     * 只看数字与上下文的求值入口，供只关心「每个形参绑定到一个数」的函数类型重写。
     *
     * <p>固定形参位是它绑到的数字，变参位取列表里的最大值。要按名字拿整份变参列表（例如
     * {@code min}/{@code max}）请改为重写 {@link #apply(Call)} 并用 {@link Call#bound()}。</p>
     *
     * @param arguments 各形参绑定到的数字，顺序与 {@link #parameters()} 一致
     * @param bound     调用点传入值加上本次调用的形参绑定
     */
    default double applyBound(List<Double> arguments, Arguments bound) {
        throw new IllegalStateException(this.getClass().getSimpleName() + " does not implement apply");
    }

    /**
     * 形参声明。名字可以用在函数体的 {@code $(name)} 里；以 {@code ...} 结尾的是变参。
     *
     * <p>默认没有形参，适用于零参函数类型。</p>
     */
    default Parameters parameters() {
        return Parameters.EMPTY;
    }

    /**
     * 求值期允许的最大调用深度。
     *
     * <p>函数体可以按名引用注册表里的任何函数，包括自己，因此自引用与互相引用必须在求值期拦下来，否则会以
     * {@link StackOverflowError} 收场。任何「能被注册、能被按名调用」的类型都要受这条约束：环可以穿过
     * {@link CustomFunction}，也可以只穿过 {@link LambdaFunction}。</p>
     */
    int MAX_CALL_DEPTH = 64;

    /**
     * 当前线程的调用深度，配合 {@link #MAX_CALL_DEPTH} 使用。
     */
    ThreadLocal<Integer> CALL_DEPTH = ThreadLocal.withInitial(() -> 0);

    /**
     * 在调用深度守卫内求值一段函数体，超深时抛出可读的异常而不是让栈溢出。
     *
     * <p>正常函数体远达不到这个深度，代价可以忽略；异常路径也会复原计数，不会污染同线程的后续调用。</p>
     *
     * @param description 超深时用来指认是哪个函数，例如形参声明
     * @param body        真正的求值逻辑
     */
    default double guarded(String description, Supplier<Double> body) {
        int depth = IFunction.CALL_DEPTH.get();
        if (depth >= IFunction.MAX_CALL_DEPTH) {
            throw new IllegalStateException(
                "Function call depth exceeded " + IFunction.MAX_CALL_DEPTH + " at " + description
            );
        }
        IFunction.CALL_DEPTH.set(depth + 1);
        try {
            return body.get();
        } finally {
            IFunction.CALL_DEPTH.set(depth);
        }
    }

    /**
     * 按注册键取函数类型，用于各 {@code type()} 的实现。
     *
     * <p>不走 {@code DeferredHolder.get()}：它要靠 {@code BuiltInRegistries} 反查注册表，而 modded 注册表在
     * {@code NewRegistryEvent} 之前不在那里（在单元测试这类注册表未挂上的环境里会直接抛
     * {@code IllegalStateException: Registry not present for DeferredHolder}）。所以注册的键要么自带读取路径，
     * 要么统一走这里。</p>
     */
    static IFunction.Type<? extends IFunction> typeOf(ResourceKey<IFunction.Type<?>> key) {
        return LibRegistries.FUNCTION_TYPE.getOrThrow(key).value();
    }

    /**
     * 按形参声明绑定一次调用的实参。
     *
     * <p>每个实参先求成一个值：整份变参列表的引用不求值，直接把列表绑上去。{@code $(x...)} 铺开成列表里
     * 的几个元素，实际占几个实参位就算几个，因此 {@code min($(x...))} 与 {@code min(a,b,c)} 完全一样。</p>
     *
     * <p>配位按实参个数算：变参先把它后面每个固定形参的位子留出来，剩下的才归它，{@code $(x...)} 再从变参位
     * 开始摊开占用若干位。所以空列表摊成零个、不占实参位，后面的固定形参照样拿得到自己的值。用实参序号去
     * 对形参序号是行不通的——变参可以落在任意位置，而且它摊开几个值要等求值才知道，得看这个实参实际喂给了
     * 哪个形参。</p>
     *
     * @param arguments  实参表达式，顺序与形参声明一致
     * @param inputs     调用点可访问的传入值
     * @param parameters 形参声明
     * @throws IllegalArgumentException 实参个数与形参声明不符时抛出
     * @throws IllegalStateException    整份列表被用在固定形参位上，或 {@code $(x...)} 铺开的实参多于该位所需时抛出
     */
    static Call bind(List<IExpression> arguments, Arguments inputs, Parameters parameters) {
        List<Arguments.Value> values = new ArrayList<>(arguments.size());
        int total = 0;
        for (IExpression argument : arguments) {
            if (argument instanceof IExpression.Reference.Spread(String name)) {
                // 名字没绑定成列表时按空列表算会先撞上「个数不符」，那个报错看不懂；
                // 这里先点明是名字写错了。绑定成空列表是合法的空变参调用，照常走
                if (!inputs.isList(name)) {
                    throw new IllegalArgumentException("$(" + name + "...) is not bound to a list");
                }
                List<Double> list = inputs.list(name);
                values.add(new Arguments.Value.Many(list));
                total += list.size();
                continue;
            }
            values.add(new Arguments.Value.Single(argument.evaluate(inputs)));
            total++;
        }
        // 个数先校验，免得后面按下标取值时越界，报出看不懂的错
        parameters.checkArity(total);
        List<Arguments.Value> bound = new ArrayList<>(parameters.size());
        List<Double> numbers = new ArrayList<>(parameters.size());
        // 变参吃掉「总数减去固定形参个数」个实参，因此变参落在任意位置都好算
        int variadicCount = total - parameters.fixedCount();
        int argument = 0;
        for (Parameter parameter : parameters.parameters()) {
            // 本形参要吃几个实参：固定形参一个，变参是 variadicCount 个
            int take = parameter.variadic() ? variadicCount : 1;
            List<Double> elements = new ArrayList<>(take);
            int remaining = take;
            while (remaining > 0) {
                if (argument >= values.size()) {
                    throw new IllegalStateException(
                        arguments + " runs out of arguments for " + parameters.declarations()
                    );
                }
                Arguments.Value value = values.get(argument);
                List<Double> expanded = IFunction.numbers(value);
                // 要不要拦下这个实参，得看它实际喂给了哪个形参，不能按形参序号去猜：
                // 变参不在末位时实参序号与形参序号本来就对不上
                if (!parameter.variadic() && value instanceof Arguments.Value.Many) {
                    throw new IllegalStateException(
                        arguments.get(argument) + " is a list and can only be passed to a variadic parameter"
                    );
                }
                if (expanded.size() > remaining) {
                    throw new IllegalStateException(
                        arguments.get(argument) + " provides " + expanded.size()
                        + " arguments but this position takes " + remaining
                    );
                }
                elements.addAll(expanded);
                remaining -= expanded.size();
                argument++;
            }
            // 变参一个实参都没吃（绑定成空列表）时上面那段整个跳过，但它那份 Many 仍占着一个实参位。
            // 不推游标的话，下一个固定形参会把这个 Many 取走当成自己的实参，
            // 然后报「列表只能传给变参位」——而它本来就待在变参位上。
            // 只在变参自己空着时推：take 大于 0 时上面那段已经把铺开的那份实参消费掉了
            if (parameter.variadic() && take == 0 && argument < values.size()
                && values.get(argument) instanceof Arguments.Value.Many) {
                argument++;
            }
            bound.add(parameter.variadic()
                ? new Arguments.Value.Many(List.copyOf(elements))
                : new Arguments.Value.Single(elements.getFirst()));
            numbers.add(elements.stream().mapToDouble(Double::doubleValue).max().orElse(0));
        }
        return new Call(
            List.copyOf(arguments),
            List.copyOf(numbers),
            inputs.withAll(parameters.names(), bound)
        );
    }

    /**
     * 一个绑定值展开成的数字：单个就是它自己，列表是全部元素。
     */
    static List<Double> numbers(Arguments.Value value) {
        return switch (value) {
            case Arguments.Value.Single(double single) -> List.of(single);
            case Arguments.Value.Many many -> many.values();
        };
    }

    /**
     * 一次调用：实参表达式、各形参绑定到的数字，以及形参绑定后的上下文。
     *
     * <p>变参要按名字取整串实参时就靠 {@link #references()} 与 {@link #bound()}：{@code $(x...)} 是一次
     * {@link IExpression.Reference.Spread}，它对应的绑定是 {@link Arguments.Value.Many}。</p>
     *
     * @param references 实参表达式，顺序与形参声明一致
     * @param values     各形参绑定到的数字，变参位是列表里的最大值
     * @param bound      调用点传入值加上本次调用的形参绑定
     */
    record Call(List<IExpression> references, List<Double> values, Arguments bound) {
        /**
         * 第 {@code index} 个形参绑定到的单个数字；绑到列表上时是列表里的最大值。
         *
         * <p>固定形参用得到它；变参位要整份列表时用 {@link #bound()} 配合
         * {@link Arguments#list(String)}。</p>
         */
        public double valueAt(int index) {
            return this.values.get(index);
        }
    }

    Type<? extends IFunction> type();

    /**
     * 一种函数定义形式的编解码与网络编解码。
     *
     * @param <F> 这种形式对应的函数实现
     */
    interface Type<F extends IFunction> extends ISerializer<F>, StringRepresentable {
        MapCodec<F> codec();

        StreamCodec<RegistryFriendlyByteBuf, F> streamCodec();
    }
}
