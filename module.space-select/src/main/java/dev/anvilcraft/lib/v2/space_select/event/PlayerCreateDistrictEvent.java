package dev.anvilcraft.lib.v2.space_select.event;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Getter
public class PlayerCreateDistrictEvent extends PlayerEvent {
    private final ItemStack itemStack;
    private final BlockPos start;
    private final BlockPos end;

    public PlayerCreateDistrictEvent(Player player, ItemStack itemStack, BlockPos start, BlockPos end) {
        super(player);
        this.itemStack = itemStack;
        this.start = start;
        this.end = end;
    }
}
