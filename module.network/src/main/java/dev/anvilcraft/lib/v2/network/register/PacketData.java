package dev.anvilcraft.lib.v2.network.register;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IInsensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.ISensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

@Slf4j
record PacketData<B extends ByteBuf, T extends IPacket>(
    CustomPacketPayload.Type<T> type,
    StreamCodec<B, T> streamCodec,
    PacketDirection direction,
    IPayloadHandler<T> handler
) {
    @SuppressWarnings("unchecked")
    static <B extends ByteBuf, T extends IPacket> PacketData<B, T> find(Class<T> packetClass) {
        CustomPacketPayload.Type<T> type = null;
        StreamCodec<B, T> codec = null;
        try {
            for (Field field : packetClass.getDeclaredFields()) {
                Set<AccessFlag> accessFlags = field.accessFlags();
                if (
                    !accessFlags.contains(AccessFlag.STATIC)
                    || !accessFlags.contains(AccessFlag.FINAL)
                ) {
                    continue;
                }
                Class<?> fieldType = field.getType();
                if (CustomPacketPayload.Type.class.isAssignableFrom(fieldType)) {
                    if (!isMatchingTypeArgument(field.getGenericType(), 0, packetClass)) {
                        continue;
                    }
                    field.setAccessible(true);
                    type = (CustomPacketPayload.Type<T>) field.get(null);
                } else if (StreamCodec.class.isAssignableFrom(fieldType)) {
                    if (!isMatchingTypeArgument(field.getGenericType(), 1, packetClass)) {
                        continue;
                    }
                    field.setAccessible(true);
                    codec = (StreamCodec<B, T>) field.get(null);
                }
            }
        } catch (IllegalAccessException e) {
            log.error("Cannot access the type/codec of packet {}", packetClass.getName(), e);
            throw new IllegalStateException();
        }
        if (type == null) {
            log.error("Cannot find static final type of packet {}", packetClass.getName());
            throw new IllegalArgumentException();
        }
        if (codec == null) {
            log.error("Cannot find static final codec of packet {}", packetClass.getName());
            throw new IllegalArgumentException();
        }

        PacketDirection direction;
        IPayloadHandler<T> handler;
        if (IInsensitiveBiPacket.class.isAssignableFrom(packetClass)) {
            direction = PacketDirection.BIDIRECTIONAL;
            handler = (packet, ctx) -> ((IInsensitiveBiPacket) packet).bidirectionalHandler(ctx);
        } else if (ISensitiveBiPacket.class.isAssignableFrom(packetClass)) {
            direction = PacketDirection.BIDIRECTIONAL;
            handler = (packet, ctx) -> ((ISensitiveBiPacket) packet).bidirectionalHandler(ctx);
        } else if (IClientboundPacket.class.isAssignableFrom(packetClass)) {
            direction = PacketDirection.CLIENTBOUND;
            handler = (packet, ctx) -> ((IClientboundPacket) packet).clientHandler(ctx);
        } else if (IServerboundPacket.class.isAssignableFrom(packetClass)) {
            direction = PacketDirection.SERVERBOUND;
            handler = (packet, ctx) -> ((IServerboundPacket) packet).serverHandler(ctx);
        } else {
            log.error("Class {} extends IPacket, but not extends IClientboundPacket or IServerboundPacket", packetClass.getName());
            throw new IllegalStateException();
        }
        return new PacketData<>(type, codec, direction, handler);
    }

    /**
     * 检查字段的参数化类型中，指定索引的类型实参是否与期望的类匹配。
     * 如果字段类型不是参数化类型、类型实参不足、或类型不匹配，返回 false。
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isMatchingTypeArgument(
        Type genericType,
        int typeArgIndex,
        Class<?> expectedClass
    ) {
        if (!(genericType instanceof ParameterizedType pt)) {
            return false;
        }
        Type[] typeArgs = pt.getActualTypeArguments();
        if (typeArgs.length <= typeArgIndex) {
            return false;
        }
        if (!(typeArgs[typeArgIndex] instanceof Class<?> typeArg)) {
            return false;
        }
        return typeArg.isAssignableFrom(expectedClass);
    }
}
