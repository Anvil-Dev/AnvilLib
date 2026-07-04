package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.LayoutRect;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

@Accessors(fluent = true)
@SuppressWarnings(
    {
        "unused",
        "UnusedReturnValue"
    }
)
public class ButtonComponent implements UIComponent {
    private static final int BG_COLOR = 0xFF404040;
    private static final int BG_HOVER_COLOR = 0xFF606060;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final float PADDING_H = 12;
    private static final float PADDING_V = 6;

    @Getter
    @Setter
    private Modifier modifier;
    @Setter
    private String label;
    @Nullable
    private Runnable onClick;
    private boolean hovered;

    private float x, y, width, height;

    public ButtonComponent(Modifier modifier, String label) {
        this.modifier = modifier;
        this.label = label;
    }

    public ButtonComponent onClick(@Nullable Runnable onClick) {
        this.onClick = onClick;
        return this;
    }

    public void setHovered(boolean hovered) {
        this.hovered = hovered;
    }

    @Override
    public List<UIComponent> children() {
        return Collections.emptyList();
    }

    @Override
    public MeasuredSize measure(Constraints constraints) {
        var font = Minecraft.getInstance().font;
        float textW = font.width(this.label);
        float textH = font.lineHeight;
        return MeasuredSize.of(
            constraints.constrainWidth(textW + ButtonComponent.PADDING_H * 2),
            constraints.constrainHeight(textH + ButtonComponent.PADDING_V * 2)
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
        // 仅在没有自定义 modifier 背景时画默认灰底
        if (this.modifier == Modifier.NONE) {
            int bg = this.hovered ? ButtonComponent.BG_HOVER_COLOR : ButtonComponent.BG_COLOR;
            int ix = (int) this.x, iy = (int) this.y, iw = (int) this.width, ih = (int) this.height;
            extractor.fill(ix, iy, ix + iw, iy + ih, bg);
        }

        var font = Minecraft.getInstance().font;
        float textW = font.width(this.label);
        int textX = (int) (this.x + (this.width - textW) / 2f);
        int textY = (int) (this.y + (this.height - font.lineHeight) / 2f);
        extractor.text(font, this.label, textX, textY, ButtonComponent.TEXT_COLOR, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() != 0) return false;
        var mc = Minecraft.getInstance();
        int mx = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        int my = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        if (this.hitRect().contains(mx, my)) {
            this.click();
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return true;
        }
        return false;
    }

    @Override
    public void updateHover(float mx, float my) {
        this.hovered = this.hitRect().contains(mx, my);
    }

    public LayoutRect hitRect() {
        return LayoutRect.of(this.x, this.y, this.width, this.height);
    }

    public void click() {
        if (this.onClick != null) this.onClick.run();
    }
}
