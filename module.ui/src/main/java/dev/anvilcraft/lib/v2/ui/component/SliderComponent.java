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
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 滑块。水平拖拽选择范围内的值。
 * 原版风格：深色轨道 + 浅色滑块按钮。
 */
@Accessors(fluent = true)
@SuppressWarnings(
    {
        "unused",
        "UnusedReturnValue"
    }
)
public class SliderComponent implements UIComponent {
    private static final int TRACK_COLOR = 0xFF404040;
    private static final int THUMB_COLOR = 0xFFAAAAAA;
    private static final float TRACK_H = 4;
    private static final float THUMB_W = 8;
    private static final float THUMB_H = 16;
    private final float min, max;
    private final float trackWidth;
    @Getter
    @Setter
    private Modifier modifier;
    @Getter
    private float value;
    private boolean hovered;
    @Setter
    private @Nullable Consumer<Float> onChange;

    @Getter
    private float x, y, width, height;

    public SliderComponent(Modifier modifier, float value, float min, float max, float trackWidth) {
        this.modifier = modifier;
        this.value = Mth.clamp(value, min, max);
        this.min = min;
        this.max = max;
        this.trackWidth = trackWidth;
    }
    public void setHovered(boolean hovered) { this.hovered = hovered; }

    @Override
    public void updateHover(float mx, float my) { this.hovered = this.hitRect().contains(mx, my); }


    @Override
    public List<UIComponent> children() {
        return Collections.emptyList();
    }

    @Override
    public MeasuredSize measure(Constraints constraints) {
        return MeasuredSize.of(
            constraints.constrainWidth(this.trackWidth),
            constraints.constrainHeight(SliderComponent.THUMB_H)
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
        int ix = (int) this.x, iy = (int) this.y;
        int iw = (int) this.width;

        // 轨道 — 居中画在 THUMB_H 中间
        int trackY = (int) (this.y + (SliderComponent.THUMB_H - SliderComponent.TRACK_H) / 2f);
        extractor.fill(ix, trackY, ix + iw, (int) (trackY + SliderComponent.TRACK_H), SliderComponent.TRACK_COLOR);

        // 滑块 — 按比例定位
        float ratio = (this.value - this.min) / (this.max - this.min);
        float thumbX = this.x + ratio * (this.width - SliderComponent.THUMB_W);
        int tix = (int) thumbX, tiy = (int) this.y;
        int thumbColor = this.hovered ? 0xFFCCCCCC : SliderComponent.THUMB_COLOR;
        extractor.fill(tix, tiy, tix + (int) SliderComponent.THUMB_W, tiy + (int) SliderComponent.THUMB_H, thumbColor);
    }

    /**
     * 根据鼠标 X 坐标更新值。
     */
    public void setValueFromMouse(float mouseX) {
        float ratio = Mth.clamp((mouseX - this.x) / Math.max(this.width - 1, 1), 0f, 1f);
        float newValue = this.min + ratio * (this.max - this.min);
        if (newValue != this.value) {
            this.value = newValue;
            if (this.onChange != null) this.onChange.accept(this.value);
        }
    }

    /**
     * 命中测试包围盒（整个轨道+滑块区域）。
     */
    public LayoutRect hitRect() {
        return LayoutRect.of(this.x, this.y, this.width, this.height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() != 0) return false;
        var mc = Minecraft.getInstance();
        int mx = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        int my = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        if (this.hitRect().contains(mx, my)) { this.setValueFromMouse(mx); mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f)); return true; }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        var mc = Minecraft.getInstance();
        int mx = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        int my = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        if (this.hitRect().contains(mx, my)) { this.setValueFromMouse(mx); return true; }
        return false;
    }
}
