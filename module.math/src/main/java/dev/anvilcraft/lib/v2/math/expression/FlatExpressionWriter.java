package dev.anvilcraft.lib.v2.math.expression;

import dev.anvilcraft.lib.v2.math.expression.function.ConstantFunction;
import dev.anvilcraft.lib.v2.math.expression.function.IFunction;
import dev.anvilcraft.lib.v2.math.expression.function.InputFunction;
import dev.anvilcraft.lib.v2.math.expression.function.LambdaFunction;
import dev.anvilcraft.lib.v2.math.expression.function.NamedFunction;
import dev.anvilcraft.lib.v2.math.init.LibBuiltInFunctions;
import dev.anvilcraft.lib.v2.math.init.LibRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;

import java.math.BigDecimal;
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
     * 并置乘法的左半段与右半段，用来在写完两段之后再决定能不能并置。
     */
    private record Juxtaposition(String text) {
    }

    /**
     * 回写一个操作数：同优先级结合性与优先级不满足时补一层括号。
     *
     * <p>文本以 {@code -} 开头的操作数一律补括号：一元负号的层次比它看起来低（{@code -2^2} 会读成
     * {@code -(2^2)}），{@code 2-1} 也会被当成减法，不补括号就会改变值。</p>
     *
     * @param parent 父运算，最外层为 {@code null}，表示不需要括号
     */
    private static Optional<String> operand(
        IExpression expression,
        HolderGetter<IFunction> functions,
        Operator parent,
        OperandPosition position
    ) {
        return FlatExpressionWriter.bare(expression, functions).map(text -> {
            if (FlatExpressionWriter.needsParentheses(expression, parent, position) || text.startsWith("-")) {
                return "(" + text + ")";
            }
            return text;
        });
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
        return FlatExpressionWriter.bare(expression, functions).map(text -> {
            if (FlatExpressionWriter.needsParentheses(expression, parentPrecedence, position)
                || text.startsWith("-")) {
                return "(" + text + ")";
            }
            return text;
        });
    }

    /**
     * 回写表达式本身，不带任何括号。
     */
    private static Optional<String> bare(IExpression expression, HolderGetter<IFunction> functions) {
        if (expression instanceof IExpression.Reference reference) {
            return Optional.of(reference instanceof IExpression.Reference.Spread
                ? "$(" + reference.name() + "...)"
                : "$(" + reference.name() + ")");
        }
        if (!(expression instanceof FunctionExpression call)) return Optional.empty();
        Optional<Double> constant = ConstantFunction.value(call);
        if (constant.isPresent()) {
            return FlatExpressionWriter.signedNumber(constant.get());
        }
        if (call.function().value() instanceof NamedFunction(String name)) {
            return Optional.of("$(" + name + ")");
        }
        if (call.function().value() instanceof InputFunction(int index)) {
            return Optional.of(FlatExpressionWriter.input(index));
        }
        if (call.function().value() instanceof LambdaFunction lambda) {
            return FlatExpressionWriter.visitLambda(lambda, functions);
        }
        Binary binary = FlatExpressionWriter.binary(call);
        if (binary == null) return FlatExpressionWriter.visitCall(call, functions);
        if (FlatExpressionWriter.isNegation(call, binary)) {
            return FlatExpressionWriter.negationOf(call.arguments().get(1), functions);
        }
        return FlatExpressionWriter.visitBinary(call, binary, functions);
    }

    /**
     * 把一个常量写成 flat 文本。
     *
     * <p>负常量统一写成一元负号，与解析器对 {@code -x} 的表示一致，写出结果才稳定；零写成 {@code 0}，
     * 负零写成 {@code -0.0}（写成 {@code -0} 会读回 {@code 0-0}，值变成正零）。非有限值写不出来。</p>
     */
    private static Optional<String> signedNumber(double value) {
        if (!Double.isFinite(value)) return Optional.empty();
        if (FlatExpressionWriter.isNegative(value)) return FlatExpressionWriter.negation(value);
        return Optional.of(FlatExpressionWriter.basicNumber(value));
    }

    /**
     * 给一个常量取负，写成 {@code -|x|}，负零写成 {@code -0.0}。非有限值写不出来。
     */
    private static Optional<String> negation(double value) {
        if (!Double.isFinite(value)) return Optional.empty();
        double magnitude = Math.abs(value);
        if (magnitude == 0) return Optional.of(Double.toString(-magnitude));
        return Optional.of("-" + FlatExpressionWriter.basicNumber(magnitude));
    }

    /**
     * 给一个操作数取负。
     *
     * <p>一律写成括号里的操作数：读回来就是解析器的 {@code 0-x} 形式，回写收敛到同一种文本。括号还能保住
     * 一元负号的层次（{@code -(x^y)} 不能写成 {@code -x^y}）。操作数本身以 {@code -} 开头时
     * （只可能是负常量）写不出来，返回空让调用方改用对象形式，免得 {@code 0-(-0.5)} 被简化成 {@code 0.5}
     * 而丢掉结构。</p>
     */
    private static Optional<String> negationOf(IExpression operand, HolderGetter<IFunction> functions) {
        return FlatExpressionWriter
            .bare(operand, functions)
            .flatMap(FlatExpressionWriter::negation);
    }

    /**
     * 给一段操作数文本取负。
     *
     * <p>操作数一律括起来：解析器把 {@code -A} 记成 {@code subtract(0, A)}，写成 {@code -(A)} 读回来正是
     * 这个结构，回写因此收敛到同一种文本。不括不行——一元负号的优先级低于乘方，取负一个乘方时
     * {@code -(x^y)} 写成 {@code -x^y} 会被读成 {@code (-x)^y}。</p>
     *
     * <p>已经带负号的文本补不了：{@code --x} 不是合法写法。调用方改成退回对象形式。</p>
     */
    private static Optional<String> negation(String operand) {
        if (operand.startsWith("-")) return Optional.empty();
        return Optional.of("-(" + operand + ")");
    }

    /**
     * 是否带负号，{@code -0.0} 也算：它写成 {@code 0} 会丢掉符号位。
     */
    private static boolean isNegative(double value) {
        return (Double.doubleToRawLongBits(value) & Long.MIN_VALUE) != 0;
    }

    /**
     * 二元运算按中缀写出，两侧操作数按优先级与结合性补最少括号。
     *
     * <p>左操作数是字面量数字时写成并置乘法（{@code 2x}、{@code 2(x+1)}），右侧无法并置时用 {@code *}。</p>
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
            Optional<Juxtaposition> juxtaposed = FlatExpressionWriter
                .operand(left, functions, Operator.MULTIPLY.precedence, OperandPosition.LEFT)
                .flatMap(multiplier -> FlatExpressionWriter
                    .operand(right, functions, Operator.MULTIPLY.precedence, OperandPosition.RIGHT)
                    // 并置只用于 2x、2(x+1) 这类写法；右侧是数字、小数点或负号时会读成另一个数（2*3 写成 23）
                    .filter(FlatExpressionWriter::juxtaPositionable)
                    .map(text -> new Juxtaposition(multiplier + text)));
            if (juxtaposed.isPresent()) return juxtaposed.map(Juxtaposition::text);
        }
        return FlatExpressionWriter
            .operand(left, functions, binary.operator(), OperandPosition.LEFT)
            .flatMap(text -> FlatExpressionWriter
                .operand(right, functions, binary.operator(), OperandPosition.RIGHT)
                .map(second -> text + binary.operator().symbol + second));
    }

    /**
     * lambda 写成 {@code x -> 函数体}，多参写成 {@code (a, b) -> 函数体}，变参名带 {@code ...}。
     *
     * <p>lambda 绑定得最松，函数体不必补括号；函数体自己又是 lambda 时会写成 {@code x -> y -> 函数体}，
     * 按右结合读回来仍是同一个嵌套结构。</p>
     */
    private static Optional<String> visitLambda(LambdaFunction lambda, HolderGetter<IFunction> functions) {
        List<String> declarations = lambda.declarations();
        String parameters = declarations.size() == 1
            ? declarations.getFirst()
            : "(" + String.join(", ", declarations) + ")";
        return FlatExpressionWriter
            .bare(lambda.body(), functions)
            .map(body -> parameters + " -> " + body);
    }

    /**
     * 其余函数写成调用形式；参数必须都能用 flat 文本表达，函数必须能取到名字。
     *
     * <p>变参函数的实参是逐个列出来的（{@code min(1,2,3)}），所以这里不区分变参位：{@code $(x...)} 由
     * {@link #bare} 写成列表引用，正好是变参函数接住整份列表实参的写法。</p>
     */
    private static Optional<String> visitCall(FunctionExpression call, HolderGetter<IFunction> functions) {
        String name = FlatExpressionWriter.functionName(call, functions);
        if (name == null) return Optional.empty();
        StringBuilder text = new StringBuilder(name).append('(');
        for (int index = 0; index < call.arguments().size(); index++) {
            Optional<String> written = FlatExpressionWriter.write(call.arguments().get(index), functions);
            if (written.isEmpty()) return Optional.empty();
            if (index > 0) text.append(',');
            text.append(written.get());
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
     * 判断一个子运算在父运算的这个位置上要不要补括号。
     */
    private static boolean needsParentheses(IExpression expression, @Nullable Operator parent, OperandPosition position) {
        if (parent == null) return false;
        return FlatExpressionWriter.needsParentheses(expression, parent.precedence, position);
    }

    private static boolean needsParentheses(IExpression expression, int parentPrecedence, OperandPosition position) {
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

    /**
     * 并置只用于 {@code 2x}、{@code 2(x+1)} 这类写法，右侧只能是标识符、括号或 {@code $(name)}；
     * 以数字开头时并置会被读成另一个数（{@code 2*3} 写成 {@code 23}），只能写 {@code *}。
     */
    private static boolean juxtaPositionable(String text) {
        if (text.isEmpty()) return false;
        char first = text.charAt(0);
        return Character.isLetter(first) || first == '_' || first == '(' || first == '$';
    }

    private static boolean isLiteralNumber(IExpression expression) {
        if (!(expression instanceof FunctionExpression call)) return false;
        // 只认不带符号的常量：带符号的常量写成 -n 之后没法并置（2-1 与 2*-1 不同），
        // 继续并置会写成 2-1 这种东西，交给 * 处理
        return ConstantFunction
            .value(call)
            .filter(value -> Double.isFinite(value) && !FlatExpressionWriter.isNegative(value))
            .isPresent();
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
     *
     * <p>只认正零：{@code subtract(-0.0, x)} 不是 {@code -x}，写成 {@code -x} 会把负零变成正零。</p>
     */
    private static boolean isNegation(FunctionExpression call, Binary binary) {
        if (binary.operator() != Operator.SUBTRACT) return false;
        if (!(call.arguments().getFirst() instanceof FunctionExpression left)) return false;
        return ConstantFunction.value(left).filter(value -> Double.doubleToRawLongBits(value) == 0).isPresent();
    }

    static String input(int index) {
        return switch (index) {
            case 0 -> "x";
            case 1 -> "y";
            case 2 -> "z";
            default -> "x" + index;
        };
    }

    /**
     * 回写一个非负数字。
     *
     * <p>能用十进制写出来时优先写十进制：整数写成整数字面量，其余交给 {@link BigDecimal} 的
     * {@code toPlainString}。{@link Double#toString} 的输出在极值处是指数记法（{@code 1.0E7}），
     * 即使用户写的是 {@code 10000000}，回写后也会变成指数记法，读起来别扭。</p>
     */
    private static String basicNumber(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 9.223372036854776E18) {
            return Long.toString((long) value);
        }
        if (Math.abs(value) >= 1.0E-4 && Math.abs(value) < 1.0E7) {
            return FlatExpressionWriter.trimTrailingZeros(BigDecimal.valueOf(value).toPlainString());
        }
        return Double.toString(value);
    }

    /**
     * 去掉小数末尾多余的 0（{@code 0.00010} → {@code 0.0001}），去掉一位就重新解析验证一次，
     * 保证文本仍然还原成同一个 double。
     */
    private static String trimTrailingZeros(String text) {
        int end = text.length();
        while (end >= 2 && text.charAt(end - 1) == '0' && text.charAt(end - 2) != '.') {
            String shorter = text.substring(0, end - 1);
            if (Double.parseDouble(shorter) != Double.parseDouble(text)) break;
            text = shorter;
            end--;
        }
        return text;
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
