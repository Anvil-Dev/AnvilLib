package dev.anvilcraft.lib.v2.rendering.foundation;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Function;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.Nullable;

/** Scoped vertex output used when preparing static feature meshes and projection previews. */
public final class FeatureGeometryCapture implements AutoCloseable {
    private static final ThreadLocal<Function<RenderType, VertexConsumer>> OUTPUT = new ThreadLocal<>();
    private final @Nullable Function<RenderType, VertexConsumer> previous;

    public FeatureGeometryCapture(Function<RenderType, VertexConsumer> output) {
        this.previous = OUTPUT.get();
        OUTPUT.set(output);
    }

    public static @Nullable VertexConsumer getBuffer(RenderType type) {
        var output = OUTPUT.get();
        return output == null ? null : output.apply(type);
    }

    @Override
    public void close() {
        if (this.previous == null) OUTPUT.remove();
        else OUTPUT.set(this.previous);
    }
}
