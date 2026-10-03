package dev.anvilcraft.lib.v2.recipe.cache.item;


import dev.anvilcraft.lib.v2.recipe.cache.ItemCache;
import dev.anvilcraft.lib.v2.recipe.cache.item.operation.CacheOperation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * 抽象缓存元素类，实现了缓存元素接口
 */
public abstract class AbstractCacheElement implements ICacheElement {
    /**
     * 物品缓存
     */
    protected final ItemCache cache;

    /**
     * 物品类型
     */
    protected final ItemStack type;

    /**
     * 模拟的物品堆
     */
    protected ItemStack simulate;

    /**
     * 增加模拟栈
     */
    protected final Deque<CacheOperation> growSimulateStack = new ArrayDeque<>();

    /**
     * 减少模拟栈
     */
    protected final Deque<CacheOperation> shrinkSimulateStack = new ArrayDeque<>();

    private final Map<CacheOperation, ItemStack> shrinkSnapshots = new IdentityHashMap<>();
    private final Map<CacheOperation, ItemStack> growSnapshots = new IdentityHashMap<>();
    private final Set<CacheOperation> releasedReservations = Collections.newSetFromMap(new IdentityHashMap<>());
    protected boolean dirty;

    /**
     * 构造一个新的抽象缓存元素
     *
     * @param cache    物品缓存
     * @param simulate 模拟的物品堆
     */
    protected AbstractCacheElement(ItemCache cache, ItemStack simulate) {
        this.cache = cache;
        this.simulate = simulate;
        this.type = simulate.copyWithCount(1);
    }

    /**
     * 减少指定数量的物品
     *
     * @param count 数量
     * @return 剩余数量
     */
    @Override
    public int shrink(int count) {
        if (count < 0) throw new IllegalArgumentException("Cannot consume a negative item count");
        int shrink = Math.min(this.simulate.getCount(), count);
        CacheOperation operation = new CacheOperation(shrink);
        this.shrinkSnapshots.put(operation, this.simulate.copyWithCount(shrink));
        this.simulate.shrink(shrink);
        this.shrinkSimulateStack.push(operation);
        this.dirty |= shrink > 0;
        return count - shrink;
    }

    @Override
    public ItemConsumption consume(int count) {
        ItemStack before = this.simulate.copy();
        int consumed = count - this.shrink(count);
        CacheOperation operation = this.shrinkSimulateStack.getFirst();
        return new ItemConsumption(this, before.copyWithCount(consumed), () -> this.restoreReservation(operation));
    }

    private void restoreReservation(CacheOperation operation) {
        ItemStack stack = this.shrinkSnapshots.get(operation);
        if (stack == null) throw new IllegalStateException("The item reservation is no longer available");
        if (!this.releasedReservations.add(operation)) return;
        this.restoreStack(stack);
    }

    private void restoreStack(ItemStack stack) {
        if (stack.isEmpty()) return;
        if (this.simulate.isEmpty()) {
            this.simulate = stack.copy();
        } else {
            if (!ItemStack.isSameItemSameComponents(this.simulate, stack)) {
                throw new IllegalStateException("Cannot restore a reservation into a different item");
            }
            this.simulate.grow(stack.getCount());
        }
        this.dirty = true;
    }

    /**
     * 增加指定物品堆
     *
     * @param stack 物品堆
     * @param spawn 是否生成
     * @return 剩余的物品堆
     */
    @Override
    public ItemStack grow(ItemStack stack, boolean spawn) {
        ItemStack copy = stack.copy();
        if (!this.is(stack)) return copy;
        if (!this.simulate.isEmpty() && !ItemStack.isSameItemSameComponents(stack, this.simulate)) return copy;
        int growCount = copy.getCount();
        int simulateCount = this.simulate.getCount();
        int grownCount = Math.max(0, Math.min(this.getCapacity(stack) - simulateCount, growCount));
        if (grownCount == 0) return copy;
        int remainingCount = growCount - grownCount;
        if (remainingCount > 0) {
            copy.setCount(remainingCount);
        } else {
            copy = ItemStack.EMPTY;
        }
        if (!this.simulate.isEmpty()) {
            this.simulate.grow(grownCount);
        } else {
            this.simulate = stack.copyWithCount(grownCount);
        }
        CacheOperation operation = new CacheOperation(grownCount);
        this.growSimulateStack.push(operation);
        this.growSnapshots.put(operation, stack.copyWithCount(grownCount));
        this.dirty = true;
        return copy;
    }

    /**
     * 回滚增加操作
     *
     * @return 回滚的物品堆
     */
    @Override
    public ItemStack rollbackGrow() {
        CacheOperation operation = this.growSimulateStack.pop();
        ItemStack copy = this.growSnapshots.remove(operation);
        this.simulate.shrink(operation.amount());
        this.dirty |= operation.amount() > 0;
        return copy;
    }

    /**
     * 回滚减少操作
     *
     * @return 回滚的数量
     */
    @Override
    public int rollbackShrink() {
        CacheOperation operation = this.shrinkSimulateStack.pop();
        ItemStack stack = this.shrinkSnapshots.remove(operation);
        if (this.releasedReservations.remove(operation)) return 0;
        this.restoreStack(stack);
        return operation.amount();
    }

    /**
     * 清空操作栈
     */
    @Override
    public void clearStack() {
        this.growSimulateStack.clear();
        this.shrinkSimulateStack.clear();
        this.growSnapshots.clear();
        this.shrinkSnapshots.clear();
        this.releasedReservations.clear();
    }

    /**
     * 判断是否满足指定谓词条件
     *
     * @param stack 物品谓词
     * @return 是否满足条件
     */
    public boolean is(Predicate<ItemStack> stack) {
        return stack.test(this.type);
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
        return ItemStack.isSameItemSameComponents(stack, this.type);
    }

    /**
     * 获取物品数量
     *
     * @return 物品数量
     */
    @Override
    public int getCount() {
        return this.simulate.isEmpty() ? 0 : this.simulate.getCount();
    }

    @Override
    public ItemStack getStack() {
        return this.simulate.copy();
    }

    @Override
    public void apply(Consumer<ItemStack> consumer) {
        consumer.accept(this.type);
    }
}
