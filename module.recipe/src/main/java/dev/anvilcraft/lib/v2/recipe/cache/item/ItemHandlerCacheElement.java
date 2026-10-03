package dev.anvilcraft.lib.v2.recipe.cache.item;

import dev.anvilcraft.lib.v2.recipe.cache.ItemCache;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import org.jspecify.annotations.Nullable;

/**
 * 物品处理器缓存元素类，继承自抽象缓存元素类
 */
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class ItemHandlerCacheElement extends AbstractCacheElement implements ICacheElement {
    /**
     * 物品处理器
     */
    @EqualsAndHashCode.Include
    private final IItemHandler iItemHandler;

    /**
     * 槽位
     */
    @EqualsAndHashCode.Include
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
        this.clearStack();
        if (!this.dirty) return;
        ItemStack stack = this.iItemHandler.getStackInSlot(this.slot).copy();
        if (ItemHandlerCacheElement.matches(stack, this.simulate)) {
            this.dirty = false;
            return;
        }
        if (this.iItemHandler instanceof IItemHandlerModifiable modifiable) {
            modifiable.setStackInSlot(this.slot, this.simulate.copy());
            this.dirty = false;
            return;
        }
        if (ItemStack.isSameItemSameComponents(stack, this.simulate)) {
            int difference = this.simulate.getCount() - stack.getCount();
            if (difference > 0) {
                ItemStack addition = this.simulate.copyWithCount(difference);
                this.insertChecked(addition, true);
                this.insertChecked(addition, false);
            } else {
                this.extractChecked(stack, -difference, true);
                this.extractChecked(stack, -difference, false);
            }
            this.dirty = false;
            return;
        }
        if (!stack.isEmpty()) this.extractChecked(stack, stack.getCount(), true);
        if (!this.simulate.isEmpty()) {
            if (!this.iItemHandler.isItemValid(this.slot, this.simulate)
                || this.simulate.getCount() > this.iItemHandler.getSlotLimit(this.slot)) {
                throw new IllegalStateException("Recipe item cache cannot insert into slot " + this.slot);
            }
            if (stack.isEmpty()) this.insertChecked(this.simulate, true);
        }
        if (!stack.isEmpty()) this.extractChecked(stack, stack.getCount(), false);
        if (!this.simulate.isEmpty()) {
            this.insertChecked(this.simulate, false);
        }
        this.dirty = false;
    }

    private void extractChecked(ItemStack expected, int count, boolean simulate) {
        ItemStack extracted = this.iItemHandler.extractItem(this.slot, count, simulate);
        if (extracted.getCount() != count || !ItemStack.isSameItemSameComponents(expected, extracted)) {
            throw new IllegalStateException("Recipe item cache could not extract " + count + " items from slot " + this.slot);
        }
    }

    private void insertChecked(ItemStack stack, boolean simulate) {
        ItemStack remaining = this.iItemHandler.insertItem(this.slot, stack.copy(), simulate);
        if (!remaining.isEmpty()) {
            throw new IllegalStateException("Recipe item cache could not insert all items into slot " + this.slot);
        }
    }

    private static boolean matches(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) return first.isEmpty() && second.isEmpty();
        return first.getCount() == second.getCount() && ItemStack.isSameItemSameComponents(first, second);
    }

    @Override
    public Object getSource() {
        return this.iItemHandler;
    }

    @Override
    public int getSourceSlot() {
        return this.slot;
    }
}
