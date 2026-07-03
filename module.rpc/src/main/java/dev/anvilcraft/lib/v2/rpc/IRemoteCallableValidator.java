package dev.anvilcraft.lib.v2.rpc;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.Method;

/**
 * {@link RemoteCallable} 方法的接收端校验器：在目标方法执行<em>之前</em>判定本次远程调用是否被允许。
 *
 * <p>校验在接收端（实际执行方法的一侧）进行，可据 {@link IPayloadContext}（发起方玩家、连接、方向等）
 * 与实参决定是否放行，用于实现「仅 OP 可调用」「目标坐标须在范围内」等安全策略。</p>
 *
 * <p>实现类须提供一个<b>无参构造器</b>（可为非 public），框架会反射实例化并缓存。
 * 不指定校验器时（注解 {@code validator} 取默认值），等价于始终放行。</p>
 *
 * @see RemoteCallable#validator()
 */
@FunctionalInterface
public interface IRemoteCallableValidator {
    /**
     * 判定一次远程调用是否被允许执行。
     *
     * @param ctx    网络包上下文（含发起方玩家、方向等）
     * @param method 即将执行的目标方法
     * @param args   已解码的实参
     * @return {@code true} 放行并执行；{@code false} 拒绝（{@link RPC#call} 静默丢弃，
     * {@link RPC#invoke} 使调用方 future 以异常失败）
     */
    boolean validate(IPayloadContext ctx, Method method, Object[] args);
}
