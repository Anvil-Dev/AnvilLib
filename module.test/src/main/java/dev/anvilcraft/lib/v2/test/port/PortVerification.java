package dev.anvilcraft.lib.v2.test.port;

import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.anvilcraft.lib.v2.cube.client.model.ModelSelection;
import dev.anvilcraft.lib.v2.cube.geometry.ConvexShape;
import dev.anvilcraft.lib.v2.cube.geometry.OutlineBuilder;
import dev.anvilcraft.lib.v2.cube.geometry.SelectionGeometry;
import dev.anvilcraft.lib.v2.multiblock.dynamic.MultiblockState;
import dev.anvilcraft.lib.v2.multiblock.init.LibRegistries;
import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.anvilcraft.lib.v2.recipe.cache.ItemCache;
import dev.anvilcraft.lib.v2.recipe.cache.item.ItemHandlerCacheElement;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.predicate.block.HasBlock;
import dev.anvilcraft.lib.v2.recipe.trigger.IRecipeTrigger;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeManager;
import dev.anvilcraft.lib.v2.registrum.client.gui.CreativeVariantPickerOverlay;
import dev.anvilcraft.lib.v2.registrum.util.CreativeTabSection;
import dev.anvilcraft.lib.v2.registrum.util.CreativeTabSections;
import dev.anvilcraft.lib.v2.registrum.util.CreativeVariantPickerRegistry;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.joml.Matrix4f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** 使用真实游戏类型验证几何、创造栏、配方索引和持久化契约。 */
public final class PortVerification {
    private static int checks;

    public static int checks() { return checks; }
    public static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void run() throws Exception {
        geometry();
        variants();
        recipeRollback();
        recipePruning();
        itemCache();
        persistence();
        PortRpcVerification.run();
    }

    private static void geometry() {
        SelectionGeometry cube = new SelectionGeometry(List.of(ConvexShape.box(new AABB(0, 0, 0, 1, 1, 1))));
        check(OutlineBuilder.build(cube, 1_000_000_000L, 1_000_000L).segmentCount() == 12, "立方体棱线数量");
        SelectionGeometry joined = new SelectionGeometry(List.of(ConvexShape.box(new AABB(0, 0, 0, 1, 1, 1)),
            ConvexShape.box(new AABB(1, 0, 0, 2, 1, 1))));
        check(OutlineBuilder.build(joined, 1_000_000_000L, 1_000_000L).segmentCount() == 12, "相邻立方体内部棱线消除");
        for (int xRot = 0; xRot < 12; xRot++) {
            for (int yRot = 0; yRot < 4; yRot++) {
                Matrix4f transform = new Matrix4f().translation(0.5F, 0.5F, 0.5F)
                    .mul(new Matrix4f(com.mojang.math.OctahedralGroup.values()[xRot * 4 + yRot].transformation()))
                    .translate(-0.5F, -0.5F, -0.5F);
                SelectionPart part = new SelectionPart(cube, transform);
                for (double height : new double[]{1.62, 1.27}) {
                    for (int x = 0; x <= 40; x++) for (int z = 0; z <= 40; z++) {
                        Vec3 start = transform(transform, new Vec3(x / 40.0, 1 + height, z / 40.0));
                        Vec3 end = transform(transform, new Vec3(x / 40.0, -1, z / 40.0));
                        var hit = part.clip(start, end);
                        check(hit != null, "旋转立方体顶面漏选: " + xRot + "," + yRot + " @ " + x + "," + z);
                        Vec3 actual = start.lerp(end, hit.fraction());
                        Vec3 expected = transform(transform, new Vec3(x / 40.0, 1, z / 40.0));
                        check(actual.distanceTo(expected) < 1.0E-7, "变换精度超过射线遍历容差");
                    }
                }
            }
        }
        ModelSelection first = new ModelSelection.Fixed(new SelectionPart(cube));
        ModelSelection second = new ModelSelection.Fixed(new SelectionPart(joined));
        ModelSelection weighted = new ModelSelection.Weighted(List.of(first, second), List.of(2, 5), 7, joined.bounds());
        WeightedList<ModelSelection> vanilla = WeightedList.<ModelSelection>builder().add(first, 2).add(second, 5).build();
        for (int seed = 0; seed < 4096; seed++) {
            RandomSource actual = RandomSource.create(seed), expected = RandomSource.create(seed);
            List<SelectionPart> chosen = new ArrayList<>(), reference = new ArrayList<>();
            weighted.collect(actual, chosen);
            vanilla.getRandomOrThrow(expected).collect(expected, reference);
            check(chosen.equals(reference) && actual.nextLong() == expected.nextLong(), "随机模型及随机数消费不一致");
        }
        SelectionPart distant = new SelectionPart(cube, new Matrix4f().translation(1_000_000F, 1_000_000F, 1_000_000F));
        var distantHit = distant.clip(new Vec3(1_000_000.37, 1_000_002.62, 1_000_000.23),
            new Vec3(1_000_000.37, 999_999, 1_000_000.23));
        check(distantHit != null && Math.abs(distantHit.fraction() - 1.62 / 3.62) < 1.0E-8, "远距离变换不能经过单精度坐标中转");
    }

    private static Vec3 transform(Matrix4f m, Vec3 p) {
        return new Vec3(m.m00() * p.x + m.m10() * p.y + m.m20() * p.z + m.m30(),
            m.m01() * p.x + m.m11() * p.y + m.m21() * p.z + m.m31(),
            m.m02() * p.x + m.m12() * p.y + m.m22() * p.z + m.m32());
    }

    private static void variants() {
        CreativeVariantPickerRegistry.register(Items.STONE, Items.DIRT);
        Collection<ItemStack> folded = CreativeVariantPickerRegistry.fold(List.of(new ItemStack(Items.STONE), new ItemStack(Items.DIRT)));
        check(folded.size() == 1, "完整变体组折叠");
        folded.add(new ItemStack(Items.DIAMOND));
        check(folded.size() == 2, "折叠结果保持可修改");
        var incomplete = CreativeVariantPickerRegistry.fold(List.of(new ItemStack(Items.DIRT)));
        check(incomplete.size() == 1 && incomplete.iterator().next().is(Items.DIRT), "不完整变体组不得隐藏");
        SimpleContainer inventory = new SimpleContainer(new ItemStack(Items.STONE));
        Slot slot = new Slot(inventory, 0, 20, 100);
        var overlay = CreativeVariantPickerOverlay.create(slot).orElseThrow();
        check(overlay.variantAt(0, 0, 4, 19 + 4).orElseThrow().is(Items.STONE), "叠加层首格坐标及左侧越界限制");
        inventory.setItem(0, new ItemStack(Items.DIAMOND));
        check(!overlay.isValid(), "源槽位变化关闭叠加层");
        ResourceLocation tab = ResourceLocation.fromNamespaceAndPath("anvillib_test", "port_sections");
        CreativeTabSection section = CreativeTabSection.builder(tab).text(net.minecraft.network.chat.Component.literal("section")).build();
        List<ItemStack> output = new ArrayList<>();
        var parameters = new CreativeModeTab.ItemDisplayParameters(FeatureFlags.DEFAULT_FLAGS, true,
            net.minecraft.core.RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY));
        CreativeTabSections.build(tab, parameters, (stack, visibility) -> output.add(stack), sections -> {
            sections.accept(Items.DIAMOND);
            sections.section(section, contents -> contents.accept(Items.STONE));
        });
        var arranged = new ArrayList<>(CreativeTabSections.arrange(tab, output));
        check(CreativeTabSections.placedSections(tab).getFirst().itemIndex() == 9 && arranged.get(12).is(Items.STONE), "分区横幅按行对齐");
        arranged.add(ItemStack.EMPTY);
        check(CreativeTabSections.arrange(ResourceLocation.withDefaultNamespace("plain"), output) == output, "普通创造栏内容保留");
        output.clear();
        CreativeTabSections.build(tab, parameters, (stack, visibility) -> output.add(stack), sections -> {
            sections.section(section, contents -> { contents.accept(Items.STONE); contents.accept(Items.DIRT); });
            sections.section(section, contents -> contents.accept(Items.DIAMOND));
        });
        arranged = new ArrayList<>(CreativeTabSections.arrange(tab, output, true));
        check(CreativeTabSections.placedSections(tab).size() == 2 && arranged.get(12).is(Items.DIAMOND),
            "变体折叠后仍保留后续分区横幅和物品");
    }

    private static InWorldRecipe recipe(IRecipeTrigger trigger, List<IRecipePredicate<?>> predicates, int priority) {
        return new InWorldRecipe(new ItemStack(Items.STONE), trigger, List.of(), predicates, List.of(), priority, true, 1);
    }

    private static void recipeRollback() {
        InWorldRecipeContext context = new InWorldRecipeContext(null, Vec3.ZERO, null);
        CountingPredicate previous = new CountingPredicate(true), successful = new CountingPredicate(true), failed = new CountingPredicate(false);
        context.push(previous);
        check(!recipe(null, List.of(successful, failed), 1).matches(context, null), "失败配方应返回不匹配");
        check(context.getStack().equals(List.of(previous)) && successful.balance == 0 && previous.balance == 1,
            "失败匹配必须回滚本轮谓词并保留外层快照");
        check(recipe(null, List.of(successful), 1).matches(context, null), "后续配方仍可匹配");
        check(previous.clears == 0 && successful.clears == 1, "成功匹配仅清理新增谓词");
    }

    @SuppressWarnings("unchecked")
    private static void recipePruning() throws Exception {
        IRecipeTrigger trigger = new IRecipeTrigger.Impl(ResourceLocation.withDefaultNamespace("port"));
        InWorldRecipeManager manager = new InWorldRecipeManager();
        HasBlock stone = new HasBlock(Vec3.ZERO, BlockStatePredicate.builder().of(Blocks.STONE).build());
        HasBlock dirt = new HasBlock(Vec3.ZERO, BlockStatePredicate.builder().of(Blocks.DIRT).build());
        List<RecipeHolder<InWorldRecipe>> registered = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            var holder = new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, ResourceLocation.withDefaultNamespace("port_" + i)),
                recipe(trigger, List.of(i % 2 == 0 ? stone : dirt), i % 5));
            registered.add(holder); manager.register(holder); manager.register(holder);
        }
        check(manager.recipeHolders.size() == 100, "重复注册及同优先级配方不得丢失");
        InWorldRecipeContext context = new InWorldRecipeContext(null, Vec3.ZERO, null);
        int[] reads = {0};
        context.put(BlockCache.BLOCK_CACHE, new BlockCache(null) {
            @Override public BlockState getBlockState(BlockPos pos) { reads[0]++; return Blocks.STONE.defaultBlockState(); }
        });
        Field field = InWorldRecipeManager.class.getDeclaredField("prunePlans"); field.setAccessible(true);
        Object plan = ((Map<?, ?>) field.get(manager)).get(trigger);
        Method resolve = plan.getClass().getDeclaredMethod("resolve", Collection.class, InWorldRecipeContext.class); resolve.setAccessible(true);
        Collection<RecipeHolder<InWorldRecipe>> holders = manager.recipeHolders.get(trigger);
        var survivors = (List<RecipeHolder<InWorldRecipe>>) resolve.invoke(plan, holders, context);
        check(reads[0] == 1 && survivors.size() == 50, "共享位置只读取一次且准确剪枝");
        for (var survivor : survivors) check(survivor.value().nonConflicting().contains(stone), "剪枝不能放行已否定约束");
        check(survivors.equals(resolve.invoke(plan, holders, context)), "剪枝计数必须在下一轮复位");
        holders.clear();
        var direct = new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, ResourceLocation.withDefaultNamespace("direct")), recipe(trigger, List.of(dirt), 1));
        holders.add(direct);
        check(((List<?>) resolve.invoke(plan, holders, context)).equals(List.of(direct)), "直接写入候选集的配方必须保守放行");
    }

    private static void itemCache() {
        ItemStackHandler storage = new ItemStackHandler(1);
        storage.setStackInSlot(0, new ItemStack(Items.STONE, 8));
        IItemHandler handler = new IItemHandler() {
            public int getSlots() { return 1; }
            public ItemStack getStackInSlot(int slot) { return storage.getStackInSlot(slot); }
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return stack; }
            public ItemStack extractItem(int slot, int amount, boolean simulate) { return storage.extractItem(slot, amount, simulate); }
            public int getSlotLimit(int slot) { return 64; }
            public boolean isItemValid(int slot, ItemStack stack) { return false; }
        };
        ItemHandlerCacheElement cache = new ItemHandlerCacheElement(new ItemCache(null), handler, 0, Vec3.ZERO, Vec3.ZERO);
        cache.sync();
        check(handler.getStackInSlot(0).getCount() == 8, "未变化输出槽不能丢物品");
        cache.shrink(3);
        cache.sync();
        check(handler.getStackInSlot(0).getCount() == 5, "拒绝插入的槽位只提取消耗差量");
    }

    private static void persistence() {
        var state = new MultiblockState(BlockPos.ZERO, ResourceKey.create(LibRegistries.DEFINITIONS_KEY, ResourceLocation.withDefaultNamespace("port")), true);
        var tag = MultiblockState.CODEC.codec().encodeStart(NbtOps.INSTANCE, state).getOrThrow();
        check(MultiblockState.CODEC.codec().parse(NbtOps.INSTANCE, tag).getOrThrow().isFormed(), "多方块形成状态持久化");
    }

    private static final class CountingPredicate implements IRecipePredicate<CountingPredicate> {
        private final boolean matches;
        private int balance;
        private int clears;
        private CountingPredicate(boolean matches) { this.matches = matches; }
        @Override public boolean test(InWorldRecipeContext context) { return matches; }
        @Override public void snapshot(InWorldRecipeContext context) { balance++; }
        @Override public void rollback(InWorldRecipeContext context) { balance--; }
        @Override public void clearStack(InWorldRecipeContext context) { clears++; }
        @Override public Type<CountingPredicate> getType() { return null; }
    }
}
