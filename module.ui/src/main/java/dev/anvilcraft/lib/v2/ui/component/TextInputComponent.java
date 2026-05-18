package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.LayoutRect;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import dev.anvilcraft.lib.v2.ui.input.KeyInputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 单行文本输入框。参照原版 {@code EditBox} 实现。
 * <p>
 * placeholder 作为占位提示（灰色），开始输入后直接替换为输入内容。
 * 字符输入通过 {@link CharacterEvent} 处理，支持所有语言和输入法。
 */
public class TextInputComponent implements UIComponent, KeyInputHandler {

    private static final int BG_COLOR        = 0xFF202020;
    private static final int TEXT_COLOR      = 0xFFFFFFFF;
    private static final int PLACEHOLDER_COLOR = 0xFF555555;
    private static final int CURSOR_COLOR    = 0xFFFFFFFF;
    private static final float PADDING_H     = 4;
    private static final float PADDING_V     = 4;
    private static final float WIDTH         = 160;

    private Modifier modifier;
    private String value = "";
    private final String placeholder;
    private boolean focused;
    private Consumer<String> onChange;
    private int displayPos;
    private int cursorPos;

    private float x, y, width, height;

    public TextInputComponent(Modifier modifier, String placeholder, Consumer<String> onChange) {
        this.modifier = modifier;
        this.placeholder = placeholder != null ? placeholder : "";
        this.onChange = onChange;
    }

    public TextInputComponent modifier(Modifier m) { this.modifier = m; return this; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value != null ? value : ""; this.cursorPos = this.value.length(); }
    public int getCursorPos() { return cursorPos; }
    public void setCursorPos(int pos) { this.cursorPos = Math.clamp(pos, 0, value.length()); }
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
        extractor.fill(ix, iy, ix + iw, iy + ih, BG_COLOR);

        var font = Minecraft.getInstance().font;
        int textX = ix + (int) PADDING_H;
        int textY = (int) (y + (height + font.lineHeight) / 2f - font.lineHeight);
        boolean hasText = !value.isEmpty();

        String display = hasText ? value : placeholder;
        int color = hasText ? TEXT_COLOR : PLACEHOLDER_COLOR;
        extractor.text(font, display, textX, textY, color);

        if (focused && hasText) {
            String before = value.substring(0, Math.min(cursorPos, value.length()));
            int cursorX = (int) (x + PADDING_H + font.width(before));
            extractor.fill(cursorX, iy + 2, cursorX + 1, iy + ih - 2, CURSOR_COLOR);
        }
    }

    // ── 键盘输入 ──

    @Override
    public boolean onKeyPressed(KeyEvent event) {
        int key = event.key();
        if (key == 259) { // 退格
            if (cursorPos > 0) { value = new StringBuilder(value).deleteCharAt(cursorPos - 1).toString(); cursorPos--; fireChange(); }
            return true;
        }
        if (key == 261) { // Delete
            if (cursorPos < value.length()) { value = new StringBuilder(value).deleteCharAt(cursorPos).toString(); fireChange(); }
            return true;
        }
        if (key == 263) { if (cursorPos > 0) cursorPos--; return true; } // ←
        if (key == 262) { if (cursorPos < value.length()) cursorPos++; return true; } // →
        if (key == 268) { cursorPos = 0; return true; } // Home
        if (key == 269) { cursorPos = value.length(); return true; } // End
        return false;
    }

    /** 字符输入——支持所有语言、输入法、小键盘。参照原版 {@code EditBox.charTyped}。 */
    @Override
    public boolean onCharTyped(CharacterEvent event) {
        if (!event.isAllowedChatCharacter()) return false;
        String text = StringUtil.filterText(event.codepointAsString());
        if (text.isEmpty()) return false;
        insertText(text);
        return true;
    }

    private void insertText(String text) {
        value = new StringBuilder(value).insert(cursorPos, text).toString();
        cursorPos += text.length();
        fireChange();
    }

    private void fireChange() {
        if (onChange != null) onChange.accept(value);
    }

    public LayoutRect hitRect() {
        return LayoutRect.of(x, y, width, height);
    }
}
