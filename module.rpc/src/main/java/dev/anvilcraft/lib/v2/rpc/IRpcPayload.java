package dev.anvilcraft.lib.v2.rpc;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Method;

@ApiStatus.Internal
public interface IRpcPayload {
    static byte[] encodeParams(RpcRegistry registry, Method method, Object[] args, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(registry.index(method));
        StreamCodec<RegistryFriendlyByteBuf, Object>[] codecs = RpcMethods.codecs(method);
        for (int i = 0; i < codecs.length; i++) {
            codecs[i].encode(buf, args[i]);
        }
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }

    static Object[] decodeParams(Method method, RegistryFriendlyByteBuf buf) {
        StreamCodec<RegistryFriendlyByteBuf, Object>[] codecs = RpcMethods.codecs(method);
        Object[] args = new Object[codecs.length];
        for (int i = 0; i < codecs.length; i++) {
            args[i] = codecs[i].decode(buf);
        }
        return args;
    }
}
