package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.LayoutRect;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 滑块。水平拖拽选择范围内的值。
 * 原版风格：深色轨道 + 浅色滑块按钮。
 */
public class SliderComponent implements UIComponent {

    private static final int TRACK_COLOR = 0xFF404040;
    private static final int THUMB_COLOR = 0xFFAAAAAA;
    private static final float TRACK_H = 4;
    private static final float THUMB_W = 8;
    private static final float THUMB_H = 16;

    private Modifier modifier;
    private float value;
    private final float min, max;
    private final float trackWidth;
    private Consumer<Float> onChange;

    private float x, y, width, height;

    public SliderComponent(Modifier modifier, float value, float min, float max, float trackWidth,
                           Consumer<Float> onChange) {
        this.modifier = modifier;
        this.value = Mth.clamp(value, min, max);
        this.min = min;
        this.max = max;
        this.trackWidth = trackWidth;
        this.onChange = onChange;
    }

    public SliderComponent modifier(Modifier m) { this.modifier = m; return this; }
    public float value() { return value; }

    @Override public Modifier modifier() { return modifier; }
    @Override public List<UIComponent> children() { return Collections.emptyList(); }

    @Override
    public MeasuredSize measure(Constraints constraints) {
        return MeasuredSize.of(
                constraints.constrainWidth(trackWidth),
                constraints.constrainHeight(THUMB_H)
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
        int ix = (int) x, iy = (int) y;
        int iw = (int) width;

        // 轨道 — 居中画在 THUMB_H 中间
        int trackY = (int) (y + (THUMB_H - TRACK_H) / 2f);
        extractor.fill(ix, trackY, ix + iw, (int) (trackY + TRACK_H), TRACK_COLOR);

        // 滑块 — 按比例定位
        float ratio = (value - min) / (max - min);
        float thumbX = x + ratio * (width - THUMB_W);
        int tix = (int) thumbX, tiy = (int) y;
        extractor.fill(tix, tiy, tix + (int) THUMB_W, tiy + (int) THUMB_H, THUMB_COLOR);
    }

    /** 根据鼠标 X 坐标更新值。 */
    public void setValueFromMouse(float mouseX) {
        float ratio = Mth.clamp((mouseX - x) / width, 0f, 1f);
        float newValue = min + ratio * (max - min);
        if (newValue != value) {
            value = newValue;
            if (onChange != null) onChange.accept(value);
        }
    }

    /** 命中测试包围盒（整个轨道+滑块区域）。 */
    public LayoutRect hitRect() {
        return LayoutRect.of(x, y, width, height);
    }
}
