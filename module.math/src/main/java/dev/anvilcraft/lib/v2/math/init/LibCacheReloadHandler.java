package dev.anvilcraft.lib.v2.math.init;

import dev.anvilcraft.lib.v2.math.AnvilLibMath;
import dev.anvilcraft.lib.v2.math.expression.FlatExpressionParser;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * 数据包重载后清掉 flat 文本的解析缓存。
 *
 * <p>缓存按函数注册表实例分组，而 {@link FlatExpressionParser#CACHE} 的弱键回收不掉——已解析的表达式树
 * 持有注册表函数的引用，那个引用又指回注册表。不主动清，每次重载都会永久留下一份旧注册表连同其中
 * 最多 512 棵表达式树。</p>
 *
 * <p>两个事件各管一边：{@link ServerStartedEvent} 覆盖首轮数据包加载（专用服务器没有玩家时也会触发），
 * {@link OnDatapackSyncEvent} 覆盖之后的 {@code /reload}。</p>
 */
@EventBusSubscriber(modid = AnvilLibMath.MOD_ID)
public class LibCacheReloadHandler {
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        FlatExpressionParser.clearCache();
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        // 本事件在「某个玩家进服」时也会触发，那时注册表并没有换，整表缓存没必要丢；
        // 只有 getPlayer() 为 null（即 /reload 这类全员同步）才说明数据包换过
        if (event.getPlayer() == null) FlatExpressionParser.clearCache();
    }
}
