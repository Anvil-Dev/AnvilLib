package dev.anvilcraft.lib.v2.math.test;

import com.google.gson.JsonElement;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 表达式往返的断言工具。
 *
 * <p>判定标准有两条：值必须一样，结构也必须一样。结构比较前先做归一，因为解析器把 {@code -x} 记成
 * {@code subtract(0, x)}，而程序化构造的负常量是 {@code const:-n}，两者在数值上等价。</p>
 */
final class MathFlatAssertions {
    /**
     * 随机树的最大深度。
     */
    private static final int MAX_DEPTH = 4;

    private MathFlatAssertions() {
    }

    /**
     * 一段 flat 文本写出再读回，结构与值都不变。
     */
    static void assertRoundTrip(String source) {
        FunctionExpression tree = MathTestBootstrap.parse(source);
        String written = MathTestBootstrap.writeFlat(tree);
        assertNotNull(written, () -> "表达式写不出 flat 文本: " + source);
        FunctionExpression reparsed = MathTestBootstrap.parse(written);
        assertEquals(value(tree), value(reparsed), () -> "值改变: " + source + " -> " + written);
        assertEquals(canonical(tree), canonical(reparsed), () -> "结构改变: " + source + " -> " + written);
        // 再写一次必须完全相同，否则缓存与比较都会在两次写出之间摇摆
        assertEquals(written, MathTestBootstrap.writeFlat(reparsed), () -> "回写不稳定: " + source);
    }

    /**
     * 程序化构造的调用树，flat 文本与对象形式都能往返。
     */
    static void assertFullRoundTrip(FunctionExpression tree) {
        assertObjectRoundTrip(tree);
        String written = MathTestBootstrap.writeFlat(tree);
        // 写不出 flat 文本时（内联定义、非有限常量、撞名的注册名等）编码退回对象形式，只保证值不变
        if (written == null) return;
        IExpression reparsed = MathTestBootstrap.parseValue(written);
        assertEquals(value(tree), value(reparsed), () -> "值改变: " + tree + " -> " + written);
        assertEquals(written, MathTestBootstrap.writeFlat(reparsed), () -> "回写不稳定: " + tree);
        // 再读一次，确认两次解析得到同一棵树
        assertEquals(
            canonical(reparsed),
            canonical(MathTestBootstrap.parseValue(written)),
            () -> "两次解析结构不同: " + written
        );
    }

    /**
     * 只比较结构、不求值的三支往返：数字 / flat 文本 / 对象形式。
     *
     * <p>用于整棵树本身不可求值的形状——lambda 落在运算符操作数位置时，它是个待绑定的值而不是数字，
     * 拿它去求值必然报「实参个数不符」，但「写出来能读回同一棵树」这条不变量照样要成立。</p>
     */
    static void assertStructuralRoundTrip(IExpression tree) {
        JsonElement encoded = MathTestBootstrap.encode(tree);
        assertNotNull(encoded, () -> "表达式写不出对象形式: " + tree);
        IExpression decoded = IExpression.CODEC
            .parse(MathTestBootstrap.ops(), encoded)
            .getOrThrow(message -> new AssertionError("对象形式读不回来: " + tree + " " + message));
        assertEquals(canonical(tree), canonical(decoded), () -> "对象形式结构改变: " + tree + " -> " + encoded);

        String written = MathTestBootstrap.writeFlat(tree);
        if (written == null) return;
        IExpression reparsed = MathTestBootstrap.parseValue(written);
        assertEquals(canonical(tree), canonical(reparsed), () -> "结构改变: " + tree + " -> " + written);
        assertEquals(written, MathTestBootstrap.writeFlat(reparsed), () -> "回写不稳定: " + tree);
    }

    /**
     * 完整编解码（数字 / flat 文本 / 对象）往返，并确认 flat 文本能原样读回。
     */
    static void assertObjectRoundTrip(IExpression tree) {
        JsonElement encoded = MathTestBootstrap.encode(tree);
        assertNotNull(encoded, () -> "表达式写不出对象形式: " + tree);
        IExpression decoded = IExpression.CODEC
            .parse(MathTestBootstrap.ops(), encoded)
            .getOrThrow(message -> new AssertionError("对象形式读不回来: " + tree + " " + message));
        assertEquals(canonical(tree), canonical(decoded), () -> "对象形式结构改变: " + tree + " -> " + encoded);
        assertEquals(value(tree), value(decoded), () -> "对象形式值改变: " + tree + " -> " + encoded);

        // 编码可能既写出 flat 文本又写出对象形式；两种写法都必须能读回同一棵树
        String written = MathTestBootstrap.writeFlat(tree);
        if (written == null) return;
        assertEquals(
            canonical(tree),
            canonical(MathTestBootstrap.parseValue(written)),
            () -> "flat 形式结构改变: " + tree + " -> " + written
        );
    }

    /**
     * 用固定算子拼一个调用树。
     */
    static void assertObjectRoundTrip(LibBuiltInFunctions builtin, IExpression... arguments) {
        MathFlatAssertions.assertObjectRoundTrip(builtin.call(arguments));
    }

    /**
     * 回写侧对整棵树的承诺：只要 {@code write} 成功，写出的文本就必须读回同一棵表达式树。
     *
     * <p>{@code CODEC} 的编码器只要 flat 写出成功就不再写对象形式，所以「写得出来但读不回去 / 读成别的
     * 意思」会静默落进存档。断言这条不变量，比只断言文本长什么样更能兜住注册名、优先级、字符集这几类缺口。</p>
     */
    static void assertWrittenTextReadsBack(IExpression tree) {
        String written = MathTestBootstrap.writeFlat(tree);
        if (written == null) return;
        assertEquals(
            MathFlatAssertions.canonical(tree),
            MathFlatAssertions.canonical(MathTestBootstrap.parseValue(written)),
            () -> "写出的文本读不回同一棵树: " + tree + " -> " + written
        );
    }

    /**
     * 随机生成一个变参 lambda，函数体只用不会求值的叶子。
     *
     * <p>专门用来喂运算符的操作数位置：随机树会被求值，而一元、二元运算符给出的实参个数不一样，
     * 变参形参对实参个数没有要求，因此哪种位置都构造得出来。单形参 lambda 只适合 {@code forEach}
     * 实参位那种明确「一次给一个值」的场景。</p>
     */
    static FunctionExpression randomVariadicLambda(Random random) {
        IExpression body = random.nextInt(2) == 0
            ? ConstantFunction.of(MathFlatAssertions.randomNumber(random)).call()
            : NamedFunction.call("x");
        return FunctionExpression.of(LambdaFunction.of(List.of("x..."), body));
    }

    /**
     * 随机生成一棵表达式树。
     */
    static FunctionExpression randomTree(Random random, int depth) {
        if (depth >= MathFlatAssertions.MAX_DEPTH || random.nextInt(4) == 0) {
            return MathFlatAssertions.randomLeaf(random);
        }
        List<LibBuiltInFunctions> candidates = new ArrayList<>();
        for (LibBuiltInFunctions builtin : LibBuiltInFunctions.values()) {
            // forEach 的末位必须是 lambda，随机树里生成不出来，单独测
            if (builtin != LibBuiltInFunctions.FOREACH && builtin.minimumArity() <= 3) candidates.add(builtin);
        }
        LibBuiltInFunctions builtin = candidates.get(random.nextInt(candidates.size()));
        int arity = builtin.minimumArity() == builtin.maximumArity()
            ? builtin.minimumArity()
            : builtin.minimumArity() + random.nextInt(2);
        IExpression[] arguments = new IExpression[arity];
        for (int index = 0; index < arity; index++) {
            arguments[index] = MathFlatAssertions.randomTree(random, depth + 1);
        }
        return builtin.call(arguments);
    }

    private static FunctionExpression randomLeaf(Random random) {
        return switch (random.nextInt(3)) {
            case 0 -> ConstantFunction.of(MathFlatAssertions.randomNumber(random)).call();
            case 1 -> InputFunction.call(random.nextInt(3));
            default -> NamedFunction.call(MathFlatAssertions.randomName(random));
        };
    }

    private static String randomName(Random random) {
        String[] names = {"cost", "value", "count", "weight"};
        return names[random.nextInt(names.length)];
    }

    /**
     * 以 {@code e} 开头的注册名，用来覆盖并置乘法被指数记法吃掉的情况：
     * {@code 2*e1(x)} 写成 {@code 2e1(x)} 会读成 {@code multiply(20, x)}。
     */
    private static final String[] EXPONENT_NAMES = {"e1", "e2", "e1abc", "e12x"};

    /**
     * 注册一批以 {@code e}/{@code E} 开头、后跟数字的函数名。
     *
     * <p>这些名字本身合法，但和数字并置时会被 {@code parseNumber} 的指数记法吞掉，必须单独覆盖。</p>
     */
    static void registerExponentNames() {
        for (String name : MathFlatAssertions.EXPONENT_NAMES) {
            MathTestBootstrap.registerFunction(name, CustomFunction.named(
                List.of("a"),
                NamedFunction.call("a")
            ));
        }
    }

    /**
     * 对某个以 {@code e} 开头的已注册函数发起一次调用，用来喂并置乘法的右侧。
     */
    static FunctionExpression randomExponentCall(Random random) {
        String name = MathFlatAssertions.EXPONENT_NAMES[random.nextInt(MathFlatAssertions.EXPONENT_NAMES.length)];
        Holder<IFunction> holder = MathTestBootstrap.functions().getOrThrow(
            ResourceKey.create(LibRegistries.FUNCTION_KEY, AnvilLibMath.of(name))
        );
        return FunctionExpression.of(holder, ConstantFunction.of(1).call());
    }

    private static double randomNumber(Random random) {
        return switch (random.nextInt(6)) {
            case 0 -> 0.0;
            case 1 -> -0.0;
            case 2 -> random.nextInt(-100, 100);
            case 3 -> random.nextDouble() * Math.pow(10, random.nextInt(-300, 300));
            case 4 -> random.nextDouble() * Math.pow(10, random.nextInt(-8, 8));
            default -> random.nextBoolean() ? random.nextInt(-10, 10) : -random.nextDouble();
        };
    }

    private static double value(IExpression expression) {
        return expression.evaluate(Arguments.of(2, 3, 4));
    }

    /**
     * 把等价的写法归一后再比较结构。
     *
     * <p>解析器把 {@code -x} 记成 {@code subtract(0, x)}，程序化构造的负常量则是 {@code const:-n}，
     * 两者都归一成 {@code neg(...)}；负零也归一到 {@code neg(const:0)}。{@code subtract(0, const:-n)}
     * 回写成 {@code n} 属于写出器的正常简化，因此双重取负也一并抵消。</p>
     */
    static String canonical(IExpression expression) {
        return MathFlatAssertions.collapse(MathFlatAssertions.canonicalRaw(expression));
    }

    /**
     * 抵消双重取负：{@code neg(neg(X))} 与 {@code neg(负常量)} 都化简成常量本身。
     */
    private static String collapse(String canonical) {
        String result = canonical;
        while (result.startsWith("neg(neg(") && result.endsWith("))")) {
            result = result.substring(8, result.length() - 2);
        }
        if (result.startsWith("neg(const:-")) {
            result = "const:" + result.substring(10, result.length() - 1);
        }
        return result;
    }

    private static String canonicalRaw(IExpression expression) {
        if (expression instanceof IExpression.Reference reference) {
            return (reference instanceof IExpression.Reference.Spread ? "spread:" : "named:")
                + reference.name() + "()";
        }
        if (!(expression instanceof FunctionExpression call)) return expression.toString();
        IFunction function = call.function().value();
        if (function instanceof ConstantFunction(double constant)) {
            return "const:" + Double.toHexString(constant);
        }
        List<IExpression> arguments = call.arguments();
        if (function == LibBuiltInFunctions.SUBTRACT
            && arguments.size() == 2
            && MathFlatAssertions.isPositiveZero(arguments.get(0))) {
            return "neg(" + MathFlatAssertions.canonical(arguments.get(1)) + ")";
        }
        StringBuilder text = new StringBuilder(MathFlatAssertions.functionName(call)).append('(');
        for (int index = 0; index < arguments.size(); index++) {
            if (index > 0) text.append(',');
            text.append(MathFlatAssertions.canonical(arguments.get(index)));
        }
        return text.append(')').toString();
    }

    private static boolean isPositiveZero(IExpression expression) {
        if (!(expression instanceof FunctionExpression call)) return false;
        return ConstantFunction.value(call).filter(value -> Double.doubleToRawLongBits(value) == 0).isPresent();
    }

    private static String functionName(FunctionExpression call) {
        if (call.function().value() instanceof NamedFunction(String name)) return "named:" + name;
        if (call.function().value() instanceof InputFunction(int index)) return "input:" + index;
        if (call.function().value() instanceof CustomFunction custom) return "custom:" + custom.declarations();
        if (call.function().value() instanceof LambdaFunction lambda) {
            return "lambda:" + lambda.declarations() + "->" + MathFlatAssertions.canonical(lambda.body());
        }
        Holder<IFunction> holder = call.function();
        Object value = holder.value();
        if (value instanceof LibBuiltInFunctions builtin) return builtin.getSerializedName();
        return holder.unwrapKey().map(key -> key.location().toString()).orElse(value.getClass().getSimpleName());
    }
}
