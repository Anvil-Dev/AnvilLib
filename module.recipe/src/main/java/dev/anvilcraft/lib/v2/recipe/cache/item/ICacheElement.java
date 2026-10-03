package dev.anvilcraft.lib.v2.recipe.cache.item;


import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * 缓存元素接口，继承自缓存输入和输出接口
 */
public interface ICacheElement extends ICacheInput, ICacheOutput {
    /**
     * 获取指定物品堆的容量
     *
     * @param stack 物品堆
     * @return 容量
     */
    int getCapacity(ItemStack stack);

    /**
     * 判断是否为指定物品堆
     *
     * @param stack 物品堆
     * @return 是否为指定物品堆
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean is(@Nullable ItemStack stack);

    /**
     * 判断是否满足指定谓词条件
     *
     * @param stack 物品谓词
     * @return 是否满足条件
     */
    boolean is(Predicate<ItemStack> stack);

    /**
     * 获取位置
     *
     * @return 位置
     */
    Vec3 getPos();

    /**
     * 获取范围
     *
     * @return 范围
     */
    default Vec3 getRange() {
        return new Vec3(0.125, 0.125, 0.125);
    }

    default Object getSource() {
        return this;
    }

    default int getSourceSlot() {
        return -1;
    }

    default boolean isGeneratedOutput() {
        return false;
    }

    default ItemStack getStack() {
        ItemStack[] result = {ItemStack.EMPTY};
        this.apply(stack -> result[0] = stack.copyWithCount(this.getCount()));
        return result[0];
    }

    default ItemConsumption consume(int count) {
        ItemStack before = this.getStack();
        int consumed = count - this.shrink(count);
        return new ItemConsumption(this, before.copyWithCount(consumed), () -> {
            throw new UnsupportedOperationException("This cache element cannot restore reservations");
        });
    }
}
