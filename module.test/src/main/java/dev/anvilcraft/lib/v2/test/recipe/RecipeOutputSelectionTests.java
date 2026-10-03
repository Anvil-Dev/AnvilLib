package dev.anvilcraft.lib.v2.test.recipe;

import dev.anvilcraft.lib.v2.recipe.cache.ItemCache;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheElement;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheOutput;
import dev.anvilcraft.lib.v2.recipe.event.ItemCacheEvent;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

@EventBusSubscriber(modid = AnvilLibTest.MOD_ID)
public final class RecipeOutputSelectionTests {
    private static final Vec3 RANGE = new Vec3(0.05, 0.05, 0.05);
    private static final Map<ItemCache, Consumer<ItemCacheEvent.SelectOutput>> LISTENERS = new IdentityHashMap<>();

    private RecipeOutputSelectionTests() {
    }

    @SubscribeEvent
    public static void selectOutput(ItemCacheEvent.SelectOutput event) {
        Consumer<ItemCacheEvent.SelectOutput> listener = LISTENERS.get(event.getCache());
        if (listener != null) listener.accept(event);
    }

    static void rejectsOrdinaryEntity(GameTestHelper helper) {
        Vec3 pos = position(helper);
        ItemEntity ordinary = addItem(helper, pos, 4);
        ItemCache cache = new ItemCache(helper.getLevel());
        int[] selections = {0};
        LISTENERS.put(cache, event -> {
            selections[0]++;
            event.getStack().setCount(99);
            event.setCanceled(!event.getElement().isGeneratedOutput());
        });
        try {
            ItemStack product = new ItemStack(Items.IRON_INGOT);
            ICacheOutput output = cache.getOutput(product, pos, RANGE);
            helper.assertTrue(output == cache.getOutput(product.copyWithCount(3), pos, RANGE),
                "Equal item components and source filters reuse the output selection");
            helper.assertTrue(selections[0] == 1, "Cached output selections do not post another event");
            output.grow(product, true);
            cache.endCache();
            helper.assertTrue(ordinary.getItem().getCount() == 4, "Products do not merge into a rejected ordinary source");
            List<ItemEntity> produced = items(helper, pos).stream().filter(entity -> entity != ordinary).toList();
            helper.assertTrue(produced.size() == 1 && produced.getFirst().getItem().getCount() == 1,
                "Selection veto spawns exactly one product, and event stack copies cannot mutate its count");
        } finally {
            LISTENERS.remove(cache);
        }
        helper.succeed();
    }

    static void mergesPendingOutput(GameTestHelper helper) {
        Vec3 pos = position(helper);
        ItemEntity ordinary = addItem(helper, pos, 4);
        ItemCache cache = new ItemCache(helper.getLevel());
        int[] generatedSelections = {0};
        LISTENERS.put(cache, event -> {
            boolean generated = event.getElement().isGeneratedOutput();
            if (generated) generatedSelections[0]++;
            event.setCanceled(!generated);
        });
        try {
            cache.getOutput(new ItemStack(Items.IRON_INGOT), pos, RANGE).grow(new ItemStack(Items.IRON_INGOT), true);
            cache.getOutput(new ItemStack(Items.IRON_INGOT), pos, new Vec3(0.04, 0.04, 0.04))
                .grow(new ItemStack(Items.IRON_INGOT, 2), true);
            cache.endCache();
            List<ItemEntity> produced = items(helper, pos).stream().filter(entity -> entity != ordinary).toList();
            helper.assertTrue(generatedSelections[0] == 1, "A later selection recognizes the pending generated destination");
            helper.assertTrue(ordinary.getItem().getCount() == 4 && produced.size() == 1
                    && produced.getFirst().getItem().getCount() == 3,
                "Compatible planned products share one destination without including ordinary inputs");
        } finally {
            LISTENERS.remove(cache);
        }
        helper.succeed();
    }

    static void outputFiltersHaveSeparateKeys(GameTestHelper helper) {
        Vec3 pos = position(helper);
        ItemEntity first = addItem(helper, pos, 2);
        ItemEntity second = addItem(helper, pos, 4);
        ItemCache cache = new ItemCache(helper.getLevel());
        Predicate<ICacheElement> firstOnly = element -> element.getSource() == first;
        Predicate<ICacheElement> secondOnly = element -> element.getSource() == second;
        ICacheOutput firstOutput = cache.getOutput(new ItemStack(Items.IRON_INGOT), pos, RANGE, firstOnly);
        ICacheOutput secondOutput = cache.getOutput(new ItemStack(Items.IRON_INGOT), pos, RANGE, secondOnly);
        helper.assertTrue(firstOutput != secondOutput, "Source filters are part of the output query key");
        helper.assertTrue(firstOutput == cache.getOutput(new ItemStack(Items.IRON_INGOT), pos, RANGE, firstOnly),
            "The same filter reuses its selection");
        firstOutput.grow(new ItemStack(Items.IRON_INGOT, 3), true);
        secondOutput.grow(new ItemStack(Items.IRON_INGOT, 5), true);
        cache.endCache();
        helper.assertTrue(first.getItem().getCount() == 5 && second.getItem().getCount() == 9,
            "Each output reaches only its own selected source");
        helper.succeed();
    }

    private static Vec3 position(GameTestHelper helper) {
        return helper.absolutePos(new BlockPos(3, 2, 3)).getCenter();
    }

    private static ItemEntity addItem(GameTestHelper helper, Vec3 pos, int count) {
        ItemEntity entity = new ItemEntity(helper.getLevel(), pos.x, pos.y - 0.125, pos.z,
            new ItemStack(Items.IRON_INGOT, count), 0, 0, 0);
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    private static List<ItemEntity> items(GameTestHelper helper, Vec3 pos) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos, pos).inflate(1));
    }
}
