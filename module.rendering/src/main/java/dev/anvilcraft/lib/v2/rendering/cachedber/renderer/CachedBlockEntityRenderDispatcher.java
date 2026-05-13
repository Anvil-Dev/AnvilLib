package dev.anvilcraft.lib.v2.rendering.cachedber.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class CachedBlockEntityRenderDispatcher {
    public static final CachedBlockEntityRenderDispatcher INSTANCE = new CachedBlockEntityRenderDispatcher();

    private final Map<BlockEntityType<?>, CachedBlockEntityRenderer<?, ?>> renderers = new HashMap<>();
    private final Logger logger = LogUtils.getLogger();

    public <T extends BlockEntity, S extends CachedBlockEntityRenderState> void registerRenderer(
        BlockEntityType<T> type,
        CachedBlockEntityRenderer<T, S> renderer
    ) {
        CachedBlockEntityRenderer<?, ?> old = renderers.put(type, renderer);
        if (old != null) {
            logger.warn("Replacing old CachedBlockEntityRenderDispatcher {} with {}", old, renderer);
        }
    }

    public <T extends BlockEntity, S extends CachedBlockEntityRenderState> void registerRenderer(
        Supplier<BlockEntityType<T>> type,
        CachedBlockEntityRenderer<T, S> renderer
    ) {
        registerRenderer(type.get(), renderer);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <E extends BlockEntity, S extends CachedBlockEntityRenderState> @Nullable S tryExtractRenderState(
        E blockEntity,
        float partialTicks,
        Camera camera
    ) {
        BlockEntityType<?> type = blockEntity.getType();
        CachedBlockEntityRenderer<E, S> renderer = (CachedBlockEntityRenderer) renderers.get(type);
        if (renderer == null) return null;
        S state = renderer.createRenderState();
        return renderer.extractRenderState(blockEntity, state, partialTicks, camera);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <S extends CachedBlockEntityRenderState> void submit(
        S renderState,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        CameraRenderState camera
    ) {
        BlockEntityType<?> type = renderState.blockEntityType;
        CachedBlockEntityRenderer<?, S> renderer = (CachedBlockEntityRenderer) renderers.get(type);
        if (renderer == null) return;

        renderer.submit(renderState, poseStack, submitNodeCollector, camera);
    }

    public boolean hasRenderer(BlockEntity be) {
        return renderers.containsKey(be.getType());
    }
}
