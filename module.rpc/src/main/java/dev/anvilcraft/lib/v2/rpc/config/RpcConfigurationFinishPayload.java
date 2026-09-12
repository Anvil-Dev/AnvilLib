package dev.anvilcraft.lib.v2.rpc.config;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.anvilcraft.lib.v2.rpc.AnvilLibRpc;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.ApiStatus;

/**
 * 客户端在采纳服务端 RPC 索引映射后回复的结束包，用于标记 {@link RpcConfigurationTask} 完成。
 */
@ApiStatus.Internal
public class RpcConfigurationFinishPayload implements IServerboundPacket {
    public static final RpcConfigurationFinishPayload INSTANCE = new RpcConfigurationFinishPayload();
    public static final Type<RpcConfigurationFinishPayload> TYPE = IPacket.type(AnvilLibRpc.mod("rpc_config_finish"));
    public static final StreamCodec<ByteBuf, RpcConfigurationFinishPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public void serverHandler(IPayloadContext ctx) {
        ctx.finishCurrentTask(RpcConfigurationTask.TYPE);
    }

    @Override
    public void handleOnServer(Player player) {
    }

    @Override
    public Type<RpcConfigurationFinishPayload> type() {
        return RpcConfigurationFinishPayload.TYPE;
    }
}
