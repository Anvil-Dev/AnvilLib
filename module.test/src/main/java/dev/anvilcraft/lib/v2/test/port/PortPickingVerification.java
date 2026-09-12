package dev.anvilcraft.lib.v2.test.port;

import dev.anvilcraft.lib.v2.cube.client.CubeSelection;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

/** 实际放置旋转模型，通过准星拾取与高亮渲染检查客户端接入路径。 */
public final class PortPickingVerification {
    private static BlockPos target;

    public static void prepare() throws Exception {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(null);
        minecraft.options.highContrastBlockOutline().set(true);
        target = minecraft.player.blockPosition().offset(0, 0, 3).immutable();
        minecraft.getSingleplayerServer().submit(() -> {
            var level = minecraft.getSingleplayerServer().overworld();
            level.setBlockAndUpdate(target.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(target, Blocks.LEVER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE, AttachFace.FLOOR));
        }).get();
        minecraft.player.setPos(target.getX() + 0.5, target.getY(), target.getZ() - 2.0);
        minecraft.player.setYRot(0);
        minecraft.player.setXRot(28);
    }

    public static void verify() {
        Minecraft minecraft = Minecraft.getInstance();
        PortVerification.check(minecraft.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(target), "准星必须命中模型控制杆");
        PortVerification.check(CubeSelection.statistics().outlines().entries() > 0, "实际高亮必须进入 Cube 轮廓缓存");
        var id = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("anvillib_test", "port_exclusion");
        var state = minecraft.level.getBlockState(target);
        CubeSelection.registerTargetExclusion(id, block -> block.is(Blocks.LEVER));
        PortVerification.check(CubeSelection.target(minecraft.level, target, state, 1) == null, "运行时排除优先于帧缓存");
        CubeSelection.registerTargetExclusion(id, block -> false);
        PortVerification.check(CubeSelection.target(minecraft.level, target, state, 1) != null, "替换排除规则无需资源重载");
        PortVerification.check(CubeSelection.unregisterTargetExclusion(id), "按 ID 注销目标排除");
    }
}
