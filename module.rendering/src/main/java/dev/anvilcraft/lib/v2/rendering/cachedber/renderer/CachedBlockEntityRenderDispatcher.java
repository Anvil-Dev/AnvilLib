package dev.anvilcraft.lib.v2.rendering.cachedber.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import javax.annotation.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Dispatcher for the {@link CachedBlockEntityRenderer}s registered with the cached BER pipeline.
 * <p>
 * Ported from 26.1: the two-phase {@code tryExtractRenderState}/{@code submit} entry points are replaced
 * by a single {@link #render} that mirrors {@code BlockEntityRenderDispatcher} (computing packed light /
 * overlay from the level, exactly like the vanilla {@code setupAndRender}).
 */
public class CachedBlockEntityRenderDispatcher {
    public static final CachedBlockEntityRenderDispatcher INSTANCE = new CachedBlockEntityRenderDispatcher();

    private final Map<BlockEntityType<?>, CachedBlockEntityRenderer<?>> renderers = new HashMap<>();
    private final Logger logger = LogUtils.getLogger();

    public <T extends BlockEntity> void registerRenderer(
        BlockEntityType<T> type,
        CachedBlockEntityRenderer<T> renderer
    ) {
        CachedBlockEntityRenderer<?> old = renderers.put(type, renderer);
        if (old != null) {
            logger.warn("Replacing old CachedBlockEntityRenderer {} with {}", old, renderer);
        }
    }

    public <T extends BlockEntity> void registerRenderer(
        Supplier<BlockEntityType<T>> type,
        CachedBlockEntityRenderer<T> renderer
    ) {
        registerRenderer(type.get(), renderer);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <E extends BlockEntity> void render(
        E blockEntity,
        float partialTick,
        PoseStack poseStack,
        MultiBufferSource bufferSource
    ) {
        CachedBlockEntityRenderer<E> renderer = (CachedBlockEntityRenderer) renderers.get(blockEntity.getType());
        if (renderer == null) {
            return;
        }
        Level level = blockEntity.getLevel();
        int packedLight;
        if (level != null) {
            BlockPos pos = blockEntity.getBlockPos();
            packedLight = LightTexture.pack(
                level.getRawBrightness(pos, level.getSkyDarken()),
                level.getRawBrightness(pos.above(), level.getSkyDarken())
            );
        } else {
            packedLight = 15728880;
        }
        renderer.render(blockEntity, partialTick, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
    }

    public boolean hasRenderer(BlockEntity be) {
        return renderers.containsKey(be.getType());
    }
}
