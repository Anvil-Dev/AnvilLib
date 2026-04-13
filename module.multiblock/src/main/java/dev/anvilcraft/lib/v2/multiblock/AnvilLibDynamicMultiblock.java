package dev.anvilcraft.lib.v2.multiblock;

import dev.anvilcraft.lib.v2.config.ConfigManager;
import dev.anvilcraft.lib.v2.network.register.NetworkRegistrar;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(AnvilLibDynamicMultiblock.MOD_ID)
@EventBusSubscriber(modid = AnvilLibDynamicMultiblock.MOD_ID)
public class AnvilLibDynamicMultiblock {
    public static final String MAIN_ID = "anvillib";
    public static final String MOD_ID = "anvillib_dynamic_multiblock";
    public static final AnvilLibDynamicMultiblockConfig CONFIG = ConfigManager.register(
        AnvilLibDynamicMultiblock.MOD_ID,
        AnvilLibDynamicMultiblockConfig::new
    );

    public AnvilLibDynamicMultiblock(IEventBus modEventBus, ModContainer modContainer) {
    }

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(MAIN_ID, path);
    }

    @SubscribeEvent
    public static void onNetwork(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        NetworkRegistrar.register(registrar, AnvilLibDynamicMultiblock.MOD_ID);
    }
}
