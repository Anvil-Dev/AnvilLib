package dev.anvilcraft.lib.v2.math.test;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import dev.anvilcraft.lib.v2.math.AnvilLibMath;
import dev.anvilcraft.lib.v2.math.expression.Arguments;
import dev.anvilcraft.lib.v2.math.expression.FlatExpressionParser;
import dev.anvilcraft.lib.v2.math.expression.FunctionExpression;
import dev.anvilcraft.lib.v2.math.expression.IExpression;
import dev.anvilcraft.lib.v2.math.expression.function.ConstantFunction;
import dev.anvilcraft.lib.v2.math.expression.function.CustomFunction;
import dev.anvilcraft.lib.v2.math.expression.function.IFunction;
import dev.anvilcraft.lib.v2.math.expression.function.InputFunction;
import dev.anvilcraft.lib.v2.math.expression.function.LambdaFunction;
import dev.anvilcraft.lib.v2.math.expression.function.NamedFunction;
import dev.anvilcraft.lib.v2.math.init.LibBuiltInFunctions;
import dev.anvilcraft.lib.v2.math.init.LibRegistries;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * 测试用的最小运行环境：不启动游戏，只准备解析与求值表达式所需的注册表。
 *
 * <p>表达式只依赖函数注册表，所以这里手工建一个装着全部内建函数的注册表，再用它包一个
 * {@link RegistryOps}。{@code LibRegistries.FUNCTION_TYPE} 平时由 mod 加载时填充，测试里也要先
 * 补上条目，否则 {@link IFunction} 的类型分发拿不到任何类型。</p>
 */
public final class MathTestBootstrap {
    private static boolean initialized;
    private static @Nullable MappedRegistry<IFunction> functions;

    private MathTestBootstrap() {
    }

    /**
     * 准备注册表，重复调用只生效一次。
     */
    public static synchronized void initialize() {
        if (MathTestBootstrap.initialized) return;
        MathTestBootstrap.bootstrapMinecraft();
        MathTestBootstrap.registerTypes();
        MathTestBootstrap.initialized = true;
    }

    /**
     * 共享的函数注册表，装着全部内建函数。
     */
    public static synchronized MappedRegistry<IFunction> functions() {
        MathTestBootstrap.initialize();
        if (MathTestBootstrap.functions == null) {
            MappedRegistry<IFunction> registry = new MappedRegistry<>(LibRegistries.FUNCTION_KEY, Lifecycle.stable());
            for (LibBuiltInFunctions builtin : LibBuiltInFunctions.values()) {
                Registry.register(registry, builtin.id(), builtin);
            }
            MathTestBootstrap.functions = registry;
        }
        return MathTestBootstrap.functions;
    }

    /**
     * 函数注册表的查询入口。
     */
    public static HolderGetter<IFunction> functionGetter() {
        return MathTestBootstrap.functions();
    }

    /**
     * 解析一个名字引用 {@code $(name)}。
     */
    public static IExpression parseNamed(String name) {
        return MathTestBootstrap.parseValue("$(" + name + ")");
    }

    /**
     * 能读到函数注册表的动态操作，flat 文本的解析与回写都需要它。
     */
    public static RegistryOps<JsonElement> ops() {
        return RegistryOps.create(JsonOps.INSTANCE, MathTestBootstrap.access());
    }

    /**
     * 测试用的注册表视图，只认得函数注册表，其余交给原版注册表。
     *
     * <p>JSON 侧（{@link #ops()}）与网络侧（{@link #registryFriendlyBuf()}）共用同一份视图，
     * 两条路径看到的注册表才是同一个。</p>
     */
    private static RegistryAccess.Frozen access() {
        RegistryAccess.Frozen delegate = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        MappedRegistry<IFunction> registry = MathTestBootstrap.functions();
        return new RegistryAccess.Frozen() {
            @Override
            public <E> Optional<Registry<E>> lookup(ResourceKey<? extends Registry<? extends E>> key) {
                if (key.equals(LibRegistries.FUNCTION_KEY)) {
                    @SuppressWarnings("unchecked")
                    Registry<E> cast = (Registry<E>) registry;
                    return Optional.of(cast);
                }
                return delegate.lookup(key);
            }

            @Override
            public Stream<RegistryEntry<?>> registries() {
                return Stream.concat(
                    delegate.registries(),
                    Stream.of(new RegistryEntry<>(LibRegistries.FUNCTION_KEY, registry))
                );
            }

            @Override
            public RegistryAccess.Frozen freeze() {
                return this;
            }
        };
    }

    /**
     * 走网络路径编解码用的缓冲区，用 {@link #access()} 里的函数注册表。
     *
     * <p>流编解码与 JSON 编解码是两条独立实现：JSON 侧靠 {@code RegistryFileCodec} 的内联分支救回
     * {@code Holder.direct} 的内建函数，网络侧走的是 {@code ByteBufCodecs}，必须单独实测。</p>
     */
    public static RegistryFriendlyByteBuf registryFriendlyBuf() {
        // noinspection deprecation
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), MathTestBootstrap.access());
    }

    /**
     * 把一个函数注册进测试用的函数注册表，返回它的引用。
     */
    public static Holder.Reference<IFunction> registerFunction(String name, IFunction function) {
        return MathTestBootstrap.registerFunction(AnvilLibMath.of(name), function);
    }

    /**
     * 按完整资源位置注册函数，用于 {@code mymod:xxx} 这类非 anvillib 命名空间的名字。
     *
     * <p>同名重复注册时改绑已有的引用，而不是新增条目：26.1 的 {@code MappedRegistry} 对重复键直接抛异常，
     * 而自引用、互相引用、以及「撞内建名」这几类用例本来就靠「同一个名字换个函数体」构造出来。</p>
     */
    public static Holder.Reference<IFunction> registerFunction(ResourceLocation id, IFunction function) {
        MappedRegistry<IFunction> registry = MathTestBootstrap.functions();
        ResourceKey<IFunction> key = ResourceKey.create(LibRegistries.FUNCTION_KEY, id);
        Optional<Holder.Reference<IFunction>> existing = registry.get(key);
        if (existing.isPresent()) {
            MathTestBootstrap.bindValue(existing.get(), function);
            return existing.get();
        }
        return registry.register(key, function, RegistrationInfo.BUILT_IN);
    }

    /**
     * 改绑一个已注册引用的值。
     *
     * <p>{@code Holder.Reference.bindValue} 是 {@code protected}，注册表也没有替换条目的接口，所以这里
     * 直接反射调用。注册表建出来的引用都是 STAND_ALONE，允许改绑。</p>
     */
    private static void bindValue(Holder.Reference<IFunction> holder, IFunction function) {
        try {
            Method method = Holder.Reference.class.getDeclaredMethod("bindValue", Object.class);
            method.setAccessible(true);
            method.invoke(holder, function);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot rebind function holder " + holder.key(), exception);
        }
    }

    /**
     * 替换注册表里已有的函数，用于构造自引用与互相引用。
     */
    public static void replaceFunction(String name, IFunction function) {
        MathTestBootstrap.registerFunction(name, function);
    }

    /**
     * 把表达式编码成 flat 文本，写不出来时返回 {@code null}。
     */
    public static @Nullable String writeFlat(IExpression expression) {
        // 表达式树里按名字取值的实参不是函数调用，回写时单独走 flat 文本这一支
        JsonElement element = expression instanceof FunctionExpression call
            ? FlatExpressionParser.codec().encodeStart(MathTestBootstrap.ops(), call).result().orElse(null)
            : IExpression.CODEC.encodeStart(MathTestBootstrap.ops(), expression).result().orElse(null);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) return null;
        return element.getAsString();
    }

    /**
     * 解析 flat 文本并回写，用来断言规范化后的写法。
     */
    public static @Nullable String writeFlat(String source) {
        return MathTestBootstrap.writeFlat(MathTestBootstrap.parse(source));
    }

    /**
     * 解析 flat 文本并回写，整段文本也可以只是一个数字或 lambda。
     */
    public static @Nullable String writeFlatValue(String source) {
        return MathTestBootstrap.writeFlat(MathTestBootstrap.parseValue(source));
    }

    /**
     * 解析 flat 文本，整段文本也可以只是一个数字或 lambda。
     */
    public static IExpression parseValue(String source) {
        return FlatExpressionParser.parseValue(source, MathTestBootstrap.functionGetter());
    }

    /**
     * 把表达式编码成完整形式（数字、flat 文本或对象）。
     */
    public static @Nullable JsonElement encode(IExpression expression) {
        return IExpression.CODEC.encodeStart(MathTestBootstrap.ops(), expression).result().orElse(null);
    }

    /**
     * 解析 flat 文本，要求整段是一次函数调用。
     */
    public static FunctionExpression parse(String source) {
        IExpression expression = MathTestBootstrap.parseValue(source);
        if (!(expression instanceof FunctionExpression call)) {
            throw new IllegalArgumentException("Not a function expression: " + source);
        }
        return call;
    }

    /**
     * 用若干常量实参直接调用一个 lambda。
     */
    public static double callLambda(IExpression function, double... values) {
        return MathTestBootstrap.callLambda(function, MathTestBootstrap.constants(values), Arguments.of());
    }

    /**
     * 用给定的实参表达式在给定上下文里直接调用一个 lambda。
     */
    public static double callLambda(IExpression function, List<IExpression> arguments, Arguments inputs) {
        if (!(function instanceof FunctionExpression expression)
            || !(expression.function().value() instanceof LambdaFunction lambda)) {
            throw new IllegalArgumentException("Not a lambda: " + function);
        }
        return lambda.apply(arguments, inputs);
    }

    /**
     * 把若干个数字包成常量实参，用于直接调用函数的场合。
     */
    public static List<IExpression> constants(double... values) {
        List<IExpression> arguments = new ArrayList<>(values.length);
        for (double value : values) {
            arguments.add(ConstantFunction.of(value).call());
        }
        return arguments;
    }

    private static void bootstrapMinecraft() {
        try {
            java.lang.reflect.Field field = Class.forName("net.minecraft.server.Bootstrap")
                .getDeclaredField("isBootstrapped");
            field.setAccessible(true);
            field.setBoolean(null, true);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot bootstrap Minecraft for tests", exception);
        }
    }

    private static void registerTypes() {
        MappedRegistry<IFunction.Type<?>> types = new MappedRegistry<>(LibRegistries.FUNCTION_TYPE_KEY, Lifecycle.stable());
        Registry.register(types, AnvilLibMath.of("input"), new InputFunction.Type());
        Registry.register(types, AnvilLibMath.of("named"), new NamedFunction.Type());
        Registry.register(types, AnvilLibMath.of("constant"), new ConstantFunction.Type());
        Registry.register(types, AnvilLibMath.of("custom"), new CustomFunction.Type());
        Registry.register(types, AnvilLibMath.of("lambda"), new LambdaFunction.Type());
        Registry.register(types, AnvilLibMath.of("builtin"), new LibBuiltInFunctions.Type());
        WritableRegistry<IFunction.Type<?>> target = (WritableRegistry<IFunction.Type<?>>) LibRegistries.FUNCTION_TYPE;
        for (IFunction.Type<?> type : types) {
            Registry.register(target, Objects.requireNonNull(types.getKey(type)), type);
        }
    }
}
