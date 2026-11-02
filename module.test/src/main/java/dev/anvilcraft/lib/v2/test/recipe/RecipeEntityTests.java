package dev.anvilcraft.lib.v2.test.recipe;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.cache.EntityCache;
import dev.anvilcraft.lib.v2.recipe.cache.ItemCache;
import dev.anvilcraft.lib.v2.recipe.outcome.IRecipeOutcome;
import dev.anvilcraft.lib.v2.recipe.outcome.SpawnEntity;
import dev.anvilcraft.lib.v2.recipe.outcome.SpawnItem;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.predicate.entity.HasEntity;
import dev.anvilcraft.lib.v2.recipe.predicate.entity.HasEntityIngredient;
import dev.anvilcraft.lib.v2.recipe.predicate.item.HasItemIngredient;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import io.netty.buffer.Unpooled;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.advancements.predicates.NbtPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import java.util.List;

/** 实体配方真实世界回归，覆盖占用、物品互斥和提交后的副作用。 */
final class RecipeEntityTests {
    private static Vec3 origin(GameTestHelper helper) {
        return Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 2, 3)));
    }

    private static Entity spawn(GameTestHelper helper, EntityType<?> type) {
        Entity entity = type.create(helper.getLevel(), EntitySpawnReason.COMMAND);
        if (entity == null) throw new IllegalStateException("Test entity could not be created");
        Vec3 pos = origin(helper);
        entity.snapTo(pos.x, pos.y, pos.z, 0, 0);
        entity.setNoGravity(true);
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    private static InWorldRecipeContext context(GameTestHelper helper) {
        return RecipeRegressionTests.context(helper, origin(helper));
    }

    private static HasEntityIngredient ingredient(EntityType<?> type) {
        return HasEntityIngredient.builder().of(type).range(4).build();
    }

    private static InWorldRecipe recipe(List<IRecipePredicate<?>> inputs, List<IRecipeOutcome<?>> outputs) {
        return RecipeRegressionTests.recipe(inputs, List.of(), outputs);
    }

    private static void assemble(GameTestHelper helper, InWorldRecipeContext context, InWorldRecipe recipe) {
        helper.assertTrue(recipe.matches(context, helper.getLevel()), "Entity recipe must match");
        recipe.assemble(context);
    }

    private static List<ItemEntity> items(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(origin(helper), origin(helper)).inflate(3));
    }

    private static int experience(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ExperienceOrb.class, new AABB(origin(helper), origin(helper)).inflate(3))
            .stream().mapToInt(ExperienceOrb::getValue).sum();
    }

    static void predicates(GameTestHelper helper) {
        Entity chicken = spawn(helper, EntityTypes.CHICKEN);
        InWorldRecipeContext context = context(helper);
        helper.assertTrue(HasEntity.builder().of(EntityTypes.CHICKEN).range(4).build().test(context), "Type predicate matches");
        helper.assertTrue(HasEntity.builder().of(BuiltInRegistries.ENTITY_TYPE, EntityTypeTags.FALL_DAMAGE_IMMUNE)
            .range(4).build().test(context), "Entity tags match");
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("NoGravity", true);
        helper.assertTrue(HasEntity.builder().predicate(builder -> builder.nbt(new NbtPredicate(tag))).range(4)
            .build().test(context), "Vanilla NBT predicates match");
        helper.assertTrue(!HasEntity.builder().of(EntityTypes.CHICKEN).range(4).count(2).build().test(context), "Count is checked");
        helper.assertTrue(!HasEntity.builder().of(EntityTypes.CHICKEN).offset(20, 0, 0).range(1).build().test(context), "Range and offset are checked");
        context.accept();
        helper.assertTrue(chicken.isAlive(), "Presence predicates do not consume entities");
        helper.succeed();
    }

    static void reservationsAndRollback(GameTestHelper helper) {
        Entity pig = spawn(helper, EntityTypes.PIG);
        HasEntityIngredient first = ingredient(EntityTypes.PIG);
        HasEntityIngredient second = ingredient(EntityTypes.PIG);
        InWorldRecipeContext context = context(helper);
        helper.assertTrue(!recipe(List.of(first, second), List.of()).matches(context, helper.getLevel()),
            "One entity cannot satisfy two consuming predicates");
        helper.assertTrue(context.getStack().isEmpty() && first.test(context) && pig.isAlive(), "Failed matching releases reservations");
        assemble(helper, context, recipe(List.of(first), List.of()));
        helper.assertTrue(pig.isAlive() && !first.test(context), "Assembly reserves without modifying the world");
        context.accept();
        context.accept();
        helper.assertTrue(pig.isRemoved() && items(helper).isEmpty() && experience(helper) == 0,
            "Default consumption removes exactly once without rewards");
        helper.succeed();
    }

    static void overlappingConditions(GameTestHelper helper) {
        Entity pig = spawn(helper, EntityTypes.PIG);
        Entity cow = spawn(helper, EntityTypes.COW);
        InWorldRecipeContext context = context(helper);
        HasEntityIngredient broad = HasEntityIngredient.builder().range(4).build();
        assemble(helper, context, recipe(List.of(broad, ingredient(EntityTypes.PIG)), List.of()));
        context.accept();
        helper.assertTrue(pig.isRemoved() && cow.isRemoved(), "Backtracking handles broad and narrow conditions");
        helper.succeed();
    }

    static void batches(GameTestHelper helper) {
        spawn(helper, EntityTypes.PIG);
        spawn(helper, EntityTypes.PIG);
        InWorldRecipeContext context = context(helper);
        InWorldRecipe recipe = recipe(List.of(ingredient(EntityTypes.PIG)), List.of());
        assemble(helper, context, recipe);
        assemble(helper, context, recipe);
        helper.assertTrue(!recipe.matches(context, helper.getLevel()), "Two batches cannot reserve a third entity");
        context.accept();
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(Pig.class, new AABB(origin(helper), origin(helper)).inflate(3)).isEmpty(),
            "All reserved batches are committed");
        helper.succeed();
    }

    static void itemConflict(GameTestHelper helper) {
        Vec3 pos = origin(helper);
        ItemEntity item = new ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, new ItemStack(Items.IRON_INGOT, 4));
        helper.getLevel().addFreshEntity(item);
        HasItemIngredient stack = HasItemIngredient.builder().of(Items.IRON_INGOT).count(1).range(4).build();
        HasEntityIngredient entity = ingredient(EntityTypes.ITEM);
        for (List<IRecipePredicate<?>> order : List.of(List.<IRecipePredicate<?>>of(stack, entity), List.<IRecipePredicate<?>>of(entity, stack))) {
            InWorldRecipeContext context = context(helper);
            helper.assertTrue(!recipe(order, List.of()).matches(context, helper.getLevel()), "Item and whole-entity reservations conflict in either order");
            helper.assertTrue(stack.getItem(context).getCount() == 4 && entity.test(context), "Both resource reservations are restored");
        }
        helper.assertTrue(item.getItem().getCount() == 4 && item.isAlive(), "Failed matching leaves the dropped stack intact");
        helper.succeed();
    }

    static void containerConflict(GameTestHelper helper) {
        ItemStacksResourceHandler handler = new ItemStacksResourceHandler(1);
        handler.set(0, ItemResource.of(Items.DIAMOND), 2);
        Vec3 pos = RecipeRegressionTests.addHandler(helper, handler, handler);
        InWorldRecipeContext context = RecipeRegressionTests.context(helper, pos);
        HasItemIngredient item = HasItemIngredient.builder().of(Items.DIAMOND).range(4).build();
        helper.assertTrue(item.test(context), "The entity inventory is a real item source");
        helper.assertTrue(!recipe(List.of(item, ingredient(EntityTypes.ARMOR_STAND)), List.of()).matches(context, helper.getLevel()),
            "A consumed container cannot also supply items");
        helper.assertTrue(item.test(context), "Failed matching restores inventory availability");
        var cachedOutput = context.get(ItemCache.ITEM_CACHE).getOutput(new ItemStack(Items.DIAMOND), pos);
        assemble(helper, context, recipe(List.of(ingredient(EntityTypes.ARMOR_STAND)), List.of()));
        cachedOutput.grow(new ItemStack(Items.DIAMOND), true);
        context.putAcceptor(ItemCache.ITEM_CACHE.location(), ItemCache.DEFAULT_ACCEPTOR);
        context.accept();
        helper.assertTrue(handler.getAmountAsInt(0) == 2, "Cached output does not write into a consumed entity");
        helper.assertTrue(items(helper).stream().filter(e -> e.getItem().is(Items.DIAMOND)).mapToInt(e -> e.getItem().getCount()).sum() == 1,
            "Output goes to the world instead of a consumed container");
        helper.succeed();
    }

    static void containerDrops(GameTestHelper helper) {
        Entity cart = spawn(helper, EntityTypes.CHEST_MINECART);
        ((Container) cart).setItem(0, new ItemStack(Items.DIAMOND, 2));
        InWorldRecipeContext first = context(helper);
        assemble(helper, first, recipe(List.of(ingredient(EntityTypes.CHEST_MINECART)), List.of()));
        first.accept();
        helper.assertTrue(cart.isRemoved() && items(helper).isEmpty(), "Default discard suppresses container contents");
        Entity other = spawn(helper, EntityTypes.CHEST_MINECART);
        ((Container) other).setItem(0, new ItemStack(Items.DIAMOND, 3));
        InWorldRecipeContext second = context(helper);
        assemble(helper, second, recipe(List.of(HasEntityIngredient.builder().of(EntityTypes.CHEST_MINECART)
            .range(4).dropItems(true).build()), List.of()));
        second.accept();
        helper.assertTrue(items(helper).stream().filter(e -> e.getItem().is(Items.DIAMOND)).mapToInt(e -> e.getItem().getCount()).sum() == 3,
            "Enabled container contents are released exactly once");
        helper.succeed();
    }

    static void experienceOnly(GameTestHelper helper) {
        rewardPig(helper);
        InWorldRecipeContext context = context(helper);
        assemble(helper, context, recipe(List.of(HasEntityIngredient.builder().of(EntityTypes.PIG).range(4)
            .experienceMultiplier(1.5f).build()), List.of()));
        context.accept();
        helper.assertTrue(experience(helper) == 7 && items(helper).isEmpty(), "Experience is floored independently of item drops");
        helper.succeed();
    }

    static void itemsOnly(GameTestHelper helper) {
        rewardPig(helper);
        InWorldRecipeContext context = context(helper);
        assemble(helper, context, recipe(List.of(HasEntityIngredient.builder().of(EntityTypes.PIG).range(4)
            .dropItems(true).build()), List.of()));
        context.accept();
        helper.assertTrue(items(helper).stream().anyMatch(item -> item.getItem().is(Items.PORKCHOP)) && experience(helper) == 0,
            "Loot can be enabled without experience or a death");
        helper.succeed();
    }

    static void gameRules(GameTestHelper helper) {
        boolean original = helper.getLevel().getGameRules().get(GameRules.MOB_DROPS);
        try {
            helper.getLevel().getGameRules().set(GameRules.MOB_DROPS, false, helper.getLevel().getServer());
            rewardPig(helper);
            InWorldRecipeContext context = context(helper);
            assemble(helper, context, recipe(List.of(HasEntityIngredient.builder().of(EntityTypes.PIG).range(4)
                .dropItems(true).experienceMultiplier(2).build()), List.of()));
            context.accept();
            helper.assertTrue(items(helper).isEmpty() && experience(helper) == 0, "Rewards respect the mob drops rule");
        } finally {
            helper.getLevel().getGameRules().set(GameRules.MOB_DROPS, original, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    static void failedCommit(GameTestHelper helper) {
        Entity pig = rewardPig(helper);
        InWorldRecipeContext context = context(helper);
        assemble(helper, context, recipe(List.of(HasEntityIngredient.builder().of(EntityTypes.PIG).range(4)
            .dropItems(true).experienceMultiplier(2).build()), List.of(SpawnEntity.builder().entity(EntityTypes.COW).build())));
        context.putAcceptor(AnvilLibTest.of("fail_entity_commit"), 50, ignored -> { throw new IllegalStateException("Expected test failure"); });
        expectFailure(helper, context::accept);
        helper.assertTrue(pig.isAlive() && items(helper).isEmpty() && experience(helper) == 0,
            "Earlier resource failure suppresses entity removal and rewards");
        helper.assertTrue(!HasEntity.builder().of(EntityTypes.COW).range(4).build().test(context(helper)), "Failed commits do not spawn entities");
        helper.succeed();
    }

    static void staleEntity(GameTestHelper helper) {
        Entity pig = spawn(helper, EntityTypes.PIG);
        InWorldRecipeContext context = context(helper);
        assemble(helper, context, recipe(List.of(ingredient(EntityTypes.PIG)),
            List.of(SpawnItem.builder().item(Items.DIAMOND).build())));
        pig.discard();
        expectFailure(helper, context::accept);
        helper.assertTrue(items(helper).isEmpty(), "Stale entity validation occurs before item output commits");
        helper.succeed();
    }

    static void spawning(GameTestHelper helper) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("id", "minecraft:cow");
        nbt.putBoolean("NoGravity", true);
        nbt.putIntArray("UUID", new int[]{1, 2, 3, 4});
        CompoundTag passenger = new CompoundTag();
        passenger.putString("id", "minecraft:chicken");
        passenger.putIntArray("UUID", new int[]{1, 2, 3, 4});
        ListTag passengers = new ListTag();
        passengers.add(passenger);
        nbt.put("Passengers", passengers);
        SpawnEntity output = SpawnEntity.builder().entity(EntityTypes.PIG).nbt(nbt).offset(0, 1, 0).count(2).build();
        InWorldRecipeContext context = context(helper);
        assemble(helper, context, recipe(List.of(), List.of(output)));
        helper.assertTrue(!HasEntity.builder().of(EntityTypes.PIG).range(6).build().test(context), "Prepared outputs remain invisible");
        context.accept();
        context.accept();
        List<Pig> pigs = helper.getLevel().getEntitiesOfClass(Pig.class, new AABB(origin(helper), origin(helper)).inflate(4));
        helper.assertTrue(pigs.size() == 2 && !pigs.getFirst().getUUID().equals(pigs.getLast().getUUID()), "Outputs have distinct identities and are spawned once");
        for (Pig pig : pigs) {
            helper.assertTrue(pig.isNoGravity() && pig.position().distanceToSqr(origin(helper).add(0, 1, 0)) < 0.001,
                "NBT and configured positions are applied");
            helper.assertTrue(pig.getPassengers().size() == 1 && !pig.getUUID().equals(pig.getFirstPassenger().getUUID()),
                "Passenger UUIDs are also regenerated");
        }
        helper.succeed();
    }

    static void validationAndCodecs(GameTestHelper helper) {
        expectFailure(helper, () -> HasEntityIngredient.builder().count(0).build());
        expectFailure(helper, () -> HasEntityIngredient.builder().experienceMultiplier(Float.NaN).build());
        expectFailure(helper, () -> HasEntityIngredient.builder().experienceMultiplier(-1).build());
        expectFailure(helper, () -> SpawnEntity.builder().entity(EntityTypes.PLAYER).build());
        var ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        HasEntityIngredient ingredient = HasEntityIngredient.builder().of(EntityTypes.PIG).range(4)
            .dropItems(true).experienceMultiplier(2.5f).build();
        var codec = IRecipePredicate.CODEC;
        HasEntityIngredient decoded = (HasEntityIngredient) codec.parse(ops, codec.encodeStart(ops, ingredient).getOrThrow()).getOrThrow();
        helper.assertTrue(decoded.dropItems() && decoded.experienceMultiplier() == 2.5f && decoded.count() == 1, "JSON preserves entity settings");
        HasEntityIngredient defaults = (HasEntityIngredient) codec.parse(ops, JsonParser.parseString(
            "{\"type\":\"anvillib_recipe:has_entity_ingredient\",\"predicate\":{}}")).getOrThrow();
        helper.assertTrue(!defaults.dropItems() && defaults.experienceMultiplier() == 0, "Missing options preserve default consumption");
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            ingredient.getType().streamCodec().encode(buffer, ingredient);
            HasEntityIngredient network = ingredient.getType().streamCodec().decode(buffer);
            SpawnEntity spawn = SpawnEntity.builder().entity(EntityTypes.PIG).count(2).build();
            spawn.getType().streamCodec().encode(buffer, spawn);
            SpawnEntity result = spawn.getType().streamCodec().decode(buffer);
            helper.assertTrue(network.experienceMultiplier() == 2.5f && network.dropItems()
                && result.entity() == EntityTypes.PIG && !buffer.isReadable(), "Network codecs preserve both entity types");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    static void players(GameTestHelper helper) {
        var player = helper.makeMockServerPlayerInLevel();
        Vec3 pos = origin(helper);
        player.snapTo(pos.x, pos.y, pos.z, 0, 0);
        InWorldRecipeContext context = context(helper);
        helper.assertTrue(!ingredient(EntityTypes.PLAYER).test(context), "Players cannot be ingredients");
        expectFailure(helper, () -> context.get(EntityCache.ENTITY_CACHE).reserve(List.of(player)));
        helper.assertTrue(HasEntity.builder().of(EntityTypes.PLAYER).range(4).build().test(context), "Players can be detected read-only");
        player.discard();
        helper.succeed();
    }

    private static Entity rewardPig(GameTestHelper helper) {
        RewardPig pig = new RewardPig(helper.getLevel());
        Vec3 pos = origin(helper);
        pig.snapTo(pos.x, pos.y, pos.z, 0, 0);
        pig.setNoAi(true);
        helper.getLevel().addFreshEntity(pig);
        return pig;
    }

    private static final class RewardPig extends Pig {
        private RewardPig(ServerLevel level) {
            super(EntityTypes.PIG, level);
        }

        @Override
        protected int getBaseExperienceReward(ServerLevel level) {
            return 5;
        }

        @Override
        public void die(DamageSource source) {
            throw new IllegalStateException("Recipe consumption must not trigger death");
        }
    }

    private static void expectFailure(GameTestHelper helper, Runnable action) {
        boolean failed = false;
        try {
            action.run();
        } catch (IllegalStateException | IllegalArgumentException expected) {
            failed = true;
        }
        helper.assertTrue(failed, "Invalid input or failed commits must be reported");
    }
}
