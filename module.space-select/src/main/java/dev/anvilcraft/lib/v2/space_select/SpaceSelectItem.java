package dev.anvilcraft.lib.v2.space_select;

import dev.anvilcraft.lib.v2.space_select.client.AnvilLibSpaceSelectClient;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface SpaceSelectItem {
    default void select(Player player, BlockPos pos) {
        DistrictManager.DistrictKey mainHand = new DistrictManager.DistrictKey(
            player.getInventory().getSelectedSlot(),
            false,
            player.getMainHandItem().getItem()
        );
        DistrictManager.DistrictKey offHand = new DistrictManager.DistrictKey(-1, true, player.getOffhandItem().getItem());
        if (mainHand.check(player) && !AnvilLibSpaceSelectClient.MANAGER.isSelecting(mainHand)) {
            AnvilLibSpaceSelectClient.MANAGER.startSelect(mainHand, pos);
        } else if (mainHand.check(player)) {
            AnvilLibSpaceSelectClient.MANAGER.endSelect(mainHand, pos);
        } else if (offHand.check(player) && !AnvilLibSpaceSelectClient.MANAGER.isSelecting(offHand)) {
            AnvilLibSpaceSelectClient.MANAGER.startSelect(offHand, pos);
        } else if (offHand.check(player)) {
            AnvilLibSpaceSelectClient.MANAGER.endSelect(offHand, pos);
        }
    }

    default void cancel(Player player) {
        DistrictManager.DistrictKey mainHand = new DistrictManager.DistrictKey(
            player.getInventory().getSelectedSlot(),
            false,
            player.getMainHandItem().getItem()
        );
        DistrictManager.DistrictKey offHand = new DistrictManager.DistrictKey(-1, true, player.getOffhandItem().getItem());
        AnvilLibSpaceSelectClient.MANAGER.clear(mainHand);
        AnvilLibSpaceSelectClient.MANAGER.clear(offHand);
    }

    default void onCreateDistrict(Player player, ItemStack itemStack, BlockPos start, BlockPos end) {
    }
}
