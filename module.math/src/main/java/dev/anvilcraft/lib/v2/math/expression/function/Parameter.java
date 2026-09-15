package dev.anvilcraft.lib.v2.math.expression.function;

import javax.annotation.Nullable;

/**
 * 一个形参的声明。
 *
 * <p>参数名以 {@code ...} 结尾表示它是变参，与 Java 的可变参数写法一致；变参吃下从它这一位开始的
 * 全部实参，因此一个签名里最多只有一个。变参至少要有一个实参可吃，且在函数体里取不到单个值：
 * 要逐个取值请把它交给 {@code forEach} 之类的函数。</p>
 *
 * @param name     参数名，也是函数体里 {@code $(name)} 引用它时用的名字；变参存不带 {@code ...} 的名字
 * @param variadic 是否为变参
 */
public record Parameter(String name, boolean variadic) {
    /**
     * 变参标记。
     */
    public static final String VARIADIC_SUFFIX = "...";

    public Parameter {
        if (name.isEmpty()) throw new IllegalArgumentException("Parameter name cannot be empty");
        if (name.endsWith(Parameter.VARIADIC_SUFFIX)) {
            throw new IllegalArgumentException(
                "Parameter name cannot contain '" + Parameter.VARIADIC_SUFFIX + "': " + name
            );
        }
    }

    /**
     * 按声明文本解析一个形参，{@code "x..."} 是变参。
     */
    public static Parameter parse(String declaration) {
        if (!declaration.endsWith(Parameter.VARIADIC_SUFFIX)) return new Parameter(declaration, false);
        String name = declaration.substring(0, declaration.length() - Parameter.VARIADIC_SUFFIX.length());
        if (name.isEmpty()) {
            throw new IllegalArgumentException(
                "Variadic parameter needs a name, not just '" + Parameter.VARIADIC_SUFFIX + "'"
            );
        }
        return new Parameter(name, true);
    }

    /**
     * 该形参的声明文本，变参带 {@code ...}。
     */
    public String declaration() {
        return this.variadic ? this.name + Parameter.VARIADIC_SUFFIX : this.name;
    }

    /**
     * 该形参最少要吃几个实参：固定形参恰好一个，变参可以是零个。
     *
     * <p>变参按 Java 的变参语义处理，{@code "x...": []} 是一次正常的空调用，例如 {@code min($(x...))} 在
     * {@code x} 绑成空列表时取不到任何值，由函数自己决定怎么兜底（内建 {@code min}/{@code max} 返回 0）。</p>
     */
    public int minimumCount() {
        return this.variadic ? 0 : 1;
    }

    @Override
    public String toString() {
        return this.declaration();
    }

    /**
     * 取出第一个变参形参，没有变参时返回 {@code null}。
     */
    public static @Nullable Parameter variadicOf(java.util.List<Parameter> parameters) {
        for (Parameter parameter : parameters) {
            if (parameter.variadic()) return parameter;
        }
        return null;
    }
}
