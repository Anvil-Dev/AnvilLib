package dev.anvilcraft.lib.v2.wheel;

import dev.anvilcraft.lib.v2.wheel.client.init.LibDynamicUniforms;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ConfigureMainRenderTargetEvent;
import org.jetbrains.annotations.ApiStatus;

@EventBusSubscriber(modid = AnvilLibWheel.MOD_ID, value = Dist.CLIENT)
@ApiStatus.Internal
public class AnvilLibWheel {
    public static final String MOD_ID = "anvillib_wheel";
    public static final String MAIN_ID = "anvillib";
    @Getter
    private static LibDynamicUniforms libDynamicUniforms;

    @SubscribeEvent
    public static void init(ConfigureMainRenderTargetEvent event) {
        AnvilLibWheel.libDynamicUniforms = new LibDynamicUniforms();
    }

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(AnvilLibWheel.MAIN_ID, path);
    }
}
