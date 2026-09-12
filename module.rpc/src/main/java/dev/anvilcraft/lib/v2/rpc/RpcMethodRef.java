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

    /**
     * 七参数方法引用。
     */
    @FunctionalInterface
    public interface R7<A, B, C, D, E, F, G> extends Serializable {
        void invoke(A a, B b, C c, D d, E e, F f, G g);
    }

    /**
     * 八参数方法引用。
     */
    @FunctionalInterface
    public interface R8<A, B, C, D, E, F, G, H> extends Serializable {
        void invoke(A a, B b, C c, D d, E e, F f, G g, H h);
    }

    /**
     * 九参数方法引用。
     */
    @FunctionalInterface
    public interface R9<A, B, C, D, E, F, G, H, I> extends Serializable {
        void invoke(A a, B b, C c, D d, E e, F f, G g, H h, I i);
    }

    /**
     * 十参数方法引用。
     */
    @FunctionalInterface
    public interface R10<A, B, C, D, E, F, G, H, I, J> extends Serializable {
        void invoke(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j);
    }

    /**
     * 十一参数方法引用。
     */
    @FunctionalInterface
    public interface R11<A, B, C, D, E, F, G, H, I, J, K> extends Serializable {
        void invoke(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k);
    }

    /**
     * 十二参数方法引用。
     */
    @FunctionalInterface
    public interface R12<A, B, C, D, E, F, G, H, I, J, K, L> extends Serializable {
        void invoke(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l);
    }

    /**
     * 十三参数方法引用。
     */
    @FunctionalInterface
    public interface R13<A, B, C, D, E, F, G, H, I, J, K, L, M> extends Serializable {
        void invoke(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m);
    }

    /**
     * 十四参数方法引用。
     */
    @FunctionalInterface
    public interface R14<A, B, C, D, E, F, G, H, I, J, K, L, M, N> extends Serializable {
        void invoke(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n);
    }

    /**
     * 十五参数方法引用。
     */
    @FunctionalInterface
    public interface R15<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O> extends Serializable {
        void invoke(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, O o);
    }

    /**
     * 十六参数方法引用。
     */
    @FunctionalInterface
    public interface R16<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P> extends Serializable {
        void invoke(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, O o, P p);
    }
}
