package dev.anvilcraft.lib.v2.rpc;

import dev.anvilcraft.lib.v2.network.packet.IInsensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.rpc.client.AnvilLibRpcClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 有返回值远程调用的请求包：携带 {@code callId}、方法索引与实参。接收端执行方法后，回复一个携带相同
 * {@code callId} 的 {@link RpcResponsePayload}。
 *
 * @see RPC#invoke
 */
public class RpcRequestPayload implements IRpcPayload, IInsensitiveBiPacket {
    public static final Type<RpcRequestPayload> TYPE = IPacket.type(AnvilLibRpc.of("rpc_request"));
    public static final StreamCodec<ByteBuf, RpcRequestPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BYTE_ARRAY,
        RpcRequestPayload::data,
        RpcRequestPayload::new
    );

    private final byte[] data;

    RpcRequestPayload(byte[] data) {
        this.data = data;
    }

    /**
     * 在发送侧编码请求。
     *
     * @param registry       发送侧索引表
     * @param registryAccess 发送侧注册表访问器
     * @param callId         调用 id
     * @param method         目标方法
     * @param args           实参
     * @return 编码后的请求包
     */
    static RpcRequestPayload encode(
        RpcRegistry registry, RegistryAccess registryAccess, int callId, Method method, Object[] args
    ) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess, ConnectionType.NEOFORGE);
        buf.writeVarInt(callId);
        byte[] data = IRpcPayload.encodeParams(registry, method, args, buf);
        return new RpcRequestPayload(data);
    }

    byte[] data() {
        return this.data;
    }

    @Override
    public void bidirectionalHandler(IPayloadContext ctx) {
        ctx.enqueueWork(() -> this.handle(ctx));
    }

    @Override
    public void handleOnBothSide(Player player) {
        // 实际处理在 handle(ctx) 中完成
    }

    private void handle(IPayloadContext ctx) {
        // 收到 clientbound 请求表示本侧为客户端，使用已采纳的客户端索引表；否则为服务端权威表
        boolean onClient = ctx.flow().isClientbound();
        RpcRegistry registry = onClient ? AnvilLibRpcClient.REGISTRY : AnvilLibRpc.REGISTRY;
        RegistryAccess registryAccess = ctx.player().registryAccess();
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
            Unpooled.wrappedBuffer(this.data), registryAccess, ConnectionType.NEOFORGE
        );
        int callId = buf.readVarInt();
        Method method = registry.byIndex(buf.readVarInt());
        Object[] args = IRpcPayload.decodeParams(method, buf);
        if (!RpcMethods.validate(method, ctx, args)) {
            ctx.reply(RpcResponsePayload.failure(callId, "RPC call rejected by validator: " + method));
            return;
        }

        Object result;
        try {
            result = method.invoke(null, args);
        } catch (IllegalAccessException e) {
            ctx.reply(RpcResponsePayload.failure(callId, "Cannot invoke RPC method " + method));
            throw new IllegalStateException("Cannot invoke RPC method " + method, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            ctx.reply(RpcResponsePayload.failure(callId, String.valueOf(cause)));
            throw new RuntimeException("RPC method " + method + " threw an exception", cause);
        }
        ctx.reply(RpcResponsePayload.success(registry, registryAccess, callId, method, result));
    }

    @Override
    public CustomPacketPayload.Type<RpcRequestPayload> type() {
        return RpcRequestPayload.TYPE;
    }
}
