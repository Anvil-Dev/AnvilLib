package dev.anvilcraft.lib.v2.ui.input;

import net.minecraft.client.input.KeyEvent;

/**
 * 可接收键盘输入的组件接口。
 * 由 {@link dev.anvilcraft.lib.v2.ui.DeclarativeScreen} 的焦点系统驱动。
 */
public interface KeyInputHandler {

    /** 按键按下时调用。返回 true 表示已处理。 */
    boolean onKeyPressed(KeyEvent event);
}
