package dev.anvilcraft.lib.v2.test.port;

import dev.anvilcraft.lib.v2.cube.client.CubeSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/** 通过 -PportSmoke 在独立运行目录验证移植，结束后自动关闭本次客户端。 */
@EventBusSubscriber(modid = "anvillib_test", value = Dist.CLIENT)
public final class PortSmokeClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(PortSmokeClient.class);
    private static final boolean ENABLED = Boolean.getBoolean("anvillib.portSmoke");
    private static boolean finished;
    private static boolean startedWorld;
    private static int worldTicks;
    private static boolean verified;
    private static int uiTicks;
    private static java.util.concurrent.CompletableFuture<Void> reload;
    private static int reloadTicks;
    private static java.util.concurrent.CompletableFuture<Void> runtime;
    private static boolean pickingVerified;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void prepare(ModelEvent.ModifyBakingResult event) {
        if (!ENABLED) return;
        CubeSelection.enableNamespace("minecraft");
        dev.anvilcraft.lib.v2.registrum.util.CreativeVariantPickerRegistry.register(
            net.minecraft.world.item.Items.STONE, net.minecraft.world.item.Items.DIRT);
        var samples = Set.of(Blocks.STONE, Blocks.OAK_FENCE, Blocks.LEVER, Blocks.GRINDSTONE, Blocks.OAK_LOG);
        for (var block : BuiltInRegistries.BLOCK) {
            if (!samples.contains(block)) CubeSelection.exclude(block);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ENABLED || finished || minecraft.getOverlay() != null) return;
        if (!startedWorld) {
            if (minecraft.screen == null) return;
            startedWorld = true;
            minecraft.options.renderDistance().set(2);
            minecraft.options.simulationDistance().set(5);
            minecraft.createWorldOpenFlows().createFreshLevel("port-verification-" + System.currentTimeMillis(),
                new net.minecraft.world.level.LevelSettings("AnvilLib port verification", net.minecraft.world.level.GameType.CREATIVE,
                    false, net.minecraft.world.Difficulty.NORMAL, true,
                    new net.minecraft.world.level.GameRules(net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS),
                    net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
                new net.minecraft.world.level.levelgen.WorldOptions(12345L, false, false),
                net.minecraft.world.level.levelgen.presets.WorldPresets::createFlatWorldDimensions, new TitleScreen());
            return;
        }
        if (minecraft.level == null || minecraft.player == null || ++worldTicks < 20) return;
        try {
            if (!verified) {
                for (var block : Set.of(Blocks.STONE, Blocks.OAK_FENCE, Blocks.LEVER, Blocks.GRINDSTONE, Blocks.OAK_LOG)) {
                    for (var state : block.getStateDefinition().getPossibleStates()) {
                        PortVerification.check(CubeSelection.modelParts(state, BlockPos.ZERO) != null, "模型捕获失败: " + state);
                    }
                }
                // 强制加载输入界面，验证 Mixin 的字段描述符和注入目标。
                Class.forName("net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen")
                    .getDeclaredField("selectedTab");
                Class.forName("net.minecraft.client.player.LocalPlayer");
                PortVerification.run();
                PortRuntimeVerification.common();
                runtime = PortRuntimeVerification.start(minecraft.getSingleplayerServer());
                PortUiVerification.open();
                verified = true;
                return;
            }
            if (++uiTicks == 10) {
                net.minecraft.client.Screenshot.grab(minecraft.gameDirectory, "port-creative-variants.png", minecraft.getMainRenderTarget(),
                    message -> LOGGER.info("Port UI screenshot: {}", message.getString()));
            }
            if (uiTicks == 30) {
                PortUiVerification.finish();
                PortPickingVerification.prepare();
            }
            if (uiTicks < 50) return;
            if (reload == null) {
                PortPickingVerification.verify();
                reload = minecraft.reloadResourcePacks();
                return;
            }
            if (!reload.isDone() || ++reloadTicks < 10) return;
            reload.join();
            if (!pickingVerified) {
                PortPickingVerification.verify();
                pickingVerified = true;
            }
            if (!PortGraphicsVerification.tick()) return;
            if (!runtime.isDone() && reloadTicks < 200) return;
            PortVerification.check(runtime.isDone(), "服务端活塞回归不能超时");
            runtime.join();
            if (!PortAttachmentVerification.tick()) return;
            finished = true;
            LOGGER.info("PORT_SMOKE_PASSED checks={} cube={}", PortVerification.checks(), CubeSelection.statistics());
            java.nio.file.Files.writeString(minecraft.gameDirectory.toPath().resolve("port-smoke-result.txt"), "PASS " + PortVerification.checks());
        } catch (Throwable error) {
            finished = true;
            LOGGER.error("PORT_SMOKE_FAILED", error);
            try {
                java.nio.file.Files.writeString(minecraft.gameDirectory.toPath().resolve("port-smoke-result.txt"), "FAIL " + error);
            } catch (java.io.IOException ignored) { }
        }
        minecraft.stop();
    }
}
