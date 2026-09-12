package dev.anvilcraft.lib.v2.registrum.util;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 为创造物品栏变体叠加层提供物品栈。 */
public interface CreativeVariantPickerItem {
    /**
     * 返回叠加层中的变体，按网格从左到右、从上到下排列，最多显示 16 项
     *
     * @param source 创造物品栏中的代表物品栈
     */
    List<ItemStack> createCreativePickerVariants(ItemStack source);

    /** 允许实现类根据客户端配置决定是否折叠代表物品。 */
    default boolean isCreativePickerEnabled(ItemStack source) {
        return true;
    }
}
