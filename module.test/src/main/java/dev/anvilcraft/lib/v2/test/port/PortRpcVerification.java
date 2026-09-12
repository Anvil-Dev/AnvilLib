package dev.anvilcraft.lib.v2.test.port;

import dev.anvilcraft.lib.v2.rpc.IRpcPayload;
import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RemoteCallable;
import dev.anvilcraft.lib.v2.rpc.RpcPendingCalls;
import dev.anvilcraft.lib.v2.rpc.RpcRegistry;
import dev.anvilcraft.lib.v2.rpc.RpcRequestPayload;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** 本地回环请求仍执行真实参数编码，验证同步调用的完成与失败传播。 */
public final class PortRpcVerification {
    @RemoteCallable
    public static int sum(int left, int right) { return left + right; }

    public static void run() throws Exception {
        for (int mode = 0; mode < 4; mode++) {
            Loopback target = new Loopback(mode);
            CompletableFuture<Integer> result = new CompletableFuture<>();
            Thread.startVirtualThread(() -> {
                try {
                    result.complete(RPC.invokeSync(target, PortRpcVerification::sum, 10, 32));
                } catch (Throwable error) {
                    result.completeExceptionally(error);
                }
            });
            if (mode == 0) {
                PortVerification.check(result.get(5, TimeUnit.SECONDS) == 42, "RPC 虚拟线程同步返回值");
                PortVerification.check(RPC.<Integer>invokeSyncByName(target, PortRpcVerification.class, "sum", 3, 4) == 7,
                    "RPC 按名称同步调用");
                PortVerification.check(RPC.invokeSync(target, true, PortRpcVerification::sum, 4, 5) == 9,
                    "RPC 显式平台线程同步调用");
            } else {
                try {
                    result.get(5, TimeUnit.SECONDS);
                    throw new AssertionError("失败 RPC 意外成功");
                } catch (java.util.concurrent.ExecutionException | java.util.concurrent.CancellationException error) {
                    Throwable cause = error;
                    while (cause.getCause() != null) cause = cause.getCause();
                    Class<?> expected = mode == 1 ? IllegalStateException.class
                        : mode == 2 ? java.util.concurrent.TimeoutException.class : java.util.concurrent.CancellationException.class;
                    PortVerification.check(expected.isInstance(cause), "RPC 异常/超时/断连传播: " + cause);
                }
            }
        }
    }

    private static final class Loopback implements RpcTarget {
        private final RpcRegistry registry = new RpcRegistry(false);
        private final RpcPendingCalls pending = new RpcPendingCalls();
        private final int mode;

        private Loopback(int mode) {
            this.mode = mode;
            registry.adopt(Map.of(0, PortRpcVerification.class.getName() + "#sum(II)I"));
        }

        @Override
        public void send(CustomPacketPayload payload) {
            try {
                sendRequest((RpcRequestPayload) payload);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException(error);
            }
        }

        private void sendRequest(RpcRequestPayload payload) throws ReflectiveOperationException {
            var data = RpcRequestPayload.class.getDeclaredMethod("data");
            data.setAccessible(true);
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.wrappedBuffer((byte[]) data.invoke(payload)), registryAccess(), ConnectionType.NEOFORGE);
            try {
                int callId = buffer.readVarInt();
                var method = registry.byIndex(buffer.readVarInt());
                Object[] arguments = IRpcPayload.decodeParams(method, buffer);
                if (mode == 2) {
                    for (int tick = 0; tick < RpcPendingCalls.TIMEOUT_TICKS; tick++) pending.tick();
                } else if (mode == 3) {
                    pending.clear();
                } else {
                    var remove = RpcPendingCalls.class.getDeclaredMethod("remove", int.class);
                    remove.setAccessible(true);
                    Object call = remove.invoke(pending, callId);
                    var future = call.getClass().getDeclaredMethod("future");
                    future.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    CompletableFuture<Object> completion = (CompletableFuture<Object>) future.invoke(call);
                    if (mode == 1) completion.completeExceptionally(new IllegalStateException("remote failure"));
                    else completion.complete(sum((int) arguments[0], (int) arguments[1]));
                }
            } finally {
                buffer.release();
            }
        }

        @Override public RpcRegistry registry() { return registry; }
        @Override public RpcPendingCalls pending() { return pending; }
        @Override public RegistryAccess registryAccess() { return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY); }
    }
}
