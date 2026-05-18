package dev.anvilcraft.lib.v2.ui;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * 循环渲染工具。将列表中的每个元素映射为一组 UI 组件。
 * 列表顺序稳定时，slot 位置保持稳定。
 *
 * <pre>{@code
 * ForEach.of(scope, items, (s, item) -> {
 *     s.Text(item.name());
 * });
 * }</pre>
 */
public final class ForEach {

    private ForEach() {}

    /** 对列表中的每个元素执行内容函数。 */
    public static <T> void of(UIScope scope, List<T> items, BiConsumer<UIScope, T> content) {
        for (T item : items) {
            content.accept(scope, item);
        }
    }
}
