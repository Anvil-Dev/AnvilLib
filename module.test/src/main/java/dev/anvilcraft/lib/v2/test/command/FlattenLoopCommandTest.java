package dev.anvilcraft.lib.v2.test.command;

import dev.anvilcraft.lib.v2.test.AnvilLibTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.List;

/**
 * 在专用 GameTest 服务端驱动 {@code /anvillib test flatten-loop} 的回归。
 *
 * <p>通过服务端命令分派器真正执行一遍命令字符串，因此同时验证命令注册、Brigadier 参数解析与
 * {@link FlattenLoopTest} 的判定：命令返回正数代表全部断言通过，返回零代表存在失败项。
 * 该测试由 {@code -PflattenLoopTest} 打开，默认构建不生成用例。
 */
@GameTestHolder(AnvilLibTest.MOD_ID)
public final class FlattenLoopCommandTest {
    private FlattenLoopCommandTest() {
    }

    @GameTestGenerator
    public static List<TestFunction> generateTests() {
        if (!Boolean.getBoolean("anvillib.flattenLoopTests")) return List.of();
        return List.of(new TestFunction(
            "flatten_loop",
            AnvilLibTest.MOD_ID + ":flatten_loop_command",
            AnvilLibTest.MOD_ID + ":recipe_empty",
            100,
            0,
            true,
            FlattenLoopCommandTest::verify
        ));
    }

    private static void verify(GameTestHelper helper) {
        FlattenLoopTest.Result result = FlattenLoopTest.check();
        helper.assertTrue(result.failures().isEmpty(), "FlattenLoop assertions failed: " + result.failures());
        MinecraftServer server = helper.getLevel().getServer();
        CommandSourceStack source = server.createCommandSourceStack()
            .withLevel(helper.getLevel())
            .withPosition(helper.absolutePos(net.minecraft.core.BlockPos.ZERO).getCenter());
        int executed;
        try {
            executed = server.getCommands().getDispatcher().execute("anvillib test flatten-loop", source);
        } catch (Exception exception) {
            throw new AssertionError("The flatten-loop command is not registered or rejected its arguments", exception);
        }
        helper.assertTrue(executed == result.checks(), "Command must report every passing check, got " + executed);
        helper.succeed();
    }
}
