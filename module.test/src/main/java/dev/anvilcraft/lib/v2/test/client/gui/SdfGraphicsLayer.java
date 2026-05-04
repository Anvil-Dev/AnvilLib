package dev.anvilcraft.lib.v2.test.client.gui;

import dev.anvilcraft.lib.v2.rendering.sdf.SdfGraphics;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.gui.GuiLayer;
import org.jspecify.annotations.NonNull;

public class SdfGraphicsLayer implements GuiLayer {

    public static final Identifier LOCATION = AnvilLibTest.of("sdf_graphics");

    @Override
    public void render(
            @NonNull GuiGraphicsExtractor graphics,
            @NonNull DeltaTracker tracker
    ) {
        SdfGraphics.getInstance()
                .center(true)
                .color(0xFFFFFFFF)
                .box(32, 20, 40, 20)
                .fill(graphics)
                .box(30, 50, 40, 20)
                .round(2)
                .fill(graphics)
                .round(0)
                .circle(80, 20, 20)
                .fill(graphics)
                .arc(130, 20, 45, 20, 5)
                .fill(graphics)
                .sector(180, 20, 45, 20, 5)
                .fill(graphics)
                .pie(230, 20, 45, 20)
                .fill(graphics)
                .box(32, 100, 40, 20)
                .stroke(graphics, 2)
                .box(30, 130, 40, 20)
                .round(2)
                .stroke(graphics, 2)
                .round(0)
                .circle(80, 100, 20)
                .stroke(graphics, 2)
                .arc(130, 100, 45, 20, 5)
                .stroke(graphics, 2)
                .sector(180, 100, 45, 20, 5)
                .stroke(graphics, 2)
                .pie(230, 100, 45, 20)
                .stroke(graphics, 2)
                .box(32, 150, 40, 20)
                .light(graphics, 5)
                .box(30, 180, 40, 20)
                .round(2)
                .light(graphics, 5)
                .round(0)
                .circle(80, 150, 20)
                .light(graphics, 5)
                .arc(130, 150, 45, 20, 5)
                .light(graphics, 5)
                .sector(180, 150, 45, 20, 5)
                .light(graphics, 5)
                .pie(230, 150, 45, 20)
                .light(graphics, 5);
    }
}
