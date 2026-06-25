package dev.anvilcraft.lib.v2.rpc.config;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.rpc.AnvilLibRpc;
import dev.anvilcraft.lib.v2.rpc.client.AnvilLibRpcClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration 阶段由服务端下发给客户端的 RPC 索引映射包。
 *
 * <p>客户端收到后用其覆盖本地索引表（{@link dev.anvilcraft.lib.v2.rpc.RpcRegistry#adopt(Map)}），从而与服务端共享同一套
 * {@code 索引 -> 规范键} 映射，保证双向 RPC 的索引一致；随后回复
 * {@link RpcConfigurationFinishPayload} 以结束该配置任务。</p>
 */
public record RpcConfigurationPayload(Map<Integer, String> indexMap) implements IClientboundPacket {
    public static final Type<RpcConfigurationPayload> TYPE = IPacket.type(AnvilLibRpc.of("rpc_config"));
    public static final StreamCodec<ByteBuf, RpcConfigurationPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, ByteBufCodecs.STRING_UTF8),
        RpcConfigurationPayload::indexMap,
        RpcConfigurationPayload::new
    );

    @Override
    public void clientHandler(IPayloadContext ctx) {
        ctx.enqueueWork(() -> AnvilLibRpcClient.REGISTRY.adopt(this.indexMap()));
        ctx.reply(RpcConfigurationFinishPayload.INSTANCE);
    }

    @Override
    public void handleOnClient(Player player) {
    }

    @Override
    public Type<RpcConfigurationPayload> type() {
        return RpcConfigurationPayload.TYPE;
    }
}
