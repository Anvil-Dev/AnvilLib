package dev.anvilcraft.lib.v2.registrum.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** 在配方查看器的前景与提示框之前绘制创造栏分区。 */
@EventBusSubscriber(modid = "anvillib_registrum", value = Dist.CLIENT)
public final class CreativeTabSectionRenderListener {
    private CreativeTabSectionRenderListener() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void renderSections(ScreenEvent.Render.Foreground event) {
        if (!(event.getScreen() instanceof Renderer renderer)) return;
        GuiGraphicsExtractor graphics = event.getGuiGraphics();
        renderer.anvillib$renderSections(graphics, event.getMouseX(), event.getMouseY());
    }

    public interface Renderer {
        void anvillib$renderSections(GuiGraphicsExtractor graphics, int mouseX, int mouseY);
    }
}
