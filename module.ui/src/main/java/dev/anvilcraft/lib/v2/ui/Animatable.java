package dev.anvilcraft.lib.v2.ui;

import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * 通用补间动画。帧时间驱动，精度到每渲染帧。
 *
 * @param <T> 动画值类型
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Animatable<T> {
    private final Interpolator<T> interpolator;
    private T from;
    private T to;
    private T value;
    private float duration;
    private float delay;
    private float elapsed;
    private Easing easing = Easing.EASE_IN_OUT;
    private boolean running;
    private int repeatCount;
    private boolean yoyo;
    private int currentRepeat;
    private boolean forward = true;
    @Nullable
    private Consumer<Animatable<T>> onFinish;

    public Animatable(Interpolator<T> interpolator, T initialValue) {
        this.interpolator = interpolator;
        this.from = initialValue;
        this.to = initialValue;
        this.value = initialValue;
    }

    public static Animatable<Float> ofFloat(float initialValue) {
        return new Animatable<>(Interpolator.FLOAT, initialValue);
    }

    public static Animatable<Integer> ofColor(int initialValue) {
        return new Animatable<>(Interpolator.COLOR, initialValue);
    }

    public T value() {
        return this.value;
    }

    public boolean isRunning() {
        return this.running;
    }

    /**
     * 启动动画到目标值。
     *
     * @param target   目标值
     * @param duration 时长（秒）
     */
    public Animatable<T> animateTo(T target, float duration) {
        this.from = this.value;
        this.to = target;
        this.duration = duration;
        this.elapsed = -this.delay;
        this.running = true;
        this.currentRepeat = 0;
        this.forward = true;
        return this;
    }

    public Animatable<T> easing(Easing easing) {
        this.easing = easing;
        return this;
    }

    public Animatable<T> delay(float delaySeconds) {
        this.delay = delaySeconds;
        return this;
    }

    /**
     * 设置重复次数。0 = 不重复（默认），-1 = 无限重复。
     */
    public Animatable<T> repeat(int count) {
        this.repeatCount = count;
        return this;
    }

    public Animatable<T> yoyo(boolean yoyo) {
        this.yoyo = yoyo;
        return this;
    }

    public Animatable<T> onFinish(@Nullable Consumer<Animatable<T>> callback) {
        this.onFinish = callback;
        return this;
    }

    /**
     * 直接设置值（无动画）。
     */
    public void setValue(T value) {
        this.value = value;
        this.from = value;
        this.to = value;
        this.running = false;
    }

    /**
     * 每帧调用。返回 true 表示动画仍在进行中（需 recompose）。
     *
     * @param deltaTime 自上帧经过的秒数
     */
    public boolean tick(float deltaTime) {
        if (!this.running) return false;

        this.elapsed += deltaTime;
        if (this.elapsed < 0) return true; // still in delay

        float t = this.duration > 0 ? Math.min(this.elapsed / this.duration, 1f) : 1f;
        float eased = this.easing.apply(this.forward ? t : 1f - t);
        this.value = this.interpolator.interpolate(this.from, this.to, eased);

        if (t >= 1f) {
            if (this.repeatCount == -1 || this.currentRepeat < this.repeatCount) {
                this.currentRepeat++;
                this.elapsed = 0;
                if (this.yoyo) {
                    this.forward = !this.forward;
                }
            } else {
                this.running = false;
                this.value = this.forward ? this.to : this.from;
                if (this.onFinish != null) this.onFinish.accept(this);
            }
        }
        return this.running;
    }
}
