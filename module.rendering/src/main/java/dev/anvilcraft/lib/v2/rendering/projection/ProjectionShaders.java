package dev.anvilcraft.lib.v2.rendering.projection;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.anvilcraft.lib.v2.rendering.AnvilLibRendering;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

@EventBusSubscriber(modid = AnvilLibRendering.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ProjectionShaders {
    private static final ShaderProgram PROGRAM = new ShaderProgram(
        ResourceLocation.fromNamespaceAndPath(AnvilLibRendering.MOD_ID, "core/projection"),
        DefaultVertexFormat.BLOCK, ShaderDefines.EMPTY);
    private static int generation;

    private ProjectionShaders() {
    }

    static ShaderProgram shader() { return PROGRAM; }
    static int generation() { return generation; }

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) { event.registerShader(PROGRAM); }

    @SubscribeEvent
    public static void reload(ModelEvent.BakingCompleted event) {
        generation++;
        ProjectionRenderTypes.clear();
    }
}
