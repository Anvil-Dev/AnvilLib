package dev.anvilcraft.lib.v2.renderer.projection;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntBiFunction;

/**
 * Local projection geometry, separate from the real world. Populate all neighbors before baking.
 * Block entities and entities are supplied by the caller and are never added to or ticked in the world.
 */
public final class ProjectionScene implements BlockAndTintGetter {
    private final BlockAndTintGetter level;
    private final ToIntBiFunction<BlockPos, ColorResolver> tint;
    private final List<Entity> entities = new ArrayList<>();
    private int minY;
    private int maxY;
    private final BlockPos tintPos;
    private final Long2ObjectLinkedOpenHashMap<BlockState> blocks = new Long2ObjectLinkedOpenHashMap<>();
    private final Long2ObjectLinkedOpenHashMap<BlockEntity> blockEntities = new Long2ObjectLinkedOpenHashMap<>();

    public ProjectionScene(BlockAndTintGetter level, BlockPos tintPos) {
        this(level, tintPos, level::getBlockTint);
    }

    /** The tint callback receives world positions, while geometry uses local positions. */
    public ProjectionScene(BlockAndTintGetter level, BlockPos tintPos, ToIntBiFunction<BlockPos, ColorResolver> tint) {
        this.level = level;
        this.tintPos = tintPos.immutable();
        this.tint = tint;
    }

    public void addEntity(Entity entity) {
        this.entities.add(entity);
    }

    List<Entity> entities() {
        return List.copyOf(this.entities);
    }

    List<BlockPos> positions() {
        return this.blocks.keySet().longStream().mapToObj(BlockPos::of).toList();
    }

    public void put(BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        long key = pos.asLong();
        this.blocks.put(key, state);
        this.minY = Math.min(this.minY, pos.getY());
        this.maxY = Math.max(this.maxY, pos.getY());
        if (blockEntity != null) {
            this.blockEntities.put(key, blockEntity);
        } else {
            this.blockEntities.remove(key);
        }
    }

    BlockState realState(BlockPos pos) {
        BlockState state = this.blocks.get(pos.asLong());
        return state != null ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.realState(pos);
    }

    /** 把查询原点挪到 {@code origin},供 {@code renderLiquid} 在 (0,0,0) 写 0-1 顶点。 */
    BlockAndTintGetter shifted(BlockPos origin) {
        return new Shifted(this, origin.immutable());
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.realState(pos).getFluidState();
    }

    @Override
    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos) {
        return this.blockEntities.get(pos.asLong());
    }

    @Override
    public int getHeight() {
        return this.maxY - this.minY + 1;
    }

    @Override
    public int getMinBuildHeight() {
        return this.minY;
    }

    @Override
    public boolean isOutsideBuildHeight(int y) {
        return false;
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return this.level.getShade(direction, shade);
    }

    @Override
    public float getShade(float normalX, float normalY, float normalZ, boolean shade) {
        return this.level.getShade(normalX, normalY, normalZ, shade);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.level.getLightEngine();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
        return this.tint.applyAsInt(pos.offset(this.tintPos), colorResolver);
    }

    @Override
    public int getBrightness(LightLayer type, BlockPos pos) {
        return 15;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int amount) {
        return 15;
    }

    @Override
    public ModelData getModelData(BlockPos pos) {
        BlockEntity blockEntity = this.blockEntities.get(pos.asLong());
        return blockEntity != null ? blockEntity.getModelData() : ModelData.EMPTY;
    }

    private record Shifted(ProjectionScene view, BlockPos origin) implements BlockAndTintGetter {
        private BlockPos map(BlockPos pos) {
            return pos.offset(this.origin);
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return this.view.realState(this.map(pos));
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return this.view.getFluidState(this.map(pos));
        }

        @Override
        @Nullable
        public BlockEntity getBlockEntity(BlockPos pos) {
            return this.view.getBlockEntity(this.map(pos));
        }

        @Override
        public int getHeight() {
            return this.view.getHeight();
        }

        @Override
        public int getMinBuildHeight() {
            return this.view.getMinBuildHeight() - this.origin.getY();
        }

        @Override
        public boolean isOutsideBuildHeight(int y) {
            return false;
        }

        @Override
        public float getShade(Direction direction, boolean shade) {
            return this.view.getShade(direction, shade);
        }

        @Override
        public float getShade(float normalX, float normalY, float normalZ, boolean shade) {
            return this.view.getShade(normalX, normalY, normalZ, shade);
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return this.view.getLightEngine();
        }

        @Override
        public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
            return this.view.getBlockTint(this.map(pos), colorResolver);
        }

        @Override
        public int getBrightness(LightLayer type, BlockPos pos) {
            return 15;
        }

        @Override
        public int getRawBrightness(BlockPos pos, int amount) {
            return 15;
        }

        @Override
        public ModelData getModelData(BlockPos pos) {
            return this.view.getModelData(this.map(pos));
        }
    }
}
