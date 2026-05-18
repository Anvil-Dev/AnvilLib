package dev.anvilcraft.lib.v2.ui;

import dev.anvilcraft.lib.v2.ui.component.DropdownComponent;
import dev.anvilcraft.lib.v2.ui.component.ScrollableComponent;
import dev.anvilcraft.lib.v2.ui.component.TextInputComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

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
public class Composition {

    private static final ThreadLocal<Composition> CURRENT = new ThreadLocal<>();

    /** 返回当前线程上的组合实例，可能为 null。 */
    @Nullable
    public static Composition currentOrNull() {
        return CURRENT.get();
    }

    /** 返回当前线程上的组合实例，不存在则抛出异常。 */
    public static Composition current() {
        Composition c = CURRENT.get();
        if (c == null) {
            throw new IllegalStateException("Not inside a composition frame");
        }
        return c;
    }

    // ── slot table ──

    private final List<Slot> slots = new ArrayList<>();
    private int currentIndex;
    private int currentRememberKey;
    private final Map<Integer, Object> rememberedValues = new HashMap<>();

    /** 当前正在 emit 的 slot（在 {@link #emit} 期间设置）。 */
    @Nullable
    Slot currentSlot;

    // ── 状态 ──

    private boolean dirty = true;
    private Consumer<UIScope> content;
    private UIScope rootScope;
    private final List<Animatable> animatables = new ArrayList<>();

    public Composition(UIScope rootScope) {
        this.rootScope = rootScope;
    }

    public void setContent(Consumer<UIScope> content) {
        this.content = content;
    }

    /** 标记组合需要在下一帧 recompose。 */
    public void invalidate() {
        dirty = true;
    }

    /** 注册动画值，每帧自动 tick。动画进行中时自动触发 recompose。 */
    public void watch(Animatable anim) {
        if (!animatables.contains(anim)) {
            animatables.add(anim);
        }
    }

    // ── remember / ref ──

    /**
     * 在多次 recompose 间持久化一个值。init supplier 只在首次组合时调用。
     */
    @SuppressWarnings("unchecked")
    public <T> T remember(Supplier<T> init) {
        int key = currentRememberKey++;
        Object existing = rememberedValues.get(key);
        if (existing != null) {
            return (T) existing;
        }
        T value = init.get();
        rememberedValues.put(key, value);
        return value;
    }

    /** {@code comp.ref(0)} 等价于 {@code comp.remember(() -> new Ref<>(0))}。 */
    public <T> Ref<T> ref(T initialValue) {
        return remember(() -> new Ref<>(initialValue));
    }

    // ── emit ──

    /**
     * 向当前调用位置的 slot 中 emit 一个组件。由组件工厂函数调用。
     */
    public void emit(UIComponent component) {
        Slot slot;
        if (currentIndex < slots.size()) {
            slot = slots.get(currentIndex);
            // 同类型组件保留运行时状态（如滚动位置）
            UIComponent old = slot.component;
            if (old != null && old.getClass() == component.getClass()) {
                copyRuntimeState(old, component);
            }
            slot.component = component;
        } else {
            slot = new Slot();
            slot.component = component;
            slots.add(slot);
        }
        currentSlot = slot;
        currentIndex++;
    }

    /** 将旧组件的运行时状态复制到新组件。 */
    private void copyRuntimeState(UIComponent old, UIComponent replacement) {
        if (old instanceof ScrollableComponent oldSc
                && replacement instanceof ScrollableComponent newSc) {
            newSc.setScrollY(oldSc.getScrollY());
        }
        if (old instanceof TextInputComponent oldTi
                && replacement instanceof TextInputComponent newTi) {
            newTi.setValue(oldTi.getValue());
            newTi.setCursorPos(oldTi.getCursorPos());
            newTi.setFocused(oldTi.isFocused());
        }
        if (old instanceof DropdownComponent oldDd
                && replacement instanceof DropdownComponent newDd) {
            newDd.setOpen(oldDd.isOpen());
        }
    }

    // ── 每帧入口 ──

    /**
     * 每帧从 {@link DeclarativeScreen#extractRenderState} 调用。
     * 若脏则 recompose，然后 measure → layout → render。
     */
    public void renderFrame(GuiGraphicsExtractor extractor, float screenWidth, float screenHeight) {
        CURRENT.set(this);
        try {
            for (Animatable anim : animatables) {
                if (anim.tick()) dirty = true;
            }
            if (dirty || hasDirtySlots()) {
                recompose();
                dirty = false;
            }
            Constraints rootConstraints = new Constraints(0, screenWidth, 0, screenHeight);
            for (UIComponent child : rootScope.getChildren()) {
                renderTree(child, extractor, rootConstraints);
            }
        } finally {
            CURRENT.set(null);
        }
    }

    // ── recompose ──

    private void recompose() {
        rootScope.clearChildren();
        currentIndex = 0;
        currentRememberKey = 0;
        // recompose 前清除 slot 脏标记
        for (Slot slot : slots) {
            slot.dirty = false;
        }
        content.accept(rootScope);
        while (slots.size() > currentIndex) {
            slots.remove(slots.size() - 1);
        }
    }

    private boolean hasDirtySlots() {
        for (Slot slot : slots) {
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
        // 1. Apply modifier to constraints
        Constraints modConstraints = component.modifier().foldIn(
                constraints,
                (c, el) -> el.modifyConstraints(c)
        );

        // 2. Measure
        MeasuredSize size = component.measure(modConstraints);
        size = component.modifier().foldOut(
                size,
                (el, s) -> el.modifyMeasuredSize(component, modConstraints, s)
        );

        // 3. Layout
        LayoutRect rect = LayoutRect.of(0, 0, size.width(), size.height());
        rect = component.modifier().foldOut(
                rect,
                (el, r) -> el.modifyLayout(r)
        );
        component.layout(rect.x(), rect.y(), rect.width(), rect.height());

        // 4. Emit modifier render states (background, border, etc.)
        final LayoutRect finalRect = rect;
        component.modifier().foldOut(
                extractor,
                (el, e) -> {
                    el.emitRenderState(e, finalRect);
                    return e;
                }
        );

        // 5. Emit component's own render states
        component.extractRenderState(extractor);

        // 6. Recurse into children (container components handle their own children
        //    in measure/layout/extractRenderState, but we also walk them here
        //    so the external renderTree call drives the full tree)
    }

    // ── slot ──

    /**
     * slot table 中的一个位置。每个 slot 持有一个组件，
     * 并追踪它读取了哪些状态，以便精确标记脏。
     */
    public static class Slot {
        UIComponent component;
        boolean dirty = true;
        final Set<Ref<?>> readStates = new HashSet<>();

        void addReadState(Ref<?> state) {
            readStates.add(state);
        }

        void markDirty() {
            dirty = true;
        }
    }
}
