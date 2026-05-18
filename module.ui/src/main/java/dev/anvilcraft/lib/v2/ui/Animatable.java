package dev.anvilcraft.lib.v2.ui;

import net.minecraft.util.Mth;

/**
 * 基于游戏 tick 的动画值。从当前值平滑过渡到目标值。
 * 每 tick 调用 {@link #tick()} 推进动画。
 */
public class Animatable {

    private float value;
    private float startValue;
    private float targetValue;
    private int durationTicks;
    private int elapsed;

    public Animatable(float initialValue) {
        this.value = initialValue;
        this.targetValue = initialValue;
    }

    /**
     * 获取当前动画值。
     */
    public float getValue() {
        return value;
    }

    /**
     * 直接设置值（无动画）。
     */
    public void setValue(float value) {
        this.value = value;
        this.targetValue = value;
        this.elapsed = 0;
    }

    /**
     * 动画到目标值，durationTicks 帧内完成。
     */
    public void animateTo(float target, int durationTicks) {
        if (durationTicks <= 0) {
            this.value = target;
            this.targetValue = target;
            this.elapsed = 0;
            return;
        }
        this.startValue = this.value;
        this.targetValue = target;
        this.durationTicks = durationTicks;
        this.elapsed = 0;
    }

    /**
     * 每 tick 调用一次以推进动画。返回 true 表示动画进行中。
     */
    public boolean tick() {
        if (elapsed >= durationTicks) return false;
        elapsed++;
        float t = durationTicks > 0 ? (float) elapsed / durationTicks : 1f;
        // 缓入缓出
        float eased = t < 0.5f ? 2f * t * t : -1f + (4f - 2f * t) * t;
        this.value = Mth.lerp(eased, startValue, targetValue);
        return elapsed < durationTicks;
    }

    /**
     * 动画是否进行中。
     */
    public boolean isRunning() {
        return elapsed < durationTicks;
    }
}
