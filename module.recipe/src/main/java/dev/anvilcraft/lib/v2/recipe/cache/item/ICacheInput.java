package dev.anvilcraft.lib.v2.recipe.cache.item;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 缓存输入接口
 */
public interface ICacheInput {
    /**
     * 减少指定数量的物品
     *
     * @param count 数量
     * @return 剩余数量
     */
    int shrink(int count);

    /**
     * 回滚减少操作
     *
     * @return 回滚的数量
     */
    int rollbackShrink();

    /**
     * 清空操作栈
     */
    void clearStack();

    /**
     * 同步更改
     */
    void sync();

    /**
     * 获取物品数量
     *
     * @return 物品数量
     */
    int getCount();

    void apply(Consumer<ItemStack> consumer);

    default boolean supportsConsumptionReceipts() {
        return false;
    }

    /** Returns copies of the items consumed by the oldest unclaimed shrink operation. */
    default List<ItemStack> getConsumedItems() {
        return List.of();
    }

    /** Claims the oldest shrink receipt without changing the reserved inventory. */
    default void clearConsumedItems() {
    }

    /** Releases the oldest reservation back to its original physical slots. */
    default void restoreConsumedItems() {
        throw new UnsupportedOperationException("This item input cannot restore reservations");
    }

    /** Exposes physical candidates for conservative matching optimizations when supported. */
    default Optional<Map<ICacheElement, Integer>> availableElements() {
        return Optional.empty();
    }
}
