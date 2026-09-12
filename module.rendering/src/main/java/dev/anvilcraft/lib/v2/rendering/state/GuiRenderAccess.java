package dev.anvilcraft.lib.v2.rendering.state;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.jspecify.annotations.Nullable;

/** 集中访问 1.21.11 的 GUI 队列，避免使用方重复声明访问转换器。 */
public final class GuiRenderAccess {
    private GuiRenderAccess() { }

    public static @Nullable ScreenRectangle scissor(GuiGraphics graphics) {
        return graphics.scissorStack.peek();
    }

    public static void submit(GuiGraphics graphics, GuiElementRenderState state) {
        graphics.guiRenderState.submitGuiElement(state);
    }
}
