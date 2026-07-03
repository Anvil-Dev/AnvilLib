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
        try (Transaction transaction = Transaction.openRoot()) {
            int extract = iItemHandler.extract(slot, resource, Integer.MAX_VALUE, transaction);
            transaction.commit();
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
        ItemResource resource = this.iItemHandler.getResource(this.slot);
        try (Transaction transaction = Transaction.openRoot()) {
            if (resource.isEmpty()) {
                this.iItemHandler.insert(this.slot, ItemResource.of(this.simulate), this.simulate.getCount(), transaction);
            } else {
                this.iItemHandler.insert(this.slot, ItemResource.of(resource.toStack()), this.simulate.getCount(), transaction);
            }
            transaction.commit();
        }
    }
}
