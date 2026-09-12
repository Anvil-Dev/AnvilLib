package dev.anvilcraft.lib.v2.rendering.state;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.render.TextureSetup;
import org.joml.Matrix3x2f;

public interface LibQuadGuiElementRenderState extends LibGuiElementRenderState {
    Matrix3x2f pose();

    float x0();

    float y0();

    float x1();

    float y1();

    int color();

    default void buildVertices(VertexConsumer consumer) {
        consumer.addVertexWith2DPose(this.pose(), this.x0(), this.y0()).setColor(this.color());
        consumer.addVertexWith2DPose(this.pose(), this.x0(), this.y1()).setColor(this.color());
        consumer.addVertexWith2DPose(this.pose(), this.x1(), this.y1()).setColor(this.color());
        consumer.addVertexWith2DPose(this.pose(), this.x1(), this.y0()).setColor(this.color());
    }

    default TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }
}
