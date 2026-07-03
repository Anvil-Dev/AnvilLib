package dev.anvilcraft.lib.v2.test.client.gui;

import dev.anvilcraft.lib.v2.rendering.sdf.SdfGraphics;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
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

        var minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof ChatScreen)) return;
        int xMouse  = (int)minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
        int yMouse  = (int)minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());

        SdfGraphics.debug(true);
        var sdf     = SdfGraphics.getInstance()
                    .reset()
                    .rotate(this.timer)
                    .center(true)

                    .stroke(0)
                    .fill();

        this.draw(graphics, sdf, 0, xMouse, yMouse);
        SdfGraphics.debug(false);

        /*sdf.stroke(2);
        this.draw(graphics, sdf, 50, xMouse, yMouse);

        sdf.stroke(0).light(5);
        this.draw(graphics, sdf, 100, xMouse, yMouse);

        sdf.stroke(2);
        this.draw(graphics, sdf, 150, xMouse, yMouse);*/

        // test segment

        /*var min = 25;
        var mx  = 75 + Mth.sin(this.timer * 0.05f) * 25.0f;
        var my  = 75 + Mth.cos(this.timer * 0.05f) * 25.0f;

        if (minecraft.screen != null) {
            sdf
                    .segment(min + 5, min + 5, mx + 5, my + 5)
                    .rotate(0)
                    .round(2.0f).color(0xFFFFFFFF)
                    .draw(graphics)
                    *//*.segment(min, min, mx, my)
                    .light(5.0f)
                    .draw(graphics)*//*;
        }*/

        sdf.reset();
    }

    private void draw(
            GuiGraphicsExtractor graphics,
            SdfGraphics sdf,
            int shift,
            int xMouse, int yMouse
    ) {
        this.draw(
                graphics,
                sdf.box(32, 40 + shift, 40, 20),
                xMouse, yMouse
        );

        this.draw(
                graphics,
                sdf.box(30, 65 + shift, 40, 20)
                        .round(5),
                xMouse, yMouse
        );

        sdf.round(0);

        this.draw(
                graphics,
                sdf.circle(80, 50 + shift, 20),
                xMouse, yMouse
        );

        this.draw(
                graphics,
                sdf.arc(130, 50 + shift, 45, 20, 5),
                xMouse, yMouse
        );

        this.draw(
                graphics,
                sdf.sector(180, 50 + shift, 45, 20, 5),
                xMouse, yMouse
        );

        this.draw(
                graphics,
                sdf.pie(230, 50 + shift, 45, 20),
                xMouse, yMouse
        );

        this.draw(
                graphics,
                sdf.triangleEquilateral(280, 50 + shift, 20),
                xMouse, yMouse
        );

        this.draw(
                graphics,
                sdf.triangleIsosceles(330, 50 + shift, 20, 40),
                xMouse, yMouse
        );
    }
    
    private void draw(
            GuiGraphicsExtractor graphics,
            SdfGraphics sdf,
            int mouseX, int mouseY
    ) {

        if (sdf.collide(mouseX, mouseY, 0.5f)) {
            sdf.color(0xFFFFFFFF);
        } else {
            sdf.color(0x80808080);
        }

        sdf.draw(graphics);

    }
}
