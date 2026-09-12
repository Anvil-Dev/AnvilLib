package dev.anvilcraft.lib.v2.registrum.attachment;

import dev.anvilcraft.lib.v2.registrum.mixin.ChunkMapAccessor;
import dev.anvilcraft.lib.v2.registrum.mixin.TrackedEntityAccessor;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 为没有原生附件同步的 1.21.4 提供相同的生命周期；可变附件可显式调用 syncData。 */
@EventBusSubscriber(modid = "anvillib_registrum")
public final class AttachmentSync {
    private static final Map<AttachmentType<?>, AttachmentSyncHandler<?>> HANDLERS = new ConcurrentHashMap<>();

    private AttachmentSync() { }

    public static <T> void register(AttachmentType<T> type, AttachmentSyncHandler<T> handler) {
        HANDLERS.put(type, handler);
    }

    public static void syncData(IAttachmentHolder holder, AttachmentType<?> type) {
        syncData(holder, type, false);
    }

    public static void syncData(IAttachmentHolder holder, AttachmentType<?> type, boolean initial) {
        if (!HANDLERS.containsKey(type)) return;
        Level world = level(holder);
        if (!(world instanceof ServerLevel server)) return;
        List<ServerPlayer> recipients;
        if (holder instanceof Entity entity) {
            recipients = new ArrayList<>();
            Object tracker = ((ChunkMapAccessor) server.getChunkSource().chunkMap).anvillib$trackedEntities().get(entity.getId());
            if (tracker instanceof TrackedEntityAccessor tracked) {
                for (ServerPlayerConnection connection : tracked.anvillib$watchers()) recipients.add(connection.getPlayer());
            }
            if (entity instanceof ServerPlayer player) recipients.add(player);
        } else if (holder instanceof BlockEntity block) {
            recipients = server.getChunkSource().chunkMap.getPlayers(new ChunkPos(block.getBlockPos()), false);
        } else if (holder instanceof LevelChunk chunk) {
            recipients = server.getChunkSource().chunkMap.getPlayers(chunk.getPos(), false);
        } else if (holder instanceof Level) {
            recipients = server.players();
        } else {
            return;
        }
        send(holder, type, server, recipients, initial);
    }

    private static @Nullable Level level(IAttachmentHolder holder) {
        if (holder instanceof Entity entity) return entity.level();
        if (holder instanceof BlockEntity block) return block.getLevel();
        if (holder instanceof LevelChunk chunk) return chunk.getLevel();
        return holder instanceof Level level ? level : null;
    }

    @SuppressWarnings("unchecked")
    private static <T> void send(IAttachmentHolder holder, AttachmentType<T> type, ServerLevel level,
                                 List<ServerPlayer> recipients, boolean initial) {
        AttachmentSyncHandler<T> handler = (AttachmentSyncHandler<T>) HANDLERS.get(type);
        if (handler == null || recipients.isEmpty()) return;
        List<ServerPlayer> allowed = recipients.stream().filter(player -> player.connection != null && handler.sendToPlayer(holder, player)).toList();
        if (allowed.isEmpty()) return;
        T value = holder.getExistingData(type).orElse(null);
        byte[] data = new byte[0];
        if (value != null) {
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
            try {
                handler.write(buffer, value, initial);
                if (!buffer.isReadable()) return;
                if (buffer.readableBytes() > 1_048_576) throw new IllegalArgumentException("Attachment sync data exceeds 1 MiB");
                data = new byte[buffer.readableBytes()];
                buffer.readBytes(data);
            } finally {
                buffer.release();
            }
        }
        int kind;
        long target;
        if (holder instanceof Entity entity) { kind = 0; target = entity.getId(); }
        else if (holder instanceof BlockEntity block) { kind = 1; target = block.getBlockPos().asLong(); }
        else if (holder instanceof LevelChunk chunk) { kind = 2; target = chunk.getPos().toLong(); }
        else { kind = 3; target = 0; }
        Payload payload = new Payload(level.dimension().location(), kind, target,
            NeoForgeRegistries.ATTACHMENT_TYPES.getKey(type), value == null, data);
        for (ServerPlayer player : allowed) PacketDistributor.sendToPlayer(player, payload);
    }

    private static void initial(IAttachmentHolder holder, ServerPlayer player) {
        if (!(level(holder) instanceof ServerLevel server)) return;
        for (AttachmentType<?> type : HANDLERS.keySet()) {
            if (holder.hasData(type)) send(holder, type, server, List.of(player), true);
        }
    }

    @SubscribeEvent
    public static void tracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player) initial(event.getTarget(), player);
    }

    @SubscribeEvent
    public static void chunkSent(ChunkWatchEvent.Sent event) {
        initial(event.getChunk(), event.getPlayer());
        for (BlockEntity block : event.getChunk().getBlockEntities().values()) initial(block, event.getPlayer());
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) { playerInitial(event); }

    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { playerInitial(event); }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) { playerInitial(event); }

    private static void playerInitial(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            initial(player, player);
            initial(player.serverLevel(), player);
        }
    }

    @SuppressWarnings("unchecked")
    private static void receive(Payload payload, IPayloadContext context) {
        Level level = context.player().level();
        if (!level.isClientSide || !level.dimension().location().equals(payload.dimension)) return;
        IAttachmentHolder holder = switch (payload.kind) {
            case 0 -> level.getEntity((int) payload.target);
            case 1 -> level.hasChunkAt(BlockPos.of(payload.target)) ? level.getBlockEntity(BlockPos.of(payload.target)) : null;
            case 2 -> {
                ChunkPos pos = new ChunkPos(payload.target);
                yield level.hasChunk(pos.x, pos.z) ? level.getChunk(pos.x, pos.z) : null;
            }
            case 3 -> level;
            default -> null;
        };
        if (holder == null) return;
        AttachmentType<Object> type = (AttachmentType<Object>) NeoForgeRegistries.ATTACHMENT_TYPES.getValue(payload.typeId);
        if (type == null) return;
        AttachmentSyncHandler<Object> handler = (AttachmentSyncHandler<Object>) HANDLERS.get(type);
        if (handler == null) return;
        if (payload.removed) { holder.removeData(type); return; }
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(payload.data), level.registryAccess());
        try {
            Object value = handler.read(holder, buffer, holder.getExistingData(type).orElse(null));
            if (value == null) holder.removeData(type);
            else holder.setData(type, value);
        } finally {
            buffer.release();
        }
    }

    public record Payload(ResourceLocation dimension, int kind, long target, ResourceLocation typeId,
                          boolean removed, byte[] data) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("anvillib_registrum", "attachments"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, Payload::dimension,
            ByteBufCodecs.VAR_INT, Payload::kind,
            ByteBufCodecs.LONG, Payload::target,
            ResourceLocation.STREAM_CODEC, Payload::typeId,
            ByteBufCodecs.BOOL, Payload::removed,
            ByteBufCodecs.byteArray(1_048_576), Payload::data,
            Payload::new);
        @Override public Type<Payload> type() { return TYPE; }
    }

    @EventBusSubscriber(modid = "anvillib_registrum")
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(Payload.TYPE, Payload.CODEC, AttachmentSync::receive);
        }
    }
}
