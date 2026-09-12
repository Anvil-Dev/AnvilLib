package dev.anvilcraft.lib.v2.network.packet;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 网络包根接口
 */
public sealed interface IPacket extends CustomPacketPayload permits IClientboundPacket, IServerboundPacket {
    /**
     * 构建网络包类型
     *
     * @param id  网络包 ID
     * @param <T> 网络包 Java 类型
     * @return 网络包类型
     */
    static <T extends IPacket> Type<T> type(Identifier id) {
        return new Type<>(id);
    }
}
