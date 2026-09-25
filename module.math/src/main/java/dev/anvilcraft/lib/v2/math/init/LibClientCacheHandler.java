package dev.anvilcraft.lib.v2.math.init;

import dev.anvilcraft.lib.v2.math.AnvilLibMath;
import dev.anvilcraft.lib.v2.math.expression.FlatExpressionParser;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * 客户端断开连接时清掉 flat 文本的解析缓存。
 *
 * <p>{@link LibCacheReloadHandler} 那两个事件只在服务端触发，而客户端的 {@code IExpression.STREAM_CODEC}
 * 解码 flat 文本时走的是同一条 {@link FlatExpressionParser#parseValue} 路径。客户端每连一次服务器就会拿到
 * 一份新的数据包注册表实例，而缓存条目又永久持有旧注册表，于是反复换服会单调增长。断开时清一次即可。</p>
 *
 * <p>用 {@link Dist#CLIENT} 限定：本类直接引用了客户端专属的 {@link ClientPlayerNetworkEvent}，
 * 专用服务器上不能让这个类被加载。</p>
 */
@EventBusSubscriber(modid = AnvilLibMath.MOD_ID, value = Dist.CLIENT)
public class LibClientCacheHandler {
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        FlatExpressionParser.clearCache();
    }
}
