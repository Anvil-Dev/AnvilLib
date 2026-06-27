package dev.anvilcraft.lib.v2.test.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.anvilcraft.lib.v2.explosion.ExplosionExecutor;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import dev.anvilcraft.lib.v2.test.recipe.RecipeIntegrationTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = AnvilLibTest.MOD_ID)
public class TestCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("anvillib").then(
                Commands.literal("test").then(
                    Commands.literal("explosion")
                        .then(
                            Commands.argument("radius", IntegerArgumentType.integer(1, 8192)).then(
                                Commands.argument("breaksPerTick", IntegerArgumentType.integer(1, 8192)).then(
                                    Commands.argument("probabilityRadius", IntegerArgumentType.integer(1, 8192)).then(
                                        Commands.argument("meltingRadius", IntegerArgumentType.integer(1, 8192)).executes(TestCommand::explosionAdvanced)
                                    )
                                )
                            )
                        )
                ).then(
                    Commands.literal("recipe")
                        .executes(TestCommand::recipe)
                )
            )
        );
    }

    public static int explosionAdvanced(CommandContext<CommandSourceStack> context) {
        int radius = IntegerArgumentType.getInteger(context, "radius");
        int breaksPerTick = IntegerArgumentType.getInteger(context, "breaksPerTick");
        int probabilityRadius = IntegerArgumentType.getInteger(context, "probabilityRadius");
        int meltingRadius = IntegerArgumentType.getInteger(context, "meltingRadius");

        ExplosionExecutor.create()
            .radius(radius)
            .maxBreakPreTick(breaksPerTick)
            .probabilityRadius(probabilityRadius)
            .meltingRadius(meltingRadius)
            .executor(context.getSource().getEntity())
            .execute(context.getSource().getLevel(), BlockPos.containing(context.getSource().getPosition()));
        return 1;
    }

    public static int recipe(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("This command must be run by a player"));
            return 0;
        }
        RecipeIntegrationTest.runAll(player);
        return 1;
    }
}
