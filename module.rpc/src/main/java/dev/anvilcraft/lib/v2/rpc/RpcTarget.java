package dev.anvilcraft.lib.v2.rpc;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 一次远程调用的目标端，决定 {@link RPC#call} 产生的网络包发往何处，并提供对应发送方向的编码上下文。
 *
 * @see #player(ServerPlayer)
 * @see #server()
 */
public interface RpcTarget {
    /**
     * 目标端为指定客户端玩家（服务端 -&gt; 客户端）。
     *
     * @param player 接收远程调用的服务端玩家
     * @return 指向该玩家的目标端
     */
    static RpcTarget player(ServerPlayer player) {
        return new RpcTarget() {
            @Override
            public void send(CustomPacketPayload payload) {
                PacketDistributor.sendToPlayer(player, payload);
            }

            @Override
            public RpcRegistry registry() {
                return AnvilLibRpc.REGISTRY;
            }

            @Override
            public RegistryAccess registryAccess() {
                return player.registryAccess();
            }

            @Override
            public RpcPendingCalls pending() {
                return AnvilLibRpc.PENDING;
            }
        };
    }

    /**
     * 目标端为服务端（客户端 -&gt; 服务端）。仅可在客户端调用。
     *
     * @return 指向服务端的目标端
     */
    static RpcTarget server() {
        return ClientRpcTargets.server();
    }

    /**
     * 将网络包发送至该目标端。
     *
     * @param payload 远程调用网络包
     */
    void send(CustomPacketPayload payload);

    /**
     * 编码本次调用应使用的索引表（由发送方向决定：服务端发送用权威表，客户端发送用已采纳表）。
     *
     * @return 发送侧索引表
     */
    RpcRegistry registry();

    /**
     * 编码所用的注册表访问器，用于构造可承载注册表对象的缓冲区。
     *
     * @return 发送侧注册表访问器
     */
    RegistryAccess registryAccess();

    /**
     * 发起方（本侧）待响应调用登记表，用于 {@link RPC#invoke} 关联响应。
     *
     * @return 发送侧的 pending 表
     */
    RpcPendingCalls pending();
}
