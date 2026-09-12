package dev.anvilcraft.lib.v2.test.rpc;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * RPC 测试用的非阻塞延迟调度器。
 *
 * <p>RPC 的响应在主线程通过 {@code ctx.enqueueWork(...)} 兑现，超时也由主线程的 tick 事件驱动。
 * 因此测试代码绝不能在主线程上 {@code Thread.sleep} 或 {@code future.get()} 阻塞——那会冻结游戏，
 * 并且会阻塞兑现 future 的同一个线程，导致 invoke 调用必然超时。本调度器将延迟任务挂到对应侧的
 * tick 事件上逐 tick 递减，到点后在主线程执行，全程不阻塞。</p>
 */
public final class RpcTestScheduler {
    private RpcTestScheduler() {
    }

    private record DelayedTask(int delayTicks, Runnable action) {
        DelayedTask withRemaining(int remaining) {
            return new DelayedTask(remaining, this.action);
        }
    }

    private static final Deque<DelayedTask> SERVER_TASKS = new ArrayDeque<>();
    private static final Deque<DelayedTask> CLIENT_TASKS = new ArrayDeque<>();

    /**
     * 在服务端主线程延迟 {@code delayTicks} 个 tick 后执行 {@code action}。{@code delayTicks <= 0} 时下一 tick 执行。
     */
    public static void onServer(int delayTicks, Runnable action) {
        SERVER_TASKS.add(new DelayedTask(Math.max(0, delayTicks), action));
    }

    /**
     * 在客户端主线程延迟 {@code delayTicks} 个 tick 后执行 {@code action}。{@code delayTicks <= 0} 时下一 tick 执行。
     */
    public static void onClient(int delayTicks, Runnable action) {
        CLIENT_TASKS.add(new DelayedTask(Math.max(0, delayTicks), action));
    }

    private static void pump(Deque<DelayedTask> tasks) {
        // 先快照本 tick 要处理的项数，避免任务自身再调度 onServer/onClient 时本 tick 重复处理
        int count = tasks.size();
        for (int i = 0; i < count; i++) {
            DelayedTask task = tasks.poll();
            if (task == null) break;
            if (task.delayTicks() <= 0) {
                task.action().run();
            } else {
                tasks.add(task.withRemaining(task.delayTicks() - 1));
            }
        }
    }

    @EventBusSubscriber(modid = "anvillib_test")
    public static final class Server {
        @SubscribeEvent
        public static void onServerTick(ServerTickEvent.Post event) {
            pump(SERVER_TASKS);
        }
    }

    @EventBusSubscriber(modid = "anvillib_test", value = Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            pump(CLIENT_TASKS);
        }
    }
}
