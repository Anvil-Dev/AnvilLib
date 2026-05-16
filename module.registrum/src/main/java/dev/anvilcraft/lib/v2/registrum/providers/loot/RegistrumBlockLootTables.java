/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/providers/loot/RegistrateBlockLootTables.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.providers.loot;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.FunctionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.ConditionUserBuilder;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class RegistrumBlockLootTables extends BlockLootSubProvider implements RegistrumLootTables {
    private final AbstractRegistrum<?> parent;
    private final Consumer<RegistrumBlockLootTables> callback;

    public RegistrumBlockLootTables(
        HolderLookup.Provider provider,
        AbstractRegistrum<?> parent,
        Consumer<RegistrumBlockLootTables> callback
    ) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), provider);
        this.parent = parent;
        this.callback = callback;
    }

    @Override
    protected void generate() {
        callback.accept(this);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return parent.getAll(Registries.BLOCK).stream().map(Supplier::get).collect(Collectors.toList());
    }

    public HolderLookup.Provider getRegistries() {
        return this.registries;
    }


    @Override
    public <T extends FunctionUserBuilder<T>> T applyExplosionDecay(ItemLike type, FunctionUserBuilder<T> builder) {
        return super.applyExplosionDecay(type, builder);
    }

    @Override
    public <T extends ConditionUserBuilder<T>> T applyExplosionCondition(ItemLike type, ConditionUserBuilder<T> builder) {
        return super.applyExplosionCondition(type, builder);
    }

    @Override
    public LootTable.Builder createSilkTouchDispatchTable(Block original, LootPoolEntryContainer.Builder<?> entry) {
        return super.createSilkTouchDispatchTable(original, entry);
    }

    @Override
    public LootTable.Builder createShearsDispatchTable(Block original, LootPoolEntryContainer.Builder<?> entry) {
        return super.createShearsDispatchTable(original, entry);
    }

    @Override
    public LootTable.Builder createSilkTouchOrShearsDispatchTable(Block original, LootPoolEntryContainer.Builder<?> entry) {
        return super.createSilkTouchOrShearsDispatchTable(original, entry);
    }

    @Override
    public LootTable.Builder createSingleItemTableWithSilkTouch(Block original, ItemLike drop) {
        return super.createSingleItemTableWithSilkTouch(original, drop);
    }

    @Override
    public LootTable.Builder createSingleItemTable(ItemLike drop, NumberProvider count) {
        return super.createSingleItemTable(drop, count);
    }

    @Override
    public LootTable.Builder createSingleItemTableWithSilkTouch(Block original, ItemLike drop, NumberProvider count) {
        return super.createSingleItemTableWithSilkTouch(original, drop, count);
    }

    @Override
    public LootTable.Builder createSilkTouchOnlyTable(ItemLike drop) {
        return super.createSilkTouchOnlyTable(drop);
    }

    @Override
    public LootTable.Builder createPotFlowerItemTable(ItemLike drop) {
        return super.createPotFlowerItemTable(drop);
    }

    @Override
    public LootTable.Builder createSlabItemTable(Block slab) {
        return super.createSlabItemTable(slab);
    }

    @Override
    public LootTable.Builder createNameableBlockEntityTable(Block drop) {
        return super.createNameableBlockEntityTable(drop);
    }

    @Override
    public LootTable.Builder createShulkerBoxDrop(Block shulkerBox) {
        return super.createShulkerBoxDrop(shulkerBox);
    }

    @Override
    public LootTable.Builder createCopperOreDrops(Block block) {
        return super.createCopperOreDrops(block);
    }

    @Override
    public LootTable.Builder createLapisOreDrops(Block block) {
        return super.createLapisOreDrops(block);
    }

    @Override
    public LootTable.Builder createRedstoneOreDrops(Block block) {
        return super.createRedstoneOreDrops(block);
    }

    @Override
    public LootTable.Builder createBannerDrop(Block original) {
        return super.createBannerDrop(original);
    }

    @Override
    public LootTable.Builder createBeeNestDrop(Block original) {
        return super.createBeeNestDrop(original);
    }

    @Override
    public LootTable.Builder createBeeHiveDrop(Block original) {
        return super.createBeeHiveDrop(original);
    }

    @Override
    public LootTable.Builder createCaveVinesDrop(Block original) {
        return super.createCaveVinesDrop(original);
    }

    @Override
    public LootTable.Builder createOreDrop(Block original, Item drop) {
        return super.createOreDrop(original, drop);
    }

    @Override
    public LootTable.Builder createMushroomBlockDrop(Block original, ItemLike drop) {
        return super.createMushroomBlockDrop(original, drop);
    }

    @Override
    public LootTable.Builder createGrassDrops(Block original) {
        return super.createGrassDrops(original);
    }

    @Override
    public LootTable.Builder createShearsOnlyDrop(ItemLike drop) {
        return super.createShearsOnlyDrop(drop);
    }

    @Override
    public LootTable.Builder createShearsOrSilkTouchOnlyDrop(ItemLike drop) {
        return super.createShearsOrSilkTouchOnlyDrop(drop);
    }

    @Override
    public LootTable.Builder createMultifaceBlockDrops(Block block, LootItemCondition.Builder condition) {
        return super.createMultifaceBlockDrops(block, condition);
    }

    @Override
    public LootTable.Builder createMultifaceBlockDrops(Block block) {
        return super.createMultifaceBlockDrops(block);
    }

    @Override
    public LootTable.Builder createMossyCarpetBlockDrops(Block block) {
        return super.createMossyCarpetBlockDrops(block);
    }

    @Override
    public LootTable.Builder createLeavesDrops(Block original, Block sapling, float... chances) {
        return super.createLeavesDrops(original, sapling, chances);
    }

    @Override
    public LootTable.Builder createOakLeavesDrops(Block original, Block sapling, float... chances) {
        return super.createOakLeavesDrops(original, sapling, chances);
    }

    @Override
    public LootTable.Builder createMangroveLeavesDrops(Block block) {
        return super.createMangroveLeavesDrops(block);
    }

    @Override
    public LootTable.Builder createCropDrops(Block original, Item crop, Item seed, LootItemCondition.Builder condition) {
        return super.createCropDrops(original, crop, seed, condition);
    }

    @Override
    public LootTable.Builder createDoublePlantShearsDrop(Block block) {
        return super.createDoublePlantShearsDrop(block);
    }

    @Override
    public LootTable.Builder createDoublePlantWithSeedDrops(Block block, Block drop) {
        return super.createDoublePlantWithSeedDrops(block, drop);
    }

    @Override
    public LootTable.Builder createCandleDrops(Block block) {
        return super.createCandleDrops(block);
    }


    public static LootTable.Builder createCandleCakeDrops(Block block) {
        return BlockLootSubProvider.createCandleCakeDrops(block);
    }

    @Override
    public void addNetherVinesDropTable(Block vineBlock, Block plantBlock) {
        super.addNetherVinesDropTable(vineBlock, plantBlock);
    }

    @Override
    public LootTable.Builder createDoorTable(Block block) {
        return super.createDoorTable(block);
    }

    @Override
    public void dropPottedContents(Block potted) {
        super.dropPottedContents(potted);
    }

    @Override
    public void otherWhenSilkTouch(Block block, Block other) {
        super.otherWhenSilkTouch(block, other);
    }

    @Override
    public void dropOther(Block block, ItemLike drop) {
        super.dropOther(block, drop);
    }

    @Override
    public void dropWhenSilkTouch(Block block) {
        super.dropWhenSilkTouch(block);
    }

    @Override
    public void dropSelf(Block block) {
        super.dropSelf(block);
    }

    @Override
    public void add(Block block, LootTable.Builder builder) {
        super.add(block, builder);
    }

}
