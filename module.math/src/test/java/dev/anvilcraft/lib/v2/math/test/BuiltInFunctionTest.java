package dev.anvilcraft.lib.v2.math.test;

import dev.anvilcraft.lib.v2.math.expression.Arguments;
import dev.anvilcraft.lib.v2.math.expression.FunctionExpression;
import dev.anvilcraft.lib.v2.math.expression.IExpression;
import dev.anvilcraft.lib.v2.math.expression.function.ConstantFunction;
import dev.anvilcraft.lib.v2.math.expression.function.CustomFunction;
import dev.anvilcraft.lib.v2.math.expression.function.NamedFunction;
import dev.anvilcraft.lib.v2.math.expression.function.Parameters;
import dev.anvilcraft.lib.v2.math.init.LibBuiltInFunctions;
import net.minecraft.core.Holder;
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
        assertEquals(1, LibBuiltInFunctions.MIN.minimumArity());
        assertEquals(Integer.MAX_VALUE, LibBuiltInFunctions.MIN.maximumArity());

        assertEquals(1.0, MathTestBootstrap.parse("min(3,1,2)").evaluate(Arguments.of()));
        assertEquals(3.0, MathTestBootstrap.parse("max(3,1,2)").evaluate(Arguments.of()));
        assertThrows(IllegalArgumentException.class, () -> MathTestBootstrap.parse("min()"));
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

        // $(x...) 是列表，落在固定形参位上会报错；这里列表只摊出一个值，个数正好，
        // 所以拦下它的只能是「列表不能当数字用」这条规则
        Arguments single = Arguments.of(List.of(), List.of("x"), List.of(
            new Arguments.Value.Many(List.of(7.0))
        ));
        FunctionExpression misused = LibBuiltInFunctions.ADD.call(
            IExpression.ref("x..."),
            ConstantFunction.of(1).call()
        );
        assertThrows(IllegalStateException.class, () -> misused.evaluate(single));
    }

    @Test
    @DisplayName("变参可以在形参列表的任意位置")
    void variadicMayAppearAnywhere() {
        Parameters parameters = Parameters.parse(List.of("x...", "last"));
        assertEquals(List.of("x", "last"), parameters.names());
        assertEquals(2, parameters.minimumArity());
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

        // 列表撑出的实参比这一位要的还多时，报可读的校验错误而不是下标越界
        CustomFunction narrow = CustomFunction.of(List.of("x...", "b"), NamedFunction.call("b"));
        assertThrows(IllegalStateException.class, () -> BuiltInFunctionTest.call(narrow, bound,
            ConstantFunction.of(1).call(), IExpression.ref("xs...")));
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
}
