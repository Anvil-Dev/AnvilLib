package dev.anvilcraft.lib.v2.math.test;

import dev.anvilcraft.lib.v2.math.AnvilLibMath;
import dev.anvilcraft.lib.v2.math.expression.FunctionExpression;
import dev.anvilcraft.lib.v2.math.expression.IExpression;
import dev.anvilcraft.lib.v2.math.expression.function.IFunction;
import dev.anvilcraft.lib.v2.math.expression.function.NamedFunction;
import dev.anvilcraft.lib.v2.math.init.LibBuiltInFunctions;
import dev.anvilcraft.lib.v2.math.init.LibRegistries;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("网络路径的编解码也能原样往返")
class StreamCodecTest {
    @BeforeAll
    static void setUp() {
        MathTestBootstrap.initialize();
    }

    /**
     * 把表达式写进缓冲区再读回来。
     */
    private static IExpression roundTrip(IExpression expression) {
        RegistryFriendlyByteBuf buf = MathTestBootstrap.registryFriendlyBuf();
        try {
            IExpression.STREAM_CODEC.encode(buf, expression);
            return IExpression.STREAM_CODEC.decode(buf);
        } finally {
            buf.release();
        }
    }

    /**
     * 是不是「按名字取一个值」的叶子：{@link NamedFunction} 或 {@code Reference.Named}。
     *
     * <p>这两者求值结果相同，只是网络往返后表示会从前者换成后者。</p>
     */
    private static boolean isNamedReference(IExpression expression) {
        return expression instanceof IExpression.Reference.Named
               || expression instanceof FunctionExpression(Holder<IFunction> function, List<IExpression> arguments)
                  && function.value() instanceof NamedFunction
                  && arguments.isEmpty();
    }

    /**
     * 树的形状是否一致（函数按注册名比，实参递归比）。
     *
     * <p>不能直接用 {@code equals}：读回来时「按名字取一个值」的叶子会换一种表示
     * （{@link NamedFunction} 读回来是 {@code Reference.Named}），两者求值结果相同。</p>
     */
    private static void assertSameShape(IExpression expected, IExpression actual, String message) {
        if (
            !(expected instanceof FunctionExpression(Holder<IFunction> function, List<IExpression> arguments))
            || !(actual instanceof FunctionExpression(Holder<IFunction> function1, List<IExpression> arguments1))
        ) {
            // 叶子：按名字取值的两种表示等价（NamedFunction 与 Reference.Named 都是「按名字取一个值」），
            // 其余按值比
            boolean sameLeaf = expected.equals(actual)
                               || StreamCodecTest.isNamedReference(expected)
                               && StreamCodecTest.isNamedReference(actual);
            assertTrue(sameLeaf, () -> message + ": 期望 " + expected + "，实际 " + actual);
            return;
        }
        String expectedName = function.unwrapKey().map(key -> key.location().toString()).orElse(null);
        String actualName = function1.unwrapKey().map(key -> key.location().toString()).orElse(null);
        if (expectedName != null || actualName != null) {
            assertEquals(expectedName, actualName, () -> message + ": 函数引用不一致");
        } else {
            assertEquals(
                function.value(),
                function1.value(),
                () -> message + ": 内联函数定义不一致"
            );
        }
        assertEquals(
            arguments.size(),
            arguments1.size(),
            () -> message + ": 实参个数不一致"
        );
        for (int index = 0; index < arguments.size(); index++) {
            StreamCodecTest.assertSameShape(
                arguments.get(index),
                arguments1.get(index),
                message + " 第 " + index + " 个实参"
            );
        }
    }

    @Test
    @DisplayName("内建函数用 Holder.direct 构造，网络路径照样往返")
    void builtinsRoundTripThroughStream() {
        // 内建函数到处是 Holder.direct(builtin)：JSON 侧靠 RegistryFileCodec 的内联分支读回来，
        // 网络侧是另一条实现（ByteBufCodecs.holderRegistry），必须单独实测
        assertTrue(Holder.direct(LibBuiltInFunctions.SQRT).unwrapKey().isEmpty(), "内建函数本来就该是直接句柄");
        for (String source : new String[]{"sqrt(4)", "min(3,1,2)", "max()", "2^(3^2)", "1+2*3"}) {
            IExpression parsed = MathTestBootstrap.parseValue(source);
            IExpression decoded = StreamCodecTest.roundTrip(parsed);
            StreamCodecTest.assertSameShape(parsed, decoded, source + " 网络往返");
            // 而且值也要一样
            assertEquals(
                parsed.evaluate(dev.anvilcraft.lib.v2.math.expression.Arguments.of()),
                decoded.evaluate(dev.anvilcraft.lib.v2.math.expression.Arguments.of()),
                () -> source + " 往返后求值结果应当一致"
            );
        }
    }

    @Test
    @DisplayName("数据包函数按注册名往返，读回来仍是同一棵调用树")
    void registeredFunctionsRoundTripThroughStream() {
        Holder.Reference<IFunction> registered = MathTestBootstrap.registerFunction(
            "streamfn",
            // 用一个注册进注册表的自定义函数体，避免和内建 ADD 撞成同一个值
            dev.anvilcraft.lib.v2.math.expression.function.CustomFunction.of(
                List.of("a", "b"),
                LibBuiltInFunctions.ADD.call(NamedFunction.call("a"), NamedFunction.call("b"))
            )
        );
        FunctionExpression call = FunctionExpression.of(registered, NamedFunction.call("x"));
        FunctionExpression decoded = (FunctionExpression) StreamCodecTest.roundTrip(call);
        // 函数必须仍指向同一个注册条目（按名字往返，不是把定义内联进来）
        assertEquals(registered, decoded.function(), "读回来还应当指向同一个注册条目");
        assertEquals(
            registered.unwrapKey(),
            decoded.function().unwrapKey(),
            "注册名应当保持不变"
        );
        StreamCodecTest.assertSameShape(call, decoded, "注册函数调用");
    }

    @Test
    @DisplayName("lambda 与自定义函数整棵往返")
    void lambdaAndCustomRoundTripThroughStream() {
        for (String source : new String[]{
            "x -> $(x)",
            "(a,b) -> $(a)+$(b)",
            "x -> $(x)*2",
            "foreach(1,2,x -> $(x))"
        }) {
            IExpression parsed = MathTestBootstrap.parseValue(source);
            IExpression decoded = StreamCodecTest.roundTrip(parsed);
            assertEquals(parsed, decoded, () -> source + " 网络往返后应当是同一棵树");
        }
    }

    @Test
    @DisplayName("flat 文本与对象形式都能从网络读回来")
    void flatTextAndObjectBothDecode() {
        // 客户端同步走的就是这条路：写进去的可能是 flat 文本，也可能是对象形式
        IExpression flat = MathTestBootstrap.parseValue("sqrt(2)+1");
        IExpression decoded = StreamCodecTest.roundTrip(flat);
        assertEquals("sqrt(2)+1", MathTestBootstrap.writeFlat(decoded), "读回来应当还是同一段规范文本");

        // 写不出 flat 文本的树走对象形式，网络路径也要照样读回来
        FunctionExpression unwritable = LibBuiltInFunctions.SQRT.call(
            FunctionExpression.of(NamedFunction.of("x..."))
        );
        assertEquals(unwritable, StreamCodecTest.roundTrip(unwritable), "对象形式也要往返");
    }

    @Test
    @DisplayName("按名字取值的实参在两条路径上读到同样的东西")
    void referencesAgreeOnBothPaths() {
        for (String source : new String[]{"$(cost)", "$(x...)", "$(mymod:value)"}) {
            IExpression parsed = MathTestBootstrap.parseValue(source);
            assertEquals(parsed, StreamCodecTest.roundTrip(parsed), () -> source + " 网络往返后应当是同一个引用");
        }
    }

    @Test
    @DisplayName("同一个表达式写两遍得到同样的字节")
    void streamEncodingIsDeterministic() {
        List<IExpression> trees = List.of(
            MathTestBootstrap.parseValue("min($(x...))"),
            MathTestBootstrap.parseValue("x -> $(x)+1"),
            FunctionExpression.of(Holder.direct(LibBuiltInFunctions.SQRT), IExpression.ref("cost"))
        );
        for (IExpression tree : trees) {
            RegistryFriendlyByteBuf first = MathTestBootstrap.registryFriendlyBuf();
            RegistryFriendlyByteBuf second = MathTestBootstrap.registryFriendlyBuf();
            try {
                IExpression.STREAM_CODEC.encode(first, tree);
                IExpression.STREAM_CODEC.encode(second, tree);
                assertEquals(first.readableBytes(), second.readableBytes(), () -> tree + " 的字节数应当一致");
                assertEquals(
                    first.toString(java.nio.charset.StandardCharsets.ISO_8859_1),
                    second.toString(java.nio.charset.StandardCharsets.ISO_8859_1),
                    () -> tree + " 的字节内容应当一致"
                );
            } finally {
                first.release();
                second.release();
            }
        }
    }

    @Test
    @DisplayName("内建函数的直接句柄走 holder 流编解码也能往返")
    void builtinHolderRoundTripsThroughStream() {
        RegistryFriendlyByteBuf buf = MathTestBootstrap.registryFriendlyBuf();
        try {
            Holder<IFunction> direct = Holder.direct(LibBuiltInFunctions.POW);
            IFunction.HOLDER_STREAM_CODEC.encode(buf, direct);
            Holder<IFunction> decoded = IFunction.HOLDER_STREAM_CODEC.decode(buf);
            // 注册表里有等值的条目时，写进去的 Holder.direct 读回来会归一成注册表引用：
            // 值相同、注册名也补了出来，下游拿到的是更规范的那一个
            assertEquals(LibBuiltInFunctions.POW, decoded.value(), "读回来的还应当是同一个内建函数");
            assertEquals(
                AnvilLibMath.of("pow"),
                decoded.unwrapKey().map(ResourceKey::location).orElse(null),
                "读回来应当补上 anvillib:pow 这个注册名"
            );
        } finally {
            buf.release();
        }
    }

    @Test
    @DisplayName("flat 编解码器认得出注册表里的函数名")
    void flatCodecSeesTheSameRegistry() {
        // 网络路径能工作的前提是它拿到的注册表与 JSON 侧一致，这里把这条前提钉住
        assertTrue(
            MathTestBootstrap.functions().containsKey(AnvilLibMath.of("sqrt")),
            "测试注册表里应当有内建 sqrt"
        );
        assertEquals(
            LibRegistries.FUNCTION_KEY,
            MathTestBootstrap.functions().key()
        );
    }
}
