package dev.anvilcraft.lib.v2.rpc;

import java.io.Serializable;

/**
 * 指向 {@link RemoteCallable} 静态方法的可序列化方法引用接口集合，按参数个数区分。
 *
 * <p>这些接口继承 {@link Serializable}，使得 {@code Foo::bar} 形式的方法引用能被编译为可序列化 lambda，
 * 从而可通过 {@link LambdaResolver} 还原出目标方法。</p>
 */
public final class RpcMethodRef {
    private RpcMethodRef() {
    }

    /**
     * 无参数方法引用。
     */
    @FunctionalInterface
    public interface R0 extends Serializable {
        void invoke();
    }

    /**
     * 单参数方法引用。
     *
     * @param <A> 第 1 个参数类型
     */
    @FunctionalInterface
    public interface R1<A> extends Serializable {
        void invoke(A a);
    }

    /**
     * 双参数方法引用。
     *
     * @param <A> 第 1 个参数类型
     * @param <B> 第 2 个参数类型
     */
    @FunctionalInterface
    public interface R2<A, B> extends Serializable {
        void invoke(A a, B b);
    }

    /**
     * 三参数方法引用。
     */
    @FunctionalInterface
    public interface R3<A, B, C> extends Serializable {
        void invoke(A a, B b, C c);
    }

    /**
     * 四参数方法引用。
     */
    @FunctionalInterface
    public interface R4<A, B, C, D> extends Serializable {
        void invoke(A a, B b, C c, D d);
    }

    /**
     * 五参数方法引用。
     */
    @FunctionalInterface
    public interface R5<A, B, C, D, E> extends Serializable {
        void invoke(A a, B b, C c, D d, E e);
    }

    /**
     * 六参数方法引用。
     */
    @FunctionalInterface
    public interface R6<A, B, C, D, E, F> extends Serializable {
        void invoke(A a, B b, C c, D d, E e, F f);
    }
}
