package dev.anvilcraft.lib.v2.math.init;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.math.AnvilLibMath;
import dev.anvilcraft.lib.v2.math.expression.Arguments;
import dev.anvilcraft.lib.v2.math.expression.FunctionExpression;
import dev.anvilcraft.lib.v2.math.expression.IExpression;
import dev.anvilcraft.lib.v2.math.expression.function.ConstantFunction;
import dev.anvilcraft.lib.v2.math.expression.function.IFunction;
import dev.anvilcraft.lib.v2.math.expression.function.LambdaFunction;
import dev.anvilcraft.lib.v2.math.expression.function.Parameters;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * 内建函数：四则运算、单参数学函数与多参聚合函数。
 *
 * <p>每个枚举常量自己就是 {@link IFunction}，自带参数名、求值行为、参数个数限制与注册名，
 * 不留中间表示。查找走 {@link #byName(String)} 的枚举硬编码，<b>不占</b>
 * {@link LibRegistries#FUNCTION_KEY} 的条目：那是数据包注册表，只在数据包加载时由 JSON 填充，代码注册不进去。
 * 序列化时内建函数因此总以内联定义写出，由 {@code RegistryFileCodec} 的 {@code {function: {...}}} 分支读回。</p>
 */
public enum LibBuiltInFunctions implements IFunction, StringRepresentable {
    /**
     * 加法，两参：{@code a + b}。
     */
    ADD(List.of("a", "b")) {
        @Override
        public double apply(Call call) {
            return call.valueAt(0) + call.valueAt(1);
        }
    },
    /**
     * 减法，两参：{@code a - b}。
     */
    SUBTRACT(List.of("a", "b")) {
        @Override
        public double apply(Call call) {
            return call.valueAt(0) - call.valueAt(1);
        }
    },
    /**
     * 乘法，两参：{@code a * b}。
     */
    MULTIPLY(List.of("a", "b")) {
        @Override
        public double apply(Call call) {
            return call.valueAt(0) * call.valueAt(1);
        }
    },
    /**
     * 除法，两参：{@code a / b}，除零得到 {@link Double#NaN} 或无穷而不抛异常。
     */
    DIVIDE(List.of("a", "b")) {
        @Override
        public double apply(Call call) {
            return call.valueAt(0) / call.valueAt(1);
        }
    },
    /**
     * 取绝对值，单参。
     */
    ABS(List.of("value")) {
        @Override
        public double apply(Call call) {
            return Math.abs(call.valueAt(0));
        }
    },
    /**
     * 向下取整，单参。
     */
    FLOOR(List.of("value")) {
        @Override
        public double apply(Call call) {
            return Math.floor(call.valueAt(0));
        }
    },
    /**
     * 向上取整，单参。
     */
    CEIL(List.of("value")) {
        @Override
        public double apply(Call call) {
            return Math.ceil(call.valueAt(0));
        }
    },
    /**
     * 四舍五入，单参。
     *
     * <p>用的是 {@link Math#round(double)}，所以边界行为与模块里其它函数不同：{@code NaN} 得到 {@code 0}，
     * 超出 {@code long} 范围时夹到 {@code Long.MIN_VALUE}/{@code Long.MAX_VALUE}。而 {@code divide}、
     * {@code sqrt} 这类是保留 {@code NaN}/{@code Infinity} 的。想要原始的 {@code NaN} 就别过这一层。</p>
     */
    ROUND(List.of("value")) {
        @Override
        public double apply(Call call) {
            return Math.round(call.valueAt(0));
        }
    },
    /**
     * 平方根，单参，底数为负时得到 {@link Double#NaN} 而不抛异常。
     */
    SQRT(List.of("value")) {
        @Override
        public double apply(Call call) {
            return Math.sqrt(call.valueAt(0));
        }
    },
    /**
     * 幂，两参：{@code base ^ exponent}。
     */
    POW(List.of("base", "exponent")) {
        @Override
        public double apply(Call call) {
            return Math.pow(call.valueAt(0), call.valueAt(1));
        }
    },
    /**
     * 最小值，变参，取不到值时返回 0。
     */
    MIN(List.of("x...")) {
        @Override
        public double apply(Call call) {
            return call.bound().list("x").stream().mapToDouble(Double::doubleValue).min().orElse(0);
        }
    },
    /**
     * 最大值，变参，取不到值时返回 0。
     */
    MAX(List.of("x...")) {
        @Override
        public double apply(Call call) {
            return call.bound().list("x").stream().mapToDouble(Double::doubleValue).max().orElse(0);
        }
    },
    /**
     * 遍历，变参列表加一个 lambda，返回各次调用结果之和。
     *
     * <p>最后一位必须是 lambda，之前的所有实参就是被遍历的列表，所以
     * {@code forEach($(x...), x -> x*2)} 能把自定义函数里拆不开的变参 {@code x} 逐个交给 lambda。</p>
     */
    FOREACH(List.of("x...", "function")) {
        @Override
        public double apply(List<IExpression> arguments, Arguments inputs) {
            // 解析期与 call() 都校验过个数，但直接构造调用会绕过它们，
            // 少了这一步 arguments.get(last) 会以 IndexOutOfBounds 失败而不是给出可读的校验错误
            this.parameters().checkArity(arguments.size());
            int last = arguments.size() - 1;
            if (!(LibBuiltInFunctions.functionOf(arguments.get(last)) instanceof LambdaFunction lambda)) {
                throw new IllegalArgumentException(
                    "forEach expects a lambda as its last argument but got " + arguments.get(last)
                );
            }
            lambda.parameters().checkArity(1);
            if (lambda.parameters().parameters().getFirst().variadic()) {
                throw new IllegalArgumentException(
                    "forEach calls its lambda with one value at a time, so the lambda cannot start with a variadic parameter"
                );
            }
            // 变参位上是 $(x...) 时拿的是整份列表，普通实参就是一个数字
            List<Double> values = new ArrayList<>(last);
            for (int index = 0; index < last; index++) {
                IExpression argument = arguments.get(index);
                if (argument instanceof IExpression.Reference.Spread(String name)) {
                    // 名字没绑定成列表时要点名报错。直接 list(name) 对「名字写错」和「绑定成空列表」
                    // 都返回空列表，拼错一个字母就会静默变成 0——IFunction.bind 同样守着这一条
                    if (!inputs.isList(name)) {
                        throw new IllegalArgumentException("$(" + name + "...) is not bound to a list");
                    }
                    values.addAll(inputs.list(name));
                } else {
                    values.add(argument.evaluate(inputs));
                }
            }
            double total = 0;
            for (double value : values) {
                total += lambda.apply(List.of(ConstantFunction.of(value).call()), inputs);
            }
            return total;
        }
    };

    public static final Codec<LibBuiltInFunctions> CODEC = StringRepresentable.fromEnum(LibBuiltInFunctions::values);
    public static final StreamCodec<RegistryFriendlyByteBuf, LibBuiltInFunctions> STREAM_CODEC = ByteBufCodecs
        .fromCodec(CODEC)
        .cast();
    private static final Map<String, LibBuiltInFunctions> BY_NAME = Arrays
        .stream(LibBuiltInFunctions.values())
        .collect(Collectors.toUnmodifiableMap(LibBuiltInFunctions::getSerializedName, function -> function));
    private static final DeferredRegister<IFunction.Type<?>> TYPE_DF = DeferredRegister.create(
        LibRegistries.FUNCTION_TYPE,
        AnvilLibMath.MAIN_ID
    );

    /**
     * 内建函数的类型，决定它的编解码方式。
     */
    public static final DeferredHolder<IFunction.Type<?>, LibBuiltInFunctions.Type> TYPE = LibBuiltInFunctions.TYPE_DF
        .register("builtin", LibBuiltInFunctions.Type::new);

    private final Parameters parameters;

    LibBuiltInFunctions(List<String> declarations) {
        this.parameters = Parameters.parse(declarations);
    }

    /**
     * 注册内建函数的类型。
     */
    public static void register(IEventBus modEventBus) {
        LibBuiltInFunctions.TYPE_DF.register(modEventBus);
    }

    /**
     * 按名字查找内建函数，名字不区分大小写，找不到时返回 {@code null}。
     */
    @Nullable
    public static LibBuiltInFunctions byName(String name) {
        return BY_NAME.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * 形参声明。名字与函数体里能引用的 {@code $(name)} 对应，变参带 {@code ...}。
     */
    @Override
    public Parameters parameters() {
        return this.parameters;
    }

    /**
     * 该函数接受的最少参数个数。
     */
    public int minimumArity() {
        return this.parameters.minimumArity();
    }

    /**
     * 该函数接受的最多参数个数。
     */
    public int maximumArity() {
        return this.parameters.maximumArity();
    }

    /**
     * 该函数在 {@link LibRegistries#FUNCTION_KEY} 里的注册名。
     */
    public ResourceLocation id() {
        return AnvilLibMath.of(this.getSerializedName());
    }

    @Override
    public IFunction.Type<? extends IFunction> type() {
        return IFunction.typeOf(LibBuiltInFunctions.TYPE.getKey());
    }

    /**
     * 调用该函数，参数个数不合法时抛出异常。
     */
    public FunctionExpression call(IExpression... arguments) {
        return this.call(List.of(arguments));
    }

    /**
     * 调用该函数并校验参数个数，个数不合法时抛出异常。
     */
    public FunctionExpression call(List<IExpression> arguments) {
        return this.callChecked(arguments).getOrThrow(IllegalArgumentException::new);
    }

    /**
     * 调用该函数并校验参数个数，个数不合法时返回错误。
     *
     * <p>校验走 {@link Parameters#checkArity(List)}，与解析同一套规则：{@code $(x...)} 只能落在变参形参位上，
     * 没有变参形参的内建函数（例如 {@code sqrt}）在这里就把 {@code $(x...)} 拦下，而不是构造出一个到求值期
     * 才炸的调用。</p>
     */
    public DataResult<FunctionExpression> callChecked(List<IExpression> arguments) {
        try {
            this.parameters.checkArity(arguments);
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Function %s %s".formatted(this.getSerializedName(), exception.getMessage()));
        }
        return DataResult.success(FunctionExpression.of(
            Holder.direct(this),
            arguments.toArray(IExpression[]::new)
        ));
    }

    /**
     * 取表达式对应的函数，不是调用时返回 {@code null}。
     */
    @Nullable
    static IFunction functionOf(IExpression expression) {
        return expression instanceof FunctionExpression call ? call.function().value() : null;
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    /**
     * 内建函数的编解码：按 {@link #getSerializedName()} 区分具体函数。
     */
    public static class Type implements IFunction.Type<LibBuiltInFunctions> {
        @Override
        public MapCodec<LibBuiltInFunctions> codec() {
            return LibBuiltInFunctions.CODEC.fieldOf("builtin");
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, LibBuiltInFunctions> streamCodec() {
            return LibBuiltInFunctions.STREAM_CODEC;
        }

        @Override
        public String getSerializedName() {
            return "builtin";
        }
    }
}
