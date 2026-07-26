package dev.anvilcraft.lib.v2.space_select.client;

import dev.anvilcraft.lib.v2.space_select.AnvilLibSpaceSelect;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.ApiStatus;

@Mod(value = AnvilLibSpaceSelect.MOD_ID, dist = Dist.CLIENT)
public class AnvilLibSpaceSelectClient {
    @ApiStatus.Internal
    public AnvilLibSpaceSelectClient() {
    }

    public static final ClientDistrictManager MANAGER = new ClientDistrictManager();
}
