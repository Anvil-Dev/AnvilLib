package dev.anvilcraft.lib.v2.recipe.cache.item;

import dev.anvilcraft.lib.v2.recipe.cache.ItemCache;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import javax.annotation.Nullable;

/**
 * 物品处理器缓存元素类，继承自抽象缓存元素类
 */
@EqualsAndHashCode(callSuper = false)
public class ItemHandlerCacheElement extends AbstractCacheElement implements ICacheElement {
    /**
     * 物品处理器
     */
    private final IItemHandler iItemHandler;

    /**
     * 槽位
     */
    private final int slot;

    /**
     * 位置
     */
    @Getter
    private final Vec3 pos;

    /**
     * 范围
     */
    @Getter
    private final Vec3 range;

    /**
     * 构造一个新的物品处理器缓存元素
     *
     * @param cache        物品缓存
     * @param iItemHandler 物品处理器
     * @param slot         槽位
     * @param pos          位置
     * @param range        范围
     */
    public ItemHandlerCacheElement(ItemCache cache, IItemHandler iItemHandler, int slot, Vec3 pos, Vec3 range) {
        super(cache, iItemHandler.getStackInSlot(slot).copy());
        this.iItemHandler = iItemHandler;
        this.slot = slot;
        this.pos = pos;
        this.range = range;
    }

    /**
     * 获取指定物品堆的容量
     *
     * @param stack 物品堆
     * @return 容量
     */
    @Override
    public int getCapacity(ItemStack stack) {
        return this.iItemHandler.getSlotLimit(this.slot);
    }

    /**
     * 判断是否为指定物品堆
     *
     * @param stack 物品堆
     * @return 是否为指定物品堆
     */
    @Override
    public boolean is(@Nullable ItemStack stack) {
        if (stack == null) return false;
        return this.iItemHandler.isItemValid(this.slot, stack);
    }

    /**
     * 同步更改
     */
    @Override
    public void sync() {
        this.growSimulateStack.clear();
        this.shrinkSimulateStack.clear();
        ItemStack stack = this.iItemHandler.getStackInSlot(this.slot);
        if (ItemHandlerCacheElement.matches(stack, this.simulate)) return;
        if (this.iItemHandler instanceof IItemHandlerModifiable modifiable) {
            modifiable.setStackInSlot(this.slot, this.simulate.copy());
            return;
        }
        if (ItemStack.isSameItemSameComponents(stack, this.simulate)) {
            int difference = this.simulate.getCount() - stack.getCount();
            if (difference > 0) {
                this.iItemHandler.insertItem(this.slot, this.simulate.copyWithCount(difference), false);
            } else {
                this.iItemHandler.extractItem(this.slot, -difference, false);
            }
            return;
        }
        if (!stack.isEmpty()) {
            this.iItemHandler.extractItem(this.slot, Integer.MAX_VALUE, false);
        }
        if (!this.simulate.isEmpty()) {
            this.iItemHandler.insertItem(this.slot, this.simulate.copy(), false);
        }
    }

    private static boolean matches(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) return first.isEmpty() && second.isEmpty();
        return first.getCount() == second.getCount() && ItemStack.isSameItemSameComponents(first, second);
    }
}
