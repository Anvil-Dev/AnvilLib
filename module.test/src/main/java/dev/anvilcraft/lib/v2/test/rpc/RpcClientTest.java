package dev.anvilcraft.lib.v2.test.rpc;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

        // 延迟执行，确保连接完全建立
        Minecraft.getInstance().execute(() -> {
            try {
                Thread.sleep(1000); // 等待 1 秒
                testClientToServer();
            } catch (Exception e) {
                LOGGER.error("Client RPC test failed", e);
            }
        });
    }

    private static void testClientToServer() {
        LOGGER.info("Testing client -> server RPC calls...");
        RpcTarget serverTarget = RpcTarget.server();

        try {
            // 测试无参调用
            RPC.call(serverTarget, TestRpcMethods::noArgs);
            LOGGER.info("Sent noArgs call to server");

            // 测试带参数调用
            RPC.call(serverTarget, TestRpcMethods::withString, "Hello from client");
            LOGGER.info("Sent withString call to server");

            // 测试有返回值的调用
            CompletableFuture<String> future = RPC.invoke(serverTarget, TestRpcMethods::returnString);
            String result = future.get(5, TimeUnit.SECONDS);
            LOGGER.info("Received result from server: {}", result);

            // 测试计算调用
            CompletableFuture<Integer> sumFuture = RPC.invoke(serverTarget, TestRpcMethods::computeSum, 15, 27);
            Integer sum = sumFuture.get(5, TimeUnit.SECONDS);
            LOGGER.info("Server computed sum: {}", sum);

            LOGGER.info("Client -> Server RPC test completed successfully");
        } catch (Exception e) {
            LOGGER.error("Client -> Server RPC test failed", e);
        }
    }

    @SubscribeEvent
    public static void onClientPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        if (!ENABLE_AUTO_TEST) return;
        LOGGER.info("Client RPC test cleanup on logout");
    }
}
