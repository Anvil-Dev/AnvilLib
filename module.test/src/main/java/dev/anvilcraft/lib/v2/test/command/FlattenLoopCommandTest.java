package dev.anvilcraft.lib.v2.test.command;

import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 在专用 GameTest 服务端驱动 {@code /anvillib test flatten-loop} 的回归。
 *
 * <p>通过服务端命令分派器真正执行一遍命令字符串，因此同时验证命令注册、Brigadier 参数解析与
 * {@link FlattenLoopTest} 的判定：命令返回正数代表全部断言通过，返回零代表存在失败项。
 * 该测试由 {@code -PflattenLoopTest} 打开，默认构建不生成用例。
 */
@EventBusSubscriber(modid = AnvilLibTest.MOD_ID)
public final class FlattenLoopCommandTest {
    private static final String TEST_NAME = "flatten_loop";

    private FlattenLoopCommandTest() {
    }

    @SubscribeEvent
    public static void registerFunction(RegisterEvent event) {
        if (!Boolean.getBoolean("anvillib.flattenLoopTests")) return;
        event.register(
            Registries.TEST_FUNCTION,
            registry -> registry.register(AnvilLibTest.of(TEST_NAME), FlattenLoopCommandTest::verify)
        );
    }

    @SubscribeEvent
    public static void registerTest(RegisterGameTestsEvent event) {
        if (!Boolean.getBoolean("anvillib.flattenLoopTests")) return;
        var environment = event.registerEnvironment(AnvilLibTest.of(TEST_NAME));
        event.registerTest(
            AnvilLibTest.of(TEST_NAME),
            new FunctionGameTestInstance(
                ResourceKey.create(Registries.TEST_FUNCTION, AnvilLibTest.of(TEST_NAME)),
                new TestData<>(environment, AnvilLibTest.of("recipe_empty"), 100, 0, true)
            )
        );
    }

    private static void verify(GameTestHelper helper) {
        FlattenLoopTest.Result result = FlattenLoopTest.check();
        helper.assertTrue(
            result.failures().isEmpty(),
            Component.literal("FlattenLoop assertions failed: " + result.failures())
        );
        MinecraftServer server = helper.getLevel().getServer();
        CommandSourceStack source = server.createCommandSourceStack()
            .withLevel(helper.getLevel())
            .withPosition(Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)));
        int executed;
        try {
            executed = server.getCommands().getDispatcher().execute("anvillib test flatten-loop", source);
        } catch (Exception exception) {
            throw new AssertionError("The flatten-loop command is not registered or rejected its arguments", exception);
        }
        helper.assertTrue(
            executed == result.checks(),
            Component.literal("Command must report every passing check, got " + executed)
        );
        helper.succeed();
    }
}
