package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.Focusable;
import dev.anvilcraft.lib.v2.ui.LayoutRect;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

@Accessors(fluent = true)
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class TextInputComponent implements UIComponent, Focusable {

    private static final int BG_COLOR = 0xFF202020;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int PLACEHOLDER_COLOR = 0xFF555555;
    private static final int CURSOR_COLOR = 0xFFFFFFFF;
    private static final int HIGHLIGHT_COLOR = 0xFF8888FF;
    private static final float PADDING_H = 4;
    private static final float PADDING_V = 4;
    private static final float WIDTH = 160;
    private final String placeholder;
    @Getter @Setter
    private Modifier modifier;
    @Getter
    private String value = "";
    @Getter
    private boolean focused;
    @Setter
    private @Nullable Consumer<String> onChange;
    @Getter
    private int displayPos;
    @Getter
    private int cursorPos;
    @Getter
    private int highlightPos;

    private float x, y, width, height;

    public TextInputComponent(Modifier modifier, @Nullable String placeholder) {
        this.modifier = modifier;
        this.placeholder = placeholder != null ? placeholder : "";
    }

    public void setValue(@Nullable String value) {
        this.value = value != null ? value : "";
        this.cursorPos = this.value.length();
        this.highlightPos = this.cursorPos;
        this.scrollTo(this.cursorPos);
    }

    public void setCursorPos(int pos) {
        this.setCursorPos(pos, false);
    }

    public void setCursorPos(int pos, boolean extendSelection) {
        this.cursorPos = Math.clamp(pos, 0, this.value.length());
        if (!extendSelection) this.highlightPos = this.cursorPos;
        this.scrollTo(this.cursorPos);
    }

    private void scrollTo(int pos) {
        if (pos < this.displayPos) { this.displayPos = pos; return; }
        var font = Minecraft.getInstance().font;
        float visibleW = this.width - PADDING_H * 2;
        // 光标右溢出时，右移 displayPos 直到光标可见
        while (this.displayPos < pos && font.width(this.value.substring(this.displayPos, pos)) > visibleW) {
            this.displayPos++;
        }
    }

    public void setFocused(boolean focused) { this.focused = focused; }
    public void setDisplayPos(int pos) { this.displayPos = pos; }

    /** 获取选中文本。 */
    public String getHighlighted() {
        int start = Math.min(this.cursorPos, this.highlightPos);
        int end = Math.max(this.cursorPos, this.highlightPos);
        return this.value.substring(start, end);
    }

    @Override public List<UIComponent> children() { return Collections.emptyList(); }

    @Override
    public MeasuredSize measure(Constraints constraints) {
        var font = Minecraft.getInstance().font;
        return MeasuredSize.of(constraints.constrainWidth(WIDTH), constraints.constrainHeight(font.lineHeight + PADDING_V * 2));
    }

    @Override
    public void layout(float x, float y, float width, float height) {
        this.x = x; this.y = y; this.width = width; this.height = height;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor) {
        int ix = (int) this.x, iy = (int) this.y, iw = (int) this.width, ih = (int) this.height;
        extractor.enableScissor(ix, iy, ix + iw, iy + ih);
        extractor.fill(ix, iy, ix + iw, iy + ih, BG_COLOR);

        var font = Minecraft.getInstance().font;
        int textX = ix + (int) PADDING_H;
        int textY = (int) (this.y + (this.height + font.lineHeight) / 2f - font.lineHeight);
        boolean hasText = !this.value.isEmpty();

        if (hasText) {
            String visible = this.value.substring(this.displayPos);
            int selStart = Math.min(this.cursorPos, this.highlightPos) - this.displayPos;
            int selEnd = Math.max(this.cursorPos, this.highlightPos) - this.displayPos;

            if (selStart < selEnd) {
                // 选中区域前
                if (selStart > 0) {
                    String before = visible.substring(0, Math.min(selStart, visible.length()));
                    extractor.text(font, before, textX, textY, TEXT_COLOR);
                    textX += font.width(before);
                }
                // 选中区域（反色高亮）
                String selected = visible.substring(Math.max(0, selStart), Math.min(selEnd, visible.length()));
                int selW = font.width(selected);
                if (selW > 0) {
                    extractor.fill(textX, iy + 1, textX + selW, iy + ih - 1, HIGHLIGHT_COLOR);
                    extractor.text(font, selected, textX, textY, TEXT_COLOR);
                    textX += selW;
                }
                // 选中区域后
                if (selEnd < visible.length()) {
                    extractor.text(font, visible.substring(selEnd), textX, textY, TEXT_COLOR);
                }
            } else {
                extractor.text(font, visible, textX, textY, TEXT_COLOR);
            }

            if (this.focused) {
                String before = this.value.substring(this.displayPos, Math.min(this.cursorPos, this.value.length()));
                int cursorX = ix + (int) PADDING_H + font.width(before);
                extractor.fill(cursorX, iy + 2, cursorX + 1, iy + ih - 2, CURSOR_COLOR);
            }
        } else {
            extractor.text(font, this.placeholder, textX, textY, PLACEHOLDER_COLOR);
        }
        extractor.disableScissor();
    }

    // ── 鼠标 ──

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() != 0) return false;
        var mc = Minecraft.getInstance();
        int mx = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        if (this.hitRect().contains(mx, (float) mc.mouseHandler.getScaledYPos(mc.getWindow()))) {
            this.setFocused(true);
            int pos = posFromMouse(mx);
            this.setCursorPos(pos, event.hasShiftDown());
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (event.button() != 0) return false;
        if (!this.focused) return false;
        var mc = Minecraft.getInstance();
        int mx = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        this.setCursorPos(posFromMouse(mx), true);
        return true;
    }

    private int posFromMouse(int mx) {
        var font = Minecraft.getInstance().font;
        float relX = mx - (this.x + PADDING_H);
        String visible = this.value.substring(this.displayPos);
        float visibleW = font.width(visible);
        // 鼠标在可见区域外右侧 → 定位到文本末尾，scrollTo 会自动跟进
        if (relX >= visibleW) return this.value.length();
        // 鼠标在可见区域外左侧 → 逐字符左移 displayPos
        if (relX <= 0) return Math.max(0, this.displayPos - 1);
        int pos = this.displayPos;
        for (int i = 0; i < visible.length(); i++) {
            if (font.width(visible.substring(0, i + 1)) > relX) break;
            pos++;
        }
        return Math.clamp(pos, 0, this.value.length());
    }

    // ── 键盘 ──

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        boolean shift = event.hasShiftDown();
        boolean ctrl = event.hasControlDownWithQuirk();

        // 剪贴板操作（默认分支处理，因 key code 不在 259-269 范围）
        if (key < 259 || key > 269) {
            if (event.isSelectAll()) { this.setCursorPos(this.value.length()); this.highlightPos = 0; return true; }
            if (event.isCopy()) { Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted()); return true; }
            if (event.isPaste()) { this.insertText(StringUtil.filterText(Minecraft.getInstance().keyboardHandler.getClipboard())); return true; }
            if (event.isCut()) {
                Minecraft.getInstance().keyboardHandler.setClipboard(this.getHighlighted());
                this.insertText("");
                return true;
            }
            return false;
        }

        if (key == 259) { // Backspace
            if (this.highlightPos != this.cursorPos) { this.insertText(""); return true; }
            if (this.cursorPos > 0) { this.deleteChars(-1); return true; }
            return true;
        }
        if (key == 261) { // Delete
            if (this.highlightPos != this.cursorPos) { this.insertText(""); return true; }
            if (this.cursorPos < this.value.length()) { this.deleteChars(1); return true; }
            return true;
        }
        if (key == 262) { // Right
            if (ctrl) { this.setCursorPos(this.getWordPosition(1), shift); }
            else { this.setCursorPos(this.nextCodepointPos(1), shift); }
            return true;
        }
        if (key == 263) { // Left
            if (ctrl) { this.setCursorPos(this.getWordPosition(-1), shift); }
            else { this.setCursorPos(this.nextCodepointPos(-1), shift); }
            return true;
        }
        if (key == 268) { this.setCursorPos(0, shift); return true; } // Home
        if (key == 269) { this.setCursorPos(this.value.length(), shift); return true; } // End
        return false;
    }

    private void deleteChars(int dir) {
        int target = this.nextCodepointPos(dir);
        int start = Math.min(this.cursorPos, target);
        int end = Math.max(this.cursorPos, target);
        if (start == end) return;
        this.value = new StringBuilder(this.value).delete(start, end).toString();
        this.setCursorPos(start);
        this.fireChange();
    }

    private int nextCodepointPos(int dir) {
        if (dir > 0) {
            if (this.cursorPos >= this.value.length()) return this.value.length();
            int pos = this.cursorPos + 1;
            if (pos < this.value.length() && Character.isLowSurrogate(this.value.charAt(pos))) pos++;
            return pos;
        } else {
            if (this.cursorPos <= 0) return 0;
            int pos = this.cursorPos - 1;
            if (pos > 0 && Character.isHighSurrogate(this.value.charAt(pos - 1))) pos--;
            return pos;
        }
    }

    private int getWordPosition(int dir) {
        if (this.value.isEmpty()) return 0;
        if (dir < 0) {
            int i = this.cursorPos;
            while (i > 0 && this.value.charAt(i - 1) == ' ') i--;
            while (i > 0 && this.value.charAt(i - 1) != ' ') i--;
            return i;
        }
        int i = this.cursorPos;
        while (i < this.value.length() && this.value.charAt(i) != ' ') i++;
        while (i < this.value.length() && this.value.charAt(i) == ' ') i++;
        return i;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!event.isAllowedChatCharacter()) return false;
        String text = StringUtil.filterText(event.codepointAsString());
        if (text.isEmpty()) return false;
        this.insertText(text);
        return true;
    }

    private void insertText(String text) {
        if (this.highlightPos != this.cursorPos) {
            int start = Math.min(this.cursorPos, this.highlightPos);
            int end = Math.max(this.cursorPos, this.highlightPos);
            this.value = new StringBuilder(this.value).replace(start, end, text).toString();
            this.cursorPos = start + text.length();
        } else {
            this.value = new StringBuilder(this.value).insert(this.cursorPos, text).toString();
            this.cursorPos += text.length();
        }
        this.highlightPos = this.cursorPos;
        this.scrollTo(this.cursorPos);
        this.fireChange();
    }

    private void fireChange() {
        if (this.onChange != null) this.onChange.accept(this.value);
    }

    public LayoutRect hitRect() {
        return LayoutRect.of(this.x, this.y, this.width, this.height);
    }

    @Override
    public void copyRuntimeState(UIComponent old) {
        if (old instanceof TextInputComponent oldTi) {
            this.setValue(oldTi.value());
            this.setCursorPos(oldTi.cursorPos());
            this.highlightPos = oldTi.highlightPos();
            this.setFocused(oldTi.focused());
            this.setDisplayPos(oldTi.displayPos());
        }
    }
}
