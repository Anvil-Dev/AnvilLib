package dev.anvilcraft.lib.v2.math.init;

import dev.anvilcraft.lib.v2.math.AnvilLibMath;
import dev.anvilcraft.lib.v2.math.expression.function.ConstantFunction;
import dev.anvilcraft.lib.v2.math.expression.function.CustomFunction;
import dev.anvilcraft.lib.v2.math.expression.function.IFunction;
import dev.anvilcraft.lib.v2.math.expression.function.InputFunction;
import dev.anvilcraft.lib.v2.math.expression.function.LambdaFunction;
import dev.anvilcraft.lib.v2.math.expression.function.NamedFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 通用函数类型注册表，决定 {@link IFunction} 的编解码方式与注册名。
 *
 * <p>内建函数由 {@code LibBuiltInFunctions} 自带类型，这里只放其余类型。类型注册在
 * {@link AnvilLibMath#MAIN_ID} 命名空间下，与 {@link LibRegistries#FUNCTION_KEY} 里的函数、
 * 以及 flat 文本里省略命名空间的名字保持一致。下游模组可以用自己的
 * {@link DeferredRegister} 向 {@link LibRegistries#FUNCTION_TYPE} 注册更多函数类型。</p>
 */
public class LibFunctionTypes {
    private static final DeferredRegister<IFunction.Type<?>> DF = DeferredRegister.create(
        LibRegistries.FUNCTION_TYPE,
        AnvilLibMath.MAIN_ID
    );

    public static final DeferredHolder<IFunction.Type<?>, InputFunction.Type> INPUT = LibFunctionTypes.DF
        .register("input", InputFunction.Type::new);
    public static final DeferredHolder<IFunction.Type<?>, NamedFunction.Type> NAMED = LibFunctionTypes.DF
        .register("named", NamedFunction.Type::new);
    public static final DeferredHolder<IFunction.Type<?>, ConstantFunction.Type> CONSTANT = LibFunctionTypes.DF
        .register("constant", ConstantFunction.Type::new);
    public static final DeferredHolder<IFunction.Type<?>, CustomFunction.Type> CUSTOM = LibFunctionTypes.DF
        .register("custom", CustomFunction.Type::new);
    public static final DeferredHolder<IFunction.Type<?>, LambdaFunction.Type> LAMBDA = LibFunctionTypes.DF
        .register("lambda", LambdaFunction.Type::new);

    public static void register(IEventBus bus) {
        LibFunctionTypes.DF.register(bus);
    }
}
