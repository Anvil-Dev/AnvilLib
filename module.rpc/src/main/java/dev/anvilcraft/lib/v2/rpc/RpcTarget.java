package dev.anvilcraft.lib.v2.rpc;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 一次远程调用的目标端，决定 {@link RPC#call} 产生的网络包发往何处。
 *
 * @see #player(ServerPlayer)
 * @see #server()
 */
@FunctionalInterface
public interface RpcTarget {
    /**
     * 将网络包发送至该目标端。
     *
     * @param payload 远程调用网络包
     */
    void send(CustomPacketPayload payload);

    /**
     * 目标端为指定客户端玩家（服务端 -&gt; 客户端）。
     *
     * @param player 接收远程调用的服务端玩家
     * @return 指向该玩家的目标端
     */
    static RpcTarget player(ServerPlayer player) {
        return payload -> PacketDistributor.sendToPlayer(player, payload);
    }

    /**
     * 目标端为服务端（客户端 -&gt; 服务端）。
     *
     * <p>仅可在客户端调用。</p>
     *
     * @return 指向服务端的目标端
     */
    static RpcTarget server() {
        return net.neoforged.neoforge.client.network.ClientPacketDistributor::sendToServer;
    }
}
