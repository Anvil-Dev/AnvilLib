package dev.anvilcraft.lib.v2.renderer.projection;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;

/**
 * An independently owned, reusable projection mesh. Call on the render thread only.
 * Rebuild after scene changes or when {@link #isValid()} becomes false after resource reload.
 * The caller positions the pose in camera-relative space and closes the renderer when finished.
 */
public final class ProjectionRenderer implements AutoCloseable {
    private @Nullable ProjectionMesh mesh;
    private @Nullable ProjectionBuffers buffers;
    private @Nullable ProjectionScene scene;
    private List<BlockPos> positions = List.of();
    private final ProjectionOverlay overlay = new ProjectionOverlay();
    private List<BlockEntity> blockEntities = List.of();
    private List<Entity> entities = List.of();
    private int opacity;
    private int generation = -1;

    public boolean isValid() {
        return this.generation == ProjectionShaders.generation();
    }

    /** Bakes a complete scene; opacity is in the inclusive range 0 to 255. */
    public void rebuild(ProjectionScene view, int alpha) {
        RenderSystem.assertOnRenderThread();
        if (alpha < 0 || alpha > 255) throw new IllegalArgumentException("Opacity must be between 0 and 255");
        this.close();
        this.scene = view;
        this.positions = view.positions();
        int[] vertexEnds = new int[this.positions.size()];
        int[] vertexCount = {0};
        Minecraft mc = Minecraft.getInstance();
        try (ByteBufferBuilder memory = new ByteBufferBuilder(2_097_152)) {
            BufferBuilder buffer = new BufferBuilder(memory, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            VertexConsumer vertices = new GhostConsumer(buffer, BlockPos.ZERO, alpha, vertexCount);
            PoseStack pose = new PoseStack();
            RandomSource random = RandomSource.create();
            for (int index = 0; index < this.positions.size(); index++) {
                BlockPos pos = this.positions.get(index);
                var state = view.realState(pos);
                if (state.isAir()) {
                    vertexEnds[index] = vertexCount[0];
                    continue;
                }
                pose.pushPose();
                pose.translate(pos.getX(), pos.getY(), pos.getZ());
                ModelData data = view.getModelData(pos);
                var model = mc.getBlockRenderer().getBlockModel(state);
                for (RenderType type : model.getRenderTypes(state, random, data)) {
                    mc.getBlockRenderer().renderBatched(state, pos, view, pose, vertices, true, random, data, type);
                }
                pose.popPose();
                if (!state.getFluidState().isEmpty()) {
                    mc.getBlockRenderer().renderLiquid(BlockPos.ZERO, view.shifted(pos), new GhostConsumer(buffer, pos, alpha, vertexCount),
                        state, state.getFluidState());
                }
                vertexEnds[index] = vertexCount[0];
            }
            MeshData data = buffer.build();
            if (data != null) this.mesh = new ProjectionMesh(data, vertexEnds);
        } catch (RuntimeException | Error exception) {
            this.close();
            throw exception;
        }
        this.blockEntities = view.positions().stream().map(view::getBlockEntity).filter(Objects::nonNull).toList();
        this.entities = view.entities();
        this.opacity = alpha;
        this.generation = ProjectionShaders.generation();
    }

    /** Draws after flushing the caller's buffers, using private buffers for the depth and color passes. */
    public void render(PoseStack pose, Matrix4f projectionMatrix, MultiBufferSource.BufferSource buffers) {
        RenderSystem.assertOnRenderThread();
        if (!this.isValid()) return;
        buffers.endBatch();
        renderAll(List.of(new Prepared(pose, projectionMatrix, this.blockEntities, null, new BitSet(), new BitSet())));
    }

    /** Draw after world post-processing, in RenderLevelStageEvent.Stage.AFTER_LEVEL, to bypass shader-pack processing. */
    public void render(PoseStack pose, Matrix4f projectionMatrix, MultiBufferSource.BufferSource buffers,
                       BlockGetter level, BlockPos origin) {
        var prepared = this.prepare(pose, projectionMatrix, level, origin);
        if (prepared == null) return;
        buffers.endBatch();
        renderAll(List.of(prepared));
    }

    /** Captures a placement for a shared depth prepass. Consume it before rebuilding this renderer. */
    public @Nullable Prepared prepare(PoseStack pose, Matrix4f projectionMatrix, BlockGetter level, BlockPos origin) {
        RenderSystem.assertOnRenderThread();
        if (!this.isValid() || this.scene == null) return null;
        var missing = new BitSet(this.positions.size());
        var wrongStates = new BitSet(this.positions.size());
        var wrongBlocks = new BitSet(this.positions.size());
        var visibleEntities = new ArrayList<BlockEntity>();
        var worldPos = new BlockPos.MutableBlockPos();
        for (int index = 0; index < this.positions.size(); index++) {
            var pos = this.positions.get(index);
            var expected = this.scene.realState(pos);
            if (expected.isAir()) continue;
            worldPos.setWithOffset(origin, pos);
            var actual = level.getBlockState(worldPos);
            if (actual == expected) continue;
            if (actual.isAir()) {
                missing.set(index);
                var entity = this.scene.getBlockEntity(pos);
                if (entity != null) visibleEntities.add(entity);
            } else if (actual.is(expected.getBlock())) {
                wrongStates.set(index);
            } else {
                wrongBlocks.set(index);
            }
        }
        return new Prepared(pose, projectionMatrix, visibleEntities, missing, wrongStates, wrongBlocks);
    }

    /** Resolves occlusion across blocks, block entities and all placements before blending any color. */
    public static void renderAll(List<Prepared> projections) {
        RenderSystem.assertOnRenderThread();
        for (var projection : projections) projection.render(true);
        for (var projection : projections) projection.render(false);
        for (var projection : projections) projection.renderOverlay();
    }

    public final class Prepared {
        private final PoseStack pose = new PoseStack();
        private final Matrix4f projectionMatrix;
        private final List<BlockEntity> visibleEntities;
        private final @Nullable BitSet missing;
        private final BitSet wrongStates;
        private final BitSet wrongBlocks;

        private Prepared(PoseStack pose, Matrix4f projectionMatrix, List<BlockEntity> visibleEntities,
                         @Nullable BitSet missing, BitSet wrongStates, BitSet wrongBlocks) {
            this.pose.last().pose().set(pose.last().pose());
            this.pose.last().normal().set(pose.last().normal());
            this.projectionMatrix = new Matrix4f(projectionMatrix);
            this.visibleEntities = visibleEntities;
            this.missing = missing;
            this.wrongStates = wrongStates;
            this.wrongBlocks = wrongBlocks;
        }

        private void render(boolean depthOnly) {
            if (!ProjectionRenderer.this.isValid() || ProjectionRenderer.this.opacity == 0) return;
            boolean reversed = this.pose.last().pose().determinant3x3() < 0;
            int frontFace = reversed ? GL11.glGetInteger(GL11.GL_FRONT_FACE) : GL11.GL_CCW;
            if (reversed) GL11.glFrontFace(frontFace == GL11.GL_CCW ? GL11.GL_CW : GL11.GL_CCW);
            try {
                var renderer = ProjectionRenderer.this;
                if (renderer.mesh != null) renderer.mesh.draw(this.pose.last().pose(), this.projectionMatrix, this.missing, depthOnly);
                renderer.renderFeatures(this.visibleEntities, this.pose, depthOnly);
            } finally {
                if (reversed) GL11.glFrontFace(frontFace);
            }
        }

        private void renderOverlay() {
            if (!ProjectionRenderer.this.isValid()) return;
            ProjectionRenderer.this.overlay.render(this.pose.last().pose(), this.projectionMatrix, ProjectionRenderer.this.positions,
                this.wrongStates, this.wrongBlocks);
        }
    }

    private void renderFeatures(List<BlockEntity> blockEntities, PoseStack pose, boolean depthOnly) {
        if (this.buffers == null) this.buffers = new ProjectionBuffers();
        this.buffers.prepare(this.opacity, depthOnly);
        Minecraft mc = Minecraft.getInstance();
        for (BlockEntity entity : blockEntities) {
            pose.pushPose();
            try {
                BlockPos pos = entity.getBlockPos();
                pose.translate(pos.getX(), pos.getY(), pos.getZ());
                var renderer = mc.getBlockEntityRenderDispatcher().getRenderer(entity);
                if (renderer != null) renderer.render(entity, 0, pose, this.buffers,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            } finally {
                pose.popPose();
            }
        }
        for (Entity entity : this.entities) {
            mc.getEntityRenderDispatcher().render(entity, entity.getX(), entity.getY(), entity.getZ(),
                0, pose, this.buffers, LightTexture.FULL_BRIGHT);
        }
        this.buffers.endBatch();
    }

    @Override
    public void close() {
        if (this.mesh != null) {
            this.mesh.close();
            this.mesh = null;
        }
        if (this.buffers != null) this.buffers.close();
        this.buffers = null;
        this.overlay.close();
        this.scene = null;
        this.positions = List.of();
        this.blockEntities = List.of();
        this.entities = List.of();
        this.generation = -1;
    }
}
