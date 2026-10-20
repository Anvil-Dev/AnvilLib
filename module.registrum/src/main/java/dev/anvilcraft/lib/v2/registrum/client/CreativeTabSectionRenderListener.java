package dev.anvilcraft.lib.v2.registrum.client;

import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;

/** 在配方查看器的前景与提示框之前绘制创造栏分区。 */
@EventBusSubscriber(modid = "anvillib_registrum", value = Dist.CLIENT)
public final class CreativeTabSectionRenderListener {
    private CreativeTabSectionRenderListener() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void renderSections(ContainerScreenEvent.Render.Foreground event) {
        if (!(event.getContainerScreen() instanceof Renderer renderer)) return;
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.pose().pushPose();
        try {
            graphics.pose().translate(
                -event.getContainerScreen().getGuiLeft(),
                -event.getContainerScreen().getGuiTop(),
                0.0F
            );
            renderer.anvillib$renderSections(graphics, event.getMouseX(), event.getMouseY());
        } finally {
            graphics.pose().popPose();
        }
    }

    public interface Renderer {
        void anvillib$renderSections(GuiGraphics graphics, int mouseX, int mouseY);
    }
}
