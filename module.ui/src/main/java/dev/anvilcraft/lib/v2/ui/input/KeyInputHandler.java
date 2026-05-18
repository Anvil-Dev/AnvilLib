package dev.anvilcraft.lib.v2.ui.input;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

/**
 * 可接收键盘输入的组件接口。
 * 由 {@link dev.anvilcraft.lib.v2.ui.DeclarativeScreen} 的焦点系统驱动。
 */
public interface KeyInputHandler {

    /**
     * 控制键按下时调用。返回 true 表示已处理。
     */
    boolean onKeyPressed(KeyEvent event);

    /**
     * 字符输入时调用（支持所有语言、输入法、小键盘）。
     * 返回 true 表示已处理。
     */
    boolean onCharTyped(CharacterEvent event);
}
