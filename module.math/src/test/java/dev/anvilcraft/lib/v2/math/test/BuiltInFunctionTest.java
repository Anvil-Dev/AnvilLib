package dev.anvilcraft.lib.v2.math.test;

import dev.anvilcraft.lib.v2.math.expression.Arguments;
import dev.anvilcraft.lib.v2.math.expression.FunctionExpression;
import dev.anvilcraft.lib.v2.math.expression.IExpression;
import dev.anvilcraft.lib.v2.math.expression.function.ConstantFunction;
import dev.anvilcraft.lib.v2.math.expression.function.CustomFunction;
import dev.anvilcraft.lib.v2.math.expression.function.IFunction;
import dev.anvilcraft.lib.v2.math.expression.function.InputFunction;
import dev.anvilcraft.lib.v2.math.expression.function.LambdaFunction;
import dev.anvilcraft.lib.v2.math.expression.function.NamedFunction;
import dev.anvilcraft.lib.v2.math.expression.function.Parameters;
import dev.anvilcraft.lib.v2.math.init.LibBuiltInFunctions;
import dev.anvilcraft.lib.v2.math.init.LibFunctionTypes;
import dev.anvilcraft.lib.v2.math.init.LibRegistries;
import net.minecraft.core.Holder;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 内建函数：形参名绑定与变参声明。
 */
class BuiltInFunctionTest {
    @BeforeAll
    static void setUp() {
        MathTestBootstrap.initialize();
    }

    /**
     * 在给定上下文里用任意实参表达式求值一次自定义函数调用。
     */
    private static double call(CustomFunction function, Arguments inputs, IExpression... arguments) {
        return FunctionExpression.of(Holder.direct(function), arguments).evaluate(inputs);
    }

    @Test
    @DisplayName("形参绑定就是求值时的名字绑定，调用点没有同名值时取到 0")
    void parametersAreTheOnlySourceOfNames() {
        FunctionExpression call = LibBuiltInFunctions.ADD.call(
            NamedFunction.call("a"),
            NamedFunction.call("b")
        );
        assertEquals(3.0, call.evaluate(Arguments.of(List.of(), List.of("a", "b"), List.of(
            new Arguments.Value.Single(1),
            new Arguments.Value.Single(2)
        ))));
        assertEquals(0.0, call.evaluate(Arguments.of()));
    }

    @Test
    @DisplayName("形参绑定覆盖调用点的同名传入值")
    void parametersShadowCallSite() {
        FunctionExpression call = LibBuiltInFunctions.ADD.call(
            ConstantFunction.of(1).call(),
            ConstantFunction.of(2).call()
        );
        // 调用点带了 a=100，但 ADD 的 a 绑的是实参 1
        Arguments site = Arguments.of(List.of(), List.of("a", "b"), List.of(
            new Arguments.Value.Single(100),
            new Arguments.Value.Single(200)
        ));
        assertEquals(3.0, call.evaluate(site));
    }

    @Test
    @DisplayName("MIN/MAX 声明为变参 x...，吃下全部实参")
    void minAndMaxAreVariadic() {
        assertEquals(List.of("x..."), LibBuiltInFunctions.MIN.parameters().declarations());
        assertEquals(List.of("x..."), LibBuiltInFunctions.MAX.parameters().declarations());
        assertTrue(LibBuiltInFunctions.MIN.parameters().variadicExists());
        // 变参的下限是 0：不给实参也是一次合法调用，取不到值由函数自己兜底
        assertEquals(0, LibBuiltInFunctions.MIN.minimumArity());
        assertEquals(Integer.MAX_VALUE, LibBuiltInFunctions.MIN.maximumArity());

        assertEquals(1.0, MathTestBootstrap.parse("min(3,1,2)").evaluate(Arguments.of()));
        assertEquals(3.0, MathTestBootstrap.parse("max(3,1,2)").evaluate(Arguments.of()));
        assertEquals(0.0, MathTestBootstrap.parse("min()").evaluate(Arguments.of()));
        assertEquals(0.0, MathTestBootstrap.parse("max()").evaluate(Arguments.of()));
    }

    @Test
    @DisplayName("变参名在函数体里是列表：$(x...) 传给变参函数，$(x) 取最大值")
    void variadicParameterIsAList() {
        Arguments bound = Arguments.of(List.of(), List.of("x"), List.of(
            new Arguments.Value.Many(List.of(9.0, 2.0, 7.0))
        ));

        FunctionExpression min = LibBuiltInFunctions.MIN.call(
            IExpression.ref("x..."),
            ConstantFunction.of(4).call()
        );
        // $(x...) 把整份列表铺成实参，所以这里是 min(9, 2, 7, 4)
        assertEquals(2.0, min.evaluate(bound));

        FunctionExpression named = LibBuiltInFunctions.MIN.call(IExpression.ref("x"));
        assertEquals(9.0, named.evaluate(bound));

        // $(x...) 是列表，落在固定形参位上会报错。add 声明了 (a, b) 两个固定形参、没有变参，
        // 整份列表一个固定位都接不住，所以这种调用在构造时就按个数不符被拦下，
        // 「列表不能当数字用」这条规则在求值期兜底
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> LibBuiltInFunctions.ADD.call(
                IExpression.ref("x..."),
                ConstantFunction.of(1).call()
            )
        );
        assertTrue(
            error.getMessage().contains("Expected 2 arguments but got 1"),
            () -> "应当是实参个数不符的报错: " + error.getMessage()
        );
    }

    @Test
    @DisplayName("变参可以在形参列表的任意位置")
    void variadicMayAppearAnywhere() {
        Parameters parameters = Parameters.parse(List.of("x...", "last"));
        assertEquals(List.of("x", "last"), parameters.names());
        // 末位固定形参一个都不能少，变参则可以一个都不给
        assertEquals(1, parameters.minimumArity());
        assertEquals(Integer.MAX_VALUE, parameters.maximumArity());
        assertThrows(IllegalArgumentException.class, () -> Parameters.parse(List.of("x...", "y...")));
        assertThrows(IllegalArgumentException.class, () -> Parameters.parse(List.of("x...", "x")));
    }

    @Test
    @DisplayName("变参不在末位时，$(x...) 照样绑给变参位，前后固定形参各拿一个")
    void variadicNotLastBindsByConsumptionOrder() {
        Arguments bound = Arguments.of(List.of(), List.of("xs"), List.of(
            new Arguments.Value.Many(List.of(5.0, 6.0))
        ));

        // 形参 [a, x..., b]：$(xs...) 摊成 5、6 落进变参位，b 拿最后一个实参
        CustomFunction three = CustomFunction.of(
            List.of("a", "x...", "b"),
            LibBuiltInFunctions.POW.call(NamedFunction.call("b"), ConstantFunction.of(1).call())
        );
        assertEquals(1.0, BuiltInFunctionTest.call(three, bound,
            ConstantFunction.of(7).call(), IExpression.ref("xs..."), ConstantFunction.of(1).call()));
        // 变参在首位，$(xs...) 同样落在变参位上
        CustomFunction two = CustomFunction.of(List.of("x...", "b"), NamedFunction.call("b"));
        assertEquals(7.0, BuiltInFunctionTest.call(two, bound,
            IExpression.ref("xs..."), ConstantFunction.of(7).call()));

        // 铺开在先时，摊出来的实参从变参位开始吃，末位固定形参照样拿最后一个实参
        CustomFunction narrow = CustomFunction.of(List.of("x...", "b"), NamedFunction.call("b"));
        assertEquals(1.0, BuiltInFunctionTest.call(narrow, bound,
            IExpression.ref("xs..."), ConstantFunction.of(1).call()));

        // 空列表不占实参位：同样写在前面，b 照样拿到那个 1，变参只是空的
        Arguments blank = Arguments.of(List.of(), List.of("xs"), List.of(
            new Arguments.Value.Many(List.of())
        ));
        assertEquals(1.0, BuiltInFunctionTest.call(narrow, blank,
            IExpression.ref("xs..."), ConstantFunction.of(1).call()));
    }

    @Test
    @DisplayName("内建函数的调用照样能回写成 flat 文本")
    void builtInCallsStayWritable() {
        // 加法回写成中缀形式，形参名照旧
        assertEquals("$(a)+$(b)", MathTestBootstrap.writeFlat("add($(a),$(b))"));
        assertEquals(3.0, MathTestBootstrap.parse("add($(a),$(b))").evaluate(
            Arguments.of(List.of(), List.of("a", "b"), List.of(
                new Arguments.Value.Single(1),
                new Arguments.Value.Single(2)
            ))
        ));
        assertEquals("min(1,2,3)", MathTestBootstrap.writeFlat("min(1,2,3)"));
        assertEquals("max(x,1)", MathTestBootstrap.writeFlat("max(x,1)"));
    }

    @Test
    @DisplayName("整个变参列表的 $(x...) 能原样回写")
    void spreadIsWrittenBackAsEllipsis() {
        FunctionExpression spread = LibBuiltInFunctions.MIN.call(IExpression.ref("x..."));
        assertEquals("min($(x...))", MathTestBootstrap.writeFlat(spread));
        assertNotNull(MathTestBootstrap.encode(spread));

        // 单参的 min 就是一个值的列表，值是列表里的 max，也就是它自己
        assertEquals("min(1)", MathTestBootstrap.writeFlat("min(1)"));
        assertEquals(1.0, MathTestBootstrap.parse("min(1)").evaluate(Arguments.of()));
    }

    @Test
    @DisplayName("所有函数类型的 type() 都能解析出注册表里的类型")
    void everyFunctionTypeResolves() {
        // type() 供 DIRECT_CODEC / STREAM_CODEC 的 dispatch 使用。走 DeferredHolder.get() 会经
        // BuiltInRegistries 反查注册表，在注册表没挂上的环境里直接抛 IllegalStateException，所以六个类型
        // 一律走 IFunction.typeOf；这条路径平时只有真正编解码内联函数时才会被触发，必须专门钉住
        assertEquals(LibBuiltInFunctions.Type.class, LibBuiltInFunctions.SQRT.type().getClass());
        assertEquals(InputFunction.Type.class, InputFunction.call(0).function().value().type().getClass());
        assertEquals(NamedFunction.Type.class, NamedFunction.call("a").function().value().type().getClass());
        assertEquals(ConstantFunction.Type.class, ConstantFunction.of(1).call().function().value().type().getClass());
        assertEquals(CustomFunction.Type.class, CustomFunction.named(List.of("a"), NamedFunction.call("a")).type().getClass());
        assertEquals(LambdaFunction.Type.class, LambdaFunction.of(List.of("a"), NamedFunction.call("a")).type().getClass());

        // 六个 DeferredHolder 都必须落在注册表里，否则 dispatch 出来的类型会找不到
        for (DeferredHolder<IFunction.Type<?>, ? extends IFunction.Type<?>> holder : List.of(
            LibBuiltInFunctions.TYPE,
            LibFunctionTypes.INPUT,
            LibFunctionTypes.NAMED,
            LibFunctionTypes.CONSTANT,
            LibFunctionTypes.CUSTOM,
            LibFunctionTypes.LAMBDA
        )) {
            assertEquals(
                holder.getKey(),
                LibRegistries.FUNCTION_TYPE.getResourceKey(IFunction.typeOf(holder.getKey())).orElseThrow(),
                () -> "type() 与注册键对不上: " + holder.getKey()
            );
        }
    }
}
