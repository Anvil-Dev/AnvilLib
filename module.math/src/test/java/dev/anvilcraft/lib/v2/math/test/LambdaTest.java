package dev.anvilcraft.lib.v2.math.test;

import dev.anvilcraft.lib.v2.math.expression.Arguments;
import dev.anvilcraft.lib.v2.math.expression.FunctionExpression;
import dev.anvilcraft.lib.v2.math.expression.IExpression;
import dev.anvilcraft.lib.v2.math.expression.function.ConstantFunction;
import dev.anvilcraft.lib.v2.math.expression.function.CustomFunction;
import dev.anvilcraft.lib.v2.math.expression.function.LambdaFunction;
import dev.anvilcraft.lib.v2.math.expression.function.NamedFunction;
import dev.anvilcraft.lib.v2.math.init.LibBuiltInFunctions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * lambda 语法（{@code ->}）与 {@code forEach}。
 */
class LambdaTest {
    @BeforeAll
    static void setUp() {
        MathTestBootstrap.initialize();
    }

    @Test
    @DisplayName("单参 lambda 解析成 LambdaFunction 并原样回写")
    void singleParameterLambdaParsesAndWrites() {
        IExpression expression = MathTestBootstrap.parseValue("x -> $(x)*2");
        assertTrue(expression instanceof FunctionExpression call && call.function().value() instanceof LambdaFunction);
        assertEquals("x -> $(x)*2", MathTestBootstrap.writeFlatValue("x -> $(x)*2"));
    }

    @Test
    @DisplayName("多参 lambda 写成括号里的参数列表")
    void multipleParametersAreParenthesized() {
        assertEquals("(a, b) -> $(a)+$(b)", MathTestBootstrap.writeFlatValue("(a, b) -> $(a)+$(b)"));
    }

    @Test
    @DisplayName("lambda 的形参名以 ... 结尾时是变参")
    void variadicLambdaParameter() {
        assertEquals("x... -> min($(x...))", MathTestBootstrap.writeFlatValue("x... -> min($(x...))"));
        // lambda 不能出现在顶层：它没有实参可绑，只能被别的函数调用
        IExpression topLevel = MathTestBootstrap.parseValue("x -> $(x)");
        assertThrows(IllegalArgumentException.class, () -> topLevel.evaluate(Arguments.of(1.0, 2.0)));
        // lambda 自己拆不开变参：它被调用时实参已经摊平，$(x...) 在体里只能喂给下一个变参函数
        FunctionExpression nested = LibBuiltInFunctions.FOREACH.call(
            MathTestBootstrap.parseValue("min($(x...))"),
            MathTestBootstrap.parseValue("x... -> add($(x...),1)")
        );
        assertThrows(IllegalArgumentException.class, () -> nested.evaluate(Arguments.of()));
        // $(x...) 是列表，落在固定形参位上会被拦下；变参位接得住，于是摊成 min(3, 7)
        Arguments many = Arguments.of(List.of(), List.of("x"), List.of(
            new Arguments.Value.Many(List.of(3.0, 7.0))
        ));
        IExpression spreadIntoLambda = MathTestBootstrap.parseValue("x... -> min($(x...))");
        assertEquals(3.0, MathTestBootstrap.callLambda(
            spreadIntoLambda,
            List.of(IExpression.ref("x...")),
            many
        ));
    }

    @Test
    @DisplayName("减法不会被误当成 lambda 的箭头")
    void subtractionIsNotAnArrow() {
        assertEquals("x-1", MathTestBootstrap.writeFlatValue("x-1"));
        // 空格只是排版上的差异，回写一律是规范形式
        assertEquals("x-1", MathTestBootstrap.writeFlatValue("x - 1"));
        assertEquals(4.0, MathTestBootstrap.parseValue("x - 1").evaluate(Arguments.of(5)));
    }

    @Test
    @DisplayName("嵌套 lambda 按右结合解析")
    void nestedLambdasAssociateRight() {
        assertEquals("x -> y -> $(x)+$(y)", MathTestBootstrap.writeFlatValue("x -> y -> $(x)+$(y)"));
    }

    @Test
    @DisplayName("forEach 把列表里的每个值交给 lambda，返回累加和")
    void forEachSumsLambdaResults() {
        assertEquals(12.0, MathTestBootstrap.parseValue("forEach(1,2,3, x -> $(x)*2)").evaluate(Arguments.of()));
        assertEquals(9.0, MathTestBootstrap
            .parseValue("forEach(1,2,3, x -> $(x)+$(base))")
            .evaluate(Arguments.of(List.of(), List.of("base"), List.of(new Arguments.Value.Single(1)))));
        assertEquals(6.0, MathTestBootstrap.parseValue("forEach(1,2,3, x -> 2)").evaluate(Arguments.of()));
    }

    @Test
    @DisplayName("forEach 的末位必须是 lambda")
    void forEachRequiresALambda() {
        // 末位是 $(x) 而不是 lambda，求值时直接报错
        FunctionExpression call = LibBuiltInFunctions.FOREACH.call(
            ConstantFunction.of(1).call(),
            NamedFunction.call("x")
        );
        assertThrows(IllegalArgumentException.class, () -> call.evaluate(Arguments.of()));
    }

    @Test
    @DisplayName("lambda 能读到外层的名字（闭包）")
    void lambdaInheritsOuterBindings() {
        // factor 来自调用点，item 来自 forEach
        assertEquals(24.0, MathTestBootstrap
            .parseValue("forEach(1,2,3, item -> $(item)*$(factor))")
            .evaluate(Arguments.of(List.of(), List.of("factor"), List.of(new Arguments.Value.Single(4)))));
    }

    @Test
    @DisplayName("自定义函数能把拆不开的变参交给 forEach")
    void customFunctionForwardsVariadicToForEach() {
        CustomFunction function = CustomFunction.of(
            List.of("x..."),
            LibBuiltInFunctions.FOREACH.call(
                IExpression.ref("x..."),
                LambdaFunction.call("item", LibBuiltInFunctions.MULTIPLY.call(
                    NamedFunction.call("item"),
                    ConstantFunction.of(2).call()
                ))
            )
        );
        assertEquals(12.0, function.apply(MathTestBootstrap.constants(1, 2, 3), Arguments.of()));
        // 变参至少要有一个实参可吃
        assertThrows(IllegalArgumentException.class, () -> function.apply(List.of(), Arguments.of()));
    }

    @Test
    @DisplayName("lambda 自己拆不开变参：forEach 一次只喂一个值")
    void forEachRejectsVariadicLambda() {
        FunctionExpression call = LibBuiltInFunctions.FOREACH.call(
            ConstantFunction.of(1).call(),
            FunctionExpression.of(LambdaFunction.of(List.of("x..."), NamedFunction.call("x")))
        );
        assertThrows(IllegalArgumentException.class, () -> call.evaluate(Arguments.of()));
    }
    @Test
    @DisplayName("lambda 作为实参照样能写进 flat 文本")
    void lambdaArgumentsStayWritable() {
        FunctionExpression call = LibBuiltInFunctions.FOREACH.call(
            ConstantFunction.of(1).call(),
            ConstantFunction.of(2).call(),
            LambdaFunction.call("x", NamedFunction.call("x"))
        );
        assertEquals("foreach(1,2,x -> $(x))", MathTestBootstrap.writeFlat(call));
        assertEquals(3.0, MathTestBootstrap.parse("forEach(1,2,x -> $(x))").evaluate(Arguments.of()));
    }

    @Test
    @DisplayName("合起来：自定义变参函数 + forEach 能整棵回写")
    void wholeTreeRoundTrips() {
        String source = "foreach(1,2,3,x -> $(x)*2)";
        assertEquals(source, MathTestBootstrap.writeFlat(source));
        assertEquals(
            MathTestBootstrap.parseValue(source).evaluate(Arguments.of()),
            MathTestBootstrap.parseValue(Objects.requireNonNull(MathTestBootstrap.writeFlat(source))).evaluate(Arguments.of())
        );
        assertEquals(12.0, MathTestBootstrap.parseValue(source).evaluate(Arguments.of()));
    }
}
