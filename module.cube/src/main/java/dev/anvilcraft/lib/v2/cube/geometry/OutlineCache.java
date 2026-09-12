package dev.anvilcraft.lib.v2.cube.geometry;

import com.mojang.logging.LogUtils;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** 后台生成并集轮廓；渲染线程只读取结果，队列满时使用已有轻量轮廓。 */
public final class OutlineCache implements AutoCloseable {
    public static final long MAX_CACHED_BYTES = 4L * 1024 * 1024;
    public static final long MAX_PENDING_BYTES = 16L * 1024 * 1024;
    public static final int MAX_ENTRIES = 128;
    public static final int MAX_PENDING = 8;
    private final ThreadPoolExecutor executor;
    private final Map<SelectionGeometry, PackedOutline> ready = new LinkedHashMap<>(16, 0.75F, true);
    private final Map<SelectionGeometry, Future<?>> pending = new IdentityHashMap<>();
    private long bytes;
    private long pendingBytes;
    private long epoch;
    private long completed;
    private long limited;
    private long failed;

    public OutlineCache() {
        this.executor = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(MAX_PENDING), work -> {
            Thread thread = new Thread(work, "AnvilLib cube outlines");
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        });
        this.executor.allowCoreThreadTimeOut(true);
    }

    public synchronized PackedOutline get(SelectionGeometry geometry) {
        PackedOutline cached = this.ready.get(geometry);
        if (cached != null) return cached;
        // 无法长期容纳的单项不排队，避免生成后立即被淘汰而在每帧反复重建。
        if (geometry.estimatedBytes() + SelectionGeometry.MAX_OUTLINE_SEGMENTS * 36L + 40 > MAX_CACHED_BYTES) {
            return geometry.fallbackOutline();
        }
        if (!this.executor.isShutdown() && !this.pending.containsKey(geometry)
            && this.pending.size() < MAX_PENDING && this.pendingBytes + geometry.estimatedBytes() <= MAX_PENDING_BYTES) {
            long generation = this.epoch;
            this.pendingBytes += geometry.estimatedBytes();
            this.pending.put(geometry, this.executor.submit(() -> this.build(geometry, generation)));
        }
        return geometry.fallbackOutline();
    }

    private void build(SelectionGeometry geometry, long generation) {
        PackedOutline result;
        boolean limited = false;
        boolean failed = false;
        try {
            result = OutlineBuilder.build(geometry, TimeUnit.MILLISECONDS.toNanos(100), 1_000_000);
        } catch (OutlineBuilder.BudgetExceededException exception) {
            result = geometry.fallbackOutline();
            limited = true;
        } catch (RuntimeException exception) {
            result = geometry.fallbackOutline();
            failed = true;
            LogUtils.getLogger().warn("Unable to build cube outline; keeping bounded fallback", exception);
        }
        synchronized (this) {
            if (generation != this.epoch) return;
            this.pending.remove(geometry);
            this.pendingBytes -= geometry.estimatedBytes();
            this.completed++;
            if (limited) this.limited++;
            if (failed) this.failed++;
            this.ready.put(geometry, result);
            // 缓存键会保留几何，因此预算同时计算几何和轮廓，动态来源也不能绕过上限。
            this.bytes += result.estimatedBytes() + geometry.estimatedBytes();
            while (this.bytes > MAX_CACHED_BYTES || this.ready.size() > MAX_ENTRIES) {
                Map.Entry<SelectionGeometry, PackedOutline> first = this.ready.entrySet().iterator().next();
                this.bytes -= first.getValue().estimatedBytes() + first.getKey().estimatedBytes();
                this.ready.remove(first.getKey());
            }
        }
    }

    public synchronized void clear() {
        this.epoch++;
        this.pending.values().forEach(future -> future.cancel(true));
        this.executor.getQueue().clear();
        this.pending.clear();
        this.ready.clear();
        this.bytes = 0;
        this.pendingBytes = 0;
    }

    public synchronized Statistics statistics() {
        return new Statistics(this.ready.size(), this.bytes, this.pending.size(), this.pendingBytes, this.completed, this.limited, this.failed);
    }

    @Override
    public synchronized void close() {
        this.clear();
        this.executor.shutdownNow();
    }

    public record Statistics(int entries, long retainedBytes, int pending, long pendingBytes, long completed, long limited, long failed) { }
}
