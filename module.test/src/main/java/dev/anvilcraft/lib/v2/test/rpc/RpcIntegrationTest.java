package dev.anvilcraft.lib.v2.test.rpc;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * RPC 模块集成测试。
 *
 * <p>测试在玩家登录时自动执行，覆盖 RPC 的各项功能：</p>
 * <ul>
 *     <li>基础类型参数的序列化与调用</li>
 *     <li>多参数方法调用</li>
 *     <li>自定义编解码器</li>
 *     <li>有返回值的远程调用（invoke）</li>
 *     <li>校验器功能</li>
 *     <li>异常处理</li>
 *     <li>超时机制</li>
 * </ul>
 */
@EventBusSubscriber(modid = "anvillib_test")
public class RpcIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcIntegrationTest.class);
    private static final boolean ENABLE_AUTO_TEST = Boolean.getBoolean("anvillib.test.rpc.auto");

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!ENABLE_AUTO_TEST) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        LOGGER.info("Starting RPC integration test for player: {}", player.getName().getString());
        TestRpcMethods.clearLog();

        try {
            testBasicCalls(player);
            testMultipleParams(player);
            testCustomCodecs(player);
            testReturnValues(player);
            testValidators(player);
            testExceptions(player);
            testEdgeCases(player);

            LOGGER.info("RPC integration test completed successfully. Total calls: {}",
                TestRpcMethods.getInvocationCounter().get());
            LOGGER.info("Call log: {}", TestRpcMethods.getCallLog());
        } catch (Exception e) {
            LOGGER.error("RPC integration test failed", e);
        }
    }

    private static void testBasicCalls(ServerPlayer player) {
        LOGGER.info("Testing basic RPC calls...");
        RpcTarget target = RpcTarget.player(player);

        RPC.call(target, TestRpcMethods::noArgs);
        RPC.call(target, TestRpcMethods::withInt, 42);
        RPC.call(target, TestRpcMethods::withLong, 9999999999L);
        RPC.call(target, TestRpcMethods::withFloat, 3.14f);
        RPC.call(target, TestRpcMethods::withDouble, 2.718281828);
        RPC.call(target, TestRpcMethods::withBoolean, true);
        RPC.call(target, TestRpcMethods::withByte, (byte) 127);
        RPC.call(target, TestRpcMethods::withShort, (short) 32000);
        RPC.call(target, TestRpcMethods::withString, "Hello RPC");
        RPC.call(target, TestRpcMethods::withBoxedInt, 100);
        RPC.call(target, TestRpcMethods::withBoxedBoolean, false);
        RPC.call(target, TestRpcMethods::withByteArray, new byte[]{1, 2, 3, 4, 5});
        RPC.call(target, TestRpcMethods::withLongArray, new long[]{100L, 200L, 300L});

        LOGGER.info("Basic calls test passed");
    }

    private static void testMultipleParams(ServerPlayer player) {
        LOGGER.info("Testing multiple parameter methods...");
        RpcTarget target = RpcTarget.player(player);

        RPC.call(target, TestRpcMethods::twoParams, "test", 5);
        RPC.call(target, TestRpcMethods::threeParams, 1, "two", true);
        RPC.call(target, TestRpcMethods::fourParams, 1, 2, 3, 4);
        RPC.call(target, TestRpcMethods::fiveParams, 1, 2, 3, 4, 5);
        RPC.call(target, TestRpcMethods::manyParams, 1, 2, 3, 4, 5, 6, 7, 8);

        LOGGER.info("Multiple params test passed");
    }

    private static void testCustomCodecs(ServerPlayer player) {
        LOGGER.info("Testing custom codecs...");
        RpcTarget target = RpcTarget.player(player);

        CompoundTag tag = new CompoundTag();
        tag.putString("key1", "value1");
        tag.putInt("key2", 42);
        RPC.call(target, TestRpcMethods::withCompoundTag, tag);

        BlockPos pos = new BlockPos(100, 64, -200);
        RPC.call(target, TestRpcMethods::withCustomCodec, pos);
        RPC.call(target, TestRpcMethods::withMixedCodecs, "location", pos, 999);

        LOGGER.info("Custom codecs test passed");
    }

    private static void testReturnValues(ServerPlayer player) throws Exception {
        LOGGER.info("Testing methods with return values...");
        RpcTarget target = RpcTarget.player(player);

        CompletableFuture<Integer> futureInt = RPC.invoke(target, TestRpcMethods::returnInt);
        Integer resultInt = futureInt.get(5, TimeUnit.SECONDS);
        assert resultInt == 42 : "Expected 42, got " + resultInt;

        CompletableFuture<String> futureString = RPC.invoke(target, TestRpcMethods::returnString);
        String resultString = futureString.get(5, TimeUnit.SECONDS);
        assert "test result".equals(resultString) : "Expected 'test result', got " + resultString;

        CompletableFuture<Boolean> futureBool = RPC.invoke(target, TestRpcMethods::returnBoolean);
        Boolean resultBool = futureBool.get(5, TimeUnit.SECONDS);
        assert resultBool : "Expected true, got false";

        CompletableFuture<Integer> futureSum = RPC.invoke(target, TestRpcMethods::computeSum, 10, 32);
        Integer sum = futureSum.get(5, TimeUnit.SECONDS);
        assert sum == 42 : "Expected 42, got " + sum;

        CompletableFuture<String> futureConcat = RPC.invoke(target, TestRpcMethods::concatenate, "Hello", "World");
        String concat = futureConcat.get(5, TimeUnit.SECONDS);
        assert "HelloWorld".equals(concat) : "Expected 'HelloWorld', got " + concat;

        CompletableFuture<byte[]> futureArray = RPC.invoke(target, TestRpcMethods::returnByteArray, 10);
        byte[] array = futureArray.get(5, TimeUnit.SECONDS);
        assert array.length == 10 : "Expected array of length 10, got " + array.length;

        CompletableFuture<BlockPos> futurePos = RPC.invoke(target, TestRpcMethods::returnCustomType, 1, 2, 3);
        BlockPos pos = futurePos.get(5, TimeUnit.SECONDS);
        assert pos.equals(new BlockPos(1, 2, 3)) : "Expected BlockPos(1,2,3), got " + pos;

        LOGGER.info("Return values test passed");
    }

    private static void testValidators(ServerPlayer player) throws Exception {
        LOGGER.info("Testing validators...");
        RpcTarget target = RpcTarget.player(player);

        // 始终拒绝的校验器 - 单向调用会静默丢弃
        RPC.call(target, TestRpcMethods::alwaysRejected);

        // 条件校验器 - 正值应通过
        RPC.call(target, TestRpcMethods::conditionalAccept, 100);

        // 条件校验器 - 负值应被拒绝
        RPC.call(target, TestRpcMethods::conditionalAccept, -1);

        // 有返回值的调用 - 正值应通过
        CompletableFuture<Integer> futureAccept = RPC.invoke(target, TestRpcMethods::conditionalReturn, 50);
        Integer result = futureAccept.get(5, TimeUnit.SECONDS);
        assert result == 100 : "Expected 100, got " + result;

        // 有返回值的调用 - 负值应失败
        CompletableFuture<Integer> futureReject = RPC.invoke(target, TestRpcMethods::conditionalReturn, -10);
        try {
            futureReject.get(5, TimeUnit.SECONDS);
            throw new AssertionError("Expected validator rejection, but call succeeded");
        } catch (Exception e) {
            // Expected
            LOGGER.info("Validator correctly rejected negative value");
        }

        LOGGER.info("Validators test passed");
    }

    private static void testExceptions(ServerPlayer player) {
        LOGGER.info("Testing exception handling...");
        RpcTarget target = RpcTarget.player(player);

        // 单向调用抛出异常 - 应在接收端记录但不影响发送端
        RPC.call(target, TestRpcMethods::throwsException);

        // 有返回值的调用抛出异常 - future 应完成为异常
        CompletableFuture<Integer> futureException = RPC.invoke(target, TestRpcMethods::throwsExceptionWithReturn);
        try {
            futureException.get(5, TimeUnit.SECONDS);
            throw new AssertionError("Expected exception, but call succeeded");
        } catch (Exception e) {
            // Expected
            LOGGER.info("Exception correctly propagated from remote call");
        }

        LOGGER.info("Exception handling test passed");
    }

    private static void testEdgeCases(ServerPlayer player) {
        LOGGER.info("Testing edge cases...");
        RpcTarget target = RpcTarget.player(player);

        // 极值测试
        RPC.call(target, TestRpcMethods::extremeValues, Integer.MAX_VALUE, Long.MAX_VALUE, Double.MAX_VALUE);
        RPC.call(target, TestRpcMethods::extremeValues, Integer.MIN_VALUE, Long.MIN_VALUE, Double.MIN_VALUE);
        RPC.call(target, TestRpcMethods::extremeValues, 0, 0L, 0.0);

        // 空字符串
        RPC.call(target, TestRpcMethods::emptyString, "");

        // 特殊字符
        RPC.call(target, TestRpcMethods::withString, "Unicode: 中文 测试");
        RPC.call(target, TestRpcMethods::withString, "Symbols: !@#$%^&*()_+-={}[]|\\:\";<>?,./");

        // 空数组
        RPC.call(target, TestRpcMethods::withByteArray, new byte[0]);
        RPC.call(target, TestRpcMethods::withLongArray, new long[0]);

        // 大数组
        RPC.call(target, TestRpcMethods::withByteArray, new byte[1000]);

        LOGGER.info("Edge cases test passed");
    }
}
