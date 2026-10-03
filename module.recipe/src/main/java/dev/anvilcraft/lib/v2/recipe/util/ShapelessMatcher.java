package dev.anvilcraft.lib.v2.recipe.util;

import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheElement;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.predicate.item.HasItemIngredient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 无序匹配器
 * <p>
 * 用于处理无序配方谓词的匹配逻辑，支持不兼容和兼容的匹配方式
 * </p>
 */
public class ShapelessMatcher {
    /**
     * 检查谓词列表是否不兼容（即是否存在一种排列使得所有谓词都能匹配）
     *
     * @param predicates 谓词列表
     * @param ctx        配方上下文
     * @return 是否不兼容
     */
    public static boolean incompatible(List<IRecipePredicate<?>> predicates, InWorldRecipeContext ctx) {
        ItemMatchPlan plan = ShapelessMatcher.checkItemCapacity(predicates, ctx);
        if (plan == ItemMatchPlan.IMPOSSIBLE) return false;
        if (plan == ItemMatchPlan.LINEAR) return ShapelessMatcher.compatible(predicates, ctx);
        return ShapelessMatcher.backtrack(predicates, ctx);
    }

    private static boolean backtrack(List<IRecipePredicate<?>> predicates, InWorldRecipeContext ctx) {
        if (predicates.isEmpty()) return true;
        for (int i = 0; i < predicates.size(); i++) {
            IRecipePredicate<?> predicate = predicates.get(i);
            if (!predicate.test(ctx)) continue;
            List<IRecipePredicate<?>> next = new ArrayList<>(predicates);
            next.remove(i);
            ctx.push(predicate);
            if (next.isEmpty() || ShapelessMatcher.backtrack(next, ctx)) return true;
            ctx.pop(predicate);
        }
        return false;
    }

    private enum ItemMatchPlan {
        IMPOSSIBLE,
        LINEAR,
        BACKTRACK
    }

    private static ItemMatchPlan checkItemCapacity(List<IRecipePredicate<?>> predicates, InWorldRecipeContext ctx) {
        for (IRecipePredicate<?> predicate : predicates) {
            if (predicate.getClass() != HasItemIngredient.class) return ItemMatchPlan.BACKTRACK;
        }
        Map<ICacheElement, Integer> capacities = new IdentityHashMap<>();
        Map<Set<ICacheElement>, Long> demands = new HashMap<>();
        long totalDemand = 0;
        for (IRecipePredicate<?> predicate : predicates) {
            HasItemIngredient ingredient = (HasItemIngredient) predicate;
            int count = ingredient.getItem().count();
            if (count < 1) return ItemMatchPlan.BACKTRACK;
            Optional<Map<ICacheElement, Integer>> available = ingredient.getItem(ctx).availableElements();
            if (available.isEmpty()) return ItemMatchPlan.BACKTRACK;
            Set<ICacheElement> candidates = Collections.newSetFromMap(new IdentityHashMap<>());
            long capacity = 0;
            for (Map.Entry<ICacheElement, Integer> entry : available.orElseThrow().entrySet()) {
                if (entry.getValue() <= 0) continue;
                candidates.add(entry.getKey());
                capacities.put(entry.getKey(), entry.getValue());
                capacity += entry.getValue();
            }
            if (count > capacity) return ItemMatchPlan.IMPOSSIBLE;
            demands.merge(candidates, (long) count, Long::sum);
            totalDemand += count;
        }
        long totalCapacity = 0;
        for (int capacity : capacities.values()) totalCapacity += capacity;
        if (totalDemand > totalCapacity) return ItemMatchPlan.IMPOSSIBLE;
        Set<ICacheElement> assigned = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean independent = true;
        for (Map.Entry<Set<ICacheElement>, Long> group : demands.entrySet()) {
            long capacity = 0;
            for (ICacheElement element : group.getKey()) {
                capacity += capacities.get(element);
                if (!assigned.add(element)) independent = false;
            }
            if (group.getValue() > capacity) return ItemMatchPlan.IMPOSSIBLE;
        }
        return independent ? ItemMatchPlan.LINEAR : ItemMatchPlan.BACKTRACK;
    }

    /**
     * 检查谓词列表是否兼容（即所有谓词都能同时匹配）
     *
     * @param predicates 谓词列表
     * @param ctx        配方上下文
     * @return 是否兼容
     */
    public static boolean compatible(List<IRecipePredicate<?>> predicates, InWorldRecipeContext ctx) {
        for (IRecipePredicate<?> predicate : predicates) {
            if (!predicate.test(ctx)) return false;
            ctx.push(predicate);
        }
        return true;
    }
}