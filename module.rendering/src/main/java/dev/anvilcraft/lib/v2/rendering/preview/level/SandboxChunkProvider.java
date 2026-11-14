package dev.anvilcraft.lib.v2.rendering.preview.level;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jspecify.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * {@link SandboxRenderLevel} 使用的轻量区块来源。
 *
 * <p>区块在首次访问时懒加载创建，并在预览会话期间常驻内存。</p>
 */
public class SandboxChunkProvider extends ChunkSource {
    private final SandboxRenderLevel level;

    private final Long2ObjectMap<SandboxLevelChunk> chunks = new Long2ObjectOpenHashMap<>();

    private final LevelLightEngine lightEngine;

    public SandboxChunkProvider(SandboxRenderLevel level) {
        this.level = level;
        this.lightEngine = new LevelLightEngine(this, true, true);
    }

    /**
     * 获取指定坐标的预览区块；若不存在则即时创建。
     */
    @Override
    public @Nullable ChunkAccess getChunk(int chunkX, int chunkZ, ChunkStatus chunkStatus, boolean load) {
        var chunkKey = ChunkPos.asLong(chunkX, chunkZ);
        var chunk = this.chunks.get(chunkKey);
        if (chunk == null) {
            chunk = new SandboxLevelChunk(this.level, new ChunkPos(chunkX, chunkZ));
            this.chunks.put(chunkKey, chunk);
        }
        return chunk;
    }

    @Override
    public void tick(BooleanSupplier booleanSupplier, boolean b) {

    }

    @Override
    public String gatherStats() {
        return "";
    }

    @Override
    public int getLoadedChunksCount() {
        return 0;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.lightEngine;
    }

    @Override
    public BlockGetter getLevel() {
        return this.level;
    }
}
