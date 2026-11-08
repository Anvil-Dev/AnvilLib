package dev.anvilcraft.lib.v2.recipe.outcome;

import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;

import java.util.List;

/**
 * 将物品结果计算与输出分开，使容量规划使用与提交相同的数量、物品、组件和位置。
 * 实现只能计算结果及读取规划上下文；禁止修改真实库存、方块、实体或执行其他世界副作用。
 * 自定义的额外物品奖励也应实现此接口，并将全部产物包含在返回值中。
 *
 * @param <O> 配方结果类型
 */
public interface IPlannedItemOutcome<O extends IPlannedItemOutcome<O>> extends IRecipeOutcome<O> {
    List<PlannedItem> plan(InWorldRecipeContext context);

    /** 每个候选批次调用一次并保存返回值；提交时不得再次调用以免重新抽取随机结果。 */
    default List<PlannedItem> planWithChance(InWorldRecipeContext context) {
        if (context.getLevel().getRandom().nextDouble() > context.getFloat(this.chance())) {
            return List.of();
        }
        return List.copyOf(this.plan(context));
    }

    @Override
    default void accept(InWorldRecipeContext context) {
        this.plan(context).forEach(item -> item.accept(context));
    }

    @Override
    default void acceptWithChance(InWorldRecipeContext context) {
        this.planWithChance(context).forEach(item -> item.accept(context));
    }
}
