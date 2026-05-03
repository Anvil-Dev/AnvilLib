package dev.anvilcraft.lib.v2.multiblock.data.provider;

import dev.anvilcraft.lib.v2.config.ConfigData;
import dev.anvilcraft.lib.v2.multiblock.AnvilLibMultiblock;
import dev.anvilcraft.lib.v2.multiblock.AnvilLibMultiblockConfig;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, AnvilLibMultiblock.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        ConfigData.readConfigClass(this, AnvilLibMultiblockConfig.class);
    }
}
