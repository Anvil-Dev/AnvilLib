package dev.anvilcraft.lib.v2.math.expression;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import dev.anvilcraft.lib.v2.math.AnvilLibMath;
import dev.anvilcraft.lib.v2.math.expression.function.ConstantFunction;
import dev.anvilcraft.lib.v2.math.expression.function.IFunction;
import dev.anvilcraft.lib.v2.math.expression.function.InputFunction;
import dev.anvilcraft.lib.v2.math.expression.function.LambdaFunction;
import dev.anvilcraft.lib.v2.math.expression.function.Parameter;
import dev.anvilcraft.lib.v2.math.expression.function.Parameters;
import dev.anvilcraft.lib.v2.math.init.LibBuiltInFunctions;
import dev.anvilcraft.lib.v2.math.init.LibRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * flat 表达式语法，把 {@code x*2}、{@code 2x}、{@code 2$(cost)+1} 这样的文本解析为表达式树。
 *
 * <p>支持数字、{@code x}/{@code y}/{@code z}（等同于 {@code x0}/{@code x1}/{@code x2}）、
 * {@code $(name)} 具名传入值、四则运算（乘号可写 {@code *}、{@code ×} 或 {@code ·}，除号可写
 * {@code /} 或 {@code ÷}）、括号、一元正负号、隐式乘法（{@code 2x}、{@code 2(x+1)}）、乘方 {@code ^}，
 * 以及 {@code sqrt(x)}、{@code pow(x,2)}、{@code max(x,1)} 等函数调用。</p>
 *
 * <p>解析结果直接是 {@link IExpression} 组成的树：四则运算解析为对应的二元内建函数调用，
 * {@code x} 解析为 {@link InputFunction} 的零参调用，{@code $(name)} 与 {@code $(name...)} 解析为
 * {@link IExpression.Reference}，数字与整段数字文本解析为 {@link ConstantFunction} 的零参调用。
 * 反向的文本化由 {@link FlatExpressionWriter} 负责。</p>
 *
 * <p>函数名一律先按注册名处理：不带命名空间的按 {@link AnvilLibMath#MAIN_ID} 补齐。{@code anvillib}
 * 命名空间下的名字按路径匹配内建函数，因此 {@code sqrt(x)} 与 {@code anvillib:sqrt(x)} 等价，参数个数
 * 在解析期就校验；其它命名空间先认数据包注册表，注册不到时退回同名内建函数。解析需要拿到函数注册表，
 * 所以只接受 {@link RegistryOps}。</p>
 */
public final class FlatExpressionParser {
    /**
     * 解析结果按函数注册表分组缓存。
     *
     * <p>键用弱引用，本意是换注册表后旧缓存能被回收，但条目里的表达式树持有注册表函数的
     * {@code Holder.Reference}，而 {@code Holder.Reference} 又带一个指回注册表的 {@code owner}，
     * 于是「值强引用键」，弱键实际回收不掉。数据包重载时请调用 {@link #clearCache()}；容量上限则保证
     * 下游拿运行时拼接的文本反复调用 {@link #parseValue} 也不会无界增长。</p>
     */
    private static final int CACHE_CAPACITY = 512;

    private static final Map<HolderGetter<IFunction>, Map<String, IExpression>> CACHE = Collections.synchronizedMap(
        new WeakHashMap<>()
    );

    /**
     * 嵌套层数上限，括号、乘方、函数实参与 lambda 函数体共用这一个计数。
     *
     * <p>取 512 是因为递归下降每层要占好几个栈帧，2000 层括号就足以打穿默认栈；离正常表达式又足够远
     * （手写表达式不会有几百层嵌套）。</p>
     */
    private static final int MAX_NESTING_DEPTH = 512;

    private final String source;
    private final HolderGetter<IFunction> functions;
    private int position;
    private int depth;

    private FlatExpressionParser(String source, HolderGetter<IFunction> functions) {
        this.source = source;
        this.functions = functions;
    }

    /**
     * flat 文本的编解码：写成一段文本，读回来是解析后的表达式树。
     *
     * <p>解析与回写都要查 {@link LibRegistries#FUNCTION_KEY}，因此只接受 {@link RegistryOps}。</p>
     */
    public static Codec<IExpression> codec() {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<IExpression, T>> decode(DynamicOps<T> ops, T input) {
                DataResult<String> source = Codec.STRING.parse(ops, input);
                if (source.result().isEmpty()) return DataResult.error(() -> "Not a flat expression: " + input);
                HolderGetter<IFunction> functions = FlatExpressionParser.functionGetter(ops);
                if (functions == null) {
                    return DataResult.error(
                        () -> "Cannot access registry " + LibRegistries.FUNCTION_KEY + ", use RegistryOps"
                    );
                }
                return FlatExpressionParser
                    .parseResult(source.getOrThrow(), functions)
                    .map(call -> Pair.of(call, input));
            }

            @Override
            public <T> DataResult<T> encode(IExpression input, DynamicOps<T> ops, T prefix) {
                HolderGetter<IFunction> functions = FlatExpressionParser.functionGetter(ops);
                if (functions == null) {
                    return DataResult.error(
                        () -> "Cannot access registry " + LibRegistries.FUNCTION_KEY + ", use RegistryOps"
                    );
                }
                return FlatExpressionWriter
                    .write(input, functions)
                    .<DataResult<T>>map(text -> Codec.STRING.encode(text, ops, prefix))
                    .orElseGet(() -> DataResult.error(() -> "Expression cannot be written as flat text: " + input));
            }
        };
    }

    /**
     * 解析 flat 表达式，整段文本也可以只是一个数字或一次按名字取值。
     *
     * @param source    flat 表达式文本
     * @param functions 函数注册表，用于解析按名引用的函数
     * @throws IllegalArgumentException 表达式不合法时抛出
     */
    public static IExpression parseValue(String source, HolderGetter<IFunction> functions) {
        Map<String, IExpression> cache = FlatExpressionParser.CACHE.computeIfAbsent(
            functions,
            key -> Collections.synchronizedMap(
                new LinkedHashMap<>(16, 0.75F, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<String, IExpression> eldest) {
                        return this.size() > FlatExpressionParser.CACHE_CAPACITY;
                    }
                }
            )
        );
        synchronized (cache) {
            IExpression cached = cache.get(source);
            if (cached != null) return cached;
            IExpression parsed = new FlatExpressionParser(source, functions).parseWhole();
            cache.put(source, parsed);
            return parsed;
        }
    }

    /**
     * 清空解析缓存，数据包重载后调用可以让旧注册表对应的表达式树立刻被回收。
     *
     * <p>{@link #CACHE} 的弱键指望「换注册表后旧条目自动消失」，但条目里的函数引用会反向指回注册表，
     * 弱键因此回收不掉。模块内的 {@code LibCacheReloadHandler} 已经挂在服务器启动与数据包同步事件上
     * 调用本方法，下游不需要再管；只有在自建注册表或测试里才需要手动调。</p>
     */
    public static void clearCache() {
        FlatExpressionParser.CACHE.clear();
    }

    /**
     * 从动态操作中取出函数注册表，取不到时返回 {@code null}。
     */
    @Nullable
    public static HolderGetter<IFunction> functionGetter(DynamicOps<?> ops) {
        if (!(ops instanceof RegistryOps<?> registryOps)) return null;
        return registryOps.getter(LibRegistries.FUNCTION_KEY).orElse(null);
    }

    /**
     * 把不带命名空间的名字补成 {@link AnvilLibMath#MAIN_ID} 命名空间的名字。
     */
    static ResourceLocation withDefaultNamespace(String name) {
        return name.indexOf(':') < 0 ? AnvilLibMath.of(name) : ResourceLocation.parse(name);
    }

    private static DataResult<IExpression> parseResult(String source, HolderGetter<IFunction> functions) {
        try {
            return DataResult.success(FlatExpressionParser.parseValue(source, functions));
        } catch (RuntimeException exception) {
            return DataResult.error(() -> "Invalid expression: " + exception.getMessage());
        }
    }

    private IExpression parseWhole() {
        IExpression expression = this.parseLambda();
        this.skipWhitespace();
        if (!this.atEnd()) throw this.error("unexpected character '" + this.peek() + "'");
        return expression;
    }

    /**
     * lambda：{@code x -> $(x)*2}、{@code (a, b) -> $(a)+$(b)}、变参写 {@code x... -> ...}。
     *
     * <p>先看参数列表后面有没有 {@code ->}，没有就交回普通表达式；只有参数列表本身合法时才认为是在写
     * lambda，这样 {@code a - b} 之类不会被误判。</p>
     */
    private IExpression parseLambda() {
        return this.guardDepth(() -> {
            this.skipWhitespace();
            int snapshot = this.position;
            List<String> parameters;
            try {
                parameters = this.parseLambdaParameters();
            } catch (IllegalArgumentException exception) {
                this.position = snapshot;
                return this.parseAdditive();
            }
            if (parameters == null) {
                this.position = snapshot;
                return this.parseAdditive();
            }
            IExpression body = this.parseLambda();
            return FunctionExpression.of(LambdaFunction.of(parameters, body));
        });
    }

    /**
     * 进入一层嵌套：递归下降的入口都要先过这里，超过 {@link #MAX_NESTING_DEPTH} 时给出可读的报错。
     *
     * <p>不加这一步的话，上万层括号或 {@code 2^2^2^…} 会抛 {@link StackOverflowError}——那是 {@link Error}，
     * {@code parseResult} 的 {@code catch (RuntimeException)} 拦不住，会直接从 codec 穿到数据包加载流程。</p>
     */
    private IExpression guardDepth(Supplier<IExpression> parse) {
        if (++this.depth > FlatExpressionParser.MAX_NESTING_DEPTH) {
            throw this.error("expression nests too deeply, the limit is " + FlatExpressionParser.MAX_NESTING_DEPTH);
        }
        try {
            return parse.get();
        } finally {
            this.depth--;
        }
    }

    /**
     * 读 lambda 的参数列表，当前位置不是 lambda 时返回 {@code null} 并保持位置不变。
     */
    @Nullable
    private List<String> parseLambdaParameters() {
        this.skipWhitespace();
        if (this.atEnd()) return null;
        if (this.peek() == '(') {
            // lambda 的参数列表不能在括号里再分组，所以右括号就是列表的结束
            int start = this.position + 1;
            int search = this.position;
            while (search < this.source.length() && this.source.charAt(search) != ')') {
                search++;
            }
            if (search >= this.source.length()) return null;
            this.position = search + 1;
            if (!this.match('-') || !this.match('>')) return null;
            return FlatExpressionParser.splitParameters(this.source.substring(start, search), this);
        }
        if (!isIdentifierStart(this.peek())) return null;
        int start = this.position;
        while (!this.atEnd() && (isIdentifierPart(this.peek()) || this.peek() == '.')) {
            this.position++;
        }
        int end = this.position;
        if (!this.match('-') || !this.match('>')) return null;
        return FlatExpressionParser.splitParameters(this.source.substring(start, end), this);
    }

    /**
     * 把 lambda 的参数列表文本按逗号切开，顺带校验每个名字。
     */
    private static List<String> splitParameters(String source, FlatExpressionParser parser) {
        List<String> names = new ArrayList<>();
        for (String piece : source.split(",", -1)) {
            String name = piece.trim();
            if (name.isEmpty()) throw parser.error("expected a lambda parameter name");
            String base = name.endsWith(Parameter.VARIADIC_SUFFIX)
                ? name.substring(0, name.length() - Parameter.VARIADIC_SUFFIX.length())
                : name;
            for (int index = 0; index < base.length(); index++) {
                char current = base.charAt(index);
                if (index == 0 ? !isIdentifierStart(current) : !isIdentifierPart(current)) {
                    throw parser.error("invalid lambda parameter name '" + name + "'");
                }
            }
            names.add(name);
        }
        // 参数名重复或出现两个变参时直接报错，不留到调用时才发现
        Parameters.parse(names);
        return names;
    }

    private IExpression parseAdditive() {
        IExpression left = this.parseMultiplicative();
        while (true) {
            if (this.match('+')) {
                left = this.call("add", left, this.parseMultiplicative());
                continue;
            }
            if (this.match('-')) {
                left = this.call("subtract", left, this.parseMultiplicative());
                continue;
            }
            return left;
        }
    }

    private IExpression parseMultiplicative() {
        IExpression left = this.parseUnary();
        while (true) {
            if (this.match('*') || this.match('×') || this.match('·')) {
                left = this.call("multiply", left, this.parseUnary());
                continue;
            }
            if (this.match('/') || this.match('÷')) {
                left = this.call("divide", left, this.parseUnary());
                continue;
            }
            if (!this.startsWithValue()) return left;
            left = this.call("multiply", left, this.parseUnary());
        }
    }

    private IExpression parseUnary() {
        // 一元符号在这里直接递归（每个 + / - 一帧），不经过 parseLambda 与 parsePower，
        // 因此必须自己也过一遍深度守卫，否则 "-"*20000 这种输入照样能把栈打穿
        return this.guardDepth(() -> {
            // 符号紧跟数字时算数字的一部分，这样 -2 与 -5.0E-4 都是常量字面量，回写结果能原样读回。
            // 后面接 ^ 时不算：-2^2 仍按一元负号优先于乘方处理，写成 (-2)^2 才是负底数
            if (this.signedNumberLiteralAhead()) {
                return this.parseNumber();
            }
            if (this.match('-')) {
                return this.call("subtract", ConstantFunction.of(0).call(), this.parseUnary());
            }
            if (this.match('+')) return this.parseUnary();
            return this.parsePower();
        });
    }

    private IExpression parsePower() {
        return this.guardDepth(() -> {
            IExpression base = this.parseValue();
            if (this.match('^')) {
                return this.call("pow", base, this.parseUnary());
            }
            return base;
        });
    }

    private IExpression parseValue() {
        this.skipWhitespace();
        if (this.atEnd()) throw this.error("unexpected end of expression");
        char current = this.peek();
        if (current == '(') {
            this.position++;
            IExpression expression = this.parseLambda();
            if (!this.match(')')) throw this.error("expected ')'");
            return expression;
        }
        if (current == '$') return this.parseNamed();
        if (isDigit(current) || current == '.') return this.parseNumber();
        // 符号后面直接跟数字时算数字的一部分（-2、+1.5），否则走 parseUnary 的一元正负号。
        // 回写极小的负数会写出 -5.0E-4 这种字面量，必须能原样读回
        if ((current == '-' || current == '+') && this.signedNumberAhead()) return this.parseNumber();
        if (isIdentifierStart(current)) return this.parseIdentifier();
        throw this.error("unexpected character '" + current + "'");
    }

    /**
     * 当前位置是 {@code -} 或 {@code +}，后面是否紧跟数字。
     */
    private boolean signedNumberAhead() {
        if (this.position + 1 >= this.source.length()) return false;
        char next = this.source.charAt(this.position + 1);
        return isDigit(next) || next == '.';
    }

    /**
     * 当前位置开始是否是一个带符号的数字字面量，并且后面不是 {@code ^}。
     *
     * <p>{@code -2} 读成常量 -2；{@code -2^2} 留给一元负号，按 {@code -(2^2)} 处理。</p>
     */
    private boolean signedNumberLiteralAhead() {
        if (this.atEnd() || this.source.charAt(this.position) != '-') return false;
        if (!this.signedNumberAhead()) return false;
        int probe = this.position + 1;
        while (probe < this.source.length() && isDigit(this.source.charAt(probe))) {
            probe++;
        }
        if (probe < this.source.length() && this.source.charAt(probe) == '.') {
            probe++;
            while (probe < this.source.length() && isDigit(this.source.charAt(probe))) {
                probe++;
            }
        }
        if (probe < this.source.length() && (this.source.charAt(probe) == 'e' || this.source.charAt(probe) == 'E')) {
            int exponent = probe + 1;
            if (exponent < this.source.length() && (this.source.charAt(exponent) == '+' || this.source.charAt(exponent) == '-')) {
                exponent++;
            }
            int digits = exponent;
            while (digits < this.source.length() && isDigit(this.source.charAt(digits))) {
                digits++;
            }
            if (digits > exponent) probe = digits;
        }
        while (probe < this.source.length() && Character.isWhitespace(this.source.charAt(probe))) {
            probe++;
        }
        return probe >= this.source.length() || this.source.charAt(probe) != '^';
    }

    /**
     * 读 {@code $(name)}，以及取整个变参列表的 {@code $(name...)}。
     */
    private IExpression parseNamed() {
        this.position++;
        if (!this.match('(')) throw this.error("expected '(' after '$'");
        int start = this.position;
        while (!this.atEnd() && this.peek() != ')') {
            this.position++;
        }
        if (this.atEnd()) throw this.error("expected ')'");
        String name = this.source.substring(start, this.position).trim();
        this.position++;
        if (name.isEmpty()) throw this.error("expected a name between '$(' and ')'");
        // $(name...) 取整份列表，与 $(name) 是不同的节点，由 IExpression.ref 按 ... 后缀区分
        return IExpression.ref(name);
    }

    /**
     * 读一个数字，支持符号与 {@code 1.5E-4} 这样的指数记法。
     *
     * <p>回写用 {@link Double#toString} 输出极值时给出的正是指数记法，负常量与负零也写成带符号的
     * 字面量，两边必须对称，否则 {@code sqrt(0.0001)} 回写成 {@code sqrt(1.0E-4)} 之后就再也读不回来了。</p>
     */
    private IExpression parseNumber() {
        final int start = this.position;
        if (!this.atEnd() && (this.peek() == '-' || this.peek() == '+')) {
            this.position++;
        }
        while (!this.atEnd() && isDigit(this.peek())) {
            this.position++;
        }
        if (!this.atEnd() && this.peek() == '.') {
            do {
                this.position++;
            } while (!this.atEnd() && isDigit(this.peek()));
        }
        int mantissaEnd = this.position;
        if (!this.atEnd() && (this.peek() == 'e' || this.peek() == 'E')) {
            this.position++;
            if (!this.atEnd() && (this.peek() == '+' || this.peek() == '-')) {
                this.position++;
            }
            int exponentStart = this.position;
            while (!this.atEnd() && isDigit(this.peek())) {
                this.position++;
            }
            // 指数过长时 Double.parseDouble 会溢出，长到没有意义时不必再收进数字里
            boolean valid = this.position > exponentStart && this.position - exponentStart <= 8;
            if (!valid) this.position = mantissaEnd;
        }
        String text = this.source.substring(start, this.position);
        try {
            double value = Double.parseDouble(text);
            // 溢出成无穷的字面量读不回来（回写侧拒绝非有限值），不如在解析期就报错。
            // 这里不回退 position：报错位置应当落在字面量末尾，回退到开头只会指错地方
            if (!Double.isFinite(value)) {
                throw this.error("number out of range '" + text + "'");
            }
            return ConstantFunction.of(value).call();
        } catch (NumberFormatException | ArithmeticException exception) {
            throw this.error("invalid number '" + text + "'");
        }
    }

    private IExpression parseIdentifier() {
        int start = this.position;
        while (!this.atEnd() && isIdentifierPart(this.peek())) {
            this.position++;
        }
        String name = this.source.substring(start, this.position);
        String lower = name.toLowerCase(Locale.ROOT);
        IExpression variable = FlatExpressionParser.variable(lower);
        if (variable != null) return variable;
        if (!this.match('(')) throw this.error("expected '(' after function '" + name + "'");
        List<IExpression> arguments = new ArrayList<>();
        if (!this.match(')')) {
            while (true) {
                arguments.add(this.parseLambda());
                if (this.match(',')) continue;
                if (this.match(')')) break;
                throw this.error("expected ',' or ')'");
            }
        }
        // anvillib 命名空间（含省略命名空间的写法）里的名字按路径匹配内建函数，这样 sqrt(x) 与
        // anvillib:sqrt(x) 等价，参数个数也在解析期就校验。
        ResourceLocation id = FlatExpressionParser.withDefaultNamespace(lower);
        if (id.getNamespace().equals(AnvilLibMath.MAIN_ID)) {
            LibBuiltInFunctions builtin = LibBuiltInFunctions.byName(id.getPath());
            if (builtin != null) return builtin.callChecked(arguments).getOrThrow(this::error);
            return FunctionExpression.of(this.function(lower, name), arguments.toArray(IExpression[]::new));
        }
        // 其它命名空间先认注册表，注册不到时再退回同名内建函数，
        // 这样 mymod:max 既可以是数据包函数，也可以只是内建 max 的另一种写法
        if (this.functions.get(ResourceKey.create(LibRegistries.FUNCTION_KEY, id)).isPresent()) {
            return FunctionExpression.of(this.function(lower, name), arguments.toArray(IExpression[]::new));
        }
        LibBuiltInFunctions builtin = LibBuiltInFunctions.byName(id.getPath());
        if (builtin == null) {
            throw this.error("unknown function '" + name + "'");
        }
        return builtin.callChecked(arguments).getOrThrow(this::error);
    }

    /**
     * 按内建名调用一个函数，用于四则运算与乘方，参数个数由定义校验。
     */
    private FunctionExpression call(String name, IExpression... arguments) {
        LibBuiltInFunctions builtin = LibBuiltInFunctions.byName(name);
        if (builtin == null) throw this.error("unknown builtin function '" + name + "'");
        return builtin.call(arguments);
    }

    /**
     * 按注册名找数据包函数，找不到时抛出带位置信息的异常。
     */
    private Holder<IFunction> function(String lower, String name) {
        ResourceLocation id = FlatExpressionParser.withDefaultNamespace(lower);
        Optional<Holder.Reference<IFunction>> reference = this.functions
            .get(ResourceKey.create(LibRegistries.FUNCTION_KEY, id));
        if (reference.isPresent()) return reference.get();
        throw this.error("unknown function '" + name + "'");
    }

    /**
     * 解析 {@code x}/{@code y}/{@code z} 与 {@code x0}/{@code x1}/{@code x2} 形式的传入值引用。
     */
    @Nullable
    static IExpression variable(String name) {
        switch (name) {
            case "x" -> {
                return InputFunction.call(0);
            }
            case "y" -> {
                return InputFunction.call(1);
            }
            case "z" -> {
                return InputFunction.call(2);
            }
            default -> {}
        }
        if (name.length() < 2 || name.charAt(0) != 'x') return null;
        String digits = name.substring(1);
        for (int index = 0; index < digits.length(); index++) {
            if (!isDigit(digits.charAt(index))) return null;
        }
        try {
            return InputFunction.call(Integer.parseInt(digits));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Input index is too large: '" + digits + "'");
        }
    }

    /**
     * 下一个值是否能直接接在左侧，用于识别隐式乘法。
     */
    private boolean startsWithValue() {
        this.skipWhitespace();
        if (this.atEnd()) return false;
        char current = this.peek();
        return current == '(' || current == '$' || isIdentifierStart(current);
    }

    private boolean match(char character) {
        this.skipWhitespace();
        if (this.atEnd() || this.peek() != character) return false;
        this.position++;
        return true;
    }

    private void skipWhitespace() {
        while (!this.atEnd() && Character.isWhitespace(this.peek())) {
            this.position++;
        }
    }

    private char peek() {
        return this.source.charAt(this.position);
    }

    private boolean atEnd() {
        return this.position >= this.source.length();
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(
            message + " at position " + this.position + " of expression \"" + this.source + "\""
        );
    }

    private static boolean isDigit(char character) {
        return character >= '0' && character <= '9';
    }

    static boolean isIdentifierStart(char character) {
        return character == '_' || (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z');
    }

    static boolean isIdentifierPart(char character) {
        return isIdentifierStart(character) || isDigit(character) || character == ':' || character == '.';
    }

    /**
     * 判断一个名字写成裸标识符后，能不能读回同一个函数。
     *
     * <p>回写侧与解析侧必须共用这一条判断，否则「写得出来、读不回去」的文本会静默落进存档。三种情况都不行：
     * 名字不是合法标识符（{@code collision-free}、{@code utils/triple}，{@code -} 与 {@code /} 都不是标识符字符）；
     * 首字符不是标识符起始字符（{@code $x}、{@code .x} 会被当成 {@code $(name)} 或小数点）；
     * 名字长得像传入值引用（{@code x}、{@code y}、{@code z}、{@code x0}）——解析器先认变量、后认函数，
     * {@code x0(5)} 会解析成 {@code <input 0> * 5}，函数本身根本不会被调用。</p>
     */
    static boolean isWritableFunctionName(String name) {
        if (name.isEmpty() || !isIdentifierStart(name.charAt(0))) return false;
        for (int index = 1; index < name.length(); index++) {
            if (!isIdentifierPart(name.charAt(index))) return false;
        }
        return FlatExpressionParser.variable(name.toLowerCase(Locale.ROOT)) == null;
    }
}
