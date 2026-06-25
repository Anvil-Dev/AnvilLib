package dev.anvilcraft.lib.v2.rpc;

import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 一侧（caller）待响应的 {@link RPC#invoke} 调用登记表。
 *
 * <p>每次 invoke 分配一个自增 {@code callId}，登记 {@code callId -> (目标方法, future, 登记时的 tick)}；
 * 当对端返回携带相同 {@code callId} 的响应时，按其完成对应的 {@link CompletableFuture}。目标方法用于在解码
 * 响应时确定返回值编解码器。</p>
 *
 * <h2>超时</h2>
 * <p>由所属侧的 tick 事件驱动 {@link #tick()}：超过 {@link #TIMEOUT_TICKS} 个 tick 仍未收到响应的调用，
 * 其 future 以 {@link TimeoutException} 失败并从表中移除，避免泄漏。</p>
 *
 * <h2>双实例</h2>
 * <p>与 {@link RpcRegistry} 同理，服务端 / 客户端各持一个实例（{@link AnvilLibRpc#PENDING} /
 * {@code AnvilLibRpcClient.PENDING}），各由自身的 tick 事件驱动。否则在单人 / 局域网（同 JVM）下两侧的
 * {@code callId} 会相互覆盖。响应处理时按 {@code ctx.flow()} 选择实例。</p>
 */
@ApiStatus.Internal
public final class RpcPendingCalls {
    /**
     * 调用超时的 tick 数：超过该时长仍无响应即判定超时。
     */
    public static final int TIMEOUT_TICKS = 100;

    private final AtomicInteger nextId = new AtomicInteger();
    private final Map<Integer, Pending> pending = new ConcurrentHashMap<>();
    /**
     * 自增的 tick 计数器，作为登记项的时间戳基准。
     */
    private volatile int currentTick;

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
        pending.put(id, new Pending(method, future, currentTick));
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

    /**
     * 由所属侧的 tick 事件每 tick 调用一次：推进计时并使超时调用失败。
     */
    @ApiStatus.Internal
    public void tick() {
        int now = ++currentTick;
        Iterator<Map.Entry<Integer, Pending>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Pending entry = it.next().getValue();
            if (now - entry.registeredTick() < TIMEOUT_TICKS) continue;
            it.remove();
            entry.future().completeExceptionally(
                new TimeoutException("RPC call timed out after " + TIMEOUT_TICKS + " ticks: " + entry.method())
            );
        }
    }

    record Pending(Method method, CompletableFuture<Object> future, int registeredTick) {
    }
}
