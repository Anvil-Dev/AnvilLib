package dev.anvilcraft.lib.v2.math.expression;

import dev.anvilcraft.lib.v2.math.init.LibRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;

import dev.anvilcraft.lib.v2.math.init.LibBuiltInFunctions;
import dev.anvilcraft.lib.v2.math.expression.function.ConstantFunction;
import dev.anvilcraft.lib.v2.math.expression.function.IFunction;
import dev.anvilcraft.lib.v2.math.expression.function.InputFunction;
import dev.anvilcraft.lib.v2.math.expression.function.NamedFunction;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * 把表达式树回写成 flat 表达式文本，是 {@link FlatExpressionParser} 的反方向。
 *
 * <p>只有当整棵树都能用 flat 文本表达时才写得出来，否则返回空：数字、{@code x}/{@code y}/{@code z}、
 * {@code $(name)}、四则运算与乘方，以及能从
 * {@link LibRegistries#FUNCTION_KEY} 取到名字的函数调用都能写。</p>
 *
 * <p>回写结果是规范形式，不保留原文：隐式乘法一律写成并置形式（{@code 2*x} 与 {@code 2·x} 都写成
 * {@code 2x}），{@code 2^(3^4)} 写成 {@code 2^3^4}，{@code anvillib} 命名空间一律省略。任何回写结果
 * 重新解析都会得到同一棵表达式树。</p>
 */
final class FlatExpressionWriter {
    private FlatExpressionWriter() {
    }

    /**
     * 尝试把表达式回写成 flat 文本，最外层不补括号。
     *
     * @param expression 待回写的表达式
     * @param functions  函数注册表，用于取函数名
     * @return 文本，或表达式无法用 flat 文本表达时的空
     */
    static Optional<String> write(IExpression expression, HolderGetter<IFunction> functions) {
        return FlatExpressionWriter.bare(expression, functions);
    }

    /**
     * 回写一个操作数：同优先级结合性与优先级不满足时补一层括号。
     *
     * @param parent 父运算，最外层为 {@code null}，表示不需要括号
     */
    private static Optional<String> operand(
        IExpression expression,
        HolderGetter<IFunction> functions,
        Operator parent,
        OperandPosition position
    ) {
        return FlatExpressionWriter.bare(expression, functions)
            .map(text -> FlatExpressionWriter.parenthesize(expression, parent, position) ? "(" + text + ")" : text);
    }

    /**
     * 回写一个操作数，只比较优先级，用于并置乘法两侧那个已知的操作数层次。
     */
    private static Optional<String> operand(
        IExpression expression,
        HolderGetter<IFunction> functions,
        int parentPrecedence,
        OperandPosition position
    ) {
        return FlatExpressionWriter.bare(expression, functions)
            .map(text -> FlatExpressionWriter.parenthesize(expression, parentPrecedence, position)
                         ? "(" + text + ")"
                         : text);
    }

    /**
     * 回写表达式本身，不带任何括号。
     */
    private static Optional<String> bare(IExpression expression, HolderGetter<IFunction> functions) {
        if (!(expression instanceof FunctionExpression call)) return Optional.empty();
        Optional<Double> constant = ConstantFunction.value(call);
        if (constant.isPresent()) {
            double value = constant.get();
            if (!Double.isFinite(value)) return Optional.empty();
            return Optional.of(FlatExpressionWriter.number(value));
        }
        if (call.function().value() instanceof NamedFunction(String name)) {
            return Optional.of("$(" + name + ")");
        }
        if (call.function().value() instanceof InputFunction(int index)) {
            return Optional.of(FlatExpressionWriter.input(index));
        }
        Binary binary = FlatExpressionWriter.binary(call);
        if (binary == null) return FlatExpressionWriter.visitCall(call, functions);
        if (FlatExpressionWriter.isNegation(call, binary)) {
            return FlatExpressionWriter
                .operand(call.arguments().get(1), functions, Operator.UNARY, OperandPosition.RIGHT)
                .map(text -> "-" + text);
        }
        return FlatExpressionWriter.visitBinary(call, binary, functions);
    }

    /**
     * 二元运算按中缀写出，两侧操作数按优先级与结合性补最少括号。
     *
     * <p>左操作数是字面量数字时写成并置乘法（{@code 2x}、{@code 2(x+1)}），其余用 {@code *}。</p>
     */
    private static Optional<String> visitBinary(
        FunctionExpression call,
        Binary binary,
        HolderGetter<IFunction> functions
    ) {
        List<IExpression> arguments = call.arguments();
        IExpression left = arguments.get(0);
        IExpression right = arguments.get(1);
        if (binary.operator() == Operator.MULTIPLY && FlatExpressionWriter.isLiteralNumber(left)) {
            return FlatExpressionWriter
                .operand(left, functions, Operator.MULTIPLY.precedence, OperandPosition.LEFT)
                .flatMap(multiplier -> FlatExpressionWriter
                    .operand(right, functions, Operator.MULTIPLY.precedence, OperandPosition.RIGHT)
                    .map(text -> multiplier + text));
        }
        return FlatExpressionWriter
            .operand(left, functions, binary.operator(), OperandPosition.LEFT)
            .flatMap(text -> FlatExpressionWriter
                .operand(right, functions, binary.operator(), OperandPosition.RIGHT)
                .map(second -> text + binary.operator().symbol + second));
    }

    /**
     * 其余函数写成调用形式；参数必须都能用 flat 文本表达，函数必须能取到名字。
     */
    private static Optional<String> visitCall(FunctionExpression call, HolderGetter<IFunction> functions) {
        String name = FlatExpressionWriter.functionName(call, functions);
        if (name == null) return Optional.empty();
        StringBuilder text = new StringBuilder(name).append('(');
        for (int index = 0; index < call.arguments().size(); index++) {
            Optional<String> argument = FlatExpressionWriter.write(call.arguments().get(index), functions);
            if (argument.isEmpty()) return Optional.empty();
            if (index > 0) text.append(',');
            text.append(argument.get());
        }
        return Optional.of(text.append(')').toString());
    }

    /**
     * 取函数在 flat 文本里的名字：内建函数用枚举名，数据包函数用注册名，
     * {@code anvillib} 命名空间省略。
     */
    private static @Nullable String functionName(FunctionExpression call, HolderGetter<IFunction> functions) {
        if (call.function().value() instanceof LibBuiltInFunctions builtin) {
            return builtin.getSerializedName();
        }
        ResourceKey<IFunction> key = call.function().unwrapKey().orElse(null);
        if (key == null || functions.get(key).isEmpty()) return null;
        return FlatExpressionParser.stripDefaultNamespace(key.location());
    }

    /**
     * 同优先级下这个子运算会不会与父运算结合成另一种树：左操作数是右结合运算，或右操作数不是右结合运算。
     */
    private static boolean parenthesize(IExpression expression, @Nullable Operator parent, OperandPosition position) {
        if (parent == null) return false;
        return FlatExpressionWriter.parenthesize(expression, parent.precedence, position);
    }

    private static boolean parenthesize(IExpression expression, int parentPrecedence, OperandPosition position) {
        Holder<IFunction> function = FlatExpressionWriter.functionOf(expression);
        if (function == null) return false;
        Operator child = Operator.of(function.value());
        if (child == null) return false;
        if (child.precedence != parentPrecedence) return child.precedence < parentPrecedence;
        return (position == OperandPosition.LEFT) == (child.associativity == Associativity.RIGHT);
    }

    private static @Nullable Holder<IFunction> functionOf(IExpression expression) {
        return expression instanceof FunctionExpression call ? call.function() : null;
    }

    private static boolean isLiteralNumber(IExpression expression) {
        if (!(expression instanceof FunctionExpression call)) return false;
        return ConstantFunction.value(call).filter(Double::isFinite).isPresent();
    }

    /**
     * 识别二元运算调用，参数个数必须正好是二元运算的两个。
     */
    private static @Nullable Binary binary(FunctionExpression call) {
        if (call.arguments().size() != 2) return null;
        Operator operator = Operator.of(call.function().value());
        return operator == null ? null : new Binary(operator);
    }

    /**
     * 识别解析器为 {@code -x} 生成的形式：零减去一个操作数。
     */
    private static boolean isNegation(FunctionExpression call, Binary binary) {
        if (binary.operator() != Operator.SUBTRACT) return false;
        if (!(call.arguments().getFirst() instanceof FunctionExpression left)) return false;
        return ConstantFunction.value(left).filter(value -> value == 0.0).isPresent();
    }

    static String input(int index) {
        return switch (index) {
            case 0 -> "x";
            case 1 -> "y";
            case 2 -> "z";
            default -> "x" + index;
        };
    }

    private static String number(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1.0E7) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    private record Binary(Operator operator) {
    }

    private enum OperandPosition {
        LEFT,
        RIGHT
    }

    private enum Associativity {
        LEFT,
        RIGHT
    }

    /**
     * 可以写成中缀的运算符，优先级与 {@link FlatExpressionParser} 的语法层次一一对应。
     *
     * <p>{@link #UNARY} 不是一个真正的内建函数，只用来表示解析器里一元负号所处的层次，
     * 让 {@code -} 后面的操作数按同一套规则补括号。</p>
     */
    enum Operator {
        ADD("+", 1, Associativity.LEFT),
        SUBTRACT("-", 1, Associativity.LEFT),
        MULTIPLY("*", 2, Associativity.LEFT),
        DIVIDE("/", 2, Associativity.LEFT),
        UNARY("-", 3, Associativity.RIGHT),
        POW("^", 4, Associativity.RIGHT);

        private final String symbol;
        private final int precedence;
        private final Associativity associativity;

        Operator(String symbol, int precedence, Associativity associativity) {
            this.symbol = symbol;
            this.precedence = precedence;
            this.associativity = associativity;
        }

        /**
         * 该运算符对应的内建函数，不是二元运算时返回 {@code null}。
         */
        static @Nullable Operator of(IFunction function) {
            if (!(function instanceof LibBuiltInFunctions builtin)) return null;
            return switch (builtin) {
                case ADD -> ADD;
                case SUBTRACT -> SUBTRACT;
                case MULTIPLY -> MULTIPLY;
                case DIVIDE -> DIVIDE;
                case POW -> POW;
                default -> null;
            };
        }
    }
}
