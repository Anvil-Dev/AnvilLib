package dev.anvilcraft.lib.v2.math.expression.function;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * 一次函数调用的形参声明：固定形参与至多一个变参。
 *
 * <p>声明文本里参数名以 {@code ...} 结尾即为变参，例如 {@code ["a", "x..."]}。变参可以在列表的任意
 * 位置：它吃掉「实参总数减去固定形参个数」个实参，因此它前后的固定形参照样拿得到值。一个签名里出现
 * 两个变参直接报错。</p>
 *
 * @param parameters 形参，按声明顺序与实参一一对应
 */
public record Parameters(List<Parameter> parameters) {
    /**
     * 没有形参。
     */
    public static final Parameters EMPTY = new Parameters(List.of());

    public Parameters {
        parameters = List.copyOf(parameters);
        Set<String> seen = new HashSet<>(parameters.size());
        Parameter variadic = null;
        for (Parameter parameter : parameters) {
            if (!seen.add(parameter.name())) {
                throw new IllegalArgumentException("Duplicate parameter name '" + parameter.name() + "'");
            }
            if (parameter.variadic()) {
                if (variadic != null) {
                    throw new IllegalArgumentException("At most one variadic parameter is allowed: " + parameters);
                }
                variadic = parameter;
            }
        }
    }

    /**
     * 按声明文本解析形参列表，{@code "x..."} 是变参。
     */
    public static Parameters parse(List<String> declarations) {
        if (declarations.isEmpty()) return Parameters.EMPTY;
        List<Parameter> parameters = new ArrayList<>(declarations.size());
        for (String declaration : declarations) {
            parameters.add(Parameter.parse(declaration));
        }
        return new Parameters(parameters);
    }

    /**
     * 按名字解析形参列表，全部当作固定形参。
     */
    public static Parameters of(List<String> names) {
        if (names.isEmpty()) return Parameters.EMPTY;
        List<Parameter> parameters = new ArrayList<>(names.size());
        for (String name : names) {
            parameters.add(new Parameter(name, false));
        }
        return new Parameters(parameters);
    }

    public boolean isEmpty() {
        return this.parameters.isEmpty();
    }

    public int size() {
        return this.parameters.size();
    }

    /**
     * 全部形参名，变参不带 {@code ...}。
     */
    public List<String> names() {
        List<String> names = new ArrayList<>(this.parameters.size());
        for (Parameter parameter : this.parameters) {
            names.add(parameter.name());
        }
        return names;
    }

    /**
     * 声明的文本形式，变参带 {@code ...}，用于报错与回写。
     */
    public List<String> declarations() {
        List<String> declarations = new ArrayList<>(this.parameters.size());
        for (Parameter parameter : this.parameters) {
            declarations.add(parameter.declaration());
        }
        return declarations;
    }

    /**
     * 变参形参，没有变参时为 {@code null}。
     */
    public @Nullable Parameter variadic() {
        return Parameter.variadicOf(this.parameters);
    }

    public boolean variadicExists() {
        return this.variadic() != null;
    }

    /**
     * 不是变参的形参个数，也就是变参至少要吃掉的实参个数。
     */
    public int fixedCount() {
        return this.parameters.size() - (this.variadicExists() ? 1 : 0);
    }

    /**
     * 一次调用最少要提供的实参个数。
     */
    public int minimumArity() {
        int minimum = 0;
        for (Parameter parameter : this.parameters) {
            minimum += parameter.minimumCount();
        }
        return minimum;
    }

    /**
     * 一次调用最多能提供的实参个数，有变参时是 {@link Integer#MAX_VALUE}。
     */
    public int maximumArity() {
        return this.variadicExists() ? Integer.MAX_VALUE : this.parameters.size();
    }

    /**
     * 校验实参个数。
     *
     * @throws IllegalArgumentException 个数不在允许范围内时抛出
     */
    public void checkArity(int size) {
        if (size < this.minimumArity() || size > this.maximumArity()) {
            throw new IllegalArgumentException("Expected " + this.range() + " arguments but got " + size);
        }
    }

    /**
     * 形参个数范围的可读描述。
     */
    public String range() {
        if (this.minimumArity() == this.maximumArity()) return Integer.toString(this.minimumArity());
        if (this.maximumArity() == Integer.MAX_VALUE) return "at least " + this.minimumArity();
        return this.minimumArity() + " to " + this.maximumArity();
    }
}
