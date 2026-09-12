package dev.anvilcraft.lib.v2.recipe.data.gen;

import dev.anvilcraft.lib.v2.recipe.AnvilLibRecipe;
import dev.anvilcraft.lib.v2.recipe.data.gen.provider.ModLanguageProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.ApiStatus;

@EventBusSubscriber(modid = AnvilLibRecipe.MOD_ID)
@ApiStatus.Internal
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
