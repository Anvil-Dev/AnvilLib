package dev.anvilcraft.lib.v2.explosion.data;

import dev.anvilcraft.lib.v2.explosion.AnvilLibExplosion;
import dev.anvilcraft.lib.v2.explosion.data.provider.ModLanguageProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.ApiStatus;

@EventBusSubscriber(modid = AnvilLibExplosion.MOD_ID)
@ApiStatus.Internal
public class AnvilLibDatagen {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        generator.addProvider(true, new ModLanguageProvider(packOutput));
    }
}
