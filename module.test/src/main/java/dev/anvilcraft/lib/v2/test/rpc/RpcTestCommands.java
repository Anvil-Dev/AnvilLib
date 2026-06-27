package dev.anvilcraft.lib.v2.test.rpc;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * RPC 测试命令。
 *
 * <p>提供手动触发 RPC 测试的命令：</p>
 * <ul>
 *     <li>/rpctest basic - 基础调用测试</li>
 *     <li>/rpctest invoke - 有返回值调用测试</li>
 *     <li>/rpctest validator - 校验器测试</li>
 *     <li>/rpctest stress - 压力测试</li>
 *     <li>/rpctest all - 运行所有测试</li>
 * </ul>
 */
@EventBusSubscriber(modid = "anvillib_test")
public class RpcTestCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcTestCommands.class);

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("rpctest")
                .then(Commands.literal("basic")
                    .executes(context -> {
                        runBasicTest(context.getSource());
                        return 1;
                    })
                )
                .then(Commands.literal("invoke")
                    .executes(context -> {
                        runInvokeTest(context.getSource());
                        return 1;
                    })
                )
                .then(Commands.literal("validator")
                    .executes(context -> {
                        runValidatorTest(context.getSource());
                        return 1;
                    })
                )
                .then(Commands.literal("stress")
                    .executes(context -> {
                        runStressTest(context.getSource());
                        return 1;
                    })
                )
                .then(Commands.literal("all")
                    .executes(context -> {
                        runAllTests(context.getSource());
                        return 1;
                    })
                )
                .then(Commands.literal("log")
                    .executes(context -> {
                        showLog(context.getSource());
                        return 1;
                    })
                )
                .then(Commands.literal("clear")
                    .executes(context -> {
                        TestRpcMethods.clearLog();
                        context.getSource().sendSuccess(() -> Component.literal("RPC test log cleared"), true);
                        return 1;
                    })
                )
        );
    }

    private static void runBasicTest(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return;
        }

        source.sendSuccess(() -> Component.literal("Running basic RPC test..."), true);
        TestRpcMethods.clearLog();
        RpcTarget target = RpcTarget.player(player);

        try {
            RPC.call(target, TestRpcMethods::noArgs);
            RPC.call(target, TestRpcMethods::withInt, 42);
            RPC.call(target, TestRpcMethods::withString, "test");
            RPC.call(target, TestRpcMethods::twoParams, "hello", 123);

            CompoundTag tag = new CompoundTag();
            tag.putString("test", "value");
            RPC.call(target, TestRpcMethods::withCompoundTag, tag);

            BlockPos pos = new BlockPos(100, 64, -200);
            RPC.call(target, TestRpcMethods::withCustomCodec, pos);

            source.sendSuccess(() -> Component.literal("Basic test completed. Sent 6 calls."), true);
            LOGGER.info("Basic test completed. Invocations: {}", TestRpcMethods.getInvocationCounter().get());
        } catch (Exception e) {
            source.sendFailure(Component.literal("Basic test failed: " + e.getMessage()));
            LOGGER.error("Basic test failed", e);
        }
    }

    private static void runInvokeTest(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return;
        }

        source.sendSuccess(() -> Component.literal("Running invoke RPC test..."), true);
        TestRpcMethods.clearLog();
        RpcTarget target = RpcTarget.player(player);

        try {
            CompletableFuture<Integer> futureInt = RPC.invoke(target, TestRpcMethods::returnInt);
            futureInt.thenAccept(result -> source.sendSuccess(() -> Component.literal("Received int: " + result), false));

            CompletableFuture<String> futureString = RPC.invoke(target, TestRpcMethods::returnString);
            futureString.thenAccept(result -> source.sendSuccess(() -> Component.literal("Received string: " + result), false));

            CompletableFuture<Integer> futureSum = RPC.invoke(target, TestRpcMethods::computeSum, 10, 32);
            futureSum.thenAccept(result -> source.sendSuccess(() -> Component.literal("Computed sum: " + result), false));

            CompletableFuture<BlockPos> futurePos = RPC.invoke(target, TestRpcMethods::returnCustomType, 5, 10, 15);
            futurePos.thenAccept(result -> source.sendSuccess(() -> Component.literal("Received BlockPos: " + result), false));

            source.sendSuccess(() -> Component.literal("Invoke test started. Check results above."), true);
        } catch (Exception e) {
            source.sendFailure(Component.literal("Invoke test failed: " + e.getMessage()));
            LOGGER.error("Invoke test failed", e);
        }
    }

    private static void runValidatorTest(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return;
        }

        source.sendSuccess(() -> Component.literal("Running validator RPC test..."), true);
        TestRpcMethods.clearLog();
        RpcTarget target = RpcTarget.player(player);

        try {
            // 这个调用会被拒绝
            RPC.call(target, TestRpcMethods::alwaysRejected);
            source.sendSuccess(() -> Component.literal("Sent alwaysRejected (should be rejected)"), false);

            // 正值应该通过
            RPC.call(target, TestRpcMethods::conditionalAccept, 100);
            source.sendSuccess(() -> Component.literal("Sent conditionalAccept(100) (should pass)"), false);

            // 负值应该被拒绝
            RPC.call(target, TestRpcMethods::conditionalAccept, -10);
            source.sendSuccess(() -> Component.literal("Sent conditionalAccept(-10) (should be rejected)"), false);

            // 有返回值的正值调用
            CompletableFuture<Integer> futureAccept = RPC.invoke(target, TestRpcMethods::conditionalReturn, 50);
            futureAccept
                .thenAccept(result -> source.sendSuccess(() -> Component.literal("conditionalReturn(50) = " + result), false))
                .exceptionally(ex -> {
                    source.sendFailure(Component.literal("conditionalReturn(50) failed: " + ex.getMessage()));
                    return null;
                });

            // 有返回值的负值调用
            CompletableFuture<Integer> futureReject = RPC.invoke(target, TestRpcMethods::conditionalReturn, -5);
            futureReject.thenAccept(result ->
                source.sendFailure(Component.literal("conditionalReturn(-5) should have been rejected!"))
            ).exceptionally(ex -> {
                source.sendSuccess(() -> Component.literal("conditionalReturn(-5) correctly rejected"), false);
                return null;
            });

            source.sendSuccess(() -> Component.literal("Validator test completed."), true);
        } catch (Exception e) {
            source.sendFailure(Component.literal("Validator test failed: " + e.getMessage()));
            LOGGER.error("Validator test failed", e);
        }
    }

    private static void runStressTest(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return;
        }

        source.sendSuccess(() -> Component.literal("Running stress RPC test..."), true);
        TestRpcMethods.clearLog();
        RpcTarget target = RpcTarget.player(player);

        try {
            long startTime = System.currentTimeMillis();
            int callCount = 100;

            // 快速发送大量调用
            for (int i = 0; i < callCount; i++) {
                RPC.call(target, TestRpcMethods::withInt, i);
            }

            // 发送大量有返回值的调用
            for (int i = 0; i < 20; i++) {
                RPC.invoke(target, TestRpcMethods::computeSum, i, i + 1);
            }

            // 发送大数据
            byte[] largeArray = new byte[5000];
            for (int i = 0; i < 10; i++) {
                RPC.call(target, TestRpcMethods::withByteArray, largeArray);
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            source.sendSuccess(() -> Component.literal(
                String.format("Stress test completed: %d calls in %d ms", callCount + 30, duration)
            ), true);
            LOGGER.info("Stress test: {} calls in {} ms", callCount + 30, duration);
        } catch (Exception e) {
            source.sendFailure(Component.literal("Stress test failed: " + e.getMessage()));
            LOGGER.error("Stress test failed", e);
        }
    }

    private static void runAllTests(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command must be run by a player"));
            return;
        }

        source.sendSuccess(() -> Component.literal("Running all RPC tests..."), true);

        scheduleTest(0, () -> runBasicTest(source));
        scheduleTest(20, () -> runInvokeTest(source));
        scheduleTest(40, () -> runValidatorTest(source));
        scheduleTest(60, () -> runStressTest(source));
        scheduleTest(80, () ->
            source.sendSuccess(() -> Component.literal("All tests completed!"), true)
        );
    }

    private static void scheduleTest(int delayTicks, Runnable test) {
        // 非阻塞延迟：挂到服务端 tick 上逐 tick 递减，到点后在主线程执行。
        // 不能用 Thread.sleep 占住主线程——那会冻结服务端，且会阻塞兑现 RPC future 的同一个线程。
        RpcTestScheduler.onServer(delayTicks, test);
    }

    private static void showLog(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("=== RPC Call Log ==="), false);
        source.sendSuccess(() -> Component.literal(
            "Total invocations: " + TestRpcMethods.getInvocationCounter().get()
        ), false);

        var log = TestRpcMethods.getCallLog();
        if (log.isEmpty()) {
            source.sendSuccess(() -> Component.literal("(empty)"), false);
        } else {
            int count = 0;
            for (String entry : log) {
                if (count >= 20) {
                    source.sendSuccess(() -> Component.literal(
                        "... and " + (log.size() - 20) + " more entries"
                    ), false);
                    break;
                }
                final String finalEntry = entry;
                source.sendSuccess(() -> Component.literal("  " + finalEntry), false);
                count++;
            }
        }
        source.sendSuccess(() -> Component.literal("==================="), false);
    }
}
