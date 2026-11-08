package dev.anvilcraft.lib.v2.recipe.outcome;

import dev.anvilcraft.lib.v2.recipe.cache.ItemCache;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** 保存已经完成随机数量和组件函数计算的产物及其目标位置，供容量规划与最终提交复用。 */
public record PlannedItem(ItemStack stack, Vec3 pos) {
    public PlannedItem {
        stack = stack.copy();
    }

    @Override
    public ItemStack stack() {
        return this.stack.copy();
    }

    /** 使用世界内配方的常规输出规则提交已确定的产物；容器批次应将其交给槽位规划器。 */
    public void accept(InWorldRecipeContext context) {
        if (this.stack.isEmpty()) {
            return;
        }
        ItemCache cache = context.computeIfAbsent(ItemCache.ITEM_CACHE);
        cache.getOutput(this.stack.copy(), this.pos).grow(this.stack.copy(), true);
        context.putAcceptor(ItemCache.ITEM_CACHE.location(), ItemCache.DEFAULT_ACCEPTOR);
    }
}
