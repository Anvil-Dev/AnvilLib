package dev.anvilcraft.lib.v2.sync.util;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.util.Util;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.function.Function;

public class SideUtil {
    public static boolean isServer() {
        if (Util.isServer()) {
            return true;
        } else if (Util.isClient()) {
            return false;
        }
        throw new IllegalStateException("Cannot determine side: not client or server");
    }

    public static @Nullable RegistryAccess registryAccess() {
        if (Util.isServer()) {
            return SideUtil.serverRegistryAccess();
        } else if (Util.isClient()) {
            return SideUtil.clientRegistryAccess();
        }
        return null;
    }

    private static @Nullable RegistryAccess clientRegistryAccess() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        return connection == null ? null : connection.registryAccess();
    }

    private static @Nullable RegistryAccess serverRegistryAccess() {
        MinecraftServer currentServer = ServerLifecycleHooks.getCurrentServer();
        return currentServer == null ? null : currentServer.registryAccess();
    }

    public static @Nullable Entity entityFinder(IPayloadContext context, UUID uuid) {
        if (context.flow().equals(PacketFlow.SERVERBOUND)) {
            return SideUtil.serverEntityFinder(context.player(), uuid);
        } else {
            return SideUtil.clientEntityFinder(uuid);
        }
    }

    private static @Nullable Entity serverEntityFinder(Player player, UUID uuid) {
        if (!(player instanceof ServerPlayer serverPlayer)) return null;
        ServerLevel level = serverPlayer.level();
        return level.getEntity(uuid);
    }

    private static @Nullable Entity clientEntityFinder(UUID uuid) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        return minecraft.level.getEntity(uuid);
    }

    public static @Nullable BlockEntity blockEntityFinder(IPayloadContext context, BlockPos blockPos) {
        if (context.flow().equals(PacketFlow.SERVERBOUND)) {
            return SideUtil.serverBlockEntityFinder(context.player(), blockPos);
        } else {
            return SideUtil.clientBlockEntityFinder(blockPos);
        }
    }

    private static @Nullable BlockEntity serverBlockEntityFinder(Player player, BlockPos blockPos) {
        if (!(player instanceof ServerPlayer serverPlayer)) return null;
        return serverPlayer.level().getBlockEntity(blockPos);
    }

    private static @Nullable BlockEntity clientBlockEntityFinder(BlockPos blockPos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }
        return minecraft.level.getBlockEntity(blockPos);
    }

    public static FriendlyByteBuf createFriendlyByteBuf(ByteBuf byteBuf) {
        RegistryAccess registryAccess = SideUtil.registryAccess();
        FriendlyByteBuf buf;
        if (registryAccess == null) {
            buf = new FriendlyByteBuf(byteBuf);
        } else {
            buf = new RegistryFriendlyByteBuf(byteBuf, registryAccess, ConnectionType.NEOFORGE);
        }
        return buf;
    }

    public static <T, ID> void send(
        SyncDirection direction,
        boolean dimension,
        @Nullable Function<T, ResourceKey<Level>> dimensionGetter,
        T obj,
        ID id,
        Function<PacketFlow, IPacket> pyload
    ) {
        if (Util.isServer() && direction.isCreateByServer()) {
            SideUtil.serverSend(obj, id, pyload, dimension, dimensionGetter);
        } else if (Util.isClient() && direction.isCreateByClient()) {
            SideUtil.clientSend(pyload);
        }
    }


    public static <T, ID> void serverSend(
        T obj,
        ID id,
        Function<PacketFlow, IPacket> pyload,
        boolean dimension,
        @Nullable Function<T, ResourceKey<Level>> dimensionGetter
    ) {
        IPacket packet = pyload.apply(PacketFlow.CLIENTBOUND);
        if (!dimension || dimensionGetter == null) {
            PacketDistributor.sendToAllPlayers(packet);
            return;
        }
        MinecraftServer currentServer = ServerLifecycleHooks.getCurrentServer();
        if (currentServer == null) return;
        ResourceKey<Level> apply = dimensionGetter.apply(obj);
        ServerLevel level = currentServer.getLevel(apply);
        if (level == null) return;
        if (id instanceof BlockPos blockPos) {
            PacketDistributor.sendToPlayersTrackingChunk(level, ChunkPos.containing(blockPos), packet);
            return;
        }
        if (obj instanceof Entity) {
            PacketDistributor.sendToPlayersTrackingEntity((Entity) obj, packet);
        }
        PacketDistributor.sendToPlayersInDimension(level, packet);
    }

    public static void clientSend(Function<PacketFlow, IPacket> pyload) {
        ClientPacketDistributor.sendToServer(pyload.apply(PacketFlow.SERVERBOUND));
    }
}
