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
@SuppressWarnings(
    {
        "unused",
        "UnusedReturnValue"
    }
)
public class TextInputComponent implements UIComponent, Focusable {
    private static final int BG_COLOR = 0xFF202020;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int PLACEHOLDER_COLOR = 0xFF555555;
    private static final int CURSOR_COLOR = 0xFFFFFFFF;
    private static final float PADDING_H = 4;
    private static final float PADDING_V = 4;
    private static final float WIDTH = 160;
    private final String placeholder;
    @Getter
    @Setter
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

    private float x, y, width, height;

    public TextInputComponent(Modifier modifier, @Nullable String placeholder) {
        this.modifier = modifier;
        this.placeholder = placeholder != null ? placeholder : "";
    }

    public void setValue(@Nullable String value) {
        this.value = value != null ? value : "";
        this.cursorPos = this.value.length();
        this.scrollTo(this.cursorPos);
    }

    public void setCursorPos(int pos) {
        this.cursorPos = Math.clamp(pos, 0, this.value.length());
        this.scrollTo(this.cursorPos);
    }

    private void scrollTo(int pos) {
        if (pos < this.displayPos) { this.displayPos = pos; return; }
        var font = Minecraft.getInstance().font;
        float visibleW = this.width - PADDING_H * 2;
        while (this.displayPos < pos) {
            String segment = this.value.substring(this.displayPos, pos);
            if (font.width(segment) <= visibleW) break;
            this.displayPos++;
        }
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public List<UIComponent> children() {
        return Collections.emptyList();
    }

    @Override
    public MeasuredSize measure(Constraints constraints) {
        var font = Minecraft.getInstance().font;
        return MeasuredSize.of(
            constraints.constrainWidth(TextInputComponent.WIDTH),
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
        int ix = (int) this.x, iy = (int) this.y, iw = (int) this.width, ih = (int) this.height;
        extractor.enableScissor(ix, iy, ix + iw, iy + ih);
        extractor.fill(ix, iy, ix + iw, iy + ih, TextInputComponent.BG_COLOR);

        var font = Minecraft.getInstance().font;
        int textX = ix + (int) TextInputComponent.PADDING_H;
        int textY = (int) (this.y + (this.height + font.lineHeight) / 2f - font.lineHeight);
        boolean hasText = !this.value.isEmpty();

        if (hasText) {
            String visible = this.value.substring(this.displayPos);
            extractor.text(font, visible, textX, textY, TextInputComponent.TEXT_COLOR);

            if (this.focused) {
                String before = this.value.substring(this.displayPos, Math.min(this.cursorPos, this.value.length()));
                int cursorX = textX + font.width(before);
                extractor.fill(cursorX, iy + 2, cursorX + 1, iy + ih - 2, TextInputComponent.CURSOR_COLOR);
            }
        } else {
            extractor.text(font, this.placeholder, textX, textY, TextInputComponent.PLACEHOLDER_COLOR);
        }
        extractor.disableScissor();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() != 0) return false;
        var mc = Minecraft.getInstance();
        int mx = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        int my = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        if (this.hitRect().contains(mx, my)) {
            this.setFocused(true);
            // 根据点击位置设置光标
            var font = Minecraft.getInstance().font;
            float relX = mx - (this.x + PADDING_H);
            String visible = this.value.substring(this.displayPos);
            int pos = this.displayPos;
            for (int i = 0; i < visible.length(); i++) {
                if (font.width(visible.substring(0, i + 1)) > relX) break;
                pos++;
            }
            this.cursorPos = Math.clamp(pos, 0, this.value.length());
            this.scrollTo(this.cursorPos);
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == 259) {
            if (this.cursorPos > 0) {
                this.value = new StringBuilder(this.value).deleteCharAt(this.cursorPos - 1).toString();
                this.cursorPos--;
                this.scrollTo(this.cursorPos);
                this.fireChange();
            }
            return true;
        }
        if (key == 261) {
            if (this.cursorPos < this.value.length()) {
                this.value = new StringBuilder(this.value).deleteCharAt(this.cursorPos).toString();
                this.scrollTo(this.cursorPos);
                this.fireChange();
            }
            return true;
        }
        if (key == 263) {
            if (this.cursorPos > 0) { this.cursorPos--; this.scrollTo(this.cursorPos); }
            return true;
        }
        if (key == 262) {
            if (this.cursorPos < this.value.length()) { this.cursorPos++; this.scrollTo(this.cursorPos); }
            return true;
        }
        if (key == 268) { this.cursorPos = 0; this.scrollTo(this.cursorPos); return true; }
        if (key == 269) { this.cursorPos = this.value.length(); this.scrollTo(this.cursorPos); return true; }
        return false;
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
        this.value = new StringBuilder(this.value).insert(this.cursorPos, text).toString();
        this.cursorPos += text.length();
        this.scrollTo(this.cursorPos);
        this.fireChange();
    }

    private void fireChange() {
        if (this.onChange != null) this.onChange.accept(this.value);
    }

    public LayoutRect hitRect() {
        return LayoutRect.of(this.x, this.y, this.width, this.height);
    }

    public void setDisplayPos(int pos) { this.displayPos = pos; }

    @Override
    public void copyRuntimeState(UIComponent old) {
        if (old instanceof TextInputComponent oldTi) {
            this.setValue(oldTi.value());
            this.setCursorPos(oldTi.cursorPos());
            this.setFocused(oldTi.focused());
            this.setDisplayPos(oldTi.displayPos());
        }
    }
}
