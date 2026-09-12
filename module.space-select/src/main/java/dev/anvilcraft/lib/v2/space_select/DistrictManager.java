package dev.anvilcraft.lib.v2.space_select;

import lombok.Getter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Getter
public class DistrictManager {
    private final Map<DistrictKey, District> districtMap = new HashMap<>();

    public void select(DistrictKey districtKey, District district) {
        this.districtMap.put(districtKey, district);
    }

    public void clear(DistrictKey districtKey) {
        this.districtMap.remove(districtKey);
    }

    public record DistrictKey(int slot, boolean offhand, Item item) {
        public boolean check(@Nullable Player player) {
            if (player == null) return false;
            ItemStack offStack = player.getOffhandItem();
            if (this.offhand()) return offStack.is(this.item()) && offStack.getItem() instanceof SpaceSelectItem;
            ItemStack mainStack = player.getInventory().getItem(this.slot());
            return mainStack.is(this.item()) && mainStack.getItem() instanceof SpaceSelectItem;
        }
    }
}
