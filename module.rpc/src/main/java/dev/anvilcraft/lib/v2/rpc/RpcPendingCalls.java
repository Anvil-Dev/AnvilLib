package dev.anvilcraft.lib.v2.rpc;

import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 一侧（caller）待响应的 {@link RPC#invoke} 调用登记表。
 *
 * <p>每次 invoke 分配一个自增 {@code callId}，登记 {@code callId -> (目标方法, future)}；当对端返回
 * 携带相同 {@code callId} 的响应时，按其完成对应的 {@link CompletableFuture}。目标方法用于在解码响应时
 * 确定返回值编解码器。</p>
 *
 * <h2>双实例</h2>
 * <p>与 {@link RpcRegistry} 同理，服务端 / 客户端各持一个实例（{@link AnvilLibRpc#PENDING} /
 * {@code AnvilLibRpcClient.PENDING}）。否则在单人 / 局域网（同 JVM）下两侧的 {@code callId} 会相互覆盖。
 * 响应处理时按 {@code ctx.flow()} 选择实例：收到 clientbound 响应表示本侧为发起调用的客户端。</p>
 */
@ApiStatus.Internal
public final class RpcPendingCalls {
    private final AtomicInteger nextId = new AtomicInteger();
    private final Map<Integer, Pending> pending = new ConcurrentHashMap<>();

    /**
     * 构造一个空的登记表。
     */
    public RpcPendingCalls() {
    }

    /**
     * 登记一次待响应调用。
     *
     * @param method 目标方法
     * @param future 调用完成时要兑现的 future
     * @return 分配的 callId
     */
    int register(Method method, CompletableFuture<Object> future) {
        int id = nextId.getAndIncrement();
        pending.put(id, new Pending(method, future));
        return id;
    }

    /**
     * 取出（并移除）指定 callId 的登记项。
     *
     * @param callId 调用 id
     * @return 登记项；若不存在（如超时已移除）返回 {@code null}
     */
    @org.jspecify.annotations.Nullable
    Pending remove(int callId) {
        return pending.remove(callId);
    }

    record Pending(Method method, CompletableFuture<Object> future) {
    }
}
