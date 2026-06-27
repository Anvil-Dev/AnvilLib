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

import java.lang.reflect.Method;
import java.util.concurrent.CompletionException;

/**
 * 有返回值远程调用的响应包：携带请求的 {@code callId}、成功标志，以及编码后的返回值或错误信息。
 *
 * <p>caller 侧据 {@code callId} 在 {@link RpcPendingCalls} 中找到对应 future 与目标方法，用方法的返回值
 * 编解码器（{@link RpcMethods#returnCodec}）解码返回值并完成 future。</p>
 *
 * @see RPC#invoke
 */
public class RpcResponsePayload implements IInsensitiveBiPacket {
    public static final Type<RpcResponsePayload> TYPE = IPacket.type(AnvilLibRpc.mod("rpc_response"));
    public static final StreamCodec<ByteBuf, RpcResponsePayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BYTE_ARRAY,
        RpcResponsePayload::data,
        RpcResponsePayload::new
    );

    private final byte[] data;

    RpcResponsePayload(byte[] data) {
        this.data = data;
    }

    /**
     * 编码一个成功响应。
     */
    static RpcResponsePayload success(RpcRegistry registry, RegistryAccess registryAccess, int callId, Method method, Object result) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess, ConnectionType.NEOFORGE);
        buf.writeVarInt(callId);
        buf.writeBoolean(true);
        RpcMethods.returnCodec(method).encode(buf, result);
        return read(buf);
    }

    /**
     * 编码一个失败响应，仅携带错误信息文本。
     */
    static RpcResponsePayload failure(int callId, String message) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.NEOFORGE);
        buf.writeVarInt(callId);
        buf.writeBoolean(false);
        buf.writeUtf(message);
        return read(buf);
    }

    private static RpcResponsePayload read(RegistryFriendlyByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return new RpcResponsePayload(data);
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
        // 收到 clientbound 响应表示本侧为发起 invoke 的客户端
        RpcPendingCalls pending = ctx.flow().isClientbound() ? AnvilLibRpcClient.PENDING : AnvilLibRpc.PENDING;
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
            Unpooled.wrappedBuffer(this.data),
            ctx.player().registryAccess(),
            ConnectionType.NEOFORGE
        );
        int callId = buf.readVarInt();
        RpcPendingCalls.Pending entry = pending.remove(callId);
        if (entry == null) {
            // 调用方未登记该 callId（可能已超时移除），忽略
            return;
        }
        boolean ok = buf.readBoolean();
        if (!ok) {
            entry.future().completeExceptionally(new CompletionException("Remote RPC failed: " + buf.readUtf(), null));
            return;
        }
        Object result = RpcMethods.returnCodec(entry.method()).decode(buf);
        entry.future().complete(result);
    }

    @Override
    public CustomPacketPayload.Type<RpcResponsePayload> type() {
        return RpcResponsePayload.TYPE;
    }
}
