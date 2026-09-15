package dev.anvilcraft.lib.v2.math.expression;

import dev.anvilcraft.lib.v2.math.AnvilLibMath;
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
import net.minecraft.resources.ResourceLocation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
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
    /**
     * 是否正在做「写出的文本读回来再写一遍」的自校验，用来避免自校验无限递归。
     */
    private static final ThreadLocal<Boolean> VERIFYING = ThreadLocal.withInitial(() -> false);

    /**
     * 是否正在做并置前的「读回来试试」预检。预检内部还要再写一次，再进去就无限递归了。
     */
    private static final ThreadLocal<Boolean> JUXTAPOSING = ThreadLocal.withInitial(() -> false);
    private FlatExpressionWriter() {
    }

    /**
     * 尝试把表达式回写成 flat 文本，最外层不补括号。
     *
     * <p>写出结果要满足「再解析一次、再写一次得到的文本完全相同」（回写是规范形式），这里在返回前实测
     * 一遍：同一棵树写两遍文本不同、或文本读不回来，都退回对象形式，而不是留下一份「读得回来但写不稳定」
     * 的文本。</p>
     *
     * @param expression 待回写的表达式
     * @param functions  函数注册表，用于取函数名
     * @return 文本，或表达式无法用 flat 文本表达时的空
     */
    static Optional<String> write(IExpression expression, HolderGetter<IFunction> functions) {
        Optional<String> written = FlatExpressionWriter.bare(expression, functions);
        if (written.isEmpty() || FlatExpressionWriter.VERIFYING.get()) return written;
        FlatExpressionWriter.VERIFYING.set(true);
        try {
            IExpression reparsed = FlatExpressionParser.parseValue(written.get(), functions);
            return written.get().equals(FlatExpressionWriter.bare(reparsed, functions).orElse(null))
                   && FlatExpressionWriter.sameMeaning(expression, reparsed)
                ? written
                : Optional.empty();
        } catch (RuntimeException exception) {
            // 读不回来同样是「写不出」
            return Optional.empty();
        } finally {
            FlatExpressionWriter.VERIFYING.set(false);
        }
    }

    /**
     * 两棵树是不是同一个意思。
     *
     * <p>写出的文本重新解析后，叶子的表示可能变了：{@link NamedFunction} 会读回
     * {@link IExpression.Reference.Named}，两个都是「按名字取一个值」；名字恰好是 {@code x} 时还会读成
     * {@link InputFunction}。三者不是同一种对象，但求值结果相同，所以自校验不能按对象相等去比，
     * 否则本来稳定的文本会被判成不稳定、白白退回对象形式。</p>
     */
    private static boolean sameMeaning(IExpression left, IExpression right) {
        if (left.equals(right)) return true;
        IExpression resolvedLeft = FlatExpressionWriter.resolveReference(left);
        IExpression resolvedRight = FlatExpressionWriter.resolveReference(right);
        if (resolvedLeft != null || resolvedRight != null) {
            return resolvedLeft != null
                   && resolvedLeft.equals(resolvedRight);
        }
        if (!FlatExpressionWriter.isNonCall(left) || !FlatExpressionWriter.isNonCall(right)) return false;
        FunctionExpression leftCall = (FunctionExpression) left;
        FunctionExpression rightCall = (FunctionExpression) right;
        // lambda 要单独比：它的 equals 把函数体也算进函数本身，而函数体的叶子表示可能变过
        // （$(x) 读回来是 Reference.Named），所以只比形参声明，函数体另外递归比
        if (leftCall.function().value() instanceof LambdaFunction leftLambda
            && rightCall.function().value() instanceof LambdaFunction rightLambda) {
            return leftLambda.declarations().equals(rightLambda.declarations())
                   && FlatExpressionWriter.sameMeaning(leftLambda.body(), rightLambda.body());
        }
        if (!FlatExpressionWriter.sameFunction(leftCall, rightCall)) return false;
        List<IExpression> leftArguments = leftCall.arguments();
        List<IExpression> rightArguments = rightCall.arguments();
        if (leftArguments.size() != rightArguments.size()) return false;
        for (int index = 0; index < leftArguments.size(); index++) {
            if (!FlatExpressionWriter.sameMeaning(leftArguments.get(index), rightArguments.get(index))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 是不是「按名字取一个值」的叶子，是的话给出归一后的形式：{@link NamedFunction} 与
     * {@link IExpression.Reference.Named} 都归一成 {@code $(name)} 引用，名字是 {@code x}/{@code y}/
     * {@code z}/{@code xN} 的还额外归一成 {@link InputFunction}。
     *
     * @return 归一后的叶子，不是这类叶子时返回 {@code null}
     */
    private static @Nullable IExpression resolveReference(IExpression expression) {
        String name = switch (expression) {
            case IExpression.Reference.Named(String named) -> named;
            case FunctionExpression call when call.function().value() instanceof NamedFunction(String named) -> named;
            default -> null;
        };
        if (name == null) return null;
        IExpression variable = FlatExpressionParser.variable(name.toLowerCase(Locale.ROOT));
        return variable == null ? IExpression.ref(name) : variable;
    }

    /**
     * 是不是一次零参调用以上的函数调用，也就是能用 {@link FunctionExpression#arguments()} 往下比的节点。
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isNonCall(IExpression expression) {
        return expression instanceof FunctionExpression
               && FlatExpressionWriter.resolveReference(expression) == null;
    }

    /**
     * 两次调用是不是调同一个函数。
     *
     * <p>对象形式里内建函数用直接句柄（没有注册键），从 flat 文本读回来的是注册表引用，
     * 同一个函数两种句柄，也得算同一个意思；数据包函数则一律按注册键比。</p>
     */
    private static boolean sameFunction(FunctionExpression left, FunctionExpression right) {
        Holder<IFunction> leftHolder = left.function();
        Holder<IFunction> rightHolder = right.function();
        if (leftHolder.equals(rightHolder)) return true;
        ResourceKey<IFunction> leftKey = leftHolder.unwrapKey().orElse(null);
        ResourceKey<IFunction> rightKey = rightHolder.unwrapKey().orElse(null);
        if (leftKey != null && rightKey != null) return leftKey.equals(rightKey);
        // 一边是引用、一边是直接句柄（内建函数在对象形式里就是直接句柄）：
        // 内建函数没有注册键，按枚举名认；否则比句柄里的值
        IFunction leftValue = leftHolder.value();
        IFunction rightValue = rightHolder.value();
        if (leftValue instanceof LibBuiltInFunctions leftBuiltin) {
            return rightValue instanceof LibBuiltInFunctions rightBuiltin
                   && leftBuiltin.getSerializedName().equals(rightBuiltin.getSerializedName());
        }
        return leftValue.equals(rightValue);
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
            // 名字写不出文本时必须退回对象形式：$(name...) 里的载荷没有校验的话，
            // 带 ')'、空格或 "..." 的名字会写出读不回来的文本，或者从「取一个数字」变成「取整份列表」
            if (!FlatExpressionParser.isWritableReferenceName(
                reference.name(),
                reference instanceof IExpression.Reference.Spread
            )) {
                return Optional.empty();
            }
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
            return FlatExpressionParser.isWritableReferenceName(name, false)
                ? Optional.of("$(" + name + ")")
                : Optional.empty();
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
     * <p>操作数本身以 {@code -} 开头（只可能是负常量，例如 {@code -0.5}）时不必拦：括号保证了不会写出
     * {@code --x} 这种不合法文本，{@code -(-0.5)} 读回来仍是 {@code subtract(0, const -0.5)}。</p>
     */
    private static Optional<String> negation(String operand) {
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
                    // lambda 也不能并置：2(x -> $(x)) 写成 2x -> $(x) 会读成「参数 2x」的 lambda，根本读不回来
                    .filter(text -> !FlatExpressionWriter.isLambda(right)
                                    && FlatExpressionWriter.juxtaPositionable(text))
                    // 首字符判断挡不住「吃掉后半段」的情况：数字后的 e/E 会被 parseNumber 当成指数，
                    // 2*e1(x) 写成 2e1(x) 就读成了 multiply(20, x)。所以并置方案必须实测能读回同一棵树，
                    // 读不回就退回显式乘号（写成 2*e1(x) 一定正确）
                    .filter(text -> FlatExpressionWriter.juxtapositionReadsBack(
                        multiplier + text,
                        functions
                    ))
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
     *
     * <p>零参 lambda 写不出来：文本里空参数串会被读成「缺参数名」而报错，而零参 lambda 用对象形式
     * （{@code parameters: []}）是合法的，所以这里返回 {@code Optional.empty()} 让它退回对象形式。</p>
     */
    private static Optional<String> visitLambda(LambdaFunction lambda, HolderGetter<IFunction> functions) {
        List<String> declarations = lambda.declarations();
        if (declarations.isEmpty()) return Optional.empty();
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
            // 递归一律走 bare：write 会为每次调用做自校验，实参里再进来一次就无限递归了
            Optional<String> written = FlatExpressionWriter.bare(call.arguments().get(index), functions);
            if (written.isEmpty()) return Optional.empty();
            if (index > 0) text.append(',');
            text.append(written.get());
        }
        return Optional.of(text.append(')').toString());
    }

    /**
     * 取函数在 flat 文本里的名字：内建函数用枚举名，数据包函数用注册名，
     * {@code anvillib} 命名空间省略。
     *
     * <p>省略命名空间的前提是「写出来还能读回同一个函数」，而解析器认为 {@code anvillib:<内建名>} 就是
     * 内建函数。若数据包里真注册了这种撞名函数，写回 {@code sqrt} 会读成内建 {@code sqrt}，所以这里
     * 返回 {@code null} 让调用方退回对象形式，而不是静默换掉求值结果。</p>
     */
    private static @Nullable String functionName(FunctionExpression call, HolderGetter<IFunction> functions) {
        IFunction function = call.function().value();
        if (function instanceof LibBuiltInFunctions builtin) {
            return builtin.getSerializedName();
        }
        ResourceKey<IFunction> key = call.function().unwrapKey().orElse(null);
        if (key == null || functions.get(key).isEmpty()) return null;
        return FlatExpressionWriter.writableName(key.location(), function);
    }

    /**
     * 一个注册名能否写成 flat 文本，不能则返回 {@code null}。
     *
     * <p>名字必须既撞不上别的含义、又能被解析器原样读回来：传入值名（{@code x}/{@code x0}）与 {@code -}、
     * {@code /} 这类标识符字符集之外的路径由
     * {@link FlatExpressionParser#isWritableFunctionName(String)} 一并排除，免得两边规则各写一份再漂移；
     * 内建名只在 {@code anvillib} 命名空间下才需要排除。</p>
     */
    private static @Nullable String writableName(ResourceLocation id, IFunction function) {
        String name = id.getNamespace().equals(AnvilLibMath.MAIN_ID) ? id.getPath() : id.toString();
        if (!FlatExpressionParser.isWritableFunctionName(name)) return null;
        // 只有 anvillib 命名空间才有被内建函数抢名的问题：解析器对省略命名空间的名字（以及显式的
        // anvillib:<名字>）一律先认内建函数。其它命名空间是先查注册表、查到就用，
        // 所以 mymod:max 撞上内建 max 也能读回，不必退回对象形式
        if (!id.getNamespace().equals(AnvilLibMath.MAIN_ID)) return name;
        LibBuiltInFunctions shadowed = LibBuiltInFunctions.byName(id.getPath());
        // 解析器对 anvillib:<内建名> 一律按内建函数处理，撞名时这个名字写出去就变了意思
        if (shadowed != null && shadowed != function) return null;
        return name;
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
        // lambda 的 -> 绑得比所有运算符都松，拿它当运算符的操作数读不回来。
        // 返回 true 会让 operand() 试图补括号，括号补得住形状；但解析器不接受 2(x -> $(x)) 这类并置写法，
        // 所以并置分支还单独排除了 lambda，两条路都写不出时 operand() 才放弃、退回对象形式
        if (function.value() instanceof LambdaFunction) return true;
        Operator child = Operator.of(function.value());
        if (child == null) return false;
        if (child.precedence != parentPrecedence) return child.precedence < parentPrecedence;
        return (position == OperandPosition.LEFT) == (child.associativity == Associativity.RIGHT);
    }

    private static @Nullable Holder<IFunction> functionOf(IExpression expression) {
        return expression instanceof FunctionExpression call ? call.function() : null;
    }

    /**
     * 判断一个表达式是不是 lambda：lambda 的 {@code ->} 绑得比所有运算符都松，不能被当成普通操作数。
     */
    private static boolean isLambda(IExpression expression) {
        Holder<IFunction> function = FlatExpressionWriter.functionOf(expression);
        return function != null && function.value() instanceof LambdaFunction;
    }

    /**
     * 并置只用于 {@code 2x}、{@code 2(x+1)}、{@code 2$(a)} 这类写法，右侧只能是标识符、括号或
     * {@code $(name)}；以数字开头时并置会被读成另一个数（{@code 2*3} 写成 {@code 23}），只能写 {@code *}。
     */
    private static boolean juxtaPositionable(String text) {
        if (text.isEmpty()) return false;
        char first = text.charAt(0);
        return Character.isLetter(first) || first == '_' || first == '(';
    }

    /**
     * 并置出来的文本能不能读回原来的那棵树。
     *
     * <p>首字符判断只能挡住「并置成另一个数」，挡不住「前半段被吃掉」：数字后面的 {@code e}/{@code E}
     * 会被 {@code parseNumber} 当成指数，于是 {@code 2*e1(x)} 写成 {@code 2e1(x)} 读回
     * {@code multiply(20, x)}——值静默改变，不报错。这类名字（{@code e1}、{@code e2}、{@code e1abc}）
     * 是合法注册名，所以只能实测：解析失败或解析结果与 {@code call} 不同，就不并置，退回显式乘号。</p>
     */
    private static boolean juxtapositionReadsBack(
        String text,
        HolderGetter<IFunction> functions
    ) {
        // 预检内部要再写一次，再进去就无限递归了
        if (FlatExpressionWriter.JUXTAPOSING.get()) return true;
        FlatExpressionWriter.JUXTAPOSING.set(true);
        try {
            return FlatExpressionWriter.bare(FlatExpressionParser.parseValue(text, functions), functions)
                .filter(text::equals)
                .isPresent();
        } catch (RuntimeException exception) {
            return false;
        } finally {
            FlatExpressionWriter.JUXTAPOSING.set(false);
        }
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
     * <p>这里只列得出来真正的内建函数：一元负号在解析器里就是 {@code subtract(0, x)}，跟着 {@code -}
     * 那一层走，不需要单独一项。</p>
     */
    enum Operator {
        ADD("+", 1, Associativity.LEFT),
        SUBTRACT("-", 1, Associativity.LEFT),
        MULTIPLY("*", 2, Associativity.LEFT),
        DIVIDE("/", 2, Associativity.LEFT),
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
