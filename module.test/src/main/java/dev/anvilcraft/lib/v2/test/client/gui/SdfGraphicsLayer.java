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
        this.draw(graphics, sdf, 150, xMouse, yMouse);

        sdf.stroke(2);
        this.draw(graphics, sdf, 200, xMouse, yMouse);

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
                sdf.box(32, 20 + shift, 40, 20),
                xMouse, yMouse
        );

        this.draw(
                graphics,
                sdf.box(30, 65 + shift, 40, 20)
                        .round(2),
                xMouse, yMouse
        );

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
    }
    
    private void draw(
            GuiGraphicsExtractor graphics,
            SdfGraphics sdf,
            int mouseX, int mouseY
    ) {

        if (sdf.collide(mouseX, mouseY)) {
            sdf.color(0xFFFFFFFF);
        } else {
            sdf.color(0x80808080);
        }

        sdf.draw(graphics);

    }
}
