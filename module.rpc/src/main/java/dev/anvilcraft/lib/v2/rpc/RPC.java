package dev.anvilcraft.lib.v2.rpc;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/**
 * 远程过程调用入口。
 *
 * <p>将一个静态方法标注为 {@link RemoteCallable}，即可通过
 * {@link #call(RpcTarget, RpcMethodRef.R1, Object) RPC.call(target, Type::method, args...)}
 * 在目标端执行该方法。</p>
 *
 * <p>方法参数默认支持 {@link net.minecraft.network.codec.ByteBufCodecs ByteBufCodecs} 中提供的类型
 * （如 {@code int}、{@code String}、{@code byte[]} 等），这些参数无需额外标注；其余类型需通过
 * {@link CallableParam} 指定对应的 {@link net.minecraft.network.codec.StreamCodec StreamCodec}。</p>
 *
 * <h2>示例</h2>
 * <pre>{@code
 * public final class Greetings {
 *     @RemoteCallable
 *     public static void hello(String name, int times) {
 *         for (int i = 0; i < times; i++) System.out.println("Hello " + name);
 *     }
 * }
 *
 * // 服务端令某个客户端执行 Greetings.hello("world", 3)
 * RPC.call(RpcTarget.player(serverPlayer), Greetings::hello, "world", 3);
 * }</pre>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RPC {
    /**
     * 调用无参方法。
     *
     * @param target    目标端
     * @param methodRef 指向 {@link RemoteCallable} 静态方法的方法引用
     */
    public static void call(RpcTarget target, RpcMethodRef.R0 methodRef) {
        dispatch(target, methodRef);
    }

    /**
     * 调用单参方法。
     *
     * @param target    目标端
     * @param methodRef 指向 {@link RemoteCallable} 静态方法的方法引用
     * @param a         第 1 个参数
     * @param <A>       第 1 个参数类型
     */
    public static <A> void call(RpcTarget target, RpcMethodRef.R1<A> methodRef, A a) {
        dispatch(target, methodRef, a);
    }

    /**
     * 调用双参方法。
     */
    public static <A, B> void call(RpcTarget target, RpcMethodRef.R2<A, B> methodRef, A a, B b) {
        dispatch(target, methodRef, a, b);
    }

    /**
     * 调用三参方法。
     */
    public static <A, B, C> void call(RpcTarget target, RpcMethodRef.R3<A, B, C> methodRef, A a, B b, C c) {
        dispatch(target, methodRef, a, b, c);
    }

    /**
     * 调用四参方法。
     */
    public static <A, B, C, D> void call(RpcTarget target, RpcMethodRef.R4<A, B, C, D> methodRef, A a, B b, C c, D d) {
        dispatch(target, methodRef, a, b, c, d);
    }

    /**
     * 调用五参方法。
     */
    public static <A, B, C, D, E> void call(RpcTarget target, RpcMethodRef.R5<A, B, C, D, E> methodRef, A a, B b, C c, D d, E e) {
        dispatch(target, methodRef, a, b, c, d, e);
    }

    /**
     * 调用六参方法。
     */
    public static <A, B, C, D, E, F> void call(
        RpcTarget target,
        RpcMethodRef.R6<A, B, C, D, E, F> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f
    ) {
        dispatch(target, methodRef, a, b, c, d, e, f);
    }

    /**
     * 调用七参方法。
     */
    public static <A, B, C, D, E, F, G> void call(
        RpcTarget target,
        RpcMethodRef.R7<A, B, C, D, E, F, G> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f,
        G g
    ) {
        dispatch(target, methodRef, a, b, c, d, e, f, g);
    }

    /**
     * 调用八参方法。
     */
    public static <A, B, C, D, E, F, G, H> void call(
        RpcTarget target,
        RpcMethodRef.R8<A, B, C, D, E, F, G, H> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f,
        G g,
        H h
    ) {
        dispatch(target, methodRef, a, b, c, d, e, f, g, h);
    }

    /**
     * 调用九参方法。
     */
    public static <A, B, C, D, E, F, G, H, I> void call(
        RpcTarget target,
        RpcMethodRef.R9<A, B, C, D, E, F, G, H, I> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f,
        G g,
        H h,
        I i
    ) {
        dispatch(target, methodRef, a, b, c, d, e, f, g, h, i);
    }

    /**
     * 调用十参方法。
     */
    public static <A, B, C, D, E, F, G, H, I, J> void call(
        RpcTarget target,
        RpcMethodRef.R10<A, B, C, D, E, F, G, H, I, J> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f,
        G g,
        H h,
        I i,
        J j
    ) {
        dispatch(target, methodRef, a, b, c, d, e, f, g, h, i, j);
    }

    /**
     * 调用十一参方法。
     */
    public static <A, B, C, D, E, F, G, H, I, J, K> void call(
        RpcTarget target,
        RpcMethodRef.R11<A, B, C, D, E, F, G, H, I, J, K> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f,
        G g,
        H h,
        I i,
        J j,
        K k
    ) {
        dispatch(target, methodRef, a, b, c, d, e, f, g, h, i, j, k);
    }

    /**
     * 调用十二参方法。
     */
    public static <A, B, C, D, E, F, G, H, I, J, K, L> void call(
        RpcTarget target,
        RpcMethodRef.R12<A, B, C, D, E, F, G, H, I, J, K, L> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f,
        G g,
        H h,
        I i,
        J j,
        K k,
        L l
    ) {
        dispatch(target, methodRef, a, b, c, d, e, f, g, h, i, j, k, l);
    }

    /**
     * 调用十三参方法。
     */
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M> void call(
        RpcTarget target,
        RpcMethodRef.R13<A, B, C, D, E, F, G, H, I, J, K, L, M> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f,
        G g,
        H h,
        I i,
        J j,
        K k,
        L l,
        M m
    ) {
        dispatch(target, methodRef, a, b, c, d, e, f, g, h, i, j, k, l, m);
    }

    /**
     * 调用十四参方法。
     */
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N> void call(
        RpcTarget target,
        RpcMethodRef.R14<A, B, C, D, E, F, G, H, I, J, K, L, M, N> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f,
        G g,
        H h,
        I i,
        J j,
        K k,
        L l,
        M m,
        N n
    ) {
        dispatch(target, methodRef, a, b, c, d, e, f, g, h, i, j, k, l, m, n);
    }

    /**
     * 调用十五参方法。
     */
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O> void call(
        RpcTarget target,
        RpcMethodRef.R15<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f,
        G g,
        H h,
        I i,
        J j,
        K k,
        L l,
        M m,
        N n,
        O o
    ) {
        dispatch(target, methodRef, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o);
    }

    /**
     * 调用十六参方法。
     */
    public static <A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P> void call(
        RpcTarget target,
        RpcMethodRef.R16<A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f,
        G g,
        H h,
        I i,
        J j,
        K k,
        L l,
        M m,
        N n,
        O o,
        P p
    ) {
        dispatch(target, methodRef, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p);
    }

    /**
     * 逃生口：按类与方法名调用 {@link RemoteCallable} 方法，参数个数不限。
     *
     * <p>当参数个数超过 {@link #call} 提供的 16 个档次，或无法使用方法引用时使用。代价是失去对实参的
     * 编译期类型检查：调用者需自行保证 {@code args} 的类型与个数同目标方法一致。若该方法名存在重载，
     * 将因歧义而抛出异常，此时应改用方法引用形式的 {@link #call}。</p>
     *
     * @param target     目标端
     * @param clazz      目标方法所属类
     * @param methodName 目标方法名（须唯一且为 {@link RemoteCallable} 静态方法）
     * @param args       实参
     */
    public static void callByName(RpcTarget target, Class<?> clazz, String methodName, Object... args) {
        Method method = RpcMethods.resolveByName(clazz, methodName);
        sendChecked(target, method, args);
    }

    /**
     * 发起无参、有返回值的远程调用。
     *
     * @param target    目标端
     * @param methodRef 指向 {@link RemoteCallable} 静态方法的方法引用
     * @param <R>       返回类型
     * @return 在收到对端响应时兑现的 future
     */
    public static <R> CompletableFuture<R> invoke(RpcTarget target, RpcFunctionRef.F0<R> methodRef) {
        return invokeDispatch(target, methodRef);
    }

    /**
     * 发起单参、有返回值的远程调用。
     */
    public static <R, A> CompletableFuture<R> invoke(RpcTarget target, RpcFunctionRef.F1<R, A> methodRef, A a) {
        return invokeDispatch(target, methodRef, a);
    }

    /**
     * 发起双参、有返回值的远程调用。
     */
    public static <R, A, B> CompletableFuture<R> invoke(RpcTarget target, RpcFunctionRef.F2<R, A, B> methodRef, A a, B b) {
        return invokeDispatch(target, methodRef, a, b);
    }

    /**
     * 发起三参、有返回值的远程调用。
     */
    public static <R, A, B, C> CompletableFuture<R> invoke(RpcTarget target, RpcFunctionRef.F3<R, A, B, C> methodRef, A a, B b, C c) {
        return invokeDispatch(target, methodRef, a, b, c);
    }

    /**
     * 发起四参、有返回值的远程调用。
     */
    public static <R, A, B, C, D> CompletableFuture<R> invoke(
        RpcTarget target,
        RpcFunctionRef.F4<R, A, B, C, D> methodRef,
        A a,
        B b,
        C c,
        D d
    ) {
        return invokeDispatch(target, methodRef, a, b, c, d);
    }

    /**
     * 发起五参、有返回值的远程调用。
     */
    public static <R, A, B, C, D, E> CompletableFuture<R> invoke(
        RpcTarget target,
        RpcFunctionRef.F5<R, A, B, C, D, E> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e
    ) {
        return invokeDispatch(target, methodRef, a, b, c, d, e);
    }

    /**
     * 发起六参、有返回值的远程调用。
     */
    public static <R, A, B, C, D, E, F> CompletableFuture<R> invoke(
        RpcTarget target,
        RpcFunctionRef.F6<R, A, B, C, D, E, F> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f
    ) {
        return invokeDispatch(target, methodRef, a, b, c, d, e, f);
    }

    /**
     * 发起七参、有返回值的远程调用。
     */
    public static <R, A, B, C, D, E, F, G> CompletableFuture<R> invoke(
        RpcTarget target,
        RpcFunctionRef.F7<R, A, B, C, D, E, F, G> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f,
        G g
    ) {
        return invokeDispatch(target, methodRef, a, b, c, d, e, f, g);
    }

    /**
     * 发起八参、有返回值的远程调用。
     */
    public static <R, A, B, C, D, E, F, G, H> CompletableFuture<R> invoke(
        RpcTarget target,
        RpcFunctionRef.F8<R, A, B, C, D, E, F, G, H> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f,
        G g,
        H h
    ) {
        return invokeDispatch(target, methodRef, a, b, c, d, e, f, g, h);
    }

    /**
     * 发起九参、有返回值的远程调用。
     */
    public static <R, A, B, C, D, E, F, G, H, I> CompletableFuture<R> invoke(
        RpcTarget target,
        RpcFunctionRef.F9<R, A, B, C, D, E, F, G, H, I> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f,
        G g,
        H h,
        I i
    ) {
        return invokeDispatch(target, methodRef, a, b, c, d, e, f, g, h, i);
    }

    /**
     * 发起十参、有返回值的远程调用。
     */
    public static <R, A, B, C, D, E, F, G, H, I, J> CompletableFuture<R> invoke(
        RpcTarget target,
        RpcFunctionRef.F10<R, A, B, C, D, E, F, G, H, I, J> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f,
        G g,
        H h,
        I i,
        J j
    ) {
        return invokeDispatch(target, methodRef, a, b, c, d, e, f, g, h, i, j);
    }

    /**
     * 发起十一参、有返回值的远程调用。
     */
    public static <R, A, B, C, D, E, F, G, H, I, J, K> CompletableFuture<R> invoke(
        RpcTarget target,
        RpcFunctionRef.F11<R, A, B, C, D, E, F, G, H, I, J, K> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f,
        G g,
        H h,
        I i,
        J j,
        K k
    ) {
        return invokeDispatch(target, methodRef, a, b, c, d, e, f, g, h, i, j, k);
    }

    /**
     * 发起十二参、有返回值的远程调用。
     */
    public static <R, A, B, C, D, E, F, G, H, I, J, K, L> CompletableFuture<R> invoke(
        RpcTarget target,
        RpcFunctionRef.F12<R, A, B, C, D, E, F, G, H, I, J, K, L> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f,
        G g,
        H h,
        I i,
        J j,
        K k,
        L l
    ) {
        return invokeDispatch(target, methodRef, a, b, c, d, e, f, g, h, i, j, k, l);
    }

    /**
     * 发起十三参、有返回值的远程调用。
     */
    public static <R, A, B, C, D, E, F, G, H, I, J, K, L, M> CompletableFuture<R> invoke(
        RpcTarget target,
        RpcFunctionRef.F13<R, A, B, C, D, E, F, G, H, I, J, K, L, M> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f,
        G g,
        H h,
        I i,
        J j,
        K k,
        L l,
        M m
    ) {
        return invokeDispatch(target, methodRef, a, b, c, d, e, f, g, h, i, j, k, l, m);
    }

    /**
     * 发起十四参、有返回值的远程调用。
     */
    public static <R, A, B, C, D, E, F, G, H, I, J, K, L, M, N> CompletableFuture<R> invoke(
        RpcTarget target,
        RpcFunctionRef.F14<R, A, B, C, D, E, F, G, H, I, J, K, L, M, N> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f,
        G g,
        H h,
        I i,
        J j,
        K k,
        L l,
        M m,
        N n
    ) {
        return invokeDispatch(target, methodRef, a, b, c, d, e, f, g, h, i, j, k, l, m, n);
    }

    /**
     * 发起十五参、有返回值的远程调用。
     */
    public static <R, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O> CompletableFuture<R> invoke(
        RpcTarget target,
        RpcFunctionRef.F15<R, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f,
        G g,
        H h,
        I i,
        J j,
        K k,
        L l,
        M m,
        N n,
        O o
    ) {
        return invokeDispatch(target, methodRef, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o);
    }

    /**
     * 发起十六参、有返回值的远程调用。
     */
    public static <R, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P> CompletableFuture<R> invoke(
        RpcTarget target,
        RpcFunctionRef.F16<R, A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P> methodRef,
        A a,
        B b,
        C c,
        D d,
        E e,
        F f,
        G g,
        H h,
        I i,
        J j,
        K k,
        L l,
        M m,
        N n,
        O o,
        P p
    ) {
        return invokeDispatch(target, methodRef, a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p);
    }

    /**
     * 逃生口：按类与方法名发起有返回值的远程调用，参数个数不限。
     *
     * <p>与 {@link #callByName} 同理，失去对实参的编译期类型检查；返回类型由调用者通过 {@code <R>} 指定，
     * 运行时若与方法实际返回类型不符将导致解码或转型失败。</p>
     *
     * @param target     目标端
     * @param clazz      目标方法所属类
     * @param methodName 目标方法名（须唯一且为 {@link RemoteCallable} 静态方法）
     * @param args       实参
     * @param <R>        返回类型
     * @return 在收到对端响应时兑现的 future
     */
    public static <R> CompletableFuture<R> invokeByName(RpcTarget target, Class<?> clazz, String methodName, Object... args) {
        Method method = RpcMethods.resolveByName(clazz, methodName);
        return invokeChecked(target, method, args);
    }

    private static void dispatch(RpcTarget target, Serializable methodRef, Object... args) {
        Method method = LambdaResolver.resolve(methodRef);
        sendChecked(target, method, args);
    }

    private static void sendChecked(RpcTarget target, Method method, Object[] args) {
        if (method.getParameterCount() != args.length) {
            throw new IllegalArgumentException("RPC method " + method + " expects " + method.getParameterCount() + " arguments, got " + args.length);
        }
        target.send(RpcPayload.encode(target.registry(), target.registryAccess(), method, args));
    }

    private static <R> CompletableFuture<R> invokeDispatch(RpcTarget target, Serializable methodRef, Object... args) {
        return invokeChecked(target, LambdaResolver.resolve(methodRef), args);
    }

    private static <R> CompletableFuture<R> invokeChecked(RpcTarget target, Method method, Object[] args) {
        if (method.getReturnType() == void.class) {
            throw new IllegalArgumentException("RPC method " + method + " returns void; use RPC.call instead");
        }
        if (method.getParameterCount() != args.length) {
            throw new IllegalArgumentException("RPC method " + method + " expects " + method.getParameterCount() + " arguments, got " + args.length);
        }
        CompletableFuture<Object> future = new CompletableFuture<>();
        int callId = target.pending().register(method, future);
        target.send(RpcRequestPayload.encode(target.registry(), target.registryAccess(), callId, method, args));
        @SuppressWarnings("unchecked") CompletableFuture<R> typed = (CompletableFuture<R>) future;
        return typed;
    }
}
