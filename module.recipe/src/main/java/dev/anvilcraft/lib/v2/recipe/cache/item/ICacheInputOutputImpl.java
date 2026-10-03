package dev.anvilcraft.lib.v2.recipe.cache.item;

import dev.anvilcraft.lib.v2.recipe.cache.ItemCache;
import dev.anvilcraft.lib.v2.recipe.cache.item.operation.InputOutputOperation;
import dev.anvilcraft.lib.v2.recipe.cache.item.operation.SpawnOperation;
import dev.anvilcraft.lib.v2.recipe.util.Range;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * 缓存输入输出实现类，实现了缓存输入和输出接口
 */
public class ICacheInputOutputImpl implements ICacheInput, ICacheOutput {
    /**
     * 物品缓存
     */
    private final ItemCache cache;

    /**
     * 元素集合
     */
    private final Set<ICacheElement> elements = new LinkedHashSet<>();

    private final Predicate<ICacheElement> sourceFilter;
    private final Deque<List<ItemConsumption>> consumedOperations = new ArrayDeque<>();
    private final Map<InputOutputOperation, List<ItemConsumption>> consumptionByOperation = new IdentityHashMap<>();

    /**
     * 增加模拟栈
     */
    private final Deque<InputOutputOperation> growSimulateStack = new ArrayDeque<>();

    /**
     * 减少模拟栈
     */
    private final Deque<InputOutputOperation> shrinkSimulateStack = new ArrayDeque<>();

    /**
     * 生成模拟栈
     */
    private final Deque<SpawnOperation> spawnSimulateStack = new ArrayDeque<>();

    /**
     * 位置
     */
    private final Vec3 pos;

    /**
     * 键
     */
    private final Object key;

    /**
     * 范围
     */
    private final Range range;

    /**
     * 构造一个新的缓存输入输出实现
     *
     * @param key      键
     * @param cache    物品缓存
     * @param pos      位置
     * @param range    范围
     * @param elements 元素集合
     */
    public ICacheInputOutputImpl(Object key, ItemCache cache, Vec3 pos, Range range, Collection<ICacheElement> elements) {
        this(key, cache, pos, range, elements, element -> true);
    }

    public ICacheInputOutputImpl(
        Object key,
        ItemCache cache,
        Vec3 pos,
        Range range,
        Collection<ICacheElement> elements,
        Predicate<ICacheElement> sourceFilter
    ) {
        this.key = key;
        this.cache = cache;
        this.pos = pos;
        this.range = range;
        this.elements.addAll(elements);
        this.sourceFilter = sourceFilter;
    }

    /**
     * 减少指定数量的物品
     *
     * @param count 数量
     * @return 剩余数量
     */
    @Override
    public int shrink(int count) {
        this.cache.ensureOpen();
        if (count < 0) throw new IllegalArgumentException("Cannot consume a negative item count");
        Set<ICacheElement> elements = new LinkedHashSet<>();
        List<ItemConsumption> consumed = new ArrayList<>();
        for (ICacheElement element : this.elements) {
            if (count == 0) break;
            if (!this.sourceFilter.test(element)) continue;
            ItemConsumption consumption = element.consume(count);
            count -= consumption.count();
            consumed.add(consumption);
            elements.add(element);
            if (count <= 0) break;
        }
        InputOutputOperation operation = new InputOutputOperation(elements);
        List<ItemConsumption> receipt = List.copyOf(consumed);
        this.shrinkSimulateStack.push(operation);
        this.consumedOperations.addLast(receipt);
        this.consumptionByOperation.put(operation, receipt);
        return count;
    }

    /**
     * 回滚减少操作
     *
     * @return 回滚的数量
     */
    @Override
    public int rollbackShrink() {
        InputOutputOperation pop = this.shrinkSimulateStack.pop();
        int count = 0;
        for (ICacheElement element : pop.elements()) {
            count += element.rollbackShrink();
        }
        List<ItemConsumption> receipt = this.consumptionByOperation.remove(pop);
        this.consumedOperations.removeIf(candidate -> candidate == receipt);
        return count;
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
        this.cache.ensureOpen();
        Set<ICacheElement> elements = new LinkedHashSet<>();
        for (ICacheElement element : this.elements) {
            int previousCount = stack.getCount();
            ItemStack remaining = element.grow(stack, false);
            if (remaining.getCount() < previousCount) {
                elements.add(element);
            }
            stack = remaining;
            if (stack.isEmpty()) break;
        }
        this.growSimulateStack.push(new InputOutputOperation(elements));
        if (spawn) {
            this.spawnSimulateStack.push(new SpawnOperation(stack.copy(), stack.getCount(), this.pos));
            return ItemStack.EMPTY;
        } else {
            this.spawnSimulateStack.push(new SpawnOperation(ItemStack.EMPTY, 0, this.pos));
            return stack;
        }
    }

    /**
     * 回滚增加操作
     *
     * @return 回滚的物品堆
     */
    @Override
    public ItemStack rollbackGrow() {
        InputOutputOperation operation = this.growSimulateStack.pop();
        ItemStack stack = ItemStack.EMPTY;
        for (ICacheElement element : operation.elements()) {
            ItemStack stack1 = element.rollbackGrow();
            if (stack.isEmpty()) {
                stack = stack1;
            } else {
                stack.grow(stack1.getCount());
            }
        }
        SpawnOperation spawnOperation = this.spawnSimulateStack.pop();
        if (stack.isEmpty()) {
            stack = spawnOperation.stack().copyWithCount(spawnOperation.count());
        } else {
            stack.grow(spawnOperation.count());
        }
        return stack;
    }

    /**
     * 清空操作栈
     */
    @Override
    public void clearStack() {
        this.growSimulateStack.clear();
        this.shrinkSimulateStack.clear();
        this.consumptionByOperation.clear();
    }

    /**
     * 同步更改
     */
    @Override
    public void sync() {
        this.prepareSync();
        this.elements.forEach(ICacheElement::sync);
    }

    public void prepareSync(Collection<ICacheElement> elements) {
        this.prepareSync();
        elements.addAll(this.elements);
    }

    private void prepareSync() {
        this.clearStack();
        this.cache.pushSpawnList(this.spawnSimulateStack);
        this.spawnSimulateStack.clear();
        this.consumedOperations.clear();
    }

    /**
     * 获取物品数量
     *
     * @return 物品数量
     */
    @Override
    public int getCount() {
        long count = this.elements.stream().filter(this.sourceFilter).mapToLong(ICacheElement::getCount).sum();
        return (int) Math.min(Integer.MAX_VALUE, count);
    }

    /**
     * 判断是否等于指定键和范围
     *
     * @param key   键
     * @param range 范围
     * @return 是否相等
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean equals(@Nullable Object key, Range range) {
        if (key == null) return false;
        if (key.getClass() != this.key.getClass()) return false;
        if (!this.range.equals(range)) return false;
        if (key instanceof ItemStack stack && this.key instanceof ItemStack stack1) {
            return ItemStack.isSameItemSameComponents(stack, stack1);
        }
        return this.key.equals(key);
    }

    @Override
    public void apply(Consumer<ItemStack> consumer) {
        this.elements.stream().filter(this.sourceFilter).forEach(element -> element.apply(consumer));
    }

    @Override
    public List<ItemStack> getConsumedItems() {
        List<ItemConsumption> receipt = this.consumedOperations.peekFirst();
        if (receipt == null) return List.of();
        return receipt.stream().filter(item -> item.count() > 0).map(ItemConsumption::stack).toList();
    }

    @Override
    public boolean supportsConsumptionReceipts() {
        return true;
    }

    @Override
    public void clearConsumedItems() {
        this.consumedOperations.pollFirst();
    }

    @Override
    public void restoreConsumedItems() {
        List<ItemConsumption> receipt = this.consumedOperations.peekFirst();
        if (receipt == null) throw new IllegalStateException("There is no item reservation to restore");
        receipt.forEach(ItemConsumption::restore);
    }

    @Override
    public Optional<Map<ICacheElement, Integer>> availableElements() {
        Map<ICacheElement, Integer> available = new IdentityHashMap<>();
        this.elements.stream().filter(this.sourceFilter).forEach(element -> available.put(element, element.getCount()));
        return Optional.of(Collections.unmodifiableMap(available));
    }
}
