package dev.anvilcraft.lib.v2.rpc.client;

import dev.anvilcraft.lib.v2.rpc.AnvilLibRpc;
import dev.anvilcraft.lib.v2.rpc.RpcPendingCalls;
import dev.anvilcraft.lib.v2.rpc.RpcRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/**
 * 客户端侧入口，持有仅采纳服务端下发映射的客户端索引表。
 *
 * <p>与权威实例 {@link AnvilLibRpc#REGISTRY} 分离，避免客户端连接某服务器后采纳的映射污染本地权威映射——
 * 从而保证客户端断开后开启局域网时，集成服务端仍下发其本地扫描得到的正确映射。</p>
 */
@Mod(value = AnvilLibRpc.MOD_ID, dist = Dist.CLIENT)
public class AnvilLibRpcClient {
    /**
     * 客户端索引表：仅通过 {@link RpcRegistry#adopt} 采纳服务端下发的映射。
     */
    public static final RpcRegistry REGISTRY = new RpcRegistry(false);

    /**
     * 客户端侧待响应的 {@link dev.anvilcraft.lib.v2.rpc.RPC#invoke} 调用登记表。
     */
    public static final RpcPendingCalls PENDING = new RpcPendingCalls();
}
