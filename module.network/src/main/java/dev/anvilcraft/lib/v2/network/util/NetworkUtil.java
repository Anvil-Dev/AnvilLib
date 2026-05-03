package dev.anvilcraft.lib.v2.network.util;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class NetworkUtil {
    public static void sendToAllPlayersExcluded(
        @Nullable ServerPlayer excluded,
        CustomPacketPayload payload,
        CustomPacketPayload... payloads
    ) {
        MinecraftServer server = Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer(), "Cannot send clientbound payloads on the client");
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (Objects.equals(player, excluded)) continue;
            PacketDistributor.sendToPlayer(player, payload, payloads);
        }
    }

    public static void sendToAllPlayersInDimensionExcluded(
        ServerLevel level,
        @Nullable ServerPlayer excluded,
        CustomPacketPayload payload,
        CustomPacketPayload... payloads
    ) {
        for (ServerPlayer player : level.players()) {
            if (Objects.equals(player, excluded)) continue;
            PacketDistributor.sendToPlayer(player, payload, payloads);
        }
    }
}
