package dev.anvilcraft.lib.v2.rendering.preview.level;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

/**
 * 预览区块实现，负责保持 {@link SandboxRenderLevel} 的方块与光照缓存同步。
 */
public class SandboxLevelChunk extends LevelChunk {
    public SandboxLevelChunk(SandboxRenderLevel level, ChunkPos pos) {
        super(level, pos);
    }

    private SandboxRenderLevel getSandboxLevel() {
        return (SandboxRenderLevel) getLevel();
    }

    /**
     * 方块状态变化时同步更新光照与非空气方块索引。
     */
    @Nullable
    public BlockState setBlockState(BlockPos pos, BlockState state, @Block.UpdateFlags int flags) {
        this.getSandboxLevel().refreshLightingAround(pos);

        var result = super.setBlockState(pos, state, flags);
        if (state.isAir()) {
            this.getSandboxLevel().removeFilledBlock(pos);
        } else {
            this.getSandboxLevel().addFilledBlock(pos);
        }
        return result;
    }

    public FullChunkStatus getFullStatus() {
        return FullChunkStatus.FULL;
    }
}
