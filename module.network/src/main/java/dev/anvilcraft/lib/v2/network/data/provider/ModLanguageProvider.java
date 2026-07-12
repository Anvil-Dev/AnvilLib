package dev.anvilcraft.lib.v2.network.data.provider;

import dev.anvilcraft.lib.v2.config.ConfigData;
import dev.anvilcraft.lib.v2.network.AnvilLibNetwork;
import dev.anvilcraft.lib.v2.network.AnvilLibNetworkServerConfig;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, AnvilLibNetwork.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        ConfigData.readConfigClass(this, AnvilLibNetworkServerConfig.class);
    }
}
