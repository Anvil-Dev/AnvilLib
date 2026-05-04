package dev.anvilcraft.lib.v2.test.client;

import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import dev.anvilcraft.lib.v2.test.client.gui.SdfGraphicsLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = AnvilLibTest.MOD_ID)
@Mod(value = AnvilLibTest.MOD_ID, dist = Dist.CLIENT)
public class AnvilLibTestClient {
    public AnvilLibTestClient() {
    }


    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent e) {
        e.registerAboveAll(SdfGraphicsLayer.LOCATION, new SdfGraphicsLayer());
    }
}
