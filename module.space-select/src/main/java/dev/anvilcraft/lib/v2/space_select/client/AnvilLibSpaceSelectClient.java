package dev.anvilcraft.lib.v2.space_select.client;

import dev.anvilcraft.lib.v2.space_select.AnvilLibSpaceSelect;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

@Mod(value = AnvilLibSpaceSelect.MOD_ID, dist = Dist.CLIENT)
public class AnvilLibSpaceSelectClient {
    public static final ClientDistrictManager MANAGER = new ClientDistrictManager();
}
