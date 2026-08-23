package dev.anvilcraft.lib.v2.recipe.util;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Iterables;
import dev.anvilcraft.lib.v2.recipe.AnvilLibRecipe;
import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.anvilcraft.lib.v2.recipe.event.InWorldRecipeEvent;
import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.predicate.block.HasBlockBase;
import dev.anvilcraft.lib.v2.recipe.trigger.IRecipeTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 世界内配方管理器类，用于管理和触发世界内配方
 * 该类负责注册配方和根据触发器执行相应的配方
 */
public class InWorldRecipeManager {
    /**
     * 存储配方的映射表，键为配方触发器，值为配方集合
     */
    public final Multimap<IRecipeTrigger, @NotNull RecipeHolder<InWorldRecipe>> recipeHolders = MultimapBuilder.hashKeys()
        .<RecipeHolder<InWorldRecipe>>treeSetValues(Comparator.comparing(RecipeHolder::value))
        .build();

    /**
     * 配方方块剪枝约束缓存，键为配方持有者，随注册构建
     * <p>
     * 约束列表为空表示该配方不含可剪枝的方块约束，永远不会被剪枝
     * </p>
     */
    private final Map<RecipeHolder<InWorldRecipe>, List<BlockConstraint>> blockConstraints = new HashMap<>();

    /**
     * 构造一个新的世界内配方管理器，并注册一个默认配方
     */
    public InWorldRecipeManager() {
    }

    /**
     * 注册一个世界内配方，并预提取其方块剪枝约束
     *
     * @param recipe 要注册的配方
     */
    public void register(RecipeHolder<InWorldRecipe> recipe) {
        recipeHolders.put(recipe.value().trigger(), recipe);
        this.blockConstraints.put(recipe, InWorldRecipeManager.extractBlockConstraints(recipe.value()));
    }

    /**
     * 从配方中提取方块剪枝约束
     * <p>
     * 仅提取限定了方块集合（blocks 非空）的 {@link HasBlockBase} 类谓词；
     * 未限制方块的谓词不产生约束，所在配方不会被剪枝。
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
            HolderSet<Block> blocks = hasBlock.getPredicate().getBlocks();
            if (blocks.size() == 0) continue; // 不限制对应位置的方块：不产生约束，禁止剪枝
            constraints.add(new BlockConstraint(hasBlock.getOffset(), blocks));
        }
        return List.copyOf(constraints);
    }

    /**
     * 触发指定的配方触发器并执行匹配的配方
     * <p>
     * 先根据注册期预提取的方块约束批量失效所有不可能匹配的配方，
     * 仅对幸存候选依次执行完整匹配
     * </p>
     *
     * @param trigger 配方触发器
     * @param ctx     配方上下文
     */
    public void trigger(IRecipeTrigger trigger, InWorldRecipeContext ctx) {
        if (ctx.getLevel().isClientSide()) return;
        Collection<RecipeHolder<InWorldRecipe>> holders = this.recipeHolders.get(trigger);
        if (holders.isEmpty()) return;

        BlockCache cache = null;
        Map<Vec3, BlockState> states = null;
        List<RecipeHolder<InWorldRecipe>> candidates = null;
        for (RecipeHolder<InWorldRecipe> holder : holders) {
            List<BlockConstraint> constraints = this.blockConstraints.get(holder);
            boolean mayMatch = true;
            if (constraints != null && !constraints.isEmpty()) {
                if (cache == null) cache = ctx.computeIfAbsent(BlockCache.BLOCK_CACHE);
                if (states == null) states = new HashMap<>();
                for (BlockConstraint constraint : constraints) {
                    Vec3 offset = constraint.offset();
                    BlockState state = states.get(offset);
                    if (state == null) {
                        state = cache.getBlockState(BlockPos.containing(ctx.getPos().add(offset)));
                        states.put(offset, state);
                    }
                    if (!state.is(constraint.blocks())) {
                        mayMatch = false; // 对应位置的实际方块不可能满足约束，整体失效该配方
                        break;
                    }
                }
            }
            if (mayMatch) {
                if (candidates == null) candidates = new ArrayList<>();
                candidates.add(holder);
            }
        }
        if (candidates == null) return;

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
     * 方块剪枝约束：配方在某相对位置对允许方块集合的限制
     *
     * @param offset 相对配方触发点的位置偏移
     * @param blocks 允许的方块集合（提取时已保证非空）
     */
    private record BlockConstraint(Vec3 offset, HolderSet<Block> blocks) {
    }
}