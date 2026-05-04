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

    private float timer;

    @Override
    public void render(
            @NonNull GuiGraphicsExtractor graphics,
            @NonNull DeltaTracker tracker
    ) {
        this.timer += tracker.getGameTimeDeltaTicks();

        SdfGraphics.getInstance()
                .center(true)
                .color(0xFFFFFFFF)
                .rotate(this.timer)
                .stroke(0)
                .box(32, 40, 40, 20)
                .fill(graphics)
                .box(30, 65, 40, 20)
                .round(2)
                .fill(graphics)
                .round(0)
                .circle(80, 50, 20)
                .fill(graphics)
                .arc(130, 50, 45, 20, 5)
                .fill(graphics)
                .sector(180, 50, 45, 20, 5)
                .fill(graphics)
                .pie(230, 50, 45, 20)
                .fill(graphics)

                .stroke(2)
                .box(32, 90, 40, 20)
                .fill(graphics)
                .box(30, 115, 40, 20)
                .round(2)
                .fill(graphics)
                .round(0)
                .circle(80, 100, 20)
                .fill(graphics)
                .arc(130, 100, 45, 20, 5)
                .fill(graphics)
                .sector(180, 100, 45, 20, 5)
                .fill(graphics)
                .pie(230, 100, 45, 20)
                .fill(graphics)

                .stroke(0)
                .box(32, 140, 40, 20)
                .light(graphics, 5)
                .box(30, 165, 40, 20)
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
                .light(graphics, 5)

                .stroke(2)
                .box(32, 190, 40, 20)
                .light(graphics, 5)
                .box(30, 215, 40, 20)
                .round(2)
                .light(graphics, 5)
                .round(0)
                .circle(80, 200, 20)
                .light(graphics, 5)
                .arc(130, 200, 45, 20, 5)
                .light(graphics, 5)
                .sector(180, 200, 45, 20, 5)
                .light(graphics, 5)
                .pie(230, 200, 45, 20)
                .light(graphics, 5);
    }
}
