package dev.anvilcraft.lib.v2.util;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import javax.annotation.Nullable;

import java.util.Optional;
import java.util.function.Consumer;

public abstract class Util {
    /**
     * 当前环境是否为客户端
     *
     * @return 是否为客户端
     */
    public static boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    /**
     * 将传入的值强转为{@code T}类型
     *
     * @param <T> 想要转为的类型
     * @param o   一个值
     * @return 传入的值，但是类型为{@code T}
     * @throws ClassCastException 当无法将传入的值强转时抛出
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(Object o) {
        return (T) o;
    }

    /**
     * 若传入的值可被强转为{@code T}类型，则返回包含传入的值的{@link Optional}
     *
     * @param <T> 想要转为的类型
     * @param o   一个值，可为null
     * @return 一个可能包含传入的值的{@link Optional}
     */
    public static <T> Optional<T> castSafely(@Nullable Object o, Class<T> clazz) {
        return Optional.ofNullable(o)
            .filter(clazz::isInstance)
            .map(Util::cast);
    }

    /**
     * 若传入的值可被强转为传入的任意类型，则返回true
     *
     * @param o 一个值，可为null
     * @return 传入的值，但是类型为{@code T}
     */
    @SuppressWarnings("TypeParameterExplicitlyExtendsObject")
    @SafeVarargs
    public static boolean instanceOfAny(@Nullable Object o, Class<? extends Object>... classes) {
        Optional<Object> op = Optional.empty();
        for (Class<?> clazz : classes) {
            op = op.or(() -> Util.castSafely(o, clazz));
        }
        return op.isPresent();
    }

    /**
     * 若传入的值可被强转为{@code T}类型，则使用传入的值执行传入的方法<br>
     * 等效于{@code Util.castSafely(o, clazz).ifPresent(action);}
     *
     * @param <T>    想要转为的类型
     * @param o      一个值，可为null
     * @param action 将要执行的操作
     */
    public static <T> void ifCastable(@Nullable Object o, Class<T> clazz, Consumer<T> action) {
        Optional.ofNullable(o)
            .filter(clazz::isInstance)
            .<T>map(Util::cast)
            .ifPresent(action);
    }

    /**
     * 使用传入的参数运行代码，并返回原参数
     *
     * @param value 原参数
     * @param consumer 需要在传入前调用的方法
     * @param <T> 原参数的类型
     * @return 原参数
     */
    public static <T> T run(T value, Consumer<T> consumer) {
        consumer.accept(value);
        return value;
    }

    /**
     * 抛出一个异常
     *
     * @param throwable 需要抛出的异常
     * @return 无，用于欺骗 IDE
     * @param <T> （并不会）返回的值的类型，用于欺骗 IDE
     * @param <E> 抛出的异常的类型
     * @throws E 抛出的异常的类型
     */
    public static <T, E extends Throwable> T throwE(E throwable) throws E {
        throw throwable;
    }
}
