package dev.anvilcraft.lib.v2.network.packet;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 双端网络包，允许两端各自向对方发送
 *
 * <p>处理逻辑时方向不敏感，两端共用一套逻辑</p>
 */
public interface IInsensitiveBiPacket extends IClientboundPacket, IServerboundPacket {
    /**
     * 双端处理器
     *
     * @param ctx 网络包上下文
     */
    default void bidirectionalHandler(IPayloadContext ctx) {
        ctx.enqueueWork(() -> this.handleOnBothSide(ctx.player()));
    }

    /**
     * 两端共用的处理逻辑
     *
     * @param player 玩家。客户端为
     *               {@link net.minecraft.client.player.LocalPlayer LocalPlayer}，服务端为
     *               {@link net.minecraft.server.level.ServerPlayer ServerPlayer}
     */
    void handleOnBothSide(Player player);

    @Override
    default void clientHandler(IPayloadContext ctx) {
        this.bidirectionalHandler(ctx);
    }

    @Override
    default void handleOnClient(Player player) {
        this.handleOnBothSide(player);
    }

    @Override
    default void serverHandler(IPayloadContext ctx) {
        this.bidirectionalHandler(ctx);
    }

    @Override
    default void handleOnServer(Player player) {
        this.handleOnBothSide(player);
    }
}
