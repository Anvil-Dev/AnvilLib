/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/providers/generators/RegistrateRecipeProvider.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.providers.generators;

import com.google.common.collect.ImmutableMap;
import dev.anvilcraft.lib.v2.registrum.providers.ProviderType;
import dev.anvilcraft.lib.v2.registrum.util.DataIngredient;
import dev.anvilcraft.lib.v2.util.nullness.NonNullSupplier;
import lombok.experimental.Delegate;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.criterion.BredAnimalsTrigger;
import net.minecraft.advancements.criterion.EnterBlockTrigger;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import net.neoforged.neoforge.common.Tags;

import java.util.List;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public class RegistrumRecipeProvider extends RecipeProvider implements RecipeOutput {

    private final RegistrumRecipeRunner runner;

    @Delegate
    private final RecipeOutput outputDelegated;

    public RegistrumRecipeProvider(RegistrumRecipeRunner runner, HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
        this.runner = runner;
        this.outputDelegated = output;
    }

    @Override
    public void buildRecipes() {
        runner.provider = this;
        runner.owner.genData(ProviderType.RECIPE, this);
        runner.provider = null;
    }

    public <T> Holder<T> resolve(ResourceKey<T> key) {
        return registries.lookupOrThrow(key.registryKey()).getOrThrow(key);
    }

    public Identifier safeId(Identifier id) {
        return Identifier.fromNamespaceAndPath(runner.owner.getModid(), safeName(id));
    }

    public Identifier safeId(DataIngredient source) {
        return safeId(source.getId());
    }

    public Identifier safeId(ItemLike registryEntry) {
        return safeId(BuiltInRegistries.ITEM.getKey(registryEntry.asItem()));
    }

    public ResourceKey<Recipe<?>> safeKey(Identifier id) {
        return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(runner.owner.getModid(), safeName(id)));
    }

    public ResourceKey<Recipe<?>> safeKey(DataIngredient source) {
        return safeKey(source.getId());
    }

    public ResourceKey<Recipe<?>> safeKey(ItemLike registryEntry) {
        return safeKey(BuiltInRegistries.ITEM.getKey(registryEntry.asItem()));
    }

    public String safeName(Identifier id) {
        return id.getPath().replace('/', '_');
    }

    public String safeName(DataIngredient source) {
        return safeName(source.getId());
    }

    public String safeName(ItemLike registryEntry) {
        return safeName(BuiltInRegistries.ITEM.getKey(registryEntry.asItem()));
    }

    public static final int DEFAULT_SMELT_TIME = 200;
    public static final int DEFAULT_BLAST_TIME = DEFAULT_SMELT_TIME / 2;
    public static final int DEFAULT_SMOKE_TIME = DEFAULT_BLAST_TIME;
    public static final int DEFAULT_CAMPFIRE_TIME = DEFAULT_SMELT_TIME * 3;

    private static final ImmutableMap<RecipeSerializer<? extends AbstractCookingRecipe>, String> COOKING_TYPE_NAMES = ImmutableMap.<RecipeSerializer<? extends AbstractCookingRecipe>, String>builder()
        .put(SmeltingRecipe.SERIALIZER, "smelting")
        .put(BlastingRecipe.SERIALIZER, "blasting")
        .put(SmokingRecipe.SERIALIZER, "smoking")
        .put(CampfireCookingRecipe.SERIALIZER, "campfire")
        .build();

    public <T extends ItemLike, S extends AbstractCookingRecipe> void cooking(
        DataIngredient source,
        RecipeCategory category,
        CookingBookCategory cookingBookCategory,
        Supplier<? extends T> result,
        float experience,
        int cookingTime,
        RecipeSerializer<? extends AbstractCookingRecipe> serializer,
        AbstractCookingRecipe.Factory<S> factory
    ) {
        cooking(source, category, cookingBookCategory, result, experience, cookingTime, COOKING_TYPE_NAMES.get(serializer), factory);
    }

    public <T extends ItemLike, S extends AbstractCookingRecipe> void cooking(
        DataIngredient source,
        RecipeCategory category,
        CookingBookCategory cookingBookCategory,
        Supplier<? extends T> result,
        float experience,
        int cookingTime,
        @Nullable String typeName,
        AbstractCookingRecipe.Factory<S> factory
    ) {
        SimpleCookingRecipeBuilder.generic(
                source.toVanilla(),
                category,
                cookingBookCategory,
                result.get(),
                experience,
                cookingTime,
                factory
            )
            .unlockedBy("has_" + safeName(source), source.getCriterion(this))
            .save(this, safeId(result.get()) + "_from_" + safeName(source) + "_" + typeName);
    }

    public <T extends ItemLike> void smelting(
        DataIngredient source,
        RecipeCategory category,
        CookingBookCategory cookingBookCategory,
        Supplier<? extends T> result,
        float experience
    ) {
        smelting(source, category, cookingBookCategory, result, experience, DEFAULT_SMELT_TIME);
    }

    public <T extends ItemLike> void smelting(
        DataIngredient source,
        RecipeCategory category,
        CookingBookCategory cookingBookCategory,
        Supplier<? extends T> result,
        float experience,
        int cookingTime
    ) {
        cooking(source, category, cookingBookCategory, result, experience, cookingTime, SmeltingRecipe.SERIALIZER, SmeltingRecipe::new);
    }

    public <T extends ItemLike> void blasting(
        DataIngredient source,
        RecipeCategory category,
        CookingBookCategory cookingBookCategory,
        Supplier<? extends T> result,
        float experience
    ) {
        blasting(source, category, cookingBookCategory, result, experience, DEFAULT_BLAST_TIME);
    }

    public <T extends ItemLike> void blasting(
        DataIngredient source,
        RecipeCategory category,
        CookingBookCategory cookingBookCategory,
        Supplier<? extends T> result,
        float experience,
        int cookingTime
    ) {
        cooking(source, category, cookingBookCategory, result, experience, cookingTime, BlastingRecipe.SERIALIZER, BlastingRecipe::new);
    }

    public <T extends ItemLike> void smoking(
        DataIngredient source,
        RecipeCategory category,
        CookingBookCategory cookingBookCategory,
        Supplier<? extends T> result,
        float experience
    ) {
        smoking(source, category, cookingBookCategory, result, experience, DEFAULT_SMOKE_TIME);
    }

    public <T extends ItemLike> void smoking(
        DataIngredient source,
        RecipeCategory category,
        CookingBookCategory cookingBookCategory,
        Supplier<? extends T> result,
        float experience,
        int cookingTime
    ) {
        cooking(source, category, cookingBookCategory, result, experience, cookingTime, SmokingRecipe.SERIALIZER, SmokingRecipe::new);
    }

    public <T extends ItemLike> void campfire(
        DataIngredient source,
        RecipeCategory category,
        CookingBookCategory cookingBookCategory,
        Supplier<? extends T> result,
        float experience
    ) {
        campfire(source, category, cookingBookCategory, result, experience, DEFAULT_CAMPFIRE_TIME);
    }

    public <T extends ItemLike> void campfire(
        DataIngredient source,
        RecipeCategory category,
        CookingBookCategory cookingBookCategory,
        Supplier<? extends T> result,
        float experience,
        int cookingTime
    ) {
        cooking(
            source,
            category,
            cookingBookCategory,
            result,
            experience,
            cookingTime,
            CampfireCookingRecipe.SERIALIZER,
            CampfireCookingRecipe::new
        );
    }

    public <T extends ItemLike> void stonecutting(DataIngredient source, RecipeCategory category, Supplier<? extends T> result) {
        stonecutting(source, category, result, 1);
    }

    public <T extends ItemLike> void stonecutting(
        DataIngredient source,
        RecipeCategory category,
        Supplier<? extends T> result,
        int resultAmount
    ) {
        SingleItemRecipeBuilder.stonecutting(source.toVanilla(), category, result.get(), resultAmount)
            .unlockedBy("has_" + safeName(source), source.getCriterion(this))
            .save(this, safeId(result.get()) + "_from_" + safeName(source) + "_stonecutting");
    }

    public <T extends ItemLike> void smeltingAndBlasting(
        DataIngredient source,
        RecipeCategory category,
        CookingBookCategory cookingBookCategory,
        Supplier<? extends T> result,
        float xp
    ) {
        smelting(source, category, cookingBookCategory, result, xp);
        blasting(source, category, cookingBookCategory, result, xp);
    }

    public <T extends ItemLike> void food(
        DataIngredient source,
        RecipeCategory category,
        CookingBookCategory cookingBookCategory,
        Supplier<? extends T> result,
        float xp
    ) {
        smelting(source, category, cookingBookCategory, result, xp);
        smoking(source, category, cookingBookCategory, result, xp);
        campfire(source, category, cookingBookCategory, result, xp);
    }

    public <T extends ItemLike> void square(DataIngredient source, RecipeCategory category, Supplier<? extends T> output, boolean small) {
        ShapedRecipeBuilder builder = shaped(category, output.get())
            .define('X', source.toVanilla());
        if (small) {
            builder.pattern("XX").pattern("XX");
        } else {
            builder.pattern("XXX").pattern("XXX").pattern("XXX");
        }
        builder.unlockedBy("has_" + safeName(source), source.getCriterion(this))
            .save(this, safeKey(output.get()));
    }

    /**
     * @param <T>
     * @param source
     * @param output
     * @deprecated Broken, use {@link #storage(NonNullSupplier, RecipeCategory, NonNullSupplier)} or {@link #storage(DataIngredient, RecipeCategory, NonNullSupplier, DataIngredient, NonNullSupplier)}.
     */
    @Deprecated
    public <T extends ItemLike> void storage(DataIngredient source, RecipeCategory category, NonNullSupplier<? extends T> output) {
        square(source, category, output, false);
        // This is backwards, but leaving in for binary compat
        singleItemUnfinished(source, category, output, 1, 9)
            .save(this, safeId(source) + "_from_" + safeName(output.get()));
    }

    public <T extends ItemLike> void storage(
        NonNullSupplier<? extends T> source,
        RecipeCategory category,
        NonNullSupplier<? extends T> output
    ) {
        storage(DataIngredient.items(source), category, source, DataIngredient.items(output), output);
    }

    public <T extends ItemLike> void storage(
        DataIngredient sourceIngredient,
        RecipeCategory category,
        NonNullSupplier<? extends T> source,
        DataIngredient outputIngredient,
        NonNullSupplier<? extends T> output
    ) {
        square(sourceIngredient, category, output, false);
        singleItemUnfinished(outputIngredient, category, source, 1, 9)
            .save(this, safeId(sourceIngredient) + "_from_" + safeName(output.get()));
    }

    public <T extends ItemLike> ShapelessRecipeBuilder singleItemUnfinished(
        DataIngredient source,
        RecipeCategory category,
        Supplier<? extends T> result,
        int required,
        int amount
    ) {
        return shapeless(category, result.get(), amount)
            .requires(source.toVanilla(), required)
            .unlockedBy("has_" + safeName(source), source.getCriterion(this));
    }

    public <T extends ItemLike> void singleItem(
        DataIngredient source,
        RecipeCategory category,
        Supplier<? extends T> result,
        int required,
        int amount
    ) {
        singleItemUnfinished(source, category, result, required, amount).save(this, safeKey(result.get()));
    }

    public <T extends ItemLike> void planks(DataIngredient source, RecipeCategory category, Supplier<? extends T> result) {
        singleItemUnfinished(source, category, result, 1, 4)
            .group("planks")
            .save(this, safeKey(result.get()));
    }

    public <T extends ItemLike> void stairs(
        DataIngredient source,
        RecipeCategory category,
        Supplier<? extends T> result,
        @Nullable String group,
        boolean stone
    ) {
        shaped(category, result.get(), 4)
            .pattern("X  ").pattern("XX ").pattern("XXX")
            .define('X', source.toVanilla())
            .group(group)
            .unlockedBy("has_" + safeName(source), source.getCriterion(this))
            .save(this, safeKey(result.get()));
        if (stone) {
            stonecutting(source, category, result);
        }
    }

    public <T extends ItemLike> void slab(
        DataIngredient source,
        RecipeCategory category,
        Supplier<? extends T> result,
        @Nullable String group,
        boolean stone
    ) {
        shaped(category, result.get(), 6)
            .pattern("XXX")
            .define('X', source.toVanilla())
            .group(group)
            .unlockedBy("has_" + safeName(source), source.getCriterion(this))
            .save(this, safeKey(result.get()));
        if (stone) {
            stonecutting(source, category, result, 2);
        }
    }

    public <T extends ItemLike> void fence(
        DataIngredient source,
        RecipeCategory category,
        Supplier<? extends T> result,
        @Nullable String group
    ) {
        shaped(category, result.get(), 3)
            .pattern("W#W").pattern("W#W")
            .define('W', source.toVanilla())
            .define('#', Tags.Items.RODS_WOODEN)
            .group(group)
            .unlockedBy("has_" + safeName(source), source.getCriterion(this))
            .save(this, safeKey(result.get()));
    }

    public <T extends ItemLike> void fenceGate(
        DataIngredient source,
        RecipeCategory category,
        Supplier<? extends T> result,
        @Nullable String group
    ) {
        shaped(category, result.get())
            .pattern("#W#").pattern("#W#")
            .define('W', source.toVanilla())
            .define('#', Tags.Items.RODS_WOODEN)
            .group(group)
            .unlockedBy("has_" + safeName(source), source.getCriterion(this))
            .save(this, safeKey(result.get()));
    }

    public <T extends ItemLike> void wall(DataIngredient source, RecipeCategory category, Supplier<? extends T> result) {
        shaped(category, result.get(), 6)
            .pattern("XXX").pattern("XXX")
            .define('X', source.toVanilla())
            .unlockedBy("has_" + safeName(source), source.getCriterion(this))
            .save(this, safeKey(result.get()));
        stonecutting(source, category, result);
    }

    public <T extends ItemLike> void door(
        DataIngredient source,
        RecipeCategory category,
        Supplier<? extends T> result,
        @Nullable String group
    ) {
        shaped(category, result.get(), 3)
            .pattern("XX").pattern("XX").pattern("XX")
            .define('X', source.toVanilla())
            .group(group)
            .unlockedBy("has_" + safeName(source), source.getCriterion(this))
            .save(this, safeKey(result.get()));
    }

    public <T extends ItemLike> void trapDoor(
        DataIngredient source,
        RecipeCategory category,
        Supplier<? extends T> result,
        @Nullable String group
    ) {
        shaped(category, result.get(), 2)
            .pattern("XXX").pattern("XXX")
            .define('X', source.toVanilla())
            .group(group)
            .unlockedBy("has_" + safeName(source), source.getCriterion(this))
            .save(this, safeKey(result.get()));
    }


    @Override
    public void generateForEnabledBlockFamilies(FeatureFlagSet p_251836_) {
        super.generateForEnabledBlockFamilies(p_251836_);
    }

    @Override
    public void oneToOneConversionRecipe(ItemLike product, ItemLike resource, @org.jspecify.annotations.Nullable String group) {
        super.oneToOneConversionRecipe(product, resource, group);
    }

    @Override
    public void oneToOneConversionRecipe(
        ItemLike product,
        ItemLike resource,
        @org.jspecify.annotations.Nullable String group,
        int productCount
    ) {
        super.oneToOneConversionRecipe(product, resource, group, productCount);
    }

    @Override
    public void oreSmelting(
        List<ItemLike> p_250172_,
        RecipeCategory p_250588_,
        CookingBookCategory cookingBookCategory,
        ItemLike p_251868_,
        float p_250789_,
        int p_252144_,
        String p_251687_
    ) {
        super.oreSmelting(p_250172_, p_250588_, cookingBookCategory, p_251868_, p_250789_, p_252144_, p_251687_);
    }

    @Override
    public void oreBlasting(
        List<ItemLike> p_251504_,
        RecipeCategory p_248846_,
        CookingBookCategory cookingBookCategory,
        ItemLike p_249735_,
        float p_248783_,
        int p_250303_,
        String p_251984_
    ) {
        super.oreBlasting(p_251504_, p_248846_, cookingBookCategory, p_249735_, p_248783_, p_250303_, p_251984_);
    }

    @Override
    public <T extends AbstractCookingRecipe> void oreCooking(
        AbstractCookingRecipe.Factory<T> factory,
        List<ItemLike> smeltables,
        RecipeCategory craftingCategory,
        CookingBookCategory cookingCategory,
        ItemLike result,
        float experience,
        int cookingTime,
        String group,
        String fromDesc
    ) {
        super.oreCooking(factory, smeltables, craftingCategory, cookingCategory, result, experience, cookingTime, group, fromDesc);
    }

    @Override
    public void netheriteSmithing(Item p_250046_, RecipeCategory p_248986_, Item p_250389_) {
        super.netheriteSmithing(p_250046_, p_248986_, p_250389_);
    }

    @Override
    public void trimSmithing(Item p_285461_, ResourceKey<TrimPattern> p_379766_, ResourceKey<Recipe<?>> p_399566_) {
        super.trimSmithing(p_285461_, p_379766_, p_399566_);
    }

    @Override
    public void twoByTwoPacker(RecipeCategory p_250881_, ItemLike p_252184_, ItemLike p_249710_) {
        super.twoByTwoPacker(p_250881_, p_252184_, p_249710_);
    }

    @Override
    public void threeByThreePacker(RecipeCategory p_259247_, ItemLike p_259376_, ItemLike p_259717_, String p_260308_) {
        super.threeByThreePacker(p_259247_, p_259376_, p_259717_, p_260308_);
    }

    @Override
    public void threeByThreePacker(RecipeCategory p_259186_, ItemLike p_259360_, ItemLike p_259263_) {
        super.threeByThreePacker(p_259186_, p_259360_, p_259263_);
    }

    @Override
    public void planksFromLog(ItemLike p_259052_, TagKey<Item> p_259045_, int p_259471_) {
        super.planksFromLog(p_259052_, p_259045_, p_259471_);
    }

    @Override
    public void planksFromLogs(ItemLike p_259193_, TagKey<Item> p_259818_, int p_259807_) {
        super.planksFromLogs(p_259193_, p_259818_, p_259807_);
    }

    @Override
    public void woodFromLogs(ItemLike p_126004_, ItemLike p_126005_) {
        super.woodFromLogs(p_126004_, p_126005_);
    }

    @Override
    public void woodenBoat(ItemLike p_126023_, ItemLike p_126024_) {
        super.woodenBoat(p_126023_, p_126024_);
    }

    @Override
    public void chestBoat(ItemLike p_236373_, ItemLike p_236374_) {
        super.chestBoat(p_236373_, p_236374_);
    }

    @Override
    public RecipeBuilder buttonBuilder(ItemLike p_176659_, Ingredient p_176660_) {
        return super.buttonBuilder(p_176659_, p_176660_);
    }

    @Override
    public RecipeBuilder doorBuilder(ItemLike p_176671_, Ingredient p_176672_) {
        return super.doorBuilder(p_176671_, p_176672_);
    }

    @Override
    public RecipeBuilder fenceBuilder(ItemLike p_176679_, Ingredient p_176680_) {
        return super.fenceBuilder(p_176679_, p_176680_);
    }

    @Override
    public RecipeBuilder fenceGateBuilder(ItemLike p_176685_, Ingredient p_176686_) {
        return super.fenceGateBuilder(p_176685_, p_176686_);
    }

    @Override
    public void pressurePlate(ItemLike p_176692_, ItemLike p_176693_) {
        super.pressurePlate(p_176692_, p_176693_);
    }

    @Override
    public RecipeBuilder pressurePlateBuilder(RecipeCategory p_251447_, ItemLike p_251989_, Ingredient p_249211_) {
        return super.pressurePlateBuilder(p_251447_, p_251989_, p_249211_);
    }

    @Override
    public void slab(RecipeCategory p_251848_, ItemLike p_249368_, ItemLike p_252133_) {
        super.slab(p_251848_, p_249368_, p_252133_);
    }

    @Override
    public void shelf(ItemLike result, ItemLike strippedLogs) {
        super.shelf(result, strippedLogs);
    }

    @Override
    public RecipeBuilder slabBuilder(RecipeCategory p_251707_, ItemLike p_251284_, Ingredient p_248824_) {
        return super.slabBuilder(p_251707_, p_251284_, p_248824_);
    }

    @Override
    public RecipeBuilder stairBuilder(ItemLike p_176711_, Ingredient p_176712_) {
        return super.stairBuilder(p_176711_, p_176712_);
    }

    @Override
    public RecipeBuilder trapdoorBuilder(ItemLike p_176721_, Ingredient p_176722_) {
        return super.trapdoorBuilder(p_176721_, p_176722_);
    }

    @Override
    public RecipeBuilder signBuilder(ItemLike p_176727_, Ingredient p_176728_) {
        return super.signBuilder(p_176727_, p_176728_);
    }

    @Override
    public void hangingSign(ItemLike p_252355_, ItemLike p_250437_) {
        super.hangingSign(p_252355_, p_250437_);
    }

    @Override
    public void colorItemWithDye(List<Item> dyeItems, List<Item> dyeableItems, String group, RecipeCategory category) {
        super.colorItemWithDye(dyeItems, dyeableItems, group, category);
    }

    @Override
    public void colorWithDye(
        List<Item> dyes,
        List<Item> dyeableItems,
        @Nullable Item dye,
        String group,
        RecipeCategory category
    ) {
        super.colorWithDye(dyes, dyeableItems, dye, group, category);
    }

    @Override
    public void carpet(ItemLike p_176718_, ItemLike p_176719_) {
        super.carpet(p_176718_, p_176719_);
    }

    @Override
    public void bedFromPlanksAndWool(ItemLike p_126075_, ItemLike p_126076_) {
        super.bedFromPlanksAndWool(p_126075_, p_126076_);
    }

    @Override
    public void banner(ItemLike p_126083_, ItemLike p_126084_) {
        super.banner(p_126083_, p_126084_);
    }

    @Override
    public void stainedGlassFromGlassAndDye(ItemLike p_126087_, ItemLike p_126088_) {
        super.stainedGlassFromGlassAndDye(p_126087_, p_126088_);
    }

    @Override
    public void dryGhast(ItemLike result) {
        super.dryGhast(result);
    }

    @Override
    public void harness(ItemLike result, ItemLike wool) {
        super.harness(result, wool);
    }

    @Override
    public void stainedGlassPaneFromStainedGlass(ItemLike p_126091_, ItemLike p_126092_) {
        super.stainedGlassPaneFromStainedGlass(p_126091_, p_126092_);
    }

    @Override
    public void stainedGlassPaneFromGlassPaneAndDye(ItemLike p_126095_, ItemLike p_126096_) {
        super.stainedGlassPaneFromGlassPaneAndDye(p_126095_, p_126096_);
    }

    @Override
    public void coloredTerracottaFromTerracottaAndDye(ItemLike p_126099_, ItemLike p_126100_) {
        super.coloredTerracottaFromTerracottaAndDye(p_126099_, p_126100_);
    }

    @Override
    public void concretePowder(ItemLike p_126103_, ItemLike p_126104_) {
        super.concretePowder(p_126103_, p_126104_);
    }

    @Override
    public void candle(ItemLike p_176544_, ItemLike p_176545_) {
        super.candle(p_176544_, p_176545_);
    }

    @Override
    public void wall(RecipeCategory p_251148_, ItemLike p_250499_, ItemLike p_249970_) {
        super.wall(p_251148_, p_250499_, p_249970_);
    }

    @Override
    public RecipeBuilder wallBuilder(RecipeCategory p_249083_, ItemLike p_250754_, Ingredient p_250311_) {
        return super.wallBuilder(p_249083_, p_250754_, p_250311_);
    }

    @Override
    public void polished(RecipeCategory p_248719_, ItemLike p_250032_, ItemLike p_250021_) {
        super.polished(p_248719_, p_250032_, p_250021_);
    }

    @Override
    public RecipeBuilder polishedBuilder(RecipeCategory p_249131_, ItemLike p_251242_, Ingredient p_251412_) {
        return super.polishedBuilder(p_249131_, p_251242_, p_251412_);
    }

    @Override
    public void cut(RecipeCategory p_252306_, ItemLike p_249686_, ItemLike p_251100_) {
        super.cut(p_252306_, p_249686_, p_251100_);
    }

    @Override
    public ShapedRecipeBuilder cutBuilder(RecipeCategory p_250895_, ItemLike p_251147_, Ingredient p_251563_) {
        return super.cutBuilder(p_250895_, p_251147_, p_251563_);
    }

    @Override
    public void chiseled(RecipeCategory p_251604_, ItemLike p_251049_, ItemLike p_252267_) {
        super.chiseled(p_251604_, p_251049_, p_252267_);
    }

    @Override
    public void mosaicBuilder(RecipeCategory p_248788_, ItemLike p_251925_, ItemLike p_252242_) {
        super.mosaicBuilder(p_248788_, p_251925_, p_252242_);
    }

    @Override
    public ShapedRecipeBuilder chiseledBuilder(RecipeCategory p_251755_, ItemLike p_249782_, Ingredient p_250087_) {
        return super.chiseledBuilder(p_251755_, p_249782_, p_250087_);
    }

    @Override
    public void stonecutterResultFromBase(RecipeCategory p_248911_, ItemLike p_251265_, ItemLike p_250033_) {
        super.stonecutterResultFromBase(p_248911_, p_251265_, p_250033_);
    }

    @Override
    public void stonecutterResultFromBase(RecipeCategory p_250609_, ItemLike p_251254_, ItemLike p_249666_, int p_251462_) {
        super.stonecutterResultFromBase(p_250609_, p_251254_, p_249666_, p_251462_);
    }

    @Override
    public void smeltingResultFromBase(ItemLike p_176741_, ItemLike p_176742_) {
        super.smeltingResultFromBase(p_176741_, p_176742_);
    }

    @Override
    public void nineBlockStorageRecipes(RecipeCategory p_250083_, ItemLike p_250042_, RecipeCategory p_248977_, ItemLike p_251911_) {
        super.nineBlockStorageRecipes(p_250083_, p_250042_, p_248977_, p_251911_);
    }

    @Override
    public void copySmithingTemplate(ItemLike p_350799_, ItemLike p_365321_) {
        super.copySmithingTemplate(p_350799_, p_365321_);
    }

    @Override
    public void copySmithingTemplate(ItemLike p_266974_, Ingredient p_360677_) {
        super.copySmithingTemplate(p_266974_, p_360677_);
    }

    @Override
    public <T extends AbstractCookingRecipe> void cookRecipes(String source, AbstractCookingRecipe.Factory<T> factory, int cookingTime) {
        super.cookRecipes(source, factory, cookingTime);
    }

    @Override
    public <T extends AbstractCookingRecipe> void simpleCookingRecipe(
        String source,
        AbstractCookingRecipe.Factory<T> factory,
        int cookingTime,
        ItemLike base,
        ItemLike result,
        float experience
    ) {
        super.simpleCookingRecipe(source, factory, cookingTime, base, result, experience);
    }

    @Override
    public void waxRecipes(FeatureFlagSet p_313879_) {
        super.waxRecipes(p_313879_);
    }

    @Override
    public void grate(Block p_309021_, Block p_309140_) {
        super.grate(p_309021_, p_309140_);
    }

    @Override
    public void copperBulb(Block p_309026_, Block p_308866_) {
        super.copperBulb(p_309026_, p_308866_);
    }

    @Override
    public void waxedChiseled(Block result, Block material) {
        super.waxedChiseled(result, material);
    }

    @Override
    public void suspiciousStew(Item p_360920_, SuspiciousEffectHolder p_361278_) {
        super.suspiciousStew(p_360920_, p_361278_);
    }

    @Override
    public void dyedItem(Item target, String group) {
        super.dyedItem(target, group);
    }

    @Override
    public void dyedShulkerBoxRecipe(Item dye, Item dyedResult) {
        super.dyedShulkerBoxRecipe(dye, dyedResult);
    }

    @Override
    public void dyedBundleRecipe(Item dye, Item dyedResult) {
        super.dyedBundleRecipe(dye, dyedResult);
    }

    @Override
    public void generateRecipes(BlockFamily p_176582_, FeatureFlagSet p_313799_) {
        super.generateRecipes(p_176582_, p_313799_);
    }

    @Override
    public Block getBaseBlockForCrafting(BlockFamily family, BlockFamily.Variant variant) {
        return super.getBaseBlockForCrafting(family, variant);
    }

    public static Criterion<EnterBlockTrigger.TriggerInstance> insideOf(Block p_125980_) {
        return RecipeProvider.insideOf(p_125980_);
    }

    @Override
    public Criterion<BredAnimalsTrigger.TriggerInstance> bredAnimal() {
        return super.bredAnimal();
    }

    @Override
    public Criterion<InventoryChangeTrigger.TriggerInstance> has(MinMaxBounds.Ints p_176521_, ItemLike p_176522_) {
        return super.has(p_176521_, p_176522_);
    }

    @Override
    public Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike p_125978_) {
        return super.has(p_125978_);
    }

    @Override
    public Criterion<InventoryChangeTrigger.TriggerInstance> has(TagKey<Item> p_206407_) {
        return super.has(p_206407_);
    }


    public static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryTrigger(ItemPredicate.Builder... p_299111_) {
        return RecipeProvider.inventoryTrigger(p_299111_);
    }


    public static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryTrigger(ItemPredicate... p_126012_) {
        return RecipeProvider.inventoryTrigger(p_126012_);
    }


    public static String getHasName(ItemLike p_176603_) {
        return RecipeProvider.getHasName(p_176603_);
    }


    public static String getItemName(ItemLike p_176633_) {
        return RecipeProvider.getItemName(p_176633_);
    }


    public static String getSimpleRecipeName(ItemLike p_176645_) {
        return RecipeProvider.getSimpleRecipeName(p_176645_);
    }


    public static String getConversionRecipeName(ItemLike p_176518_, ItemLike p_176519_) {
        return RecipeProvider.getConversionRecipeName(p_176518_, p_176519_);
    }


    public static String getSmeltingRecipeName(ItemLike p_176657_) {
        return RecipeProvider.getSmeltingRecipeName(p_176657_);
    }


    public static String getBlastingRecipeName(ItemLike p_176669_) {
        return RecipeProvider.getBlastingRecipeName(p_176669_);
    }

    @Override
    public Ingredient tag(TagKey<Item> p_364630_) {
        return super.tag(p_364630_);
    }

    @Override
    public ShapedRecipeBuilder shaped(RecipeCategory category, ItemStackTemplate stack) {
        return super.shaped(category, stack);
    }

    @Override
    public ShapedRecipeBuilder shaped(RecipeCategory p_360632_, ItemLike p_365035_) {
        return super.shaped(p_360632_, p_365035_);
    }

    @Override
    public ShapedRecipeBuilder shaped(RecipeCategory p_363994_, ItemLike p_365113_, int p_362095_) {
        return super.shaped(p_363994_, p_365113_, p_362095_);
    }

    @Override
    public ShapelessRecipeBuilder shapeless(RecipeCategory category, ItemStackTemplate result) {
        return super.shapeless(category, result);
    }

    @Override
    public ShapelessRecipeBuilder shapeless(RecipeCategory p_364319_, ItemLike p_364774_) {
        return super.shapeless(p_364319_, p_364774_);
    }

    @Override
    public ShapelessRecipeBuilder shapeless(RecipeCategory p_362256_, ItemLike p_363786_, int p_365368_) {
        return super.shapeless(p_362256_, p_363786_, p_365368_);
    }

    @Override
    public void nineBlockStorageRecipes(
        RecipeCategory unpackedFormCategory,
        ItemLike unpackedForm,
        RecipeCategory packedFormCategory,
        ItemLike packedForm,
        String packingRecipeId,
        @Nullable String packingRecipeGroup,
        String unpackingRecipeId,
        @Nullable String unpackingRecipeGroup
    ) {
        super.nineBlockStorageRecipes(
            unpackedFormCategory,
            unpackedForm,
            packedFormCategory,
            packedForm,
            packingRecipeId,
            packingRecipeGroup,
            unpackingRecipeId,
            unpackingRecipeGroup
        );
    }

    @Override
    public void nineBlockStorageRecipesRecipesWithCustomUnpacking(
        RecipeCategory unpackedFormCategory,
        ItemLike unpackedForm,
        RecipeCategory packedFormCategory,
        ItemLike packedForm,
        String unpackingRecipeId,
        String unpackingRecipeGroup
    ) {
        super.nineBlockStorageRecipesRecipesWithCustomUnpacking(
            unpackedFormCategory,
            unpackedForm,
            packedFormCategory,
            packedForm,
            unpackingRecipeId,
            unpackingRecipeGroup
        );
    }

    @Override
    public void nineBlockStorageRecipesWithCustomPacking(
        RecipeCategory unpackedFormCategory,
        ItemLike unpackedForm,
        RecipeCategory packedFormCategory,
        ItemLike packedForm,
        String packingRecipeId,
        String packingRecipeGroup
    ) {
        super.nineBlockStorageRecipesWithCustomPacking(
            unpackedFormCategory,
            unpackedForm,
            packedFormCategory,
            packedForm,
            packingRecipeId,
            packingRecipeGroup
        );
    }

    public RecipeOutput getOutput() {
        return this.output;
    }

    public HolderGetter<Item> getItems() {
        return this.items;
    }

    public HolderLookup.Provider getRegistries() {
        return this.registries;
    }
}
