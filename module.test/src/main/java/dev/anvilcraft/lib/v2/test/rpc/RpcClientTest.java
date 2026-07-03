package dev.anvilcraft.lib.v2.test.rpc;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RPC 客户端测试。
 *
 * <p>测试客户端到服务端的远程调用，以及客户端接收来自服务端的调用。</p>
 */
@EventBusSubscriber(modid = "anvillib_test", value = Dist.CLIENT)
public class RpcClientTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClientTest.class);
    private static final boolean ENABLE_AUTO_TEST = Boolean.getBoolean("anvillib.test.rpc.client.auto");

    @SubscribeEvent
    public static void onClientPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (!ENABLE_AUTO_TEST) return;

        LocalPlayer player = event.getPlayer();

        LOGGER.info("Starting client-side RPC test for player: {}", player.getName().getString());

        // 延迟 20 tick(约 1 秒)执行，确保连接完全建立。不能用 Thread.sleep 占住客户端主线程——
        // 那会冻结画面，且会阻塞兑现 RPC future 的同一个线程。改用 tick 调度器非阻塞延迟。
        RpcTestScheduler.onClient(20, RpcClientTest::testClientToServer);
    }

    private static void testClientToServer() {
        LOGGER.info("Testing client -> server RPC calls...");
        RpcTarget serverTarget = RpcTarget.server();

        // 测试无参调用
        RPC.call(serverTarget, TestRpcMethods::noArgs);
        LOGGER.info("Sent noArgs call to server");

        // 测试带参数调用
        RPC.call(serverTarget, TestRpcMethods::withString, "Hello from client");
        LOGGER.info("Sent withString call to server");

        // 测试有返回值的调用 - 不能在主线程 future.get()，改用回调
        RPC.invoke(serverTarget, TestRpcMethods::returnString)
            .thenAccept(result -> LOGGER.info("Received result from server: {}", result))
            .exceptionally(ex -> {
                LOGGER.error("client -> server returnString failed", ex);
                return null;
            });

        // 测试计算调用
        RPC.invoke(serverTarget, TestRpcMethods::computeSum, 15, 27)
            .thenAccept(sum -> LOGGER.info("Server computed sum: {}", sum))
            .exceptionally(ex -> {
                LOGGER.error("client -> server computeSum failed", ex);
                return null;
            });

        LOGGER.info("Client -> Server RPC test dispatched (results logged asynchronously)");
    }

    @SubscribeEvent
    public static void onClientPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (!ENABLE_AUTO_TEST) return;
        LOGGER.info("Client RPC test cleanup on logout");
    }
}
