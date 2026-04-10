package dev.anvilcraft.lib.v2.recipe.data.advancement.predicate.item;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import net.minecraft.advancements.critereon.ItemSubPredicate;
import net.minecraft.world.item.ItemStack;

public record NotPredicate(Type<?> type, ItemSubPredicate subPredicate) implements ItemSubPredicate {
    public static final Codec<NotPredicate> CODEC = CodecUtil.byMap(
        ItemSubPredicate.CODEC,
        NotPredicate::type,
        NotPredicate::subPredicate,
        NotPredicate::new
    );

    @Override
    public boolean matches(ItemStack itemStack) {
        return !this.subPredicate.matches(itemStack);
    }

    public static NotPredicate of(Type<?> type, ItemSubPredicate subPredicate) {
        return new NotPredicate(type, subPredicate);
    }
}
