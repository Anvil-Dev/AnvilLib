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

import java.util.ArrayList;
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
        // 变参可以一个实参都不吃，此时 $(x...) 绑成空列表，min 取不到值返回 0
        assertEquals(0.0, CustomFunctionTest.call(function, 9));
        // a 是固定形参，一个都不能少
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

        // $(x...) 是列表，只有变参函数接得住；add 两个形参都是固定的，
        // 这种函数体在构造时就按个数不符被拦下，而不是留到求值期才炸
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> CustomFunction.of(
                List.of("x..."),
                LibBuiltInFunctions.ADD.call(IExpression.ref("x..."), ConstantFunction.of(1).call())
            )
        );
        assertTrue(
            error.getMessage().contains("Expected 2 arguments but got 1"),
            () -> "应当是实参个数不符的报错: " + error.getMessage()
        );
    }

    @Test
    @DisplayName("自定义函数拆不开自己的变参：$(x...) 只能交给下一个变参函数，forEach 是唯一的出口")
    void variadicCannotBeUnpackedInsideItsOwnBody() {
        // $(x...) 落在固定形参位上是列表用法错误；add 两个形参都固定，
        // 这种函数体在构造时就按个数不符被拦下
        assertThrows(
            IllegalArgumentException.class,
            () -> CustomFunction.of(
                List.of("x..."),
                LibBuiltInFunctions.ADD.call(IExpression.ref("x..."), ConstantFunction.of(1).call())
            )
        );

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
        // 变参按 Java 的变参语义，可以一个实参都不给
        assertEquals(0.0, MathTestBootstrap.parseValue("varargsfn()").evaluate(Arguments.of()));
        assertEquals(0.0, MathTestBootstrap.parseValue("varargsfn()").evaluate(
            Arguments.of(List.of(), List.of("xs"), List.of(new Arguments.Value.Many(List.of(3.0, 7.0))))
        ));

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

    @Test
    @DisplayName("$(x...) 按区间参与解析期校验，不再被记成 1 个实参")
    void spreadCountsAsARangeNotOne() {
        MathTestBootstrap.registerFunction("twofixed", CustomFunction.named(
            List.of("a", "b"),
            NamedFunction.call("a")
        ));
        // $(x...) 是整份列表，bind 只肯把它交给变参形参，固定形参位一个都接不住。
        // 所以没有变参的函数无论给几个实参都不合法，解析期就该报出来
        for (String source : new String[]{"twofixed($(x...))", "twofixed($(x...),1)", "twofixed($(x...),0)"}) {
            IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> MathTestBootstrap.parseValue(source),
                () -> "固定形参位接不住列表，解析期就该拒绝: " + source
            );
            assertTrue(
                error.getMessage().contains("Expected 2 arguments"),
                () -> "应当是实参个数不符的报错: " + error.getMessage()
            );
        }

        // 内建函数与数据包函数报错口径一致：都按「非铺开实参个数 + 铺开算 0 个」表述
        IllegalArgumentException builtin = assertThrows(
            IllegalArgumentException.class,
            () -> MathTestBootstrap.parseValue("sqrt($(x...))")
        );
        assertTrue(
            builtin.getMessage().startsWith("function 'anvillib:sqrt' Expected 1 arguments but got 0"),
            () -> "内建函数也应当按同一条规则报错: " + builtin.getMessage()
        );
        // 有变参形参时列表长度未知，解析期放行，长度不合适由求值期按真实长度判定
        MathTestBootstrap.registerFunction("twofixedsum", CustomFunction.of(
            List.of("a", "b", "rest..."),
            NamedFunction.call("a")
        ));
        assertNotNull(MathTestBootstrap.parseValue("twofixedsum($(x...),1,2)"));
    }

    @Test
    @DisplayName("名字没绑定成列表时点名报错，而不是看不懂的个数不符")
    void unboundSpreadNameIsReportedClearly() {
        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> MathTestBootstrap.parseValue("min($(unbound...))").evaluate(Arguments.of())
        );
        assertTrue(
            error.getMessage().contains("is not bound to a list"),
            () -> "应当点明名字没绑定: " + error.getMessage()
        );

        // 名字绑成空列表是合法的空变参调用，与「名字写错」必须分得开：
        // min 取不到任何值，按函数自己的兜底返回 0
        assertEquals(0.0, MathTestBootstrap.parseValue("min($(empty...))").evaluate(
            Arguments.of(List.of(), List.of("empty"), List.of(new Arguments.Value.Many(List.of())))
        ));
    }

    @Test
    @DisplayName("变参可以一个实参都不给：\"x...\": [] 是合法的空调用")
    void emptyVariadicCallIsLegal() {
        // 变参的下限是 0，所以 min() 这种「一个值都没有」的写法解析期与求值期都放行，
        // 取不到值时由函数自己兜底
        assertEquals(0.0, MathTestBootstrap.parseValue("min()").evaluate(Arguments.of()));
        assertEquals(0.0, MathTestBootstrap.parseValue("max()").evaluate(Arguments.of()));

        // 绑定成空列表与上面等价
        Arguments empty = Arguments.of(
            List.of(), List.of("x"), List.of(new Arguments.Value.Many(List.of()))
        );
        assertEquals(0.0, MathTestBootstrap.parseValue("min($(x...))").evaluate(empty));

        // 下限为 0 只针对变参：固定形参照样一个都不能少
        MathTestBootstrap.registerFunction("onefixed", CustomFunction.named(
            List.of("only"),
            NamedFunction.call("only")
        ));
        IllegalArgumentException missing = assertThrows(
            IllegalArgumentException.class,
            () -> MathTestBootstrap.parseValue("onefixed()")
        );
        assertTrue(
            missing.getMessage().contains("Expected 1 arguments"),
            () -> "固定形参应当报个数不符: " + missing.getMessage()
        );

        // 固定形参与变参混在一起时，下限就是固定形参个数
        MathTestBootstrap.registerFunction("twoplus", CustomFunction.of(
            List.of("a", "b", "rest..."),
            NamedFunction.call("a")
        ));
        assertEquals(1.0, MathTestBootstrap.parseValue("twoplus(1,2)").evaluate(Arguments.of()));
        assertThrows(IllegalArgumentException.class, () -> MathTestBootstrap.parseValue("twoplus(1)"));
    }

    @Test
    @DisplayName("$(name) 载荷写不出文本时退回对象形式")
    void unwritableReferenceNamesFallBackToObject() {
        // $(x...) 的载荷是「取一个数字」，而解析器看到 "..." 后缀会读成 Spread（整份列表），
        // 语义变了、求值时还会抛异常；带空格或 ')' 的名字则直接读不回来。两种都必须退回对象形式。
        // 名字是 x... 的具名引用只能从对象形式构造：flat 文本里的 $(x...) 本来就是 Spread
        List<IExpression> unwritable = new ArrayList<>();
        unwritable.add(FunctionExpression.of(NamedFunction.of("x...")));
        for (String name : new String[]{"a)b", "a b", "x."}) {
            unwritable.add(FunctionExpression.of(NamedFunction.of(name)));
        }
        for (IExpression bare : unwritable) {
            assertNull(
                MathTestBootstrap.writeFlat(bare),
                () -> bare + " 不应当写得出 flat 文本"
            );
            // 包一层 sqrt：对象形式里也带上这个引用，才测得出它有没有被内联成 flat 文本
            FunctionExpression call = LibBuiltInFunctions.SQRT.call(bare);
            assertNull(
                MathTestBootstrap.writeFlat(call),
                () -> "含 " + bare + " 的调用不应当写得出 flat 文本"
            );
            MathFlatAssertions.assertObjectRoundTrip(call);
        }

        // $(x...) 本身是 Spread，写得出也读得回，不能跟上面那种混为一谈
        IExpression spread = MathTestBootstrap.parseNamed("x...");
        assertEquals("$(x...)", MathTestBootstrap.writeFlat(spread));

        // 正常名字照旧写得出来，并且必须读回同一棵树
        for (String name : new String[]{"cost", "x0", "a.b", "mymod:value"}) {
            IExpression bare = MathTestBootstrap.parseNamed(name);
            assertEquals("$(" + name + ")", MathTestBootstrap.writeFlat(bare));
            MathFlatAssertions.assertWrittenTextReadsBack(LibBuiltInFunctions.SQRT.call(bare));
        }
    }

    @Test
    @DisplayName("零参 lambda 退回对象形式")
    void zeroParameterLambdaFallsBackToObject() {
        // 文本里空参数串会被读成「缺参数名」，而对象形式的 parameters: [] 是合法的
        FunctionExpression lambda = FunctionExpression.of(
            LambdaFunction.of(List.of(), ConstantFunction.of(1).call())
        );
        assertNull(MathTestBootstrap.writeFlat(lambda));
        MathFlatAssertions.assertObjectRoundTrip(lambda);
    }
}
