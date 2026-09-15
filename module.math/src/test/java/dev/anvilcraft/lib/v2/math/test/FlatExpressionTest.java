package dev.anvilcraft.lib.v2.math.test;

import dev.anvilcraft.lib.v2.math.expression.FunctionExpression;
import dev.anvilcraft.lib.v2.math.expression.IExpression;
import dev.anvilcraft.lib.v2.math.expression.function.ConstantFunction;
import dev.anvilcraft.lib.v2.math.expression.function.InputFunction;
import dev.anvilcraft.lib.v2.math.init.LibBuiltInFunctions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * flat 文本的写出与读回必须对称：写出结果再读回来，值不能变，结构也要一样。
 *
 * <p>覆盖的都是真实踩过的坑：常量并置写成 {@code 2*3 -> 23}、负字面量写成 {@code pow(-2,2) -> -2^2}、
 * 指数记法写成 {@code 0.0001 -> 1.0E-4}、负零被写丢符号位。</p>
 */
class FlatExpressionTest {
    @BeforeAll
    static void setUp() {
        MathTestBootstrap.initialize();
    }

    @Test
    @DisplayName("常量之间的乘法不会被写成并置")
    void constantsAreNotJuxtaposed() {
        MathFlatAssertions.assertRoundTrip("2*3");
        MathFlatAssertions.assertRoundTrip("2*0.5");
        MathFlatAssertions.assertRoundTrip("0.5*4");
        MathFlatAssertions.assertRoundTrip("2.5*2.5");
        assertEquals("2*3", MathTestBootstrap.writeFlat("2*3"));
    }

    @Test
    @DisplayName("并置乘法只在右侧能直接相接时使用")
    void juxtapositionOnlyForAttachableOperands() {
        assertEquals("2x", MathTestBootstrap.writeFlat("2*x"));
        assertEquals("2(x+1)", MathTestBootstrap.writeFlat("2(x+1)"));
        MathFlatAssertions.assertRoundTrip("2*x");
        MathFlatAssertions.assertRoundTrip("2(x+1)");
        MathFlatAssertions.assertRoundTrip("2(x+1)+1");
    }

    @Test
    @DisplayName("负常量写成一元负号，操作数位置补括号")
    void negativeConstantsRoundTrip() {
        MathFlatAssertions.assertObjectRoundTrip(LibBuiltInFunctions.POW, constant(-2), constant(2));
        MathFlatAssertions.assertObjectRoundTrip(LibBuiltInFunctions.MULTIPLY, constant(2), constant(-1));
        MathFlatAssertions.assertObjectRoundTrip(LibBuiltInFunctions.MULTIPLY, constant(-1), input(0));
        MathFlatAssertions.assertObjectRoundTrip(LibBuiltInFunctions.ADD, constant(-1), constant(-2));
        MathFlatAssertions.assertObjectRoundTrip(LibBuiltInFunctions.SUBTRACT, constant(1), constant(-2));
        MathFlatAssertions.assertObjectRoundTrip(LibBuiltInFunctions.POW, constant(-2), constant(-3));
    }

    @Test
    @DisplayName("写出的负底数带括号，读回来仍是负底数")
    void negativeBaseKeepsParentheses() {
        FunctionExpression power = LibBuiltInFunctions.POW.call(constant(-2), constant(2));
        String written = MathTestBootstrap.writeFlat(power);
        assertEquals("(-2)^2", written);
        assertNotNull(written);
        assertEquals(4.0, MathTestBootstrap.parse(written).evaluate());
        // 不带括号的写法仍是一元负号优先于乘方
        assertEquals(-4.0, MathTestBootstrap.parse("-2^2").evaluate());
    }

    @Test
    @DisplayName("极值与极小值用能读回的写法")
    void exponentNotationRoundTrips() {
        MathFlatAssertions.assertRoundTrip("0.0001+x");
        MathFlatAssertions.assertRoundTrip("10000000+x");
        MathFlatAssertions.assertRoundTrip("x*10000000");
        MathFlatAssertions.assertRoundTrip("sqrt(0.0001)");
        MathFlatAssertions.assertRoundTrip("x*0.000123456789");
        MathFlatAssertions.assertRoundTrip("-5.0E-4+x");
        MathFlatAssertions.assertRoundTrip("-1.0E7+x");
        assertEquals("sqrt(0.0001)", MathTestBootstrap.writeFlat(MathTestBootstrap.parse("sqrt(0.0001)")));
    }

    @Test
    @DisplayName("负零保留符号位")
    void negativeZeroKeepsSign() {
        assertEquals("0", MathTestBootstrap.writeFlat(constant(0.0)));
        // -0 会读回 0-0，值变成正零，只能写小数写法
        assertEquals("-0.0", MathTestBootstrap.writeFlat(constant(-0.0)));
        for (String source : new String[]{"-0.0", "sqrt(-0.0)"}) {
            double value = MathTestBootstrap.parse(source).evaluate();
            assertEquals(
                Double.doubleToRawLongBits(-0.0),
                Double.doubleToRawLongBits(value),
                () -> "负零读回来不能变成正零: " + source
            );
        }
        MathFlatAssertions.assertRoundTrip("sqrt(-0.0)");
    }

    @Test
    @DisplayName("取负一个负常量写得成 -(n)，读回来还是同结构")
    void negationOfNegativeConstantStaysWritable() {
        // 0-(-0.71) 写成 -(-0.71)：括号保证不会变成不合法的 --0.71
        FunctionExpression tree = LibBuiltInFunctions.SUBTRACT.call(
            constant(0.0),
            ConstantFunction.of(-0.7107560582693808).call()
        );
        assertEquals("-(-0.7107560582693808)", MathTestBootstrap.writeFlat(tree));
        MathFlatAssertions.assertRoundTrip("-(-0.5)");
        MathFlatAssertions.assertRoundTrip("0-(-1)");
        MathFlatAssertions.assertRoundTrip("x*(-(-2))");
        MathFlatAssertions.assertRoundTrip("sqrt(-(-0.25))");
    }

    @Test
    @DisplayName("溢出成无穷的字面量在解析期就被拒绝")
    void nonFiniteLiteralRejected() {
        // 回写侧写不出非有限值，解析侧再收下它就成了「读得进来写不回去」
        assertThrows(IllegalArgumentException.class, () -> MathTestBootstrap.parseValue("1e99999"));
        assertThrows(IllegalArgumentException.class, () -> MathTestBootstrap.parseValue("1e309"));
        // 报错要指出整个字面量与它在文本里的结束位置，不能只截出尾数、也不能指回开头
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> MathTestBootstrap.parseValue("12+1e99999")
        );
        assertTrue(error.getMessage().contains("'1e99999'"), () -> "应当写出整个字面量: " + error.getMessage());
        assertTrue(error.getMessage().contains("at position 10"), () -> "应当指向字面量末尾: " + error.getMessage());
    }

    @Test
    @DisplayName("嵌套过深时报可读错误，不让 StackOverflowError 穿出 codec")
    void tooDeeplyNestedIsRejected() {
        // 递归下降遇到上万层括号或乘方会打穿栈；StackOverflowError 是 Error，
        // parseResult 的 catch (RuntimeException) 拦不住，会直接打到数据包加载流程
        for (String deep : new String[]{
            "(".repeat(5000) + "1" + ")".repeat(5000),
            "1" + "^1".repeat(5000)
        }) {
            IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> MathTestBootstrap.parseValue(deep)
            );
            assertTrue(
                error.getMessage().contains("nests too deeply"),
                () -> "应当是嵌套过深的报错: " + error.getMessage()
            );
        }
        // 正常深度不受影响
        MathFlatAssertions.assertRoundTrip("((((1+2))))*3");
        MathFlatAssertions.assertRoundTrip("2^2^2");
    }

    @Test
    @DisplayName("一元符号链也受嵌套上限约束")
    void unarySymbolChainIsBounded() {
        // parseUnary 每个符号递归一帧、且不经过 parseLambda/parsePower，
        // 所以它必须自己也过一遍深度守卫，否则 "-"*20000 照样打穿栈
        for (String symbol : new String[]{"-", "+"}) {
            IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> MathTestBootstrap.parseValue(symbol.repeat(20000) + "1")
            );
            assertTrue(
                error.getMessage().contains("nests too deeply"),
                () -> "应当是嵌套过深的报错: " + error.getMessage()
            );
            // 短符号链照旧正常
            MathFlatAssertions.assertRoundTrip(symbol.repeat(100) + "1");
        }
    }

    @Test
    @DisplayName("括号嵌套的可用层数钉在 169 层")
    void parenthesisNestingBoundaryIsPinned() {
        // 一层括号要过 parseLambda/parseUnary/parsePower 三处守卫，各计一次，
        // 所以 512 的计数对应 169 层实嵌套。这个边界必须钉住：
        // 挪动任何一处守卫都会静默改变可用深度，而这类表达式多是程序生成的
        String allowed = "sqrt(".repeat(169) + "1" + ")".repeat(169);
        assertNotNull(MathTestBootstrap.parseValue(allowed));

        String tooDeep = "sqrt(".repeat(170) + "1" + ")".repeat(170);
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> MathTestBootstrap.parseValue(tooDeep)
        );
        assertTrue(
            error.getMessage().contains("nests too deeply"),
            () -> "应当是嵌套过深的报错: " + error.getMessage()
        );
    }

    @Test
    @DisplayName("带符号的字面量整段都能读")
    void signedLiteralsParse() {
        assertEquals(-2.0, MathTestBootstrap.parse("-2").evaluate());
        assertEquals(-0.0005, MathTestBootstrap.parse("-5.0E-4").evaluate());
        assertEquals(0.25, MathTestBootstrap.parse("2^-2").evaluate());
        assertEquals(2.0, MathTestBootstrap.parse("+2").evaluate());
        assertEquals(-4.0, MathTestBootstrap.parse("-2^2").evaluate());
        assertEquals(4.0, MathTestBootstrap.parse("(-2)^2").evaluate());
        assertEquals(-2.0, MathTestBootstrap.parse("0-2").evaluate());
    }

    @Test
    @DisplayName("乘方后接指数记法不会截断")
    void powerWithExponentNotation() {
        assertEquals(Double.POSITIVE_INFINITY, MathTestBootstrap.parse("2^1E293").evaluate());
        // 负底数被一元负号挡在外面，所以是 -(9999999^1.6E293)
        assertEquals(Double.NEGATIVE_INFINITY, MathTestBootstrap.parse("-9999999^1.6E293").evaluate());
        assertEquals(Double.NEGATIVE_INFINITY, MathTestBootstrap.parse("min(-9999999^1E293,1)").evaluate());
    }

    @Test
    @DisplayName("对象形式与 flat 文本互相可读")
    void objectAndFlatFormsAgree() {
        FunctionExpression call = LibBuiltInFunctions.ADD.call(
            LibBuiltInFunctions.MULTIPLY.call(constant(2), input(0)),
            constant(1)
        );
        assertEquals("2x+1", MathTestBootstrap.writeFlat(call));
        MathFlatAssertions.assertObjectRoundTrip(call);
        MathFlatAssertions.assertFullRoundTrip(call);
    }

    @Test
    @DisplayName("随机表达式树往返不变")
    void randomTreesRoundTrip() {
        Random random = new Random(20240607L);
        int rounds = Integer.getInteger("math.fuzz.rounds", 20000);
        for (int round = 0; round < rounds; round++) {
            MathFlatAssertions.assertFullRoundTrip(MathFlatAssertions.randomTree(random, 0));
        }
    }

    @Test
    @DisplayName("lambda 落在运算符操作数位置的随机树也往返不变")
    void randomLambdaOperandTreesRoundTrip() {
        // randomTree 的叶子集合里放不进 lambda：整棵树会被求值，而 lambda 在顶层不可求值。
        // 所以这里专门构造「二元运算 + lambda 操作数」，把上一轮漏掉的维度补进随机往返。
        // 用只比结构不求值的断言：lambda 操作数是待绑定的值，整棵树本来就不可求值
        Random random = new Random(20240608L);
        int rounds = Integer.getInteger("math.fuzz.rounds", 20000);
        LibBuiltInFunctions[] operators = {
            LibBuiltInFunctions.ADD,
            LibBuiltInFunctions.SUBTRACT,
            LibBuiltInFunctions.MULTIPLY,
            LibBuiltInFunctions.DIVIDE,
            LibBuiltInFunctions.POW
        };
        for (int round = 0; round < rounds; round++) {
            LibBuiltInFunctions operator = operators[random.nextInt(operators.length)];
            IExpression left = random.nextBoolean()
                ? MathFlatAssertions.randomVariadicLambda(random)
                : MathFlatAssertions.randomTree(random, 2);
            IExpression right = random.nextBoolean()
                ? MathFlatAssertions.randomVariadicLambda(random)
                : MathFlatAssertions.randomTree(random, 2);
            MathFlatAssertions.assertStructuralRoundTrip(operator.call(left, right));
        }
    }

    private static FunctionExpression constant(double value) {
        return ConstantFunction.of(value).call();
    }

    private static FunctionExpression input(@SuppressWarnings("SameParameterValue") int index) {
        return InputFunction.call(index);
    }
}
