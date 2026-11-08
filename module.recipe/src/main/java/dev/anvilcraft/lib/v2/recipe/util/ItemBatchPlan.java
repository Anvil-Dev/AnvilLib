package dev.anvilcraft.lib.v2.recipe.util;

import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 在库存快照上寻找产物能够完整装入容器的最长连续批次，允许后续批次消耗原料腾出槽位。
 * 规划回调只能修改批次对象；世界修改须通过 {@link Batch#afterCommit(Runnable)} 延迟执行。
 * 新产物不参与本次规划的原料匹配，调用方在提交时负责检查库存仍与初始快照一致。
 */
public final class ItemBatchPlan {
    private final List<ItemStack> inputs;
    private final List<ItemStack> outputs;
    private final int batches;
    private final List<Runnable> effects;
    private boolean committed;

    private ItemBatchPlan(List<ItemStack> inputs, List<ItemStack> outputs, int batches, List<Runnable> effects) {
        this.inputs = copy(inputs);
        this.outputs = copy(outputs);
        this.batches = batches;
        this.effects = List.copyOf(effects);
    }

    /** 输入、输出分开存放时，只将产物装入输出快照。容量回调必须是无副作用的槽位规则。 */
    public static ItemBatchPlan separate(
        List<ItemStack> inputs, List<ItemStack> outputs, int limit, SlotCapacity capacity, Predicate<Batch> recipe
    ) {
        return plan(inputs, outputs, false, limit, capacity, recipe);
    }

    /** 输入、输出共用槽位时，先扣除所选批次的原料，再检查全部产物的最终布局。 */
    public static ItemBatchPlan shared(
        List<ItemStack> inventory, int limit, SlotCapacity capacity, Predicate<Batch> recipe
    ) {
        return plan(inventory, inventory, true, limit, capacity, recipe);
    }

    private static ItemBatchPlan plan(
        List<ItemStack> inputs, List<ItemStack> outputs, boolean shared,
        int limit, SlotCapacity capacity, Predicate<Batch> recipe
    ) {
        if (limit < 0) {
            throw new IllegalArgumentException("Batch limit must not be negative");
        }
        Objects.requireNonNull(capacity);
        Objects.requireNonNull(recipe);
        List<ItemStack> remaining = copy(inputs);
        List<ItemStack> initialOutput = copy(outputs);
        List<Product> products = new ArrayList<>();
        List<Runnable> effects = new ArrayList<>();
        List<ItemStack> acceptedInputs = remaining;
        List<ItemStack> acceptedOutputs = initialOutput;
        int acceptedBatches = 0;
        int acceptedEffects = 0;
        for (int index = 0; index < limit; index++) {
            Batch batch = new Batch(remaining);
            boolean matched;
            try {
                matched = recipe.test(batch);
            } finally {
                batch.open = false;
            }
            if (!matched) {
                break;
            }
            remaining = batch.remaining();
            for (ItemStack stack : batch.products) {
                addProduct(products, stack);
            }
            effects.addAll(batch.effects);
            List<ItemStack> layout = fit(shared ? remaining : initialOutput, products, capacity);
            if (layout != null) {
                acceptedInputs = shared ? layout : remaining;
                acceptedOutputs = layout;
                acceptedBatches = index + 1;
                acceptedEffects = effects.size();
            }
        }
        return new ItemBatchPlan(acceptedInputs, acceptedOutputs, acceptedBatches, effects.subList(0, acceptedEffects));
    }

    /** 返回已包含原料消耗的输入快照；共用槽位时同时包含最终产物。 */
    public List<ItemStack> inputs() {
        return copy(this.inputs);
    }

    /** 返回全部产物装入后的输出快照。 */
    public List<ItemStack> outputs() {
        return copy(this.outputs);
    }

    public int batches() {
        return this.batches;
    }

    /**
     * 调用方在回调中校验并提交两份库存，成功返回后才执行所选批次的副作用。
     * 回调抛出异常时不执行副作用；计划只允许提交一次，失败后必须重新读取库存并规划。
     */
    public void commit(Consumer<ItemBatchPlan> inventoryCommit) {
        Objects.requireNonNull(inventoryCommit);
        if (this.committed) {
            throw new IllegalStateException("The item batch plan was already committed");
        }
        this.committed = true;
        if (this.batches == 0) {
            return;
        }
        inventoryCommit.accept(this);
        this.effects.forEach(Runnable::run);
    }

    private static void addProduct(List<Product> products, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        for (int index = 0; index < products.size(); index++) {
            Product product = products.get(index);
            if (ItemStack.isSameItemSameComponents(product.stack(), stack)) {
                products.set(index, new Product(product.stack(), Math.addExact(product.count(), stack.getCount())));
                return;
            }
        }
        products.add(new Product(stack.copyWithCount(1), stack.getCount()));
    }

    private static @Nullable List<ItemStack> fit(List<ItemStack> initial, List<Product> products, SlotCapacity capacity) {
        List<ItemStack> layout = copy(initial);
        long[] remaining = products.stream().mapToLong(Product::count).toArray();
        List<Integer> empty = new ArrayList<>();
        for (int slot = 0; slot < layout.size(); slot++) {
            ItemStack stored = layout.get(slot);
            if (stored.isEmpty()) {
                empty.add(slot);
                continue;
            }
            for (int productIndex = 0; productIndex < products.size(); productIndex++) {
                Product product = products.get(productIndex);
                if (!ItemStack.isSameItemSameComponents(stored, product.stack())) {
                    continue;
                }
                int space = Math.max(0, checkedCapacity(capacity, slot, product.stack()) - stored.getCount());
                int inserted = (int) Math.min(remaining[productIndex], space);
                stored.grow(inserted);
                remaining[productIndex] -= inserted;
            }
        }
        return fillEmpty(layout, empty, 0, products, remaining, capacity, new HashSet<>()) ? layout : null;
    }

    private static boolean fillEmpty(
        List<ItemStack> layout, List<Integer> empty, int index,
        List<Product> products, long[] remaining, SlotCapacity capacity, Set<LayoutState> failed
    ) {
        int kinds = 0;
        for (long count : remaining) {
            if (count > 0) {
                kinds++;
            }
        }
        if (kinds == 0) {
            return true;
        }
        if (kinds > empty.size() - index) {
            return false;
        }
        LayoutState state = new LayoutState(index, Arrays.stream(remaining).boxed().toList());
        if (failed.contains(state)) {
            return false;
        }
        long requiredSlots = 0;
        for (int productIndex = 0; productIndex < products.size(); productIndex++) {
            long space = 0;
            int largest = 0;
            for (int emptyIndex = index; emptyIndex < empty.size(); emptyIndex++) {
                int limit = checkedCapacity(capacity, empty.get(emptyIndex), products.get(productIndex).stack());
                space += limit;
                largest = Math.max(largest, limit);
            }
            if (remaining[productIndex] > space) {
                return false;
            }
            if (remaining[productIndex] > 0) {
                requiredSlots += 1 + (remaining[productIndex] - 1) / largest;
            }
        }
        if (requiredSlots > empty.size() - index) {
            return false;
        }
        int slot = empty.get(index);
        for (int productIndex = 0; productIndex < products.size(); productIndex++) {
            if (remaining[productIndex] == 0) {
                continue;
            }
            ItemStack stack = products.get(productIndex).stack();
            int inserted = (int) Math.min(remaining[productIndex], checkedCapacity(capacity, slot, stack));
            if (inserted == 0) {
                continue;
            }
            layout.set(slot, stack.copyWithCount(inserted));
            remaining[productIndex] -= inserted;
            if (fillEmpty(layout, empty, index + 1, products, remaining, capacity, failed)) {
                return true;
            }
            remaining[productIndex] += inserted;
            layout.set(slot, ItemStack.EMPTY);
        }
        if (fillEmpty(layout, empty, index + 1, products, remaining, capacity, failed)) {
            return true;
        }
        failed.add(state);
        return false;
    }

    private static int checkedCapacity(SlotCapacity capacity, int slot, ItemStack stack) {
        int limit = capacity.capacity(slot, stack.copy());
        if (limit < 0) {
            throw new IllegalArgumentException("Slot capacity must not be negative: " + slot);
        }
        return limit;
    }

    private static List<ItemStack> copy(List<ItemStack> stacks) {
        return new ArrayList<>(stacks.stream().map(ItemStack::copy).toList());
    }

    private record LayoutState(int index, List<Long> remaining) {
    }

    private record Product(ItemStack stack, long count) {
    }

    /** 返回槽位允许存放该物品的最大数量，零表示拒绝；不得依赖真实槽位尚未提交的内容。 */
    @FunctionalInterface
    public interface SlotCapacity {
        int capacity(int slot, ItemStack stack);
    }

    /** 一次候选配方的原料预约、确定产物及延迟副作用，实例仅在规划回调执行期间可修改。 */
    public static final class Batch {
        private final List<ItemStack> available;
        private final List<ItemStack> catalysts;
        private final List<ItemStack> products = new ArrayList<>();
        private final List<Runnable> effects = new ArrayList<>();
        private boolean open = true;

        private Batch(List<ItemStack> inputs) {
            this.available = copy(inputs);
            this.catalysts = new ArrayList<>();
            for (int slot = 0; slot < inputs.size(); slot++) {
                this.catalysts.add(ItemStack.EMPTY);
            }
        }

        /** 按槽位顺序分配原料，返回包含实际组件的消耗快照；不足时返回空列表且不改变预约。 */
        public List<ItemStack> take(Predicate<ItemStack> ingredient, int count) {
            return this.reserve(ingredient, count, true);
        }

        /** 在本批中独占催化剂，下批可重复使用；返回值保留用于产物函数的实际物品组件。 */
        public List<ItemStack> catalyst(Predicate<ItemStack> ingredient, int count) {
            return this.reserve(ingredient, count, false);
        }

        private List<ItemStack> reserve(Predicate<ItemStack> ingredient, int count, boolean consume) {
            this.ensureOpen();
            if (count <= 0) {
                throw new IllegalArgumentException("Ingredient count must be positive");
            }
            List<Integer> slots = new ArrayList<>();
            long total = 0;
            for (int slot = 0; slot < this.available.size(); slot++) {
                ItemStack stack = this.available.get(slot);
                if (!stack.isEmpty() && ingredient.test(stack.copy())) {
                    slots.add(slot);
                    total += stack.getCount();
                }
            }
            if (total < count) {
                return List.of();
            }
            List<ItemStack> receipt = new ArrayList<>();
            for (int slot : slots) {
                ItemStack stack = this.available.get(slot);
                int taken = Math.min(count, stack.getCount());
                receipt.add(stack.copyWithCount(taken));
                if (!consume) {
                    ItemStack catalyst = this.catalysts.get(slot);
                    this.catalysts.set(slot, stack.copyWithCount(catalyst.getCount() + taken));
                }
                stack.shrink(taken);
                count -= taken;
                if (count == 0) {
                    break;
                }
            }
            return receipt;
        }

        /** 记录已确定数量、物品及组件的产物，不再次执行随机数或结果函数。 */
        public void produce(ItemStack stack) {
            this.ensureOpen();
            if (!stack.isEmpty()) {
                this.products.add(stack.copy());
            }
        }

        /**
         * 仅在本批被选中且库存提交成功后执行，适用于燃料消耗、事件及其他世界修改。
         * 回调须捕获本批参数快照，禁止读取后续批次已经覆盖的共享规划上下文。
         */
        public void afterCommit(Runnable effect) {
            this.ensureOpen();
            this.effects.add(Objects.requireNonNull(effect));
        }

        private List<ItemStack> remaining() {
            List<ItemStack> result = copy(this.available);
            for (int slot = 0; slot < result.size(); slot++) {
                ItemStack catalyst = this.catalysts.get(slot);
                if (!catalyst.isEmpty()) {
                    result.set(slot, catalyst.copyWithCount(result.get(slot).getCount() + catalyst.getCount()));
                }
            }
            return result;
        }

        private void ensureOpen() {
            if (!this.open) {
                throw new IllegalStateException("The recipe batch is no longer being planned");
            }
        }
    }
}
