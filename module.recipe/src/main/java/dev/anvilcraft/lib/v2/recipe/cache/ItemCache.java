package dev.anvilcraft.lib.v2.recipe.cache;


import dev.anvilcraft.lib.v2.recipe.AnvilLibRecipe;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheElement;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheInput;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheInputOutputImpl;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheOutput;
import dev.anvilcraft.lib.v2.recipe.cache.item.ItemEntityCacheElement;
import dev.anvilcraft.lib.v2.recipe.cache.item.ItemCacheSlotProvider;
import dev.anvilcraft.lib.v2.recipe.cache.item.ItemResourceHandlerCacheElement;
import dev.anvilcraft.lib.v2.recipe.cache.item.operation.SpawnOperation;
import dev.anvilcraft.lib.v2.recipe.event.ItemCacheEvent;
import dev.anvilcraft.lib.v2.recipe.init.LibBlockEntityTags;
import dev.anvilcraft.lib.v2.recipe.init.LibEntityTypeTags;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeData;
import dev.anvilcraft.lib.v2.recipe.util.Range;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 物品缓存类，用于在配方执行过程中缓存和管理物品
 * 该类提供了对物品的输入、输出、生成等操作的缓存和同步功能
 * 支持与方块实体和实体进行物品交互
 */
@SuppressWarnings("unused")
public class ItemCache {
    /**
     * 物品缓存的数据键
     */
    public static final InWorldRecipeData<ItemCache> ITEM_CACHE = InWorldRecipeData.of(AnvilLibRecipe.of("item_cache"), ItemCache::of);

    /**
     * 默认接受者
     */
    public static final Consumer<InWorldRecipeContext> DEFAULT_ACCEPTOR = (ctx) -> ctx.get(ItemCache.ITEM_CACHE).endCache();

    /**
     * 世界实例
     */
    @Getter
    private final Level level;

    /**
     * 输入元素集合
     */
    private final Set<ICacheElement> inputs = new LinkedHashSet<>();

    /**
     * 输出元素集合
     */
    private final Set<ICacheElement> outputs = new LinkedHashSet<>();

    /**
     * 范围
     */
    private Range range = Range.EMPTY;

    /**
     * 生成操作列表
     */
    private final List<SpawnOperation> spawnList = new ArrayList<>();

    /**
     * 输入缓存集合
     */
    private final Set<ICacheInputOutputImpl> inputCache = new LinkedHashSet<>();

    /**
     * 输出缓存集合
     */
    private final Set<ICacheInputOutputImpl> outputCache = new LinkedHashSet<>();

    private static final Predicate<ICacheElement> ALL_SOURCES = element -> true;
    private final Map<ItemEntity, ItemEntityCacheElement> entityElements = new IdentityHashMap<>();
    private final Map<ResourceHandler<ItemResource>, Map<Integer, ItemResourceHandlerCacheElement>> handlerElements =
        new IdentityHashMap<>();
    private final Map<Container, ResourceHandler<ItemResource>> containerHandlers = new IdentityHashMap<>();
    private final Map<ICacheElement, List<Range>> elementRanges = new IdentityHashMap<>();
    private final List<Range> scannedRanges = new ArrayList<>();
    private boolean committed;

    private record InputKey(Predicate<ItemStack> item, Predicate<ICacheElement> source) {
    }

    private record OutputKey(ItemStack item, Predicate<ICacheElement> source) {
        @Override
        public boolean equals(Object other) {
            return other instanceof OutputKey key && this.source.equals(key.source)
                && ItemStack.isSameItemSameComponents(this.item, key.item);
        }

        @Override
        public int hashCode() {
            return this.source.hashCode();
        }
    }

    /**
     * 构造一个新的物品缓存
     *
     * @param level 世界实例
     */
    public ItemCache(Level level) {
        this.level = level;
    }

    /**
     * 创建一个新的物品缓存实例
     *
     * @param level 配方上下文
     * @param key   物品缓存数据键
     * @return 物品缓存实例
     */
    private static ItemCache of(InWorldRecipeContext level, InWorldRecipeData<ItemCache> key) {
        return new ItemCache(level.getLevel());
    }

    /**
     * 判断指定位置和范围是否在缓存范围内
     *
     * @param pos   位置
     * @param range 范围
     * @return 是否在范围内
     */
    public boolean inRange(Vec3 pos, Vec3 range) {
        return this.scannedRanges.stream().anyMatch(scanned -> scanned.contains(pos, range));
    }

    private ItemResourceHandlerCacheElement handlerElement(
        ResourceHandler<ItemResource> handler,
        int slot,
        Vec3 pos,
        Vec3 range
    ) {
        Set<ResourceHandler<ItemResource>> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (handler instanceof ItemCacheSlotProvider provider) {
            ItemCacheSlotProvider.Slot physical = provider.getItemCacheSlot(slot);
            if (physical.handler() == handler && physical.slot() == slot) break;
            if (!visited.add(handler)) throw new IllegalArgumentException("Cyclic item cache slot mapping");
            handler = physical.handler();
            slot = physical.slot();
        }
        Map<Integer, ItemResourceHandlerCacheElement> slots = this.handlerElements.computeIfAbsent(handler, key -> new HashMap<>());
        ItemResourceHandlerCacheElement element = slots.get(slot);
        if (element == null) {
            element = new ItemResourceHandlerCacheElement(this, handler, slot, pos, range);
            slots.put(slot, element);
        }
        this.addRange(element, Range.of(pos, range));
        return element;
    }

    private void addRange(ICacheElement element, Range range) {
        List<Range> ranges = this.elementRanges.computeIfAbsent(element, key -> new ArrayList<>());
        if (!ranges.contains(range)) ranges.add(range);
    }

    private boolean matchesRange(ICacheElement element, Range range, boolean output) {
        List<Range> ranges = this.elementRanges.get(element);
        if (ranges == null) ranges = List.of(Range.of(element.getPos(), element.getRange()));
        return ranges.stream().anyMatch(source -> output ? source.contains(range) : source.cross(range));
    }

    /**
     * 将物品处理器缓存转换为缓存元素
     *
     * @param itemCache    物品缓存
     * @param cache        物品处理器缓存
     * @param input        输入元素集合
     * @param output       输出元素集合
     * @param elementPos   元素位置
     * @param elementRange 元素范围
     */
    private static void toElement(
        ItemCache itemCache,
        ItemResourceHandlerCache cache,
        Set<ICacheElement> input,
        Set<ICacheElement> output,
        Vec3 elementPos,
        Vec3 elementRange
    ) {
        ResourceHandler<ItemResource> inputHandler = cache.getInput();
        for (int i = 0; i < inputHandler.size(); i++) {
            ItemResourceHandlerCacheElement element = itemCache.handlerElement(inputHandler, i, elementPos, elementRange);
            input.add(element);
        }
        ResourceHandler<ItemResource> outputHandler = cache.getOutput();
        for (int i = 0; i < outputHandler.size(); i++) {
            ItemResourceHandlerCacheElement element = itemCache.handlerElement(outputHandler, i, elementPos, elementRange);
            output.add(element);
        }
    }

    /**
     * 将物品处理器转换为缓存元素
     *
     * @param itemCache    物品缓存
     * @param handler      物品处理器
     * @param input        输入元素集合
     * @param output       输出元素集合
     * @param elementPos   元素位置
     * @param elementRange 元素范围
     */
    private static void toElement(
        ItemCache itemCache,
        ResourceHandler<ItemResource> handler,
        Set<ICacheElement> input,
        Set<ICacheElement> output,
        Vec3 elementPos,
        Vec3 elementRange
    ) {
        for (int i = 0; i < handler.size(); i++) {
            ItemResourceHandlerCacheElement element = itemCache.handlerElement(handler, i, elementPos, elementRange);
            input.add(element);
            output.add(element);
        }
    }

    /**
     * 将实体转换为缓存元素
     *
     * @param itemCache 物品缓存
     * @param entity    实体
     * @return 包含输入和输出元素集合的映射条目
     */
    private static Map.Entry<Set<ICacheElement>, Set<ICacheElement>> toElement(
        ItemCache itemCache,
        Entity entity
    ) {
        Set<ICacheElement> input = new LinkedHashSet<>();
        Set<ICacheElement> output = new LinkedHashSet<>();
        if (entity instanceof ItemEntity itemEntity) {
            ItemEntityCacheElement element = itemCache.entityElements.computeIfAbsent(
                itemEntity, source -> new ItemEntityCacheElement(itemCache, source)
            );
            itemCache.addRange(element, Range.of(element.getPos(), element.getRange()));
            input.add(element);
            output.add(element);
            return Map.entry(input, output);
        }
        double xRange = Math.abs(entity.getBoundingBox().maxX - entity.getBoundingBox().minX);
        double yRange = Math.abs(entity.getBoundingBox().maxY - entity.getBoundingBox().minY);
        double zRange = Math.abs(entity.getBoundingBox().maxZ - entity.getBoundingBox().minZ);
        double minRange = Math.min(xRange, Math.min(yRange, zRange));
        Vec3 elementPos = entity.position().add(0.0, yRange / 2.0, 0.0);
        Vec3 elementRange = new Vec3(minRange, minRange, minRange);
        if (entity instanceof ItemResourceHandlerCache cache) {
            ItemCache.toElement(itemCache, cache, input, output, elementPos, elementRange);
        } else if (entity instanceof Container container && entity.is(LibEntityTypeTags.ITEM_CACHE)) {
            ResourceHandler<ItemResource> handler = itemCache.containerHandlers.computeIfAbsent(container, VanillaContainerWrapper::of);
            ItemCache.toElement(itemCache, handler, input, output, elementPos, elementRange);
        }
        return Map.entry(input, output);
    }

    /**
     * 将方块实体转换为缓存元素
     *
     * @param itemCache 物品缓存
     * @param entity    方块实体
     * @return 包含输入和输出元素集合的映射条目
     */
    private static Map.Entry<Set<ICacheElement>, Set<ICacheElement>> toElement(
        ItemCache itemCache,
        BlockEntity entity
    ) {
        Set<ICacheElement> input = new LinkedHashSet<>();
        Set<ICacheElement> output = new LinkedHashSet<>();
        Vec3 elementPos = entity.getBlockPos().getCenter();
        Vec3 elementRange = new Vec3(1, 1, 1);
        Predicate<BlockEntity> inTag;
        Iterable<Holder<BlockEntityType<?>>> tagOrEmpty = BuiltInRegistries.BLOCK_ENTITY_TYPE.getTagOrEmpty(LibBlockEntityTags.ITEM_CACHE);
        List<ResourceKey<BlockEntityType<?>>> keys = new ArrayList<>();
        for (Holder<BlockEntityType<?>> holder : tagOrEmpty) {
            ResourceKey<BlockEntityType<?>> key = holder.getKey();
            keys.add(key);
        }
        inTag = blockEntity -> {
            Optional<ResourceKey<BlockEntityType<?>>> key = BuiltInRegistries.BLOCK_ENTITY_TYPE.getResourceKey(blockEntity.getType());
            return key.filter(keys::contains).isPresent();
        };

        if (entity instanceof ItemResourceHandlerCache cache) {
            ItemCache.toElement(itemCache, cache, input, output, elementPos, elementRange);
        } else if (entity instanceof Container container && inTag.test(entity)) {
            ResourceHandler<ItemResource> handler = itemCache.containerHandlers.computeIfAbsent(container, VanillaContainerWrapper::of);
            ItemCache.toElement(itemCache, handler, input, output, elementPos, elementRange);
        }
        return Map.entry(input, output);
    }

    /**
     * 扩展缓存范围并添加相关实体和方块实体
     *
     * @param pos   位置
     * @param range 范围
     */
    public void grow(Vec3 pos, Vec3 range) {
        this.ensureOpen();
        Range newRange = Range.of(pos, range);
        if (this.inRange(pos, range)) return;
        this.scannedRanges.add(newRange);
        if (!this.range.isEmpty()) {
            this.range.grow(newRange);
        } else {
            this.range = Range.of(pos, range);
        }
        List<Entity> entities = this.level.getEntities(EntityTypeTest.forClass(Entity.class), newRange.toAABB(), (entity) -> true);
        for (Entity entity : entities) {
            Map.Entry<Set<ICacheElement>, Set<ICacheElement>> entry = ItemCache.toElement(this, entity);
            this.inputs.addAll(entry.getKey());
            this.outputs.addAll(entry.getValue());
        }
        for (BlockPos blockPos : newRange) {
            BlockEntity entity = level.getBlockEntity(blockPos);
            if (entity == null) continue;
            Map.Entry<Set<ICacheElement>, Set<ICacheElement>> entry = ItemCache.toElement(this, entity);
            this.inputs.addAll(entry.getKey());
            this.outputs.addAll(entry.getValue());
        }
    }

    /**
     * 获取指定物品的输入缓存
     *
     * @param itemLike 物品
     * @param pos      位置
     * @return 输入缓存
     */
    public ICacheInput getInput(ItemLike itemLike, Vec3 pos) {
        return this.getInput(stack1 -> stack1.is(itemLike.asItem()), pos);
    }

    /**
     * 获取指定物品的输入缓存
     *
     * @param itemLike 物品
     * @param pos      位置
     * @param range    范围
     * @return 输入缓存
     */
    public ICacheInput getInput(ItemLike itemLike, Vec3 pos, Vec3 range) {
        return this.getInput(stack1 -> stack1.is(itemLike.asItem()), pos, range);
    }

    /**
     * 获取指定物品堆的输入缓存
     *
     * @param stack 物品堆
     * @param pos   位置
     * @return 输入缓存
     */
    public ICacheInput getInput(ItemStack stack, Vec3 pos) {
        return this.getInput(stack1 -> ItemStack.isSameItemSameComponents(stack, stack1), pos);
    }

    /**
     * 获取指定物品堆的输入缓存
     *
     * @param stack 物品堆
     * @param pos   位置
     * @param range 范围
     * @return 输入缓存
     */
    public ICacheInput getInput(ItemStack stack, Vec3 pos, Vec3 range) {
        return this.getInput(stack1 -> ItemStack.isSameItemSameComponents(stack, stack1), pos, range);
    }

    /**
     * 获取满足谓词条件的输入缓存
     *
     * @param predicate 物品谓词
     * @param pos       位置
     * @return 输入缓存
     */
    public ICacheInput getInput(Predicate<ItemStack> predicate, Vec3 pos) {
        return this.getInput(predicate, pos, new Vec3(0.25, 0.25, 0.25));
    }

    /**
     * 获取满足谓词条件的输入缓存
     *
     * @param predicate 物品谓词
     * @param pos       位置
     * @param range     范围
     * @return 输入缓存
     */
    public ICacheInput getInput(Predicate<ItemStack> predicate, Vec3 pos, Vec3 range) {
        return this.getInput(predicate, pos, range, ALL_SOURCES);
    }

    public ICacheInput getInput(
        Predicate<ItemStack> predicate,
        Vec3 pos,
        Vec3 range,
        Predicate<ICacheElement> sourceFilter
    ) {
        this.ensureOpen();
        Range range1 = Range.of(pos, range);
        InputKey key = new InputKey(predicate, sourceFilter);
        for (ICacheInputOutputImpl element : this.inputCache) {
            if (!element.equals(key, range1)) continue;
            return element;
        }
        this.grow(pos, range);
        Set<ICacheElement> inputs = new LinkedHashSet<>();
        for (ICacheElement input : this.inputs) {
            if (!this.matchesRange(input, range1, false)) continue;
            inputs.add(input);
        }
        ICacheInputOutputImpl input = new ICacheInputOutputImpl(
            key, this, pos, range1, inputs,
            element -> sourceFilter.test(element) && predicate.test(element.getStack())
        );
        this.inputCache.add(input);
        return input;
    }

    /**
     * 获取指定物品堆的输出缓存
     *
     * @param stack 物品堆
     * @param pos   位置
     * @return 输出缓存
     */
    public ICacheOutput getOutput(ItemStack stack, Vec3 pos) {
        return this.getOutput(stack, pos, new Vec3(0.05, 0.05, 0.05));
    }

    /**
     * 获取指定物品堆的输出缓存
     *
     * @param stack 物品堆
     * @param pos   位置
     * @param range 范围
     * @return 输出缓存
     */
    public ICacheOutput getOutput(ItemStack stack, Vec3 pos, Vec3 range) {
        return this.getOutput(stack, pos, range, ALL_SOURCES);
    }

    public ICacheOutput getOutput(
        ItemStack stack,
        Vec3 pos,
        Vec3 range,
        Predicate<ICacheElement> sourceFilter
    ) {
        this.ensureOpen();
        Range range1 = Range.of(pos, range);
        OutputKey key = new OutputKey(stack.copyWithCount(1), sourceFilter);
        for (ICacheInputOutputImpl element : this.outputCache) {
            if (!element.equals(key, range1)) continue;
            return element;
        }
        this.grow(pos, range);
        Set<ICacheElement> outputs = new LinkedHashSet<>();
        for (ICacheElement output : this.outputs) {
            if (!this.matchesRange(output, range1, true)) continue;
            if (!output.is(stack)) continue;
            if (!sourceFilter.test(output)) continue;
            ItemCacheEvent.SelectOutput event = new ItemCacheEvent.SelectOutput(this, stack, pos, range, output);
            if (NeoForge.EVENT_BUS.post(event).isCanceled()) continue;
            outputs.add(output);
        }
        if (outputs.isEmpty()) {
            ItemEntityCacheElement output = ItemEntityCacheElement.create(this, stack, pos);
            this.outputs.add(output);
            outputs.add(output);
        }
        ICacheInputOutputImpl output = new ICacheInputOutputImpl(key, this, pos, range1, outputs);
        this.outputCache.add(output);
        return output;
    }

    /**
     * 添加生成操作到列表中
     *
     * @param spawnOperations 生成操作集合
     */
    public void pushSpawnList(Collection<SpawnOperation> spawnOperations) {
        this.spawnList.addAll(spawnOperations);
    }

    public void ensureOpen() {
        if (this.committed) throw new IllegalStateException("This item cache has already been committed");
    }

    /**
     * 结束缓存并同步所有更改
     */
    public void endCache() {
        if (this.committed) return;
        this.committed = true;
        List<ICacheElement> elements = new ArrayList<>();
        for (ICacheInputOutputImpl input : this.inputCache) {
            input.prepareSync(elements);
        }
        for (ICacheInputOutputImpl output : this.outputCache) {
            output.prepareSync(elements);
        }
        Set<ICacheElement> synchronizedElements = Collections.newSetFromMap(new IdentityHashMap<>());
        for (ICacheElement element : elements) {
            if (synchronizedElements.add(element)) element.sync();
        }
        Map<Map.Entry<ItemStack, Vec3>, Integer> map = new LinkedHashMap<>();
        List<SpawnOperation> pending = List.copyOf(this.spawnList);
        this.spawnList.clear();
        for (SpawnOperation spawnOperation : pending) {
            ItemStack stack = spawnOperation.stack().copyWithCount(1);
            if (stack.isEmpty()) continue;
            int count = spawnOperation.count();
            if (count <= 0) continue;
            Vec3 spawnPos = spawnOperation.pos();
            Map.Entry<ItemStack, Vec3> key = Map.entry(stack, spawnPos);
            for (Map.Entry<ItemStack, Vec3> mapKey : map.keySet()) {
                ItemStack stack1 = mapKey.getKey();
                Vec3 pos = mapKey.getValue();
                if (!ItemStack.isSameItemSameComponents(stack, stack1)) continue;
                if (!pos.closerThan(spawnPos, 0.25)) continue;
                key = mapKey;
                break;
            }
            map.put(key, map.getOrDefault(key, 0) + count);
        }
        for (Map.Entry<ItemStack, Vec3> stackEntry : map.keySet()) {
            int count = map.get(stackEntry);
            ItemStack stack = stackEntry.getKey();
            Vec3 pos = stackEntry.getValue();
            int maxStackSize = stack.getMaxStackSize();
            while (count > 0) {
                ItemStack stack1 = stack.copy();
                int newCount = Math.min(maxStackSize, count);
                stack1.setCount(newCount);
                ItemEntity entity = new ItemEntity(this.level, pos.x, pos.y, pos.z, stack1, 0, 0, 0);
                NeoForge.EVENT_BUS.post(new ItemCacheEvent.SpawnItemEntity(this, entity));
                if (!entity.isRemoved() && !entity.getItem().isEmpty()) this.level.addFreshEntity(entity);
                count -= newCount;
            }
        }
    }
}
