package dev.anvilcraft.lib.v2.registrum.mixin;

import dev.anvilcraft.lib.v2.registrum.util.CreativeTabSections;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(CreativeModeTab.class)
abstract class CreativeModeTabMixin {
    @Shadow
    private Collection<ItemStack> displayItems;

    @Inject(method = "buildContents", at = @At("TAIL"))
    private void anvillib$arrangeSections(
        CreativeModeTab.ItemDisplayParameters parameters,
        CallbackInfo ci
    ) {
        CreativeModeTab tab = (CreativeModeTab) (Object) this;
        Identifier tabId = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
        if (tabId == null) return;
        this.displayItems = CreativeTabSections.arrange(tabId, this.displayItems, tab.getType() == CreativeModeTab.Type.CATEGORY);
    }
}
