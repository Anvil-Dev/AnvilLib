package dev.anvilcraft.lib.v2.wheel;

import dev.anvilcraft.lib.v2.wheel.client.init.LibDynamicUniforms;
import lombok.Getter;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ConfigureMainRenderTargetEvent;

@EventBusSubscriber(modid = AnvilLibWheel.MOD_ID, value = Dist.CLIENT)
public class AnvilLibWheel {
    public static final String MOD_ID = "anvillib_wheel";
    @Getter
    private static LibDynamicUniforms libDynamicUniforms;

    @SubscribeEvent
    public static void init(ConfigureMainRenderTargetEvent event) {
        AnvilLibWheel.libDynamicUniforms = new LibDynamicUniforms();
    }

    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(AnvilLibWheel.MOD_ID, path);
    }
}
