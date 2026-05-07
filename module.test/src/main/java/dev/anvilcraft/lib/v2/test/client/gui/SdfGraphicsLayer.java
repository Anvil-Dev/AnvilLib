package dev.anvilcraft.lib.v2.test.client.gui;

import dev.anvilcraft.lib.v2.rendering.sdf.SdfGraphics;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
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

        var minecraft = Minecraft.getInstance();
        int xMouse  = (int)minecraft.mouseHandler.getScaledXPos(minecraft.getWindow());
        int yMouse  = (int)minecraft.mouseHandler.getScaledYPos(minecraft.getWindow());

        /*SdfGraphics.getInstance()
                .center(true)
                .color(0xFFFFFFFF)
                .rotate(this.timer)
                .stroke(0)
                .box(32, 40, 40, 20)
                .fill()
                .draw(graphics)
                .box(30, 65, 40, 20)
                .round(2)
                .fill()
                .draw(graphics)
                .round(0)
                .circle(80, 50, 20)
                .fill()
                .draw(graphics)
                .arc(130, 50, 45, 20, 5)
                .fill()
                .draw(graphics)
                .sector(180, 50, 45, 20, 5)
                .fill()
                .draw(graphics)
                .pie(230, 50, 45, 20)
                .fill()
                .draw(graphics)
                .reset();*/
        var sdf     = SdfGraphics.getInstance()
                    .reset()
                    .rotate(this.timer)
                    .center(true)

                    .stroke(0)
                    .fill();

        this.draw(graphics, sdf, 0, xMouse, yMouse);

        sdf.stroke(2);
        this.draw(graphics, sdf, 50, xMouse, yMouse);

        sdf.stroke(0).light(5);
        this.draw(graphics, sdf, 100, xMouse, yMouse);

        sdf.stroke(2);
        this.draw(graphics, sdf, 150, xMouse, yMouse);

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
                sdf.capsule(280, 50 + shift, 8, 10, 18),
                xMouse, yMouse
        );

        this.draw(
                graphics,
                sdf.egg(330, 50 + shift, 2, 10, 12),
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
