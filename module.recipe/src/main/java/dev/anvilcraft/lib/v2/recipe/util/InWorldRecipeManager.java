package dev.anvilcraft.lib.v2.recipe.util;

import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import dev.anvilcraft.lib.v2.recipe.AnvilLibRecipe;
import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.anvilcraft.lib.v2.recipe.event.InWorldRecipeEvent;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.predicate.block.HasBlockBase;
import dev.anvilcraft.lib.v2.recipe.trigger.IRecipeTrigger;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 世界内配方管理器类，用于管理和触发世界内配方
 * <p>
 * 该类负责注册配方和根据触发器执行相应的配方。
 * 注册时为每个触发器构建 {@link PrunePlan} 剪枝计划：
 * 相同位置偏移与方块集合限制的约束合并为共享判定节点，
 * 触发查找时每个节点至多判定一次实际方块状态，
 * 判定失败沿连线一票否决全部相连配方，全部失效时立即终止
 * </p>
 */
public class InWorldRecipeManager {
    /**
     * 配方排序器，与 {@link #recipeHolders} 候选集的排序保持一致，确保剪枝不改变执行顺序
     * <p>
     * 优先级相等时以配方 id 决胜：{@link IPrioritized#compareTo} 对同优先级的不同对象恒返回 1
     * （不满足比较器对称性），追加确定性的 id 决胜以保证集合行为与遍历顺序确定
     * </p>
     */
    private static final Comparator<RecipeHolder<InWorldRecipe>> RECIPE_ORDER = Comparator
        .comparing(RecipeHolder<InWorldRecipe>::value)
        .thenComparing(RecipeHolder::id);

    /**
     * 存储配方的映射表，键为配方触发器，值为配方集合
     */
    public final Multimap<IRecipeTrigger, RecipeHolder<InWorldRecipe>> recipeHolders = MultimapBuilder.hashKeys()
        .treeSetValues(RECIPE_ORDER)
        .build();

    /**
     * 各触发器对应的方块约束剪枝计划，随注册逐步构建
     */
    private final Map<IRecipeTrigger, PrunePlan> prunePlans = new HashMap<>();

    /**
     * 构造一个新的世界内配方管理器，并注册一个默认配方
     */
    public InWorldRecipeManager() {
    }

    /**
     * 注册一个世界内配方，并将其方块约束并入对应触发器的剪枝计划
     * <p>
     * 直接写入 {@link #recipeHolders} 而绕过此方法的配方不会进入剪枝计划，
     * 查找时将被保守放行（不做剪枝），行为与未引入剪枝前一致
     * </p>
     *
     * @param recipe 要注册的配方
     */
    public void register(RecipeHolder<InWorldRecipe> recipe) {
        recipeHolders.put(recipe.value().trigger(), recipe);
        this.prunePlans.computeIfAbsent(recipe.value().trigger(), trigger -> new PrunePlan())
            .add(recipe, InWorldRecipeManager.extractBlockConstraints(recipe.value()));
    }

    /**
     * 从配方中提取方块剪枝约束
     * <p>
     * 仅提取限定了方块集合（blocks 非空）的 {@link HasBlockBase} 类谓词；
     * 未限制对应位置方块的谓词不产生约束，所在配方不会被剪枝。
     * 该提取假设继承 {@link HasBlockBase} 的谓词在 test 时按基类契约检查
     * 其 offset 位置的方块状态是否满足 predicate 的方块限制
     * </p>
     *
     * @param recipe 世界内配方
     * @return 方块剪枝约束列表（不可变）
     */
    private static List<BlockConstraint> extractBlockConstraints(InWorldRecipe recipe) {
        List<BlockConstraint> constraints = new ArrayList<>();
        for (IRecipePredicate<?> predicate : Iterables.concat(recipe.nonConflicting(), recipe.conflicting())) {
            if (!(predicate instanceof HasBlockBase<?> hasBlock)) continue;
            if (hasBlock.getPredicate().getBlocks().size() == 0) continue; // 不限制对应位置的方块：不产生约束，禁止剪枝
            constraints.add(new BlockConstraint(hasBlock.getOffset(), hasBlock.getPredicate()));
        }
        return List.copyOf(constraints);
    }

    /**
     * 触发指定的配方触发器并执行匹配的配方
     * <p>
     * 先经剪枝计划批量失效所有不可能匹配的配方，仅对幸存候选依次执行完整匹配；
     * 幸存候选的顺序与未引入剪枝时的遍历顺序完全一致
     * </p>
     *
     * @param trigger 配方触发器
     * @param ctx     配方上下文
     */
    public void trigger(IRecipeTrigger trigger, InWorldRecipeContext ctx) {
        if (ctx.getLevel().isClientSide()) return;
        Collection<RecipeHolder<InWorldRecipe>> holders = this.recipeHolders.get(trigger);
        if (holders.isEmpty()) return;

        PrunePlan plan = this.prunePlans.get(trigger);
        List<RecipeHolder<InWorldRecipe>> candidates;
        if (plan == null) {
            candidates = new ArrayList<>(holders); // 无剪枝计划：全量放行
        } else {
            candidates = plan.resolve(holders, ctx);
            if (candidates.isEmpty()) return;
        }

        for (RecipeHolder<InWorldRecipe> holder : candidates) {
            InWorldRecipe recipe = holder.value();
            boolean accept = false;
            for (int i = 0; i < AnvilLibRecipe.CONFIG.inWorldRecipeMaxEfficiency; i++) {
                if (i >= recipe.maxEfficiency()) break;
                if (!recipe.matches(ctx, ctx.getLevel())) {
                    if (!accept) break;
                    return;
                }
                accept = true;
                recipe.assemble(ctx, ctx.getLevel().registryAccess());
                NeoForge.EVENT_BUS.post(new InWorldRecipeEvent(recipe.getType(), holder.id(), recipe, ctx));
            }
            if (accept) break;
        }
    }

    /**
     * 触发指定的配方触发器并执行匹配的配方
     *
     * @param trigger 配方触发器
     * @param ctx     配方上下文
     */
    public void trigger(Supplier<IRecipeTrigger> trigger, InWorldRecipeContext ctx) {
        this.trigger(trigger.get(), ctx);
    }

    /**
     * 方块剪枝约束：配方在某相对位置上的方块状态谓词
     *
     * @param offset    相对配方触发点的位置偏移
     * @param predicate 方块状态谓词（提取时已保证其方块集合非空）
     */
    private record BlockConstraint(Vec3 offset, BlockStatePredicate predicate) {
    }

    /**
     * 共享判定节点的键：相同的位置偏移与方块集合限制合并为同一节点
     * <p>
     * 位置偏移在构造时归一化（-0.0 归为 0.0），避免数学上相同的偏移因浮点表示差异而无法合并
     * </p>
     *
     * @param offset 相对配方触发点的位置偏移
     * @param blocks 允许方块集合的规范化键
     */
    private record NodeKey(Vec3 offset, BlockSetKey blocks) {
        private NodeKey {
            offset = new Vec3(
                offset.x == 0 ? 0 : offset.x,
                offset.y == 0 ? 0 : offset.y,
                offset.z == 0 ? 0 : offset.z
            );
        }
    }

    /**
     * 方块集合的规范化键：标签按其 {@link TagKey} 值相等合并，
     * 直接集合按排序后的方块 id 列表值相等合并；
     * 含无键 Holder 的集合无法规范化，以 {@link HolderSet} 实例身份兜底（不合并，仅降低共享率）
     *
     * @param key 规范化后的键（TagKey / 排序后的 ResourceLocation 列表 / HolderSet 实例）
     */
    private record BlockSetKey(Object key) {
        private static BlockSetKey of(HolderSet<Block> blocks) {
            return blocks.unwrap().map(
                BlockSetKey::new,
                holders -> {
                    List<ResourceLocation> ids = new ArrayList<>(holders.size());
                    for (Holder<Block> holder : holders) {
                        if (holder.unwrapKey().isEmpty()) return new BlockSetKey(blocks); // 无键 Holder：实例身份兜底
                        ids.add(holder.unwrapKey().orElseThrow().location());
                    }
                    ids.sort(null);
                    return new BlockSetKey(List.copyOf(ids));
                }
            );
        }
    }

    /**
     * 单个触发器的剪枝计划：判定节点与配方槽位构成的二部连线结构
     * <p>
     * 仅在服务端主线程访问，不做同步
     * </p>
     */
    private static final class PrunePlan {
        /**
         * 共享判定节点，按注册顺序排列以保证确定性的读取次序
         */
        private final Map<NodeKey, JudgeNode> nodes = new LinkedHashMap<>();

        /**
         * 配方槽位映射，用于解析时识别未经 {@link InWorldRecipeManager#register} 注册的配方并保守放行
         * <p>
         * 当前不存在配方移除路径，本映射与 {@link #nodes} 只增不减；
         * 若未来支持移除配方，必须同步清理二者，否则 alive 初值偏大会使提前终止静默失效
         * （正确性不受影响，仅损失性能）
         * </p>
         */
        private final Map<RecipeHolder<InWorldRecipe>, Slot> slotMap = new HashMap<>();

        /**
         * 向计划中添加一个配方及其方块约束连线
         *
         * @param holder      配方持有者
         * @param constraints 方块剪枝约束列表
         */
        void add(RecipeHolder<InWorldRecipe> holder, List<BlockConstraint> constraints) {
            Slot slot = new Slot();
            for (BlockConstraint constraint : constraints) {
                JudgeNode node = this.nodes.computeIfAbsent(
                    new NodeKey(constraint.offset(), BlockSetKey.of(constraint.predicate().getBlocks())),
                    key -> new JudgeNode(key, constraint.predicate()));
                node.edges.add(slot); // 建立节点到配方的连线
            }
            this.slotMap.put(holder, slot);
        }

        /**
         * 解析当前上下文下的幸存候选配方
         * <p>
         * 流程：逐节点判定一次实际方块状态 → 失效沿连线传播（一票否决）→
         * 全部失效时立即返回 → 按候选集既有顺序收集幸存者并对未注册配方兜底放行。
         * 相同 offset 的多个节点共享同一次世界读取。
         * 判定经 {@link BlockStatePredicate#cannotMatch} 进行：返回 true 时对应谓词必然失败，
         * 否决健全；返回 false 时不做结论（属性与 NBT 条件留给完整匹配检查）
         * </p>
         *
         * @param holders 该触发器下的全部配方持有者（含可能绕过注册的直接写入）
         * @param ctx     配方上下文
         * @return 幸存候选列表，顺序与候选集遍历顺序一致；全部失效时为空列表
         */
        List<RecipeHolder<InWorldRecipe>> resolve(
            Collection<RecipeHolder<InWorldRecipe>> holders,
            InWorldRecipeContext ctx
        ) {
            try {
                if (!this.nodes.isEmpty()) {
                    int alive = this.slotMap.size();
                    // 存在未经注册直接写入候选集的配方时不做整体提前终止，保证其被保守放行
                    boolean fullyIndexed = this.slotMap.size() >= holders.size();
                    BlockCache cache = ctx.computeIfAbsent(BlockCache.BLOCK_CACHE);
                    Map<Vec3, BlockState> states = new HashMap<>();
                    for (JudgeNode node : this.nodes.values()) {
                        Vec3 offset = node.key.offset();
                        BlockState state = states.get(offset);
                        if (state == null) {
                            state = cache.getBlockState(BlockPos.containing(ctx.getPos().add(offset)));
                            states.put(offset, state);
                        }
                        if (!node.judge.cannotMatch(state)) continue; // 节点判定通过
                        // 节点失效：沿连线一票否决全部相连配方
                        for (Slot edge : node.edges) {
                            if (edge.fails++ == 0 && --alive == 0 && fullyIndexed) {
                                return Collections.emptyList();
                            }
                        }
                    }
                }
                List<RecipeHolder<InWorldRecipe>> candidates = new ArrayList<>();
                for (RecipeHolder<InWorldRecipe> holder : holders) {
                    Slot slot = this.slotMap.get(holder);
                    // 未建立索引的配方保守放行；已索引的仅当无任何约束节点失效时保留
                    if (slot == null || slot.fails == 0) candidates.add(holder);
                }
                return candidates;
            } finally {
                this.reset(); // 复位计数（含异常路径），等待下次触发
            }
        }

        /**
         * 复位全部配方槽位的失效计数
         */
        private void reset() {
            for (Slot slot : this.slotMap.values()) slot.fails = 0;
        }
    }

    /**
     * 共享判定节点：聚合相同 (offset, blocks) 约束的判定与受影响配方连线
     */
    private static final class JudgeNode {
        /**
         * 节点键：位置偏移与允许方块集合的规范化键
         */
        private final NodeKey key;

        /**
         * 判定用代表谓词：合并到本节点的所有谓词方块集合相同，
         * 任取其一经 {@link BlockStatePredicate#cannotMatch} 判定即可
         */
        private final BlockStatePredicate judge;

        /**
         * 连线到的配方槽位
         */
        private final List<Slot> edges = new ArrayList<>();

        /**
         * 构造一个判定节点
         *
         * @param key   节点键
         * @param judge 判定用代表谓词
         */
        private JudgeNode(NodeKey key, BlockStatePredicate judge) {
            this.key = key;
            this.judge = judge;
        }
    }

    /**
     * 配方槽位：记录配方在当前触发中的失效计数
     */
    private static final class Slot {
        /**
         * 已失效的约束边数，0 表示尚无任何约束节点判定失败（即存活）
         */
        private int fails;
    }
}
