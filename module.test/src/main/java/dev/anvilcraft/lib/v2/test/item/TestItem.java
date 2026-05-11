package dev.anvilcraft.lib.v2.test.item;

import dev.anvilcraft.lib.v2.space_select.SpaceSelectItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class TestItem extends Item implements SpaceSelectItem {
    public TestItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            SpaceSelectItem.super.cancel(context.getItemInHand());
        } else {
            SpaceSelectItem.super.select(context.getItemInHand(), context.getClickedPos());
        }
        return InteractionResult.SUCCESS;
    }
}
