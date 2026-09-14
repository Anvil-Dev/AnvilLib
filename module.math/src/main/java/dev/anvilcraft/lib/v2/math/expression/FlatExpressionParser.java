package dev.anvilcraft.lib.v2.math.expression;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import dev.anvilcraft.lib.v2.math.AnvilLibMath;
import dev.anvilcraft.lib.v2.math.init.LibBuiltInFunctions;
import dev.anvilcraft.lib.v2.math.expression.function.ConstantFunction;
import dev.anvilcraft.lib.v2.math.expression.function.IFunction;
import dev.anvilcraft.lib.v2.math.expression.function.InputFunction;
import dev.anvilcraft.lib.v2.math.expression.function.NamedFunction;
import dev.anvilcraft.lib.v2.math.init.LibRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;

/**
 * flat 表达式语法，把 {@code x*2}、{@code 2x}、{@code 2$(cost)+1} 这样的文本解析为表达式树。
 *
 * <p>支持数字、{@code x}/{@code y}/{@code z}（等同于 {@code x0}/{@code x1}/{@code x2}）、
 * {@code $(name)} 具名传入值、四则运算（乘号可写 {@code *}、{@code ×} 或 {@code ·}，除号可写
 * {@code /} 或 {@code ÷}）、括号、一元正负号、隐式乘法（{@code 2x}、{@code 2(x+1)}）、乘方 {@code ^}，
 * 以及 {@code sqrt(x)}、{@code pow(x,2)}、{@code max(x,1)} 等函数调用。</p>
 *
 * <p>解析结果直接是 {@link FunctionExpression} 组成的树：四则运算解析为对应的二元内建函数调用，
 * {@code x} 与 {@code $(name)} 解析为 {@link InputFunction} 与 {@link NamedFunction} 的零参调用，
 * 数字与整段数字文本解析为 {@link ConstantFunction} 的零参调用。反向的文本化由
 * {@link FlatExpressionWriter} 负责。</p>
 *
 * <p>函数名一律先按注册名处理：不带命名空间的按 {@link AnvilLibMath#MAIN_ID} 补齐，然后去
 * {@link LibRegistries#FUNCTION_KEY} 里找，因此 {@code sqrt(x)} 与 {@code anvillib:sqrt(x)} 等价，
 * 数据包注册的函数可以直接写进文本。解析需要拿到函数注册表，所以只接受 {@link RegistryOps}。</p>
 */
public final class FlatExpressionParser {
    /**
     * 解析结果按函数注册表分组缓存。注册表实例在每次数据包重载时更换，因此换实例即等于换缓存。
     */
    private static final Map<HolderGetter<IFunction>, Map<String, IExpression>> CACHE = new ConcurrentHashMap<>();

    private final String source;
    private final HolderGetter<IFunction> functions;
    private int position;

    private FlatExpressionParser(String source, HolderGetter<IFunction> functions) {
        this.source = source;
        this.functions = functions;
    }

    /**
     * flat 文本的编解码：写成一段文本，读回来是解析后的调用树。
     *
     * <p>解析与回写都要查 {@link LibRegistries#FUNCTION_KEY}，因此只接受 {@link RegistryOps}。</p>
     */
    public static Codec<FunctionExpression> codec() {
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<FunctionExpression, T>> decode(DynamicOps<T> ops, T input) {
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
            public <T> DataResult<T> encode(FunctionExpression input, DynamicOps<T> ops, T prefix) {
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
     * 解析 flat 表达式，要求整段文本是一次函数调用。
     *
     * @param source    flat 表达式文本
     * @param functions 函数注册表，用于解析按名引用的函数
     * @throws IllegalArgumentException 表达式不合法时抛出
     */
    public static FunctionExpression parse(String source, HolderGetter<IFunction> functions) {
        return (FunctionExpression) FlatExpressionParser.parseValue(source, functions);
    }

    /**
     * 解析 flat 表达式，整段文本也可以只是一个数字。
     *
     * @param source    flat 表达式文本
     * @param functions 函数注册表，用于解析按名引用的函数
     * @throws IllegalArgumentException 表达式不合法时抛出
     */
    public static IExpression parseValue(String source, HolderGetter<IFunction> functions) {
        return FlatExpressionParser.CACHE
            .computeIfAbsent(functions, key -> new ConcurrentHashMap<>())
            .computeIfAbsent(source, key -> new FlatExpressionParser(key, functions).parseWhole());
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

    /**
     * 去掉 {@link AnvilLibMath#MAIN_ID} 命名空间前缀，其余命名空间原样保留。
     */
    static String stripDefaultNamespace(ResourceLocation id) {
        return id.getNamespace().equals(AnvilLibMath.MAIN_ID) ? id.getPath() : id.toString();
    }

    private static DataResult<FunctionExpression> parseResult(String source, HolderGetter<IFunction> functions) {
        try {
            return DataResult.success(FlatExpressionParser.parse(source, functions));
        } catch (RuntimeException exception) {
            return DataResult.error(() -> "Invalid expression: " + exception.getMessage());
        }
    }

    private IExpression parseWhole() {
        IExpression expression = this.parseAdditive();
        this.skipWhitespace();
        if (!this.atEnd()) throw this.error("unexpected character '" + this.peek() + "'");
        return expression;
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
        if (this.match('-')) {
            return this.call("subtract", ConstantFunction.of(0).call(), this.parseUnary());
        }
        if (this.match('+')) return this.parseUnary();
        return this.parsePower();
    }

    private IExpression parsePower() {
        IExpression base = this.parseValue();
        if (this.match('^')) {
            return this.call("pow", base, this.parseUnary());
        }
        return base;
    }

    private IExpression parseValue() {
        this.skipWhitespace();
        if (this.atEnd()) throw this.error("unexpected end of expression");
        char current = this.peek();
        if (current == '(') {
            this.position++;
            IExpression expression = this.parseAdditive();
            if (!this.match(')')) throw this.error("expected ')'");
            return expression;
        }
        if (current == '$') return this.parseNamed();
        if (isDigit(current) || current == '.') return this.parseNumber();
        if (isIdentifierStart(current)) return this.parseIdentifier();
        throw this.error("unexpected character '" + current + "'");
    }

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
        return NamedFunction.call(name);
    }

    private IExpression parseNumber() {
        final int start = this.position;
        while (!this.atEnd() && isDigit(this.peek())) {
            this.position++;
        }
        if (!this.atEnd() && this.peek() == '.') {
            do {
                this.position++;
            } while (!this.atEnd() && isDigit(this.peek()));
        }
        String text = this.source.substring(start, this.position);
        try {
            return ConstantFunction.of(Double.parseDouble(text)).call();
        } catch (NumberFormatException exception) {
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
                arguments.add(this.parseAdditive());
                if (this.match(',')) continue;
                if (this.match(')')) break;
                throw this.error("expected ',' or ')'");
            }
        }
        LibBuiltInFunctions builtin = LibBuiltInFunctions.byName(FlatExpressionParser.withDefaultNamespace(lower).getPath());
        if (builtin != null) {
            // 内建函数在解析期就校验参数个数，报错比求值时才发现更靠前
            return builtin.callChecked(arguments).getOrThrow(this::error);
        }
        return FunctionExpression.of(this.function(lower, name), arguments.toArray(IExpression[]::new));
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
    private static IExpression variable(String name) {
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

    private static boolean isIdentifierStart(char character) {
        return character == '_' || (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z');
    }

    private static boolean isIdentifierPart(char character) {
        return isIdentifierStart(character) || isDigit(character) || character == ':' || character == '.';
    }
}
