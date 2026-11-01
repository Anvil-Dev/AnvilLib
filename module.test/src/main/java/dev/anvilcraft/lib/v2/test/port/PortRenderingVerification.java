package dev.anvilcraft.lib.v2.test.port;

import dev.anvilcraft.lib.v2.rendering.ALROptimizations;
import dev.anvilcraft.lib.v2.font.AnvilLibFont;
import dev.anvilcraft.lib.v2.test.wheel.WheelDemoMenus;
import dev.anvilcraft.lib.v2.wheel.api.WheelSelectionEffect;
import dev.anvilcraft.lib.v2.wheel.client.gui.screen.WheelScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import dev.anvilcraft.lib.v2.rendering.cachedber.pipeline.CachedBlockEntityRenderingPipeline;
import dev.anvilcraft.lib.v2.test.all.TestBlocks;
import dev.anvilcraft.lib.v2.test.client.cber.TestCachedRenderer;
import dev.anvilcraft.lib.v2.test.client.compute.ComputeSupport;
import dev.anvilcraft.lib.v2.test.client.screen.GuiTestScreen;
import dev.anvilcraft.lib.v2.test.client.tesr.TestBloomTESR;
import dev.anvilcraft.lib.v2.test.client.tesr.TestOcclusionTESR;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import org.lwjgl.opengl.GL11;

/** 在真实世界中验证缓存 BER、泛光、遮挡、计算着色器和画中画路径。 */
public final class PortRenderingVerification {
    private static BlockPos origin;
    private static int ticks;

    public static void prepare() throws Exception {
        Minecraft minecraft = Minecraft.getInstance();
        origin = minecraft.player.blockPosition().offset(0, 0, 6).immutable();
        minecraft.getSingleplayerServer().submit(() -> {
            var level = minecraft.getSingleplayerServer().overworld();
            for (int dx : new int[]{-3, 0, 3}) {
                level.setBlockAndUpdate(origin.offset(dx, -1, 0), Blocks.STONE.defaultBlockState());
            }
            level.setBlockAndUpdate(origin.offset(-3, 0, 0), TestBlocks.TEST_CACHED_RENDERING.get().defaultBlockState());
            level.setBlockAndUpdate(origin, TestBlocks.TEST_BLOOM.get().defaultBlockState());
            level.setBlockAndUpdate(origin.offset(3, 0, 0), TestBlocks.TEST_OCCLUSION.get().defaultBlockState());
        }).get();
        TestCachedRenderer.submittedFrames = 0;
        TestBloomTESR.submittedFrames = 0;
        TestOcclusionTESR.submittedFrames = 0;
        minecraft.player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() - 6.0);
        minecraft.player.setYRot(0);
        minecraft.player.setXRot(0);
    }

    public static boolean tick() throws Exception {
        Minecraft minecraft = Minecraft.getInstance();
        ticks++;
        if (ticks == 10) {
            var blockEntity = minecraft.level.getBlockEntity(origin.offset(-3, 0, 0));
            PortVerification.check(blockEntity != null, "缓存渲染测试方块同步到客户端");
            CachedBlockEntityRenderingPipeline.getInstance().update(blockEntity, true);
        }
        if (ticks == 50) {
            PortVerification.check(TestCachedRenderer.submittedFrames > 0, "缓存 BER 必须实际提交模型");
            PortVerification.check(TestBloomTESR.submittedFrames > 0, "泛光 BER 必须实际提交模型");
            PortVerification.check(TestOcclusionTESR.submittedFrames > 0, "遮挡 BER 必须实际提交模型");
            var region = CachedBlockEntityRenderingPipeline.getInstance().getRenderRegion(ChunkPos.containing(origin.offset(-3, 0, 0)));
            PortVerification.check(!region.isEmpty(), "缓存 BER 必须保留可绘制的顶点缓冲");
            var statistics = ALROptimizations.getOcclusionCuller().collectStatistics();
            PortVerification.check(statistics == null || statistics.features() > 0, "遮挡器必须收到特性几何");
            PortVerification.check(statistics == null || statistics.rendered() > 0, "无遮挡演示方块必须通过遮挡测试：" + statistics);
            PortVerification.check(GL11.glGetError() == GL11.GL_NO_ERROR, "世界演示渲染不得产生 OpenGL 错误");
            screenshot("port-rendering-world.png");
            setOccluder(true);
        }
        if (ticks == 80) {
            var statistics = ALROptimizations.getOcclusionCuller().collectStatistics();
            PortVerification.check(statistics == null || statistics.culled() > 0, "实体墙后的演示方块必须被剔除：" + statistics);
            screenshot("port-rendering-occluded.png");
            setOccluder(false);
        }
        if (ticks == 110) {
            var statistics = ALROptimizations.getOcclusionCuller().collectStatistics();
            PortVerification.check(statistics == null || statistics.rendered() > 0, "移除遮挡墙后必须恢复绘制：" + statistics);
            float[] actual = ComputeSupport.INSTANCE.add(new float[]{1, 2, 3, 4}, 2);
            PortVerification.check(java.util.Arrays.equals(actual, new float[]{3, 4, 5, 6}), "GPU 计算及缓冲读回结果");
            minecraft.gui.setScreen(new GuiTestScreen() {
                @Override
                public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                    super.extractRenderState(graphics, mouseX, mouseY, partialTick);
                    graphics.anvillib$text(AnvilLibFont.getSelectFont(), "AnvilLib 26.2 字体渲染", 10, 5, 0xFFFFFFFF);
                }
            });
        }
        if (ticks == 140) {
            screenshot("port-rendering-gui.png");
            PortVerification.check(GL11.glGetError() == GL11.GL_NO_ERROR, "画中画与 GUI 渲染不得产生 OpenGL 错误");
        }
        if (ticks == 150) {
            minecraft.gui.setScreen(WheelScreen.tap(WheelDemoMenus.buildTapDemo(8, WheelSelectionEffect.ANNULAR_SECTOR)));
        }
        if (ticks == 180) {
            screenshot("port-rendering-wheel.png");
            PortVerification.check(GL11.glGetError() == GL11.GL_NO_ERROR, "轮盘模糊与选中效果不得产生 OpenGL 错误");
        }
        if (ticks < 190) return false;
        minecraft.gui.setScreen(null);
        return true;
    }

    private static void screenshot(String name) {
        Minecraft minecraft = Minecraft.getInstance();
        Screenshot.grab(minecraft.gameDirectory, name, minecraft.gameRenderer.mainRenderTarget(), 1, message -> {});
    }

    private static void setOccluder(boolean present) throws Exception {
        Minecraft.getInstance().getSingleplayerServer().submit(() -> {
            var level = Minecraft.getInstance().getSingleplayerServer().overworld();
            for (int x = 1; x <= 4; x++) {
                for (int y = 0; y <= 3; y++) {
                    level.setBlockAndUpdate(origin.offset(x, y, -2),
                        (present ? Blocks.STONE : Blocks.AIR).defaultBlockState());
                }
            }
        }).get();
    }
}
