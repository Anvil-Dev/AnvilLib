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

/**
 * 复选框。点击切换 boolean 状态。
 * 16x16 方框，未选中=深色空心，选中=浅色填充。
 */
@Accessors(fluent = true)
@SuppressWarnings(
    {
        "unused",
        "UnusedReturnValue"
    }
)
public class CheckboxComponent implements UIComponent {
    private static final int BOX_COLOR = 0xFF404040;
    private static final int CHECKED_COLOR = 0xFFFFFFFF;
    private static final float SIZE = 16;
    private static final float INSET = 3;

    @Getter
    @Setter
    private Modifier modifier;
    @Setter
    private String label;
    private boolean checked;
    @Setter
    private @Nullable Runnable onToggle;

    @Getter
    private float x, y, width, height;

    public CheckboxComponent(Modifier modifier, String label, boolean checked) {
        this.modifier = modifier;
        this.label = label;
        this.checked = checked;
    }


    @Override
    public List<UIComponent> children() {
        return Collections.emptyList();
    }

    @Override
    public MeasuredSize measure(Constraints constraints) {
        // 方框 + 间距 + 标签文字宽度（简化：估算每字符 7px 宽）
        float labelW = this.label.length() * 7f;
        return MeasuredSize.of(
            constraints.constrainWidth(CheckboxComponent.SIZE + 4 + labelW),
            constraints.constrainHeight(CheckboxComponent.SIZE)
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

        // 外层深灰方块（始终显示）
        extractor.fill(ix, iy, ix + (int) CheckboxComponent.SIZE, iy + (int) CheckboxComponent.SIZE, CheckboxComponent.BOX_COLOR);

        // 选中时中间白色小方块
        if (this.checked) {
            int iix = ix + (int) CheckboxComponent.INSET;
            int iiy = iy + (int) CheckboxComponent.INSET;
            int iiw = (int) CheckboxComponent.SIZE - (int) CheckboxComponent.INSET * 2;
            int iih = (int) CheckboxComponent.SIZE - (int) CheckboxComponent.INSET * 2;
            extractor.fill(iix, iiy, iix + iiw, iiy + iih, CheckboxComponent.CHECKED_COLOR);
        }

        // 标签文字
        // TODO: 用 font.text() 渲染标签
    }

    /**
     * 切换状态。
     */
    public void toggle() {
        this.checked = !this.checked;
        if (this.onToggle != null) this.onToggle.run();
    }

    /**
     * 命中测试包围盒。
     */
    public LayoutRect hitRect() {
        return LayoutRect.of(this.x, this.y, SIZE, SIZE);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        if (event.button() != 0) return false;
        var mc = Minecraft.getInstance();
        int mx = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        int my = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());
        if (this.hitRect().contains(mx, my)) { this.toggle(); mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f)); return true; }
        return false;
    }
}
