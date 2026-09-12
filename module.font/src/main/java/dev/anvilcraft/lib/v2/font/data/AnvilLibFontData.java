package dev.anvilcraft.lib.v2.font.data;

import dev.anvilcraft.lib.v2.font.AnvilLibFont;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.util.Util;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@EventBusSubscriber(modid = AnvilLibFont.MOD_ID)
public class AnvilLibFontData {
    @SubscribeEvent
    public static void onData(GatherDataEvent.Client event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        event.addProvider(new AnvilLibFontLanguageProvider(packOutput, AnvilLibFont.MOD_ID, "en_us"));
    }

    public static class AnvilLibFontLanguageProvider extends LanguageProvider {
        public AnvilLibFontLanguageProvider(PackOutput output, String modid, String locale) {
            super(output, modid, locale);
        }

        @Override
        protected void addTranslations() {
            this.addDesc("screen", "config", "Font Config Screen");
            this.addDesc("screen", "config.family", "Font Family");
            this.addDesc("screen", "config.font", "Font");
            this.addDesc("screen", "config.test", "Font Test");
            this.addDesc("narration", "dropdown.expanded", "Expanded");
            this.addDesc("narration", "dropdown.collapsed", "Collapsed");
        }

        protected void addDesc(String prefix, String key, String value) {
            this.add(Util.makeDescriptionId(prefix, AnvilLibFont.of(key)), value);
        }
    }
}
