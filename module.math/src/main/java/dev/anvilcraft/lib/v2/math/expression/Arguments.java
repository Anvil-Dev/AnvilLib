package dev.anvilcraft.lib.v2.math.expression;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 表达式求解时的传入值，既可以按下标引用，也可以按名字引用。
 *
 * <p>名字来自两种绑定：调用点提供的传入值，以及正在求值的那次调用为形参建立的名字绑定
 * （见 {@link dev.anvilcraft.lib.v2.math.expression.function.IFunction#apply}）。形参绑定覆盖调用点
 * 的同名传入值。</p>
 *
 * <p>名字绑定的值有两种：单个数字，以及变参绑定的数字列表。列表用 {@code $(name...)} 取出来交给别的
 * 变参函数；直接用 {@code $(name)} 取时得到列表里的最大值。</p>
 *
 * @param values 按下标引用的传入值，越界时取 0
 * @param named  按名字引用的传入值
 */
public record Arguments(List<Double> values, Map<String, Value> named) {
    public Arguments {
        values = List.copyOf(values);
        named = Map.copyOf(named);
    }

    /**
     * 一个名字绑定到的值。
     */
    public sealed interface Value permits Value.Single, Value.Many {
        /**
         * 单个数字。
         */
        record Single(double value) implements Value {
        }

        /**
         * 一串数字，来自变参绑定，可能为空。
         */
        record Many(List<Double> values) implements Value {
            public Many {
                values = List.copyOf(values);
            }
        }
    }

    /**
     * 只按下标引用的传入值。
     */
    public static Arguments of(double... values) {
        return new Arguments(Arguments.box(values), Map.of());
    }

    /**
     * 一次绑定多个具名传入值，用于把实参按名字绑定给形参。
     *
     * @param values 按下标引用的传入值，沿用调用点的上下文
     * @param names  绑定的名字，与 {@code bound} 按下标一一对应
     * @param bound  绑定的值，列表对应变参绑定
     */
    public static Arguments of(List<Double> values, List<String> names, List<Value> bound) {
        return new Arguments(values, Map.of()).withAll(names, bound);
    }

    /**
     * 在已有名字绑定之上再加一批绑定，同名的以新值为准。
     */
    public Arguments withAll(List<String> names, List<Value> bound) {
        Map<String, Value> extended = new LinkedHashMap<>(this.named);
        for (int index = 0; index < names.size(); index++) {
            extended.put(names.get(index), bound.get(index));
        }
        return new Arguments(this.values, extended);
    }

    /**
     * 该名字绑定到的列表，不是列表绑定时为空。
     */
    public List<Double> list(String name) {
        return this.named.get(name) instanceof Value.Many(List<Double> values1) ? values1 : List.of();
    }

    /**
     * 该名字是不是绑定成了一份列表。
     *
     * <p>{@link #list(String)} 对「没绑定的名字」和「绑定成空列表的变参」都给空列表，二者的区别只在
     * 这里看得出来：前者说明 {@code $(name...)} 写错了名字，后者是一次正常的空变参调用。</p>
     */
    public boolean isList(String name) {
        return this.named.get(name) instanceof Value.Many;
    }

    public double value(int index) {
        return index >= 0 && index < this.values.size() ? this.values.get(index) : 0;
    }

    /**
     * 按下标取到的单个数字；名字绑定到列表时取列表里的最大值。
     */
    public double value(String name) {
        return switch (this.named.get(name)) {
            case null -> 0;
            case Value.Single(double value) -> value;
            case Value.Many many -> many.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        };
    }

    private static List<Double> box(double... values) {
        List<Double> list = new ArrayList<>(values.length);
        for (double value : values) {
            list.add(value);
        }
        return list;
    }
}
