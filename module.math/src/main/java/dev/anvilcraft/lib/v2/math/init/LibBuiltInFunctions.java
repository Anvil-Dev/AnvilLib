package dev.anvilcraft.lib.v2.math.init;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.math.expression.function.IFunction;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.anvilcraft.lib.v2.math.AnvilLibMath;
import dev.anvilcraft.lib.v2.math.expression.FunctionExpression;
import dev.anvilcraft.lib.v2.math.expression.IExpression;
import dev.anvilcraft.lib.v2.math.expression.NumberArguments;

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
 * 不留中间表示。全部按 {@code anvillib:<名字>} 注册进 {@link LibRegistries#FUNCTION_KEY}，
 * 由 {@link #register(IEventBus)} 完成注册。</p>
 */
public enum LibBuiltInFunctions implements IFunction, StringRepresentable {
    /**
     * 加法，两参：{@code a + b}。
     */
    ADD(List.of("a", "b")) {
        @Override
        public double apply(List<Double> arguments, NumberArguments inputs) {
            return arguments.get(0) + arguments.get(1);
        }
    },
    /**
     * 减法，两参：{@code a - b}。
     */
    SUBTRACT(List.of("a", "b")) {
        @Override
        public double apply(List<Double> arguments, NumberArguments inputs) {
            return arguments.get(0) - arguments.get(1);
        }
    },
    /**
     * 乘法，两参：{@code a * b}。
     */
    MULTIPLY(List.of("a", "b")) {
        @Override
        public double apply(List<Double> arguments, NumberArguments inputs) {
            return arguments.get(0) * arguments.get(1);
        }
    },
    /**
     * 除法，两参：{@code a / b}，除零得到 {@link Double#NaN} 或无穷而不抛异常。
     */
    DIVIDE(List.of("a", "b")) {
        @Override
        public double apply(List<Double> arguments, NumberArguments inputs) {
            return arguments.get(0) / arguments.get(1);
        }
    },
    /**
     * 取绝对值，单参。
     */
    ABS(List.of("value")) {
        @Override
        public double apply(List<Double> arguments, NumberArguments inputs) {
            return Math.abs(arguments.getFirst());
        }
    },
    /**
     * 向下取整，单参。
     */
    FLOOR(List.of("value")) {
        @Override
        public double apply(List<Double> arguments, NumberArguments inputs) {
            return Math.floor(arguments.getFirst());
        }
    },
    /**
     * 向上取整，单参。
     */
    CEIL(List.of("value")) {
        @Override
        public double apply(List<Double> arguments, NumberArguments inputs) {
            return Math.ceil(arguments.getFirst());
        }
    },
    /**
     * 四舍五入，单参。
     */
    ROUND(List.of("value")) {
        @Override
        public double apply(List<Double> arguments, NumberArguments inputs) {
            return Math.round(arguments.getFirst());
        }
    },
    /**
     * 平方根，单参，底数为负时得到 {@link Double#NaN} 而不抛异常。
     */
    SQRT(List.of("value")) {
        @Override
        public double apply(List<Double> arguments, NumberArguments inputs) {
            return Math.sqrt(arguments.getFirst());
        }
    },
    /**
     * 幂，两参：{@code base ^ exponent}。
     */
    POW(List.of("base", "exponent")) {
        @Override
        public double apply(List<Double> arguments, NumberArguments inputs) {
            return Math.pow(arguments.get(0), arguments.get(1));
        }
    },
    /**
     * 最小值，至少一参。
     */
    MIN(List.of("values"), 1, Integer.MAX_VALUE) {
        @Override
        public double apply(List<Double> arguments, NumberArguments inputs) {
            return arguments.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        }
    },
    /**
     * 最大值，至少一参。
     */
    MAX(List.of("values"), 1, Integer.MAX_VALUE) {
        @Override
        public double apply(List<Double> arguments, NumberArguments inputs) {
            return arguments.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        }
    };

    public static final Codec<LibBuiltInFunctions> CODEC = StringRepresentable.fromEnum(LibBuiltInFunctions::values);
    public static final StreamCodec<RegistryFriendlyByteBuf, LibBuiltInFunctions> STREAM_CODEC = ByteBufCodecs
        .fromCodec(CODEC)
        .cast();
    private static final Map<String, LibBuiltInFunctions> BY_NAME = Arrays
        .stream(LibBuiltInFunctions.values())
        .collect(Collectors.toUnmodifiableMap(LibBuiltInFunctions::getSerializedName, function -> function));
    private static final DeferredRegister<IFunction> DF = DeferredRegister.create(
        LibRegistries.FUNCTION_KEY,
        AnvilLibMath.MAIN_ID
    );
    private static final DeferredRegister<IFunction.Type<?>> TYPE_DF = DeferredRegister.create(
        LibRegistries.FUNCTION_TYPE,
        AnvilLibMath.MAIN_ID
    );

    /**
     * 内建函数的类型，决定它的编解码方式。
     */
    public static final DeferredHolder<IFunction.Type<?>, LibBuiltInFunctions.Type> TYPE = LibBuiltInFunctions.TYPE_DF
        .register("builtin", LibBuiltInFunctions.Type::new);

    private final List<String> parameters;
    private final int minimumArity;
    private final int maximumArity;

    LibBuiltInFunctions(List<String> parameters) {
        this(parameters, parameters.size(), parameters.size());
    }

    LibBuiltInFunctions(List<String> parameters, int minimumArity, int maximumArity) {
        this.parameters = List.copyOf(parameters);
        this.minimumArity = minimumArity;
        this.maximumArity = maximumArity;
    }

    /**
     * 注册类型与全部内建函数。
     */
    public static void register(IEventBus modEventBus) {
        LibBuiltInFunctions.TYPE_DF.register(modEventBus);
        LibBuiltInFunctions.DF.register(modEventBus);
    }

    /**
     * 按名字查找内建函数，名字不区分大小写，找不到时返回 {@code null}。
     */
    @Nullable
    public static LibBuiltInFunctions byName(String name) {
        return BY_NAME.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * 参数名，与函数体里能引用的 {@code $(name)} 对应。
     */
    public List<String> parameters() {
        return this.parameters;
    }

    /**
     * 该函数接受的最少参数个数。
     */
    public int minimumArity() {
        return this.minimumArity;
    }

    /**
     * 该函数接受的最多参数个数。
     */
    public int maximumArity() {
        return this.maximumArity;
    }

    /**
     * 该函数在 {@link LibRegistries#FUNCTION_KEY} 里的注册名。
     */
    public ResourceLocation id() {
        return AnvilLibMath.of(this.getSerializedName());
    }

    @Override
    public IFunction.Type<? extends IFunction> type() {
        // 走注册表引用而不是 TYPE.get()：DeferredHolder 要靠 BuiltInRegistries 反查注册表，
        // 而 modded 注册表在 NewRegistryEvent 之前不在那里
        return LibRegistries.FUNCTION_TYPE.getHolderOrThrow(LibBuiltInFunctions.TYPE.getKey()).value();
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
     */
    public DataResult<FunctionExpression> callChecked(List<IExpression> arguments) {
        if (arguments.size() < this.minimumArity || arguments.size() > this.maximumArity) {
            return DataResult.error(() -> "Function %s requires %s arguments but got %s".formatted(
                this.getSerializedName(),
                this.arityDescription(),
                arguments.size()
            ));
        }
        return DataResult.success(FunctionExpression.of(
            Holder.direct(this),
            arguments.toArray(IExpression[]::new)
        ));
    }

    private String arityDescription() {
        if (this.minimumArity == this.maximumArity) return Integer.toString(this.minimumArity);
        if (this.maximumArity == Integer.MAX_VALUE) return "at least " + this.minimumArity;
        return this.minimumArity + " to " + this.maximumArity;
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
