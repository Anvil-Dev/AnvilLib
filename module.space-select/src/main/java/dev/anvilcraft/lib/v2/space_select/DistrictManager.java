package dev.anvilcraft.lib.v2.space_select;

import lombok.Getter;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

@Getter
public class DistrictManager {
    private final Map<ItemStack, District> districtMap = new HashMap<>();

    public void select(ItemStack stack, District district) {
        this.districtMap.put(stack, district);
    }

    public void clear(ItemStack stack) {
        this.districtMap.remove(stack);
    }
}
