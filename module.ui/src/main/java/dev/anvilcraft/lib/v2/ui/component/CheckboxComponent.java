package dev.anvilcraft.lib.v2.ui.component;

import dev.anvilcraft.lib.v2.ui.Constraints;
import dev.anvilcraft.lib.v2.ui.LayoutRect;
import dev.anvilcraft.lib.v2.ui.MeasuredSize;
import dev.anvilcraft.lib.v2.ui.Modifier;
import dev.anvilcraft.lib.v2.ui.UIComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Collections;
import java.util.List;

/**
 * 复选框。点击切换 boolean 状态。
 * 原版风格：16x16 方框 + 选中时内部对勾。
 */
public class CheckboxComponent implements UIComponent {

    private static final int BOX_COLOR   = 0xFF404040;
    private static final int CHECK_COLOR = 0xFFFFFFFF;
    private static final float SIZE      = 16;

    private Modifier modifier;
    private String label;
    private boolean checked;
    private Runnable onToggle;

    private float x, y, width, height;

    public CheckboxComponent(Modifier modifier, String label, boolean checked, Runnable onToggle) {
        this.modifier = modifier;
        this.label = label;
        this.checked = checked;
        this.onToggle = onToggle;
    }

    public CheckboxComponent modifier(Modifier m) { this.modifier = m; return this; }

    @Override public Modifier modifier() { return modifier; }
    @Override public List<UIComponent> children() { return Collections.emptyList(); }

    @Override
    public MeasuredSize measure(Constraints constraints) {
        // 方框 + 间距 + 标签文字宽度（简化：估算每字符 7px 宽）
        float labelW = label.length() * 7f;
        return MeasuredSize.of(
                constraints.constrainWidth(SIZE + 4 + labelW),
                constraints.constrainHeight(SIZE)
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

        // 方框背景
        extractor.fill(ix, iy, ix + (int) SIZE, iy + (int) SIZE, BOX_COLOR);

        // 选中对勾（简化：十字线）
        if (checked) {
            int cx = ix + (int) SIZE / 2, cy = iy + (int) SIZE / 2;
            int s = 4;
            extractor.fill(cx - s, cy, cx, cy + s, CHECK_COLOR); // 左下-中心
            extractor.fill(cx, cy, cx + s + 2, cy - s, CHECK_COLOR); // 中心-右上
        }

        // 标签文字
        // TODO: 用 font.text() 渲染标签
    }

    /** 切换状态。 */
    public void toggle() {
        checked = !checked;
        if (onToggle != null) onToggle.run();
    }

    /** 命中测试包围盒。 */
    public LayoutRect hitRect() {
        return LayoutRect.of(x, y, SIZE, SIZE);
    }
}
