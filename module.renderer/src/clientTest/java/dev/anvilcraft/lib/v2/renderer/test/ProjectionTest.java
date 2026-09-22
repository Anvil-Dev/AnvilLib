package dev.anvilcraft.lib.v2.renderer.test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import dev.anvilcraft.lib.v2.renderer.projection.ProjectionRenderer;
import dev.anvilcraft.lib.v2.renderer.projection.ProjectionScene;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.CompletableFuture;

@Mod(value = "anvillib_renderer_test", dist = Dist.CLIENT)
public final class ProjectionTest {
    private final ProjectionRenderer first = new ProjectionRenderer();
    private final ProjectionRenderer second = new ProjectionRenderer();
    private boolean modelsReady;
    private int stage;
    private int frames;
    private CompletableFuture<Void> reload;

    public ProjectionTest(IEventBus bus) {
        bus.addListener(this::modelsReady);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::render);
    }

    private void modelsReady(ModelEvent.BakingCompleted event) {
        this.modelsReady = true;
    }

    private void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        mc.options.pauseOnLostFocus = false;
        GLFW.glfwHideWindow(mc.getWindow().getWindow());
        if (mc.getOverlay() != null || this.stage != 0 || !this.modelsReady) return;
        this.stage = 1;
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_SPAWN_CHUNK_RADIUS).set(0, null);
        LevelSettings settings = new LevelSettings("Projection regression", GameType.CREATIVE,
            false, Difficulty.PEACEFUL, false, rules, WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel("projection-" + System.currentTimeMillis(), settings,
            new WorldOptions(17, false, false),
            access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                .value().createWorldDimensions(), mc.screen);
    }

    private void render(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
            || mc.level == null || mc.player == null || mc.screen != null || mc.getOverlay() != null || this.stage == 4) return;
        try {
            if (this.stage == 1) {
                check(!ModList.get().isLoaded("anvilcraft"), "standalone renderer must not load AnvilCraft");
                this.verifyScene(mc);
                this.first.rebuild(scene(mc), 110);
                this.second.rebuild(scene(mc), 12);
                check(this.first.isValid() && this.second.isValid(), "independent meshes bake");
                this.stage = 2;
            }
            if (this.stage == 2) {
                draw(this.first, event, mc, 0);
                draw(this.second, event, mc, 4);
                if (++this.frames == 3) {
                    this.stage = 3;
                    this.reload = mc.reloadResourcePacks();
                }
            } else if (this.stage == 3 && this.reload.isDone()) {
                this.reload.join();
                check(!this.first.isValid() && !this.second.isValid(), "resource reload invalidates both meshes");
                this.first.rebuild(scene(mc), 110);
                draw(this.first, event, mc, 0);
                this.second.close();
                check(this.first.isValid() && !this.second.isValid(), "closing one renderer preserves the other");
                this.first.close();
                this.first.close();
                check(!this.first.isValid(), "close is repeatable");
                this.first.rebuild(new ProjectionScene(mc.level, BlockPos.ZERO), 0);
                draw(this.first, event, mc, 0);
                check(this.first.isValid(), "empty scene can be rebuilt after close");
                this.first.close();
                LogUtils.getLogger().info("PROJECTION_TEST ALL PASS: culling, tint, fluids, block entities, entities, "
                    + "mesh isolation, reload, cleanup");
                this.stage = 4;
                mc.stop();
            }
        } catch (Throwable exception) {
            LogUtils.getLogger().error("PROJECTION_TEST FAILED", exception);
            this.first.close();
            this.second.close();
            this.stage = 4;
            mc.stop();
        }
    }

    private static void draw(ProjectionRenderer renderer, RenderLevelStageEvent event, Minecraft mc, int offset) {
        PoseStack pose = new PoseStack();
        var pos = mc.player.position().subtract(event.getCamera().getPosition());
        pose.translate(pos.x + offset, pos.y, pos.z - 4);
        renderer.render(pose, event.getProjectionMatrix(), mc.renderBuffers().bufferSource());
    }

    private static ProjectionScene scene(Minecraft mc) {
        ProjectionScene scene = new ProjectionScene(mc.level, BlockPos.ZERO);
        scene.put(new BlockPos(-3, -2, -1), Blocks.RED_WOOL.defaultBlockState(), null);
        scene.put(new BlockPos(-2, -2, -1), Blocks.LIME_STAINED_GLASS.defaultBlockState(), null);
        scene.put(new BlockPos(-1, -2, -1), Blocks.WATER.defaultBlockState(), null);
        scene.put(new BlockPos(-1, -2, -2), Blocks.WATER.defaultBlockState(), null);
        BlockPos pos = new BlockPos(0, -2, -1);
        var state = Blocks.CHEST.defaultBlockState();
        var chest = new ChestBlockEntity(pos, state);
        chest.setLevel(mc.level);
        scene.put(pos, state, chest);
        var pig = EntityType.PIG.create(mc.level);
        check(pig != null, "create preview entity");
        pig.setPos(1, -2, -1);
        scene.addEntity(pig);
        return scene;
    }

    private void verifyScene(Minecraft mc) {
        var wool = Blocks.RED_WOOL.defaultBlockState();
        var glass = Blocks.LIME_STAINED_GLASS.defaultBlockState();
        pair(mc, wool, wool, Direction.EAST, false, false);
        pair(mc, glass, glass, Direction.UP, false, false);
        pair(mc, glass, Blocks.RED_STAINED_GLASS.defaultBlockState(), Direction.UP, true, true);
        pair(mc, wool, glass, Direction.EAST, true, false);
        pair(mc, wool, Blocks.AIR.defaultBlockState(), Direction.UP, true, true);
        pair(mc, wool, Blocks.REDSTONE_WIRE.defaultBlockState(), Direction.UP, true, false);
        var slab = Blocks.STONE_SLAB.defaultBlockState();
        pair(mc, slab, slab, Direction.EAST, false, false);
        pair(mc, wool, slab, Direction.EAST, true, false);
        ProjectionScene scene = scene(mc);
        BlockPos water = new BlockPos(-1, -2, -1);
        check(scene.getFluidState(water).isSource(), "local fluid lookup");
        check(scene.getMinBuildHeight() <= -2 && !scene.isOutsideBuildHeight(-2), "negative coordinates");
        BlockPos chestPos = new BlockPos(0, -2, -1);
        check(scene.getBlockEntity(chestPos) instanceof ChestBlockEntity, "block entity lookup");
        scene.put(chestPos, wool, null);
        check(scene.getBlockEntity(chestPos) == null, "replacement removes stale block entity");
        BlockPos origin = new BlockPos(15, 70, 18);
        ProjectionScene tint = new ProjectionScene(mc.level, origin, (pos, resolver) -> {
            check(pos.equals(origin.offset(water)), "world tint coordinates");
            return 0x123456;
        });
        check(tint.getBlockTint(water, (biome, x, z) -> 0) == 0x123456, "custom tint callback");
    }

    private static void pair(Minecraft mc, BlockState first, BlockState second, Direction direction, boolean ab, boolean ba) {
        ProjectionScene scene = new ProjectionScene(mc.level, BlockPos.ZERO);
        BlockPos pos = new BlockPos(-3, -2, -1);
        BlockPos adjacent = pos.relative(direction);
        scene.put(pos, first, null);
        scene.put(adjacent, second, null);
        check(scene.getBlockState(pos) == first && scene.getBlockState(adjacent) == second, "exact neighboring states");
        check(Block.shouldRenderFace(first, scene, pos, direction, adjacent) == ab, "first face culling");
        if (!second.isAir()) {
            check(Block.shouldRenderFace(second, scene, adjacent, direction.getOpposite(), pos) == ba, "second face culling");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
