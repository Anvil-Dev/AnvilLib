package dev.anvilcraft.lib.v2.test.client;

import com.mojang.brigadier.arguments.FloatArgumentType;
import dev.anvilcraft.lib.v2.rendering.cachedber.renderer.CachedBlockEntityRenderDispatcher;
import dev.anvilcraft.lib.v2.rendering.event.MainTargetResizeEvent;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import dev.anvilcraft.lib.v2.test.all.TestTiles;
import dev.anvilcraft.lib.v2.test.client.cber.TestCachedRenderer;
import dev.anvilcraft.lib.v2.test.client.compute.ComputeSupport;
import dev.anvilcraft.lib.v2.test.client.gui.SdfGraphicsLayer;
import dev.anvilcraft.lib.v2.test.client.screen.GuiTestScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Arrays;
import java.util.Random;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@EventBusSubscriber(modid = AnvilLibTest.MOD_ID, value = Dist.CLIENT)
@Mod(value = AnvilLibTest.MOD_ID, dist = Dist.CLIENT)
public class AnvilLibTestClient {
    public AnvilLibTestClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
            CachedBlockEntityRenderDispatcher.INSTANCE.registerRenderer(
                TestTiles.TEST_CACHED_RENDERING,
                new TestCachedRenderer()
            )
        );
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent e) {
        e.registerAboveAll(SdfGraphicsLayer.LOCATION, new SdfGraphicsLayer());
    }

    @SubscribeEvent
    public static void on(MainTargetResizeEvent event) {
        ComputeSupport.INSTANCE.resize(event.getNewWidth(), event.getNewHeight());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void on(RenderLevelStageEvent.AfterLevel level) {
        ComputeSupport.INSTANCE.computeBlur();
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
                ).
                then(
                    literal("compute").
                        then(
                            literal("add").
                                then(
                                    argument("what", FloatArgumentType.floatArg()).
                                        executes(
                                            ctx -> {
                                                try {
                                                    float[] fs = new float[32];
                                                    Random random = new Random(System.nanoTime());
                                                    for (int i = 0; i < 32; i++) {
                                                        fs[i] = random.nextInt(0, 10);
                                                    }
                                                    float with = ctx.getArgument("what", Float.class);
                                                    float[] add = ComputeSupport.INSTANCE.add(fs, with);
                                                    String a = Arrays.toString(fs);
                                                    String result = Arrays.toString(add);
                                                    System.out.println("a = " + a);
                                                    System.out.println("result = " + result);
                                                } catch (Throwable ex) {
                                                    ex.printStackTrace();
                                                }
                                                return 0;
                                            }
                                        )
                                )
                        )
                )
        );
    }
}
