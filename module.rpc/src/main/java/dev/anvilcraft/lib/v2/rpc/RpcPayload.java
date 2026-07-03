package dev.anvilcraft.lib.v2.rpc;

import dev.anvilcraft.lib.v2.network.packet.IInsensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.rpc.client.AnvilLibRpcClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 承载一次远程调用的网络包。
 *
 * <p>线上仅传输一段不透明字节（{@code 索引 + 编码后的实参}）。仿照
 * {@link dev.anvilcraft.lib.v2.network.packet 网络包} 中 SyncPayload 的做法，真正依赖「当前侧」的
 * 编解码在已知方向的位置进行：</p>
 * <ul>
 *     <li><b>编码</b>在发送侧（{@link RpcTarget} 已知发送方向，故已知使用哪个 {@link RpcRegistry}）；</li>
 *     <li><b>解码</b>在处理侧（{@link IPayloadContext#flow()} 已知接收方向）——而非 {@link StreamCodec}
 *     的解码回调中（其运行于网络线程，无法可靠判断当前侧）。</li>
 * </ul>
 *
 * @see RPC#call
 */
public class RpcPayload implements IRpcPayload, IInsensitiveBiPacket {
    public static final Type<RpcPayload> TYPE = IPacket.type(AnvilLibRpc.mod("rpc"));
    public static final StreamCodec<ByteBuf, RpcPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BYTE_ARRAY,
        RpcPayload::data,
        RpcPayload::new
    );

    private final byte[] data;

    RpcPayload(byte[] data) {
        this.data = data;
    }

    /**
     * 在发送侧将方法索引与实参编码为字节。
     *
     * @param registry       发送侧索引表（服务端为权威表，客户端为已采纳表）
     * @param registryAccess 发送侧注册表访问器，用于构造可承载注册表对象的缓冲区
     * @param method         目标方法
     * @param args           实参
     * @return 编码后的网络包
     */
    static RpcPayload encode(RpcRegistry registry, RegistryAccess registryAccess, Method method, Object[] args) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess, ConnectionType.NEOFORGE);
        return new RpcPayload(IRpcPayload.encodeParams(registry, method, args, buf));
    }

    byte[] data() {
        return this.data;
    }

    @Override
    public void bidirectionalHandler(IPayloadContext ctx) {
        ctx.enqueueWork(() -> this.handle(ctx));
    }

    @Override
    public void handleOnBothSide(Player player) {
        // 实际处理在 handle(ctx) 中完成（需要 ctx.flow() 与 ctx.player()）
    }

    private void handle(IPayloadContext ctx) {
        // 接收 clientbound 表示本侧为客户端，使用已采纳的客户端表；serverbound 则本侧为服务端，使用权威表
        RpcRegistry registry = ctx.flow().isClientbound() ? AnvilLibRpcClient.REGISTRY : AnvilLibRpc.REGISTRY;
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(
            Unpooled.wrappedBuffer(this.data), ctx.player().registryAccess(), ConnectionType.NEOFORGE
        );
        Method method = registry.byIndex(buf.readVarInt());
        Object[] args = IRpcPayload.decodeParams(method, buf);
        if (!RpcMethods.validate(method, ctx, args)) {
            // 校验未通过：静默丢弃该单向调用
            return;
        }
        try {
            method.invoke(null, args);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot invoke RPC method " + method, e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("RPC method " + method + " threw an exception", e.getCause());
        }
    }

    @Override
    public CustomPacketPayload.Type<RpcPayload> type() {
        return RpcPayload.TYPE;
    }
}
