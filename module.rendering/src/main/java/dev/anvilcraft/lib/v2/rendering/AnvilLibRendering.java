package dev.anvilcraft.lib.v2.rendering;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.jetbrains.annotations.ApiStatus;

@Slf4j
@Mod(value = AnvilLibRendering.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(Dist.CLIENT)
@ApiStatus.Internal
public class AnvilLibRendering {
    public static final boolean DEBUG = System.getProperty("anvillib.rendering.debugMode") != null;
    public static final String MODID = "anvillib_rendering";

    public AnvilLibRendering(IEventBus modBus) {
    }

    public static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    @SubscribeEvent
    public static void on(RegisterClientReloadListenersEvent event) {
        // #P7e: register ALRComputeShaderManager.INSTANCE via event.registerReloadListener(...)
    }
}
