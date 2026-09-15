package dev.anvilcraft.lib.v2.math.test;

import dev.anvilcraft.lib.v2.math.AnvilLibMath;
import dev.anvilcraft.lib.v2.math.expression.Arguments;
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
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 数据包自定义函数：参数绑定、参数个数校验、变参与递归保护。
 */
class CustomFunctionTest {
    @BeforeAll
    static void setUp() {
        MathTestBootstrap.initialize();
    }

    /**
     * 直接求值一次自定义函数调用。
     */
    private static double call(CustomFunction function, double... arguments) {
        return FunctionExpression
            .of(Holder.direct(function), MathTestBootstrap.constants(arguments).toArray(IExpression[]::new))
            .evaluate(Arguments.of());
    }

    @Test
    @DisplayName("参数按声明顺序绑定，函数体里的 $(name) 取到实参")
    void parametersBindInOrder() {
        CustomFunction function = CustomFunction.named(
            List.of("a", "b"),
            LibBuiltInFunctions.ADD.call(NamedFunction.call("a"), NamedFunction.call("b"))
        );
        assertEquals(3.0, CustomFunctionTest.call(function, 1, 2));
        // 交换实参顺序结果不同，确认不是按下标错位绑定
        assertEquals(5.0, CustomFunctionTest.call(function, 4, 1));
    }

    @Test
    @DisplayName("函数体仍能读到调用点的传入值")
    void bodyStillSeesCallSiteInputs() {
        CustomFunction function = CustomFunction.named(
            List.of("a"),
            LibBuiltInFunctions.ADD.call(NamedFunction.call("a"), InputFunction.call(0))
        );
        assertEquals(12.0, function.apply(MathTestBootstrap.constants(10), Arguments.of(2)));
    }

    @Test
    @DisplayName("参数个数不符时直接报错，不会把缺失参数当成 0")
    void arityMismatchFails() {
        CustomFunction function = CustomFunction.named(
            List.of("a", "b"),
            LibBuiltInFunctions.ADD.call(NamedFunction.call("a"), NamedFunction.call("b"))
        );
        assertThrows(IllegalArgumentException.class, () -> CustomFunctionTest.call(function, 1));
        assertThrows(IllegalArgumentException.class, () -> CustomFunctionTest.call(function));
        assertThrows(IllegalArgumentException.class, () -> CustomFunctionTest.call(function, 1, 2, 3));
    }

    @Test
    @DisplayName("重复或空的参数名被拒绝")
    void invalidParametersRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> CustomFunction.named(List.of("a", "a"), NamedFunction.call("a"))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> CustomFunction.named(List.of(""), NamedFunction.call("a"))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> CustomFunction.of(List.of("..."), NamedFunction.call("a"))
        );
    }

    @Test
    @DisplayName("零参自定义函数可以直接求值")
    void nullaryFunctionEvaluates() {
        CustomFunction function = CustomFunction.named(
            List.of(),
            LibBuiltInFunctions.SQRT.call(ConstantFunction.of(9).call())
        );
        assertEquals(3.0, CustomFunctionTest.call(function));
    }

    @Test
    @DisplayName("变参声明在 parameters 里写 ...，吃下从它这一位开始的全部实参")
    void variadicParameterTakesTheRest() {
        // 变参不在末位：a 绑第一个实参，x 绑住剩下的全部
        CustomFunction function = CustomFunction.of(
            List.of("a", "x..."),
            LibBuiltInFunctions.MIN.call(IExpression.ref("x..."))
        );
        assertEquals(2.0, CustomFunctionTest.call(function, 9, 5, 2, 7));
        assertEquals(5.0, CustomFunctionTest.call(function, 9, 5));
        // 变参至少要有一个实参，只剩 a 一个时个数不符
        assertThrows(IllegalArgumentException.class, () -> CustomFunctionTest.call(function, 9));
        assertThrows(IllegalArgumentException.class, () -> CustomFunctionTest.call(function));
    }

    @Test
    @DisplayName("函数体里 $(x) 取变参里的最大值，$(x...) 才能拿到整个列表")
    void variadicAggregatesByName() {
        CustomFunction max = CustomFunction.of(List.of("x..."), NamedFunction.call("x"));
        assertEquals(7.0, CustomFunctionTest.call(max, 3, 7, 2));

        // $(x...) 是列表，只有变参函数接得住，别处用会报错
        CustomFunction spread = CustomFunction.of(
            List.of("x..."),
            LibBuiltInFunctions.MIN.call(IExpression.ref("x..."))
        );
        assertEquals(2.0, CustomFunctionTest.call(spread, 3, 7, 2));

        // $(x...) 是列表，只有变参函数接得住；喂一个值让个数正好，剩下的只能是列表用法本身出错
        CustomFunction misused = CustomFunction.of(
            List.of("x..."),
            LibBuiltInFunctions.ADD.call(IExpression.ref("x..."), ConstantFunction.of(1).call())
        );
        assertThrows(IllegalStateException.class, () -> CustomFunctionTest.call(misused, 3));
    }

    @Test
    @DisplayName("自定义函数拆不开自己的变参：$(x...) 只能交给下一个变参函数，forEach 是唯一的出口")
    void variadicCannotBeUnpackedInsideItsOwnBody() {
        // $(x...) 落在固定形参位上是列表用法错误
        CustomFunction intoFixed = CustomFunction.of(
            List.of("x..."),
            LibBuiltInFunctions.ADD.call(IExpression.ref("x..."), ConstantFunction.of(1).call())
        );
        assertThrows(IllegalStateException.class, () -> CustomFunctionTest.call(intoFixed, 3));

        // $(x) 只能折叠成一个数（列表最大值），不是逐个取值
        CustomFunction aggregate = CustomFunction.of(
            List.of("x..."),
            LibBuiltInFunctions.MULTIPLY.call(NamedFunction.call("x"), ConstantFunction.of(2).call())
        );
        assertEquals(14.0, CustomFunctionTest.call(aggregate, 3, 7, 2));

        // 要逐个拆开只能把 $(x...) 交给 forEach，由它一次喂一个值给 lambda
        CustomFunction viaForEach = CustomFunction.of(
            List.of("x..."),
            LibBuiltInFunctions.FOREACH.call(
                IExpression.ref("x..."),
                LambdaFunction.call("item", LibBuiltInFunctions.MULTIPLY.call(
                    NamedFunction.call("item"),
                    ConstantFunction.of(2).call()
                ))
            )
        );
        assertEquals(24.0, CustomFunctionTest.call(viaForEach, 3, 7, 2));
    }

    @Test
    @DisplayName("一个签名里出现两个变参直接报错")
    void multipleVariadicsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> CustomFunction.of(List.of("x...", "y..."), NamedFunction.call("x"))
        );
    }

    @Test
    @DisplayName("函数体直接引用自身时报错，而不是栈溢出")
    void selfReferenceIsCaught() {
        // 占位函数先注册，拿到引用后再把函数体换成对自身的调用
        CustomFunction placeholder = CustomFunction.named(List.of("a"), NamedFunction.call("a"));
        Holder<IFunction> self = MathTestBootstrap.registerFunction("self", placeholder);
        CustomFunction recursive = CustomFunction.named(
            List.of("a"),
            FunctionExpression.of(self, NamedFunction.call("a"))
        );
        MathTestBootstrap.replaceFunction("self", recursive);

        FunctionExpression call = FunctionExpression.of(self, ConstantFunction.of(1).call());
        assertThrows(IllegalStateException.class, () -> call.evaluate(Arguments.of()));
    }

    @Test
    @DisplayName("互相引用的两个函数也会被拦下")
    void mutualReferenceIsCaught() {
        CustomFunction placeholderA = CustomFunction.named(List.of("a"), NamedFunction.call("a"));
        CustomFunction placeholderB = CustomFunction.named(List.of("a"), NamedFunction.call("a"));
        Holder<IFunction> first = MathTestBootstrap.registerFunction("first", placeholderA);
        Holder<IFunction> second = MathTestBootstrap.registerFunction("second", placeholderB);
        MathTestBootstrap.replaceFunction(
            "first",
            CustomFunction.named(List.of("a"), FunctionExpression.of(second, NamedFunction.call("a")))
        );
        MathTestBootstrap.replaceFunction(
            "second",
            CustomFunction.named(List.of("a"), FunctionExpression.of(first, NamedFunction.call("a")))
        );

        FunctionExpression call = FunctionExpression.of(first, ConstantFunction.of(1).call());
        assertThrows(IllegalStateException.class, () -> call.evaluate(Arguments.of()));
    }

    @Test
    @DisplayName("注册表里能按注册名找到自定义函数")
    void customFunctionIsRegistered() {
        CustomFunction function = CustomFunction.named(
            List.of("a"),
            LibBuiltInFunctions.MULTIPLY.call(NamedFunction.call("a"), ConstantFunction.of(3).call())
        );
        MathTestBootstrap.registerFunction("triple", function);
        IFunction registered = MathTestBootstrap
            .functions()
            .getHolderOrThrow(ResourceKey.create(LibRegistries.FUNCTION_KEY, AnvilLibMath.of("triple")))
            .value();
        assertEquals(function, registered);
        assertEquals(6.0, registered.apply(MathTestBootstrap.constants(2), Arguments.of()));
    }
}
