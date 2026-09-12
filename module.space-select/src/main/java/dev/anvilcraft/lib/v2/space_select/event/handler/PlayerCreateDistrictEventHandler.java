package dev.anvilcraft.lib.v2.space_select.event.handler;

import dev.anvilcraft.lib.v2.space_select.AnvilLibSpaceSelect;
import dev.anvilcraft.lib.v2.space_select.SpaceSelectItem;
import dev.anvilcraft.lib.v2.space_select.event.PlayerCreateDistrictEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.ApiStatus;

@EventBusSubscriber(modid = AnvilLibSpaceSelect.MOD_ID)
@ApiStatus.Internal
public class PlayerCreateDistrictEventHandler {
    @SubscribeEvent
    public static void onPlayerCreateDistrict(PlayerCreateDistrictEvent event) {
        if (!(event.getItemStack().getItem() instanceof SpaceSelectItem item)) {
            return;
        }
        item.onCreateDistrict(event.getEntity(), event.getItemStack(), event.getStart(), event.getEnd());
    }
}
