package dev.anvilcraft.lib.v2.recipe.cache.item;

import dev.anvilcraft.lib.v2.recipe.cache.ItemCache;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import javax.annotation.Nullable;

/**
 * 物品处理器缓存元素类，继承自抽象缓存元素类
 */
@EqualsAndHashCode(callSuper = false)
public class ItemResourceHandlerCacheElement extends AbstractCacheElement implements ICacheElement {
    /**
     * 物品处理器
     */
    private final ResourceHandler<ItemResource> iItemHandler;

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
    public ItemResourceHandlerCacheElement(ItemCache cache, ResourceHandler<ItemResource> iItemHandler, int slot, Vec3 pos, Vec3 range) {
        super(cache, ItemResourceHandlerCacheElement.extract(iItemHandler, slot).copy());
        this.iItemHandler = iItemHandler;
        this.slot = slot;
        this.pos = pos;
        this.range = range;
    }

    private static ItemStack extract(ResourceHandler<ItemResource> iItemHandler, int slot) {
        ItemResource resource = iItemHandler.getResource(slot);
        if (resource.isEmpty()) return ItemStack.EMPTY;
        try (Transaction transaction = Transaction.openRoot()) {
            int extract = iItemHandler.extract(slot, resource, Integer.MAX_VALUE, transaction);
            return resource.toStack(extract);
        }
    }

    /**
     * 获取指定物品堆的容量
     *
     * @param stack 物品堆
     * @return 容量
     */
    @Override
    public int getCapacity(ItemStack stack) {
        return this.iItemHandler.getCapacityAsInt(this.slot, ItemResource.of(stack));
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
        return this.iItemHandler.isValid(this.slot, ItemResource.of(stack));
    }

    /**
     * 同步更改
     */
    @Override
    public void sync() {
        this.growSimulateStack.clear();
        this.shrinkSimulateStack.clear();
        try (Transaction transaction = Transaction.openRoot()) {
            ItemResource resource = this.iItemHandler.getResource(this.slot);
            ItemResource result = ItemResource.of(this.simulate);
            int amount = this.iItemHandler.getAmountAsInt(this.slot);
            int resultAmount = this.simulate.getCount();
            // 未变化的槽位无需取出再放回；相同物品只同步差量，兼容拒绝插入的输出槽。
            if (resource.equals(result)) {
                int difference = resultAmount - amount;
                if (difference == 0) return;
                int changed = difference > 0
                    ? this.iItemHandler.insert(this.slot, result, difference, transaction)
                    : this.iItemHandler.extract(this.slot, resource, -difference, transaction);
                if (changed != Math.abs(difference)) {
                    throw new IllegalStateException("Recipe item cache could not synchronize slot " + this.slot);
                }
                transaction.commit();
                return;
            }
            if (!resource.isEmpty()) {
                if (this.iItemHandler.extract(this.slot, resource, amount, transaction) != amount) {
                    throw new IllegalStateException("Recipe item cache could not extract slot " + this.slot);
                }
            }
            if (!this.simulate.isEmpty()) {
                int inserted = this.iItemHandler.insert(
                    this.slot,
                    result,
                    resultAmount,
                    transaction
                );
                if (inserted != resultAmount) {
                    throw new IllegalStateException("Recipe item cache could not insert slot " + this.slot);
                }
            }
            transaction.commit();
        }
    }
}
