package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.LayoutRect;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import dev.anvilcraft.lib.v2.ui.input.KeyInputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 单行文本输入框。支持键盘输入、光标、Backspace/Delete、Home/End。
 * 需要焦点：点击获得焦点，点击外部失去焦点。
 */
public class TextFieldComponent implements UIComponent, KeyInputHandler {

    private static final int BG_COLOR       = 0xFF202020;
    private static final int TEXT_COLOR     = 0xFFFFFFFF;
    private static final int CURSOR_COLOR   = 0xFFFFFFFF;
    private static final float PADDING_H    = 4;
    private static final float PADDING_V    = 4;
    private static final float WIDTH        = 160;

    private Modifier modifier;
    private final StringBuilder buffer = new StringBuilder();
    private int cursorPos;
    private boolean focused;
    private Consumer<String> onChange;

    private float x, y, width, height;

    public TextFieldComponent(Modifier modifier, String initialText,
                               Consumer<String> onChange) {
        this.modifier = modifier;
        this.buffer.append(initialText != null ? initialText : "");
        this.cursorPos = this.buffer.length();
        this.onChange = onChange;
    }

    public TextFieldComponent modifier(Modifier m) { this.modifier = m; return this; }
    public String text() { return buffer.toString(); }
    public void setFocused(boolean focused) { this.focused = focused; }
    public boolean isFocused() { return focused; }

    @Override public Modifier modifier() { return modifier; }
    @Override public List<UIComponent> children() { return Collections.emptyList(); }

    @Override
    public MeasuredSize measure(Constraints constraints) {
        var font = Minecraft.getInstance().font;
        return MeasuredSize.of(
                constraints.constrainWidth(WIDTH),
                constraints.constrainHeight(font.lineHeight + PADDING_V * 2)
        );
    }

    @Override
    public void layout(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor) {
        int ix = (int) x, iy = (int) y, iw = (int) width, ih = (int) height;

        // 背景
        extractor.fill(ix, iy, ix + iw, iy + ih, BG_COLOR);

        // 显示文字
        var font = Minecraft.getInstance().font;
        int textX = ix + (int) PADDING_H;
        int textY = (int) (y + (height + font.lineHeight) / 2f - font.lineHeight);
        extractor.text(font, buffer.toString(), textX, textY, TEXT_COLOR);

        // 光标（聚焦时绘制）
        if (focused) {
            String textBefore = buffer.substring(0, cursorPos);
            int cursorX = (int) (x + PADDING_H + font.width(textBefore));
            extractor.fill(cursorX, iy + 2, cursorX + 1, iy + ih - 2, CURSOR_COLOR);
        }
    }

    // ── KeyInputHandler ──

    @Override
    public boolean onKeyPressed(KeyEvent event) {
        int key = event.key();
        int mods = event.modifiers();

        // 控制键
        if (key == 259) { // Backspace
            if (cursorPos > 0) { buffer.deleteCharAt(cursorPos - 1); cursorPos--; fireChange(); }
            return true;
        }
        if (key == 261) { // Delete
            if (cursorPos < buffer.length()) { buffer.deleteCharAt(cursorPos); fireChange(); }
            return true;
        }
        if (key == 263) { if (cursorPos > 0) cursorPos--; return true; } // Left
        if (key == 262) { if (cursorPos < buffer.length()) cursorPos++; return true; } // Right
        if (key == 268) { cursorPos = 0; return true; } // Home
        if (key == 269) { cursorPos = buffer.length(); return true; } // End

        // 可打印字符 (GLFW key codes: 32=Space, 39=',', 44='.', 45='-', 47='/', 48-57=0-9, 59=';', 61='=', 65-90=A-Z, 91-93=[\])
        char c = glfwKeyToChar(key, (mods & 0x1) != 0);
        if (c != 0) {
            buffer.insert(cursorPos, c);
            cursorPos++;
            fireChange();
            return true;
        }
        return false;
    }

    /** GLFW 按键码 → 字符（英文键盘布局）。 */
    private static char glfwKeyToChar(int key, boolean shift) {
        if (key >= 65 && key <= 90) return (char) (shift ? key : key + 32); // A-Z / a-z
        if (key >= 48 && key <= 57) return (char) (shift ? ")!@#$%^&*(".charAt(key - 48) : key); // 0-9
        return (char) switch (key) {
            case 32 -> ' '; case 39 -> '\''; case 44 -> ','; case 45 -> '-';
            case 46 -> '.'; case 47 -> '/'; case 59 -> ';'; case 61 -> '=';
            case 91 -> '['; case 92 -> '\\'; case 93 -> ']'; case 96 -> '`';
            default -> 0;
        };
    }

    private void fireChange() {
        if (onChange != null) onChange.accept(buffer.toString());
    }

    /** 命中测试包围盒。 */
    public LayoutRect hitRect() {
        return LayoutRect.of(x, y, width, height);
    }
}
