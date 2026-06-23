package dev.anvilcraft.lib.v2.test.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.anvilcraft.lib.v2.explosion.ExplosionExecutor;
import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
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
                    Commands.literal("explosion").then(
                        Commands.argument("radius", IntegerArgumentType.integer(1, 8192)).then(
                            Commands.argument("breaksPerTick", IntegerArgumentType.integer(1, 8192)).executes(TestCommand::explosion)
                        )
                    )
                )
            )
        );
    }

    public static int explosion(CommandContext<CommandSourceStack> context) {
        int radius = IntegerArgumentType.getInteger(context, "radius");
        int breaksPerTick = IntegerArgumentType.getInteger(context, "breaksPerTick");
        ExplosionExecutor.create()
            .radius(radius)
            .maxBreakPreTick(breaksPerTick)
            .execute(context.getSource().getLevel(), BlockPos.containing(context.getSource().getPosition()));
        return 1;
    }
}
