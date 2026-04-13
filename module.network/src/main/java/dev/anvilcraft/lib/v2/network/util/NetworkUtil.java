package dev.anvilcraft.lib.v2.network.util;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class NetworkUtil {
    public static void sendToAllPlayersExcluded(
        ServerLevel level,
        @Nullable ServerPlayer excluded,
        CustomPacketPayload payload,
        CustomPacketPayload... payloads
    ) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.equals(excluded)) continue;
            PacketDistributor.sendToPlayer(player, payload, payloads);
        }
    }
}
