package dev.anvilcraft.lib.v2.network.data;

import dev.anvilcraft.lib.v2.network.AnvilLibNetwork;
import dev.anvilcraft.lib.v2.network.data.provider.ModLanguageProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = AnvilLibNetwork.MOD_ID)
public class AnvilLibDatagen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        generator.addProvider(true, new ModLanguageProvider(packOutput));
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Server event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        generator.addProvider(false, new ModLanguageProvider(packOutput));
    }
}
