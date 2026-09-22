package dev.anvilcraft.lib.v2.renderer.projection;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.anvilcraft.lib.v2.renderer.AnvilLibRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

@EventBusSubscriber(modid = AnvilLibRenderer.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ProjectionShaders {
    private static @Nullable ShaderInstance shader;
    private static int generation;

    private ProjectionShaders() {
    }

    static @Nullable ShaderInstance shader() {
        return shader;
    }

    static int generation() {
        return generation;
    }

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
            ResourceLocation.fromNamespaceAndPath(AnvilLibRenderer.MOD_ID, "projection"), DefaultVertexFormat.BLOCK), value -> {
                shader = value;
                generation++;
                ProjectionRenderTypes.clear();
            });
    }
}
