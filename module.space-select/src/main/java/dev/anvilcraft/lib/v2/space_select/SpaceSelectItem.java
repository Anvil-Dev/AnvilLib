package dev.anvilcraft.lib.v2.space_select;

import dev.anvilcraft.lib.v2.space_select.client.AnvilLibSpaceSelectClient;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface SpaceSelectItem {
    default void select(ItemStack stack, BlockPos pos) {
        if (!AnvilLibSpaceSelectClient.MANAGER.isSelecting(stack)) {
            AnvilLibSpaceSelectClient.MANAGER.startSelect(stack, pos);
        } else {
            AnvilLibSpaceSelectClient.MANAGER.endSelect(stack, pos);
        }
    }

    default void cancel(ItemStack stack) {
        AnvilLibSpaceSelectClient.MANAGER.clear(stack);
    }

    default void onCreateDistrict(Player player, ItemStack itemStack, BlockPos start, BlockPos end) {
    }
}
