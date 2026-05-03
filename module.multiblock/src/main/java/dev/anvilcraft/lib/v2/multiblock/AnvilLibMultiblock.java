package dev.anvilcraft.lib.v2.multiblock;

import dev.anvilcraft.lib.v2.config.ConfigManager;
import dev.anvilcraft.lib.v2.network.register.NetworkRegistrar;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(AnvilLibMultiblock.MOD_ID)
@EventBusSubscriber(modid = AnvilLibMultiblock.MOD_ID)
public class AnvilLibMultiblock {
    public static final String MAIN_ID = "anvillib";
    public static final String MOD_ID = "anvillib_multiblock";
    public static final AnvilLibMultiblockConfig CONFIG = ConfigManager.register(
        AnvilLibMultiblock.MOD_ID,
        AnvilLibMultiblockConfig::new
    );

    public AnvilLibMultiblock(IEventBus modEventBus, ModContainer modContainer) {
    }

    public static Identifier of(String path) {
        return Identifier.fromNamespaceAndPath(MAIN_ID, path);
    }

    @SubscribeEvent
    public static void onNetwork(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        NetworkRegistrar.register(registrar, AnvilLibMultiblock.MOD_ID);
    }
}
