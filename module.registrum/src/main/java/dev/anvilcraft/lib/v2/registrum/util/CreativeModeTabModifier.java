/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/util/CreativeModeTabModifier.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.util;

import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class CreativeModeTabModifier implements CreativeModeTab.Output {
    private final Supplier<FeatureFlagSet> flags;
    private final BooleanSupplier hasPermissions;
    private final BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptFunc;
    private final Supplier<CreativeModeTab.ItemDisplayParameters> parameters;

    @ApiStatus.Internal
    public CreativeModeTabModifier(
        Supplier<FeatureFlagSet> flags,
        BooleanSupplier hasPermissions,
        BiConsumer<ItemStack, CreativeModeTab.TabVisibility> acceptFunc,
        Supplier<CreativeModeTab.ItemDisplayParameters> parameters
    ) {
        this.flags = flags;
        this.hasPermissions = hasPermissions;
        this.acceptFunc = acceptFunc;
        this.parameters = parameters;
    }

    public FeatureFlagSet getFlags() {
        return flags.get();
    }

    public CreativeModeTab.ItemDisplayParameters getParameters() {
        return parameters.get();
    }

    public boolean hasPermissions() {
        return hasPermissions.getAsBoolean();
    }

    @Override
    public void accept(ItemStack stack, CreativeModeTab.TabVisibility visibility) {
        acceptFunc.accept(stack, visibility);
    }

    public void accept(Supplier<? extends ItemLike> item, CreativeModeTab.TabVisibility visibility) {
        accept(item.get(), visibility);
    }

    public void accept(Supplier<? extends ItemLike> item) {
        accept(item.get());
    }
}
