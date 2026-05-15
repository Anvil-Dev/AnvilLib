package dev.anvilcraft.lib.v2.test.client;

import dev.anvilcraft.lib.v2.rendering.cachedber.renderer.CachedBlockEntityRenderDispatcher;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import dev.anvilcraft.lib.v2.test.all.TestTiles;
import dev.anvilcraft.lib.v2.test.client.cber.TestCachedRenderer;
import dev.anvilcraft.lib.v2.test.client.gui.SdfGraphicsLayer;
import dev.anvilcraft.lib.v2.test.client.screen.GuiTestScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber(modid = AnvilLibTest.MOD_ID)
@Mod(value = AnvilLibTest.MOD_ID, dist = Dist.CLIENT)
public class AnvilLibTestClient {
    public AnvilLibTestClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        CachedBlockEntityRenderDispatcher.INSTANCE.registerRenderer(TestTiles.TEST_CACHED_RENDERING, new TestCachedRenderer());
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent e) {
        e.registerAboveAll(SdfGraphicsLayer.LOCATION, new SdfGraphicsLayer());
    }

    @SubscribeEvent
    public static void on(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            literal("anvillib_test_client").
                then(
                    literal("screen").
                        executes(_ -> {
                            Minecraft.getInstance().setScreen(new GuiTestScreen());
                            return 1;
                        })
                )
        );
    }
}
