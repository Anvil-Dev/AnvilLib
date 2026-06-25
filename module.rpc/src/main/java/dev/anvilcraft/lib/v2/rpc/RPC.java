package dev.anvilcraft.lib.v2.rpc;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.lang.reflect.Method;

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
    public static <A, B, C, D, E> void call(
        RpcTarget target, RpcMethodRef.R5<A, B, C, D, E> methodRef, A a, B b, C c, D d, E e
    ) {
        dispatch(target, methodRef, a, b, c, d, e);
    }

    /**
     * 调用六参方法。
     */
    public static <A, B, C, D, E, F> void call(
        RpcTarget target, RpcMethodRef.R6<A, B, C, D, E, F> methodRef, A a, B b, C c, D d, E e, F f
    ) {
        dispatch(target, methodRef, a, b, c, d, e, f);
    }

    private static void dispatch(RpcTarget target, Serializable methodRef, Object... args) {
        Method method = LambdaResolver.resolve(methodRef);
        if (method.getParameterCount() != args.length) {
            throw new IllegalArgumentException(
                "RPC method " + method + " expects " + method.getParameterCount() + " arguments, got " + args.length
            );
        }
        target.send(new RpcPayload(method, args));
    }
}
