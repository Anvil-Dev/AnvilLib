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
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    @DisplayName("写不回来的注册名一律退回对象形式，不会被静默换掉")
    void unwritableNamesFallBackToObject() {
        // 这些名字写成裸标识符后都读不回同一个函数，必须退回对象形式。
        // 判定依据是解析器自己的规则，所以逐名对照：能写的必须能读回同一棵树，不能写的必须是 null
        MathTestBootstrap.registerFunction("sqrt", CustomFunction.named(
            List.of("a"),
            LibBuiltInFunctions.MULTIPLY.call(NamedFunction.call("a"), ConstantFunction.of(100).call())
        ));
        String[] unwritable = {
            "sqrt",          // 撞内建名：sqrt(4) 会读成内建 sqrt
            "x", "y", "z",   // 撞传入值名：x(5) 会读成 <input 0> * 5
            "x0", "x12",     // 撞带下标的传入值名
            "collision-free", // '-' 不是标识符字符：读到 collision 就停了
            "utils/triple"   // '/' 同理
        };
        for (String path : unwritable) {
            MathTestBootstrap.registerFunction(path, CustomFunction.named(List.of("a"), NamedFunction.call("a")));
            Holder<IFunction> holder = MathTestBootstrap
                .functions()
                .getHolderOrThrow(ResourceKey.create(LibRegistries.FUNCTION_KEY, AnvilLibMath.of(path)));
            FunctionExpression call = FunctionExpression.of(holder, ConstantFunction.of(5).call());
            assertNull(MathTestBootstrap.writeFlat(call), () -> "这个名字不应当写得出 flat 文本: " + path);
            // 退回对象形式后仍要能完整往返
            MathFlatAssertions.assertObjectRoundTrip(call);
        }

        // 合法的 anvillib 名字照旧省命名空间，并且读回来还是同一个函数
        MathTestBootstrap.registerFunction("collisionfree", CustomFunction.named(
            List.of("a"),
            NamedFunction.call("a")
        ));
        Holder<IFunction> free = MathTestBootstrap
            .functions()
            .getHolderOrThrow(ResourceKey.create(LibRegistries.FUNCTION_KEY, AnvilLibMath.of("collisionfree")));
        FunctionExpression writable = FunctionExpression.of(free, ConstantFunction.of(3).call());
        assertEquals("collisionfree(3)", MathTestBootstrap.writeFlat(writable));
        // 关键：写出的文本必须读回同一棵树，而不是只对文本本身断言
        MathFlatAssertions.assertWrittenTextReadsBack(writable);
    }

    @Test
    @DisplayName("非 anvillib 命名空间撞内建名时照旧写全名")
    void otherNamespaceMayShadowBuiltInName() {
        // 解析器对非 anvillib 命名空间是先查注册表、查到就用，所以 mymod:max 撞上内建 max 也能读回；
        // 之前把撞名检查套在所有命名空间上，白白退化成对象形式
        MathTestBootstrap.registerFunction(
            ResourceLocation.parse("mymod:max"),
            CustomFunction.named(List.of("a"), NamedFunction.call("a"))
        );
        Holder<IFunction> holder = MathTestBootstrap.functions().getHolderOrThrow(
            ResourceKey.create(LibRegistries.FUNCTION_KEY, ResourceLocation.parse("mymod:max"))
        );
        FunctionExpression call = FunctionExpression.of(holder, ConstantFunction.of(5).call());
        assertEquals("mymod:max(5)", MathTestBootstrap.writeFlat(call));
        MathFlatAssertions.assertWrittenTextReadsBack(call);

        // 对照：同一个函数写在 anvillib 命名空间下就必须退回对象形式
        MathTestBootstrap.registerFunction("max", CustomFunction.named(List.of("a"), NamedFunction.call("a")));
        Holder<IFunction> shadowed = MathTestBootstrap
            .functions()
            .getHolderOrThrow(ResourceKey.create(LibRegistries.FUNCTION_KEY, AnvilLibMath.of("max")));
        assertNull(MathTestBootstrap.writeFlat(FunctionExpression.of(shadowed, ConstantFunction.of(5).call())));
    }

    @Test
    @DisplayName("数据包函数的实参个数在解析期就校验")
    void datapackArityIsCheckedWhileParsing() {
        // 不校验的话 mymod:twoparams(1) 能正常解析并落进存档，直到求值时才在 BE tick 深处抛错
        MathTestBootstrap.registerFunction("twoparams", CustomFunction.named(
            List.of("a", "b"),
            NamedFunction.call("a")
        ));
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> MathTestBootstrap.parseValue("twoparams(1)")
        );
        assertTrue(
            error.getMessage().contains("Expected 2 arguments but got 1"),
            () -> "应当是实参个数不符的报错: " + error.getMessage()
        );
        // 个数正确时照常解析
        assertEquals(7.0, MathTestBootstrap.parseValue("twoparams(7, 1)").evaluate(Arguments.of()));

        // 变参函数落在区间内不受影响：of 的声明文本里带 "..." 才是变参，named 全是固定形参
        MathTestBootstrap.registerFunction("varargsfn", CustomFunction.of(
            List.of("xs..."),
            NamedFunction.call("xs")
        ));
        assertNotNull(MathTestBootstrap.parseValue("varargsfn(4, 5, 6)"));
        IllegalArgumentException tooFew = assertThrows(
            IllegalArgumentException.class,
            () -> MathTestBootstrap.parseValue("varargsfn()")
        );
        assertTrue(
            tooFew.getMessage().contains("at least 1"),
            () -> "应当是变参下限的报错: " + tooFew.getMessage()
        );

        // 固定形参写成 named 时下限就是形参个数
        MathTestBootstrap.registerFunction("onefixed", CustomFunction.named(
            List.of("only"),
            NamedFunction.call("only")
        ));
        assertNotNull(MathTestBootstrap.parseValue("onefixed(1)"));
        assertThrows(
            IllegalArgumentException.class,
            () -> MathTestBootstrap.parseValue("onefixed()")
        );
    }
}
