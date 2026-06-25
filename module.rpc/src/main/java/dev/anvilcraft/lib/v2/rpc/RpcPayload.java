package dev.anvilcraft.lib.v2.rpc;

import dev.anvilcraft.lib.v2.network.packet.IInsensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 承载一次远程调用的网络包：携带目标方法的定位信息与编码后的实参，在目标端解码后执行该方法。
 *
 * <p>双向包：服务端与客户端均可作为发送端或接收端。</p>
 *
 * @see RPC#call
 */
public class RpcPayload implements IInsensitiveBiPacket {
    public static final Type<RpcPayload> TYPE = IPacket.type(AnvilLibRpc.of("rpc"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RpcPayload> STREAM_CODEC = StreamCodec.of(
        RpcPayload::encode,
        RpcPayload::decode
    );

    private final Method method;
    private final Object[] args;

    RpcPayload(Method method, Object[] args) {
        this.method = method;
        this.args = args;
    }

    private static void encode(RegistryFriendlyByteBuf buf, RpcPayload payload) {
        ByteBufCodecs.VAR_INT.encode(buf, RpcRegistry.index(payload.method));
        StreamCodec<RegistryFriendlyByteBuf, Object>[] codecs = RpcMethods.codecs(payload.method);
        for (int i = 0; i < codecs.length; i++) {
            codecs[i].encode(buf, payload.args[i]);
        }
    }

    private static RpcPayload decode(RegistryFriendlyByteBuf buf) {
        Method method = RpcRegistry.byIndex(ByteBufCodecs.VAR_INT.decode(buf));
        StreamCodec<RegistryFriendlyByteBuf, Object>[] codecs = RpcMethods.codecs(method);
        Object[] args = new Object[codecs.length];
        for (int i = 0; i < codecs.length; i++) {
            args[i] = codecs[i].decode(buf);
        }
        return new RpcPayload(method, args);
    }

    @Override
    public void handleOnBothSide(Player player) {
        try {
            this.method.invoke(null, this.args);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot invoke RPC method " + this.method, e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("RPC method " + this.method + " threw an exception", e.getCause());
        }
    }

    @Override
    public CustomPacketPayload.Type<RpcPayload> type() {
        return RpcPayload.TYPE;
    }
}
