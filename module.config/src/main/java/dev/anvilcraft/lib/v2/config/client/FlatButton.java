package dev.anvilcraft.lib.v2.config.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** 保留原版按钮的键盘、音效和朗读行为，仅替换绘制。 */
final class FlatButton extends Button {
    boolean selected;
    boolean dropdown;
    boolean expanded;
    private long scrollStarted;

    FlatButton(Component text, Runnable action) {
        this(text, button -> action.run());
    }

    FlatButton(Component text, OnPress action) {
        super(0, 0, 120, 24, text, action, DEFAULT_NARRATION);
        setTooltip(Tooltip.create(text));
    }

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX(), getY(), getRight(), getBottom(),
            selected ? 0xFF293F5B : isHoveredOrFocused() && active ? 0xFF344152 : 0xFF252D39);
        if (selected || isFocused()) graphics.fill(getX(), getY(), getX() + 2, getBottom(), 0xFF73AEF5);
        if (dropdown) {
            int border = expanded || isHoveredOrFocused() ? 0xFF73AEF5 : 0xFF53637A;
            graphics.fill(getX(), getY(), getRight(), getY() + 1, border);
            graphics.fill(getX(), getBottom() - 1, getRight(), getBottom(), border);
            graphics.fill(getX(), getY(), getX() + 1, getBottom(), border);
            graphics.fill(getRight() - 1, getY(), getRight(), getBottom(), border);
            int arrowX = getRight() - 11;
            int arrowY = getY() + getHeight() / 2 - 1;
            for (int row = 0; row < 3; row++) {
                int halfWidth = expanded ? row : 2 - row;
                graphics.fill(arrowX - halfWidth, arrowY + row, arrowX + halfWidth + 1, arrowY + row + 1, 0xFFABB7C8);
            }
        }
        var font = Minecraft.getInstance().font;
        String text = getMessage().getString();
        int available = Math.max(1, getWidth() - (dropdown ? 26 : 14));
        int overflow = font.width(text) - available;
        int offset = 0;
        if (overflow > 0 && (selected || isHoveredOrFocused())) {
            long now = System.nanoTime() / 1_000_000;
            if (scrollStarted == 0) scrollStarted = now;
            double travel = overflow / 24.0 * 1000;
            double phase = (now - scrollStarted) % (2 * travel + 1600);
            double position = phase < travel + 800 ? phase - 800
                : phase < travel + 1600 ? travel : 2 * travel + 1600 - phase;
            offset = (int) (Math.clamp(position, 0, travel) / travel * overflow);
        } else {
            scrollStarted = 0;
            if (overflow > 0) text = font.plainSubstrByWidth(text, Math.max(0, available - 10)) + "…";
        }
        graphics.enableScissor(getX() + 7, getY(), getX() + 7 + available, getBottom());
        graphics.drawString(font, text, getX() + 7 - offset, getY() + (getHeight() - 9) / 2,
            active ? 0xFFE8EDF5 : 0xFF79818E, false);
        graphics.disableScissor();
    }
}
