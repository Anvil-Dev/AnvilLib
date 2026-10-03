package dev.anvilcraft.lib.v2.recipe.cache.item;

import dev.anvilcraft.lib.v2.recipe.cache.ItemCache;
import dev.anvilcraft.lib.v2.recipe.event.ItemCacheEvent;
import dev.anvilcraft.lib.v2.recipe.mixin.ItemEntityAccessor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 物品实体缓存元素类，继承自抽象缓存元素类
 */
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class ItemEntityCacheElement extends AbstractCacheElement implements ICacheElement {
    /**
     * 物品实体
     */
    @EqualsAndHashCode.Include
    private final ItemEntity entity;

    /**
     * 是否在世界中
     */
    private boolean isInLevel;

    private final boolean generatedOutput;

    /**
     * 位置
     */
    @Getter
    private final Vec3 pos;

    /**
     * 构造一个新的物品实体缓存元素
     *
     * @param cache  物品缓存
     * @param entity 物品实体
     */
    public ItemEntityCacheElement(ItemCache cache, ItemEntity entity) {
        this(cache, entity, false);
    }

    private ItemEntityCacheElement(ItemCache cache, ItemEntity entity, boolean generatedOutput) {
        super(cache, entity.getItem().copy());
        this.pos = generatedOutput ? entity.position() : entity.position().add(0.0, 0.125, 0.0);
        this.entity = entity;
        this.isInLevel = !generatedOutput;
        this.generatedOutput = generatedOutput;
    }

    /**
     * 创建一个新的物品实体缓存元素
     *
     * @param cache 物品缓存
     * @param stack 物品堆
     * @param pos   位置
     * @return 物品实体缓存元素
     */
    public static ItemEntityCacheElement create(ItemCache cache, ItemStack stack, Vec3 pos) {
        ItemEntity itemEntity = new ItemEntity(cache.getLevel(), pos.x, pos.y, pos.z, stack, 0, 0, 0);
        ItemEntityCacheElement element = new ItemEntityCacheElement(cache, itemEntity, true);
        element.simulate.setCount(0);
        return element;
    }

    /**
     * 同步更改
     */
    @Override
    public void sync() {
        this.clearStack();
        if (!this.dirty) return;
        this.dirty = false;
        if (this.simulate.isEmpty()) {
            this.entity.discard();
            return;
        }
        ((ItemEntityAccessor) this.entity).setAge(0);
        this.entity.setItem(this.simulate.copy());
        if (this.isInLevel) return;
        this.isInLevel = true;
        NeoForge.EVENT_BUS.post(new ItemCacheEvent.SpawnItemEntity(this.cache, this.entity));
        if (!this.entity.isRemoved() && !this.entity.getItem().isEmpty()) {
            this.cache.getLevel().addFreshEntity(this.entity);
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
        return this.simulate.isEmpty() ? stack.getMaxStackSize() : this.simulate.getMaxStackSize();
    }

    @Override
    public Object getSource() {
        return this.entity;
    }

    @Override
    public boolean isGeneratedOutput() {
        return this.generatedOutput;
    }
}

