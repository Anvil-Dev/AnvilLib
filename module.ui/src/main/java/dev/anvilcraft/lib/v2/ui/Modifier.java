package dev.anvilcraft.lib.v2.ui;

import dev.anvilcraft.lib.v2.ui.modifier.ModifierElement;
import dev.anvilcraft.lib.v2.ui.modifier.SingleElementModifier;

import java.util.function.BiFunction;

/**
 * 链式修饰符 API。修饰符通过 {@link #then} 形成链表。
 * <p>
 * 每个修饰符元素可参与 measure、layout、render 阶段。
 * 调用方通过 {@link #foldIn} / {@link #foldOut} 遍历链。
 */
public interface Modifier {

    Modifier NONE = new Modifier() {
        @Override
        public Modifier then(Modifier other) {
            return other;
        }

        @Override
        public <R> R foldIn(R initial, BiFunction<R, ModifierElement, R> operation) {
            return initial;
        }

        @Override
        public <R> R foldOut(R initial, BiFunction<ModifierElement, R, R> operation) {
            return initial;
        }
    };

    Modifier then(Modifier other);

    <R> R foldIn(R initial, BiFunction<R, ModifierElement, R> operation);

    <R> R foldOut(R initial, BiFunction<ModifierElement, R, R> operation);

    // ── 工厂快捷方法 ──

    /**
     * 返回一个前置了给定元素的新链。
     */
    default Modifier prepend(ModifierElement element) {
        return then(new SingleElementModifier(element));
    }

    default Modifier size(float width, float height) {
        return prepend(ModifierElement.size(width, height));
    }

    default Modifier width(float width) {
        return prepend(ModifierElement.width(width));
    }

    default Modifier height(float height) {
        return prepend(ModifierElement.height(height));
    }

    default Modifier fillMaxWidth() {
        return prepend(ModifierElement.fillMaxWidth());
    }

    default Modifier fillMaxHeight() {
        return prepend(ModifierElement.fillMaxHeight());
    }

    default Modifier fillMaxSize() {
        return prepend(ModifierElement.fillMaxSize());
    }

    default Modifier padding(float all) {
        return prepend(ModifierElement.padding(all));
    }

    default Modifier padding(float horizontal, float vertical) {
        return prepend(ModifierElement.padding(horizontal, vertical));
    }

    default Modifier background(int color) {
        return prepend(ModifierElement.background(color));
    }

    default Modifier border(float width, int color) {
        return prepend(ModifierElement.border(width, color));
    }

    default Modifier border(float width, int color, float round) {
        return prepend(ModifierElement.border(width, color, round));
    }
}
