package dev.anvilcraft.lib.v2.ui;

/**
 * 可获取键盘焦点的组件接口。
 * 由 {@link DeclarativeScreen} 的焦点系统驱动。
 */
public interface Focusable {
    /**
     * 是否已获取焦点。
     */
    boolean focused();

    /**
     * 设置焦点状态。
     */
    void setFocused(boolean focused);
}
