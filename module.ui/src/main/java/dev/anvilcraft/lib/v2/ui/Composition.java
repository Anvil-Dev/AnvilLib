package dev.anvilcraft.lib.v2.ui;

import lombok.Setter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 组合引擎，驱动 recompose、状态追踪和渲染。
 * <p>
 * 每个 {@link DeclarativeScreen} 创建一个 Composition。
 * 它维护一个按调用位置索引的扁平 slot table。
 * recompose 时内容 lambda 重执行，每次 {@link #emit(UIComponent)} 调用
 * 与同索引的 slot 做 diff。
 * <p>
 * 状态读取按 slot 追踪，写入只标记受影响 slot 为脏——不会波及整棵树。
 */
@SuppressWarnings(
    {
        "unused",
        "UnusedReturnValue"
    }
)
public class Composition {
    private static final ThreadLocal<@Nullable Composition> CURRENT = new ThreadLocal<>();
    private final List<Slot> slots = new ArrayList<>();
    private final Map<Integer, Object> rememberedValues = new HashMap<>();

    // ── slot table ──
    private final List<Animatable<?>> animatables = new ArrayList<>();
    private final @Nullable UIScope rootScope;
    /**
     * 当前正在 emit 的 slot（在 {@link #emit} 期间设置）。
     */
    @Nullable Slot currentSlot;
    private int currentIndex;
    private int currentRememberKey;
    private boolean dirty = true;
    private long lastFrameNanos = -1;
    // ── 状态 ──
    @Setter
    private @Nullable Consumer<UIScope> content;

    public Composition(@Nullable UIScope rootScope) {
        this.rootScope = rootScope;
    }

    /**
     * 返回当前线程上的组合实例，可能为 null。
     */
    @Nullable
    public static Composition currentOrNull() {
        return CURRENT.get();
    }

    /**
     * 返回当前线程上的组合实例，不存在则抛出异常。
     */
    public static Composition current() {
        Composition c = CURRENT.get();
        if (c == null) {
            throw new IllegalStateException("Not inside a composition frame");
        }
        return c;
    }

    /**
     * 标记组合需要在下一帧 recompose。
     */
    public void invalidate() {
        this.dirty = true;
    }

    /**
     * 注册 Tween 动画，每帧自动 tick。动画进行中时自动触发 recompose。
     */
    public void watch(Animatable<?> animatable) {
        if (!this.animatables.contains(animatable)) {
            this.animatables.add(animatable);
        }
    }

    // ── remember / ref ──

    /**
     * 在多次 recompose 间持久化一个值。init supplier 只在首次组合时调用。
     */
    @SuppressWarnings("unchecked")
    public <T> T remember(Supplier<T> init) {
        int key = this.currentRememberKey++;
        Object existing = this.rememberedValues.get(key);
        if (existing != null) {
            return (T) existing;
        }
        T value = init.get();
        this.rememberedValues.put(key, value);
        return value;
    }

    /**
     * {@code comp.ref(0)} 等价于 {@code comp.remember(() -> new Ref<>(0))}。
     */
    public <T> Ref<T> ref(T initialValue) {
        return this.remember(() -> new Ref<>(initialValue));
    }

    // ── emit ──

    /**
     * 向当前调用位置的 slot 中 emit 一个组件。由组件工厂函数调用。
     */
    public void emit(UIComponent component) {
        Slot slot;
        if (this.currentIndex < this.slots.size()) {
            slot = this.slots.get(this.currentIndex);
            UIComponent old = slot.component;
            if (old != null && old.getClass() == component.getClass()) {
                component.copyRuntimeState(old);
            }
            slot.component = component;
        } else {
            slot = new Slot();
            slot.component = component;
            this.slots.add(slot);
        }
        this.currentSlot = slot;
        this.currentIndex++;
    }

    // ── 每帧入口 ──

    /**
     * 每帧从 {@link DeclarativeScreen#extractRenderState} 调用。
     * 若脏则 recompose，然后 measure → layout → render。
     */
    public void renderFrame(GuiGraphicsExtractor extractor, float screenWidth, float screenHeight) {
        CURRENT.set(this);
        try {
            long now = System.nanoTime();
            float deltaTime = this.lastFrameNanos < 0 ? 0f : (now - this.lastFrameNanos) / 1_000_000_000f;
            this.lastFrameNanos = now;
            deltaTime = Math.min(deltaTime, 0.1f);
            for (Animatable<?> animatable : this.animatables) {
                if (animatable.tick(deltaTime)) this.dirty = true;
            }
            if (this.dirty || this.hasDirtySlots()) {
                this.recompose();
                this.dirty = false;
            }
            Constraints rootConstraints = new Constraints(0, screenWidth, 0, screenHeight);
            if (this.rootScope != null) {
                List<UIComponent> sorted = this.rootScope.getChildren().stream()
                    .sorted(Comparator.comparingInt(UIComponent::renderingPriority))
                    .toList();
                for (UIComponent child : sorted) {
                    this.renderTree(child, extractor, rootConstraints);
                }
            }
        } finally {
            CURRENT.set(null);
        }
    }

    // ── recompose ──

    private void recompose() {
        if (this.rootScope != null) {
            this.rootScope.clearChildren();
        }
        this.currentIndex = 0;
        this.currentRememberKey = 0;
        for (Slot slot : this.slots) {
            slot.dirty = false;
        }
        if (this.content != null && this.rootScope != null) {
            this.content.accept(this.rootScope);
        }
        while (this.slots.size() > this.currentIndex) {
            Slot removed = this.slots.removeLast();
            for (Ref<?> ref : removed.readStates) {
                ref.removeReader(removed);
            }
            removed.readStates.clear();
        }
    }

    private boolean hasDirtySlots() {
        for (Slot slot : this.slots) {
            if (slot.dirty) return true;
        }
        return false;
    }

    // ── measure → layout → render 遍历 ──

    // 1. 修饰符作用于约束
    // 2. 测量
    // 3. 布局
    // 4. 发射修饰符渲染状态（背景、边框等）
    // 5. 发射组件自身渲染状态
    // 6. 递归子组件（容器组件的 measure/layout/extractRenderState 自行处理）

    private void renderTree(UIComponent component, GuiGraphicsExtractor extractor, Constraints constraints) {
        MeasuredSize size = LayoutHelper.measureChild(component, constraints);
        LayoutRect rect = LayoutRect.of(0, 0, size.width(), size.height());
        LayoutHelper.layoutChild(component, rect.x(), rect.y(), rect.width(), rect.height());
        LayoutHelper.renderChild(component, extractor, rect.x(), rect.y(), rect.width(), rect.height());
    }

    // ── slot ──

    /**
     * slot table 中的一个位置。每个 slot 持有一个组件，
     * 并追踪它读取了哪些状态，以便精确标记脏。
     */
    public static class Slot {
        final Set<Ref<?>> readStates = new HashSet<>();
        @Nullable UIComponent component;
        boolean dirty = true;

        void addReadState(Ref<?> state) {
            this.readStates.add(state);
        }

        void markDirty() {
            this.dirty = true;
        }
    }
}
