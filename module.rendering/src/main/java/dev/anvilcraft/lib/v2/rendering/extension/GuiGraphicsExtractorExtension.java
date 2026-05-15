package dev.anvilcraft.lib.v2.rendering.extension;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

public interface GuiGraphicsExtractorExtension {
    default void translucentItem(ItemStack stack, int x, int y, float alpha) {
        translucentItem(null, null, stack, x, y, alpha, 42);
    }


    void translucentItem(@Nullable LivingEntity owner, @Nullable Level level, ItemStack stack, int x, int y, float alpha, int seed);

    static GuiGraphicsExtractorExtension of(GuiGraphicsExtractor thiz) {
        return (GuiGraphicsExtractorExtension) thiz;
    }
}
