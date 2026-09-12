package dev.anvilcraft.lib.v2.rpc;

import java.io.Serializable;

/**
 * 指向有返回值的 {@link RemoteCallable} 静态方法的可序列化方法引用接口集合，按参数个数区分。
 *
 * <p>与 {@link RpcMethodRef} 对应，但方法返回类型为 {@code R}，供 {@link RPC#invoke} 使用。</p>
 */
public final class RpcFunctionRef {
    private RpcFunctionRef() {
    }

    /**
     * 无参数、返回 {@code R} 的方法引用。
     */
    @FunctionalInterface
    public interface F0<R> extends Serializable {
        R invoke();
    }

    /**
     * 单参数、返回 {@code R} 的方法引用。
     */
    @FunctionalInterface
    public interface F1<R, A> extends Serializable {
        R invoke(A a);
    }

    /**
     * 双参数、返回 {@code R} 的方法引用。
     */
    @FunctionalInterface
    public interface F2<R, A, B> extends Serializable {
        R invoke(A a, B b);
    }

    /**
     * 三参数、返回 {@code R} 的方法引用。
     */
    @FunctionalInterface
    public interface F3<R, A, B, C> extends Serializable {
        R invoke(A a, B b, C c);
    }

    /**
     * 四参数、返回 {@code R} 的方法引用。
     */
    @FunctionalInterface
    public interface F4<R, A, B, C, D> extends Serializable {
        R invoke(A a, B b, C c, D d);
    }

    /**
     * 五参数、返回 {@code R} 的方法引用。
     */
    @FunctionalInterface
    public interface F5<R, A, B, C, D, E> extends Serializable {
        R invoke(A a, B b, C c, D d, E e);
    }

    /**
     * 六参数、返回 {@code R} 的方法引用。
     */
    @FunctionalInterface
    public interface F6<R, A, B, C, D, E, F> extends Serializable {
        R invoke(A a, B b, C c, D d, E e, F f);
    }

    /**
     * 七参数、返回 {@code R} 的方法引用。
     */
    @FunctionalInterface
    public interface F7<R, A, B, C, D, E, F, G> extends Serializable {
        R invoke(A a, B b, C c, D d, E e, F f, G g);
    }

    /**
     * 八参数、返回 {@code R} 的方法引用。
     */
    @FunctionalInterface
    public interface F8<R, A, B, C, D, E, F, G, H> extends Serializable {
        R invoke(A a, B b, C c, D d, E e, F f, G g, H h);
    }

    /**
     * 九参数、返回 {@code R} 的方法引用。
     */
    @FunctionalInterface
    public interface F9<R, A, B, C, D, E, F, G, H, I> extends Serializable {
        R invoke(A a, B b, C c, D d, E e, F f, G g, H h, I i);
    }

    /**
     * 十参数、返回 {@code R} 的方法引用。
     */
    @FunctionalInterface
    public interface F10<R, A, B, C, D, E, F, G, H, I, J> extends Serializable {
        R invoke(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j);
    }

    /**
     * 十一参数、返回 {@code R} 的方法引用。
     */
    @FunctionalInterface
    public interface F11<R, A, B, C, D, E, F, G, H, I, J, K> extends Serializable {
        R invoke(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k);
    }

    /**
     * 十二参数、返回 {@code R} 的方法引用。
     */
    @FunctionalInterface
    public interface F12<R, A, B, C, D, E, F, G, H, I, J, K, L> extends Serializable {
        R invoke(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l);
    }

    /**
     * 十三参数、返回 {@code R} 的方法引用。
     */
    @FunctionalInterface
    public interface F13<R, A, B, C, D, E, F, G, H, I, J, K, L, M> extends Serializable {
        R invoke(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m);
    }

    /**
     * 十四参数、返回 {@code R} 的方法引用。
     */
    @FunctionalInterface
    public interface F14<R, A, B, C, D, E, F, G, H, I, J, K, L, M, N> extends Serializable {
        R invoke(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n);
    }

    /**
     * 十五参数、返回 {@code R} 的方法引用。
     */
    @FunctionalInterface
    public interface F15<R, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O> extends Serializable {
        R invoke(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, O o);
    }

    /**
     * 十六参数、返回 {@code R} 的方法引用。
     */
    @FunctionalInterface
    public interface F16<R, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P> extends Serializable {
        R invoke(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n, O o, P p);
    }
}
