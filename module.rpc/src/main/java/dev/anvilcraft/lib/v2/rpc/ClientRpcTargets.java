package dev.anvilcraft.lib.v2.rpc;

import dev.anvilcraft.lib.v2.rpc.client.AnvilLibRpcClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.Objects;

/**
 * {@link RpcTarget#server()} 的客户端侧实现。
 *
 * <p>独立成类以便其引用的客户端专用类（{@link Minecraft} 等）仅在客户端被加载，
 * 专用服务器不会在链接期触及。</p>
 */
final class ClientRpcTargets {
    private ClientRpcTargets() {
    }

    static RpcTarget server() {
        return new RpcTarget() {
            @Override
            public void send(CustomPacketPayload payload) {
                ClientPacketDistributor.sendToServer(payload);
            }

            @Override
            public RpcRegistry registry() {
                return AnvilLibRpcClient.REGISTRY;
            }

            @Override
            public RegistryAccess registryAccess() {
                ClientPacketListener connection = Objects.requireNonNull(
                    Minecraft.getInstance().getConnection(), "Not connected to a server"
                );
                return connection.registryAccess();
            }

            @Override
            public RpcPendingCalls pending() {
                return AnvilLibRpcClient.PENDING;
            }
        };
    }
}
