package dev.anvilcraft.lib.v2.rendering.optimization.occlusion.hiz.buffers;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObject;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutDefinition;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutEntry;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutEntryType;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.ShaderBufferObjectUsage;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector2i;

/// ```glsl
/// layout(std430, binding = 0) buffer ShaderInput {
///     ivec2 mipLayers[MAX_MIP_LEVELS + 1];
///     AABB aabbs[];
/// };
/// ```
@Getter
@Setter
@ApiStatus.Internal
public class OcclusionTestSSBO extends BufferObject<OcclusionTestSSBO> {

    public static final int MAX_MIP_LEVELS = 12;
    public static final int MIP_LAYER_COUNT = MAX_MIP_LEVELS + 1;

    private final BufferObjectLayoutEntryType.Array<Vector2i[]> mipLayersType =
        BufferObjectLayoutEntryType.createIVec2Array(MIP_LAYER_COUNT);

    private final BufferObjectLayoutEntryType.Array<Aabb[]> aabbsType = BufferObjectLayoutEntryType.createStructArray(
        Aabb.DEFINITION,
        0
    );

    private final BufferObjectLayoutEntryType.Array<Aabb[]> aabbsTypeForSizeCalc = BufferObjectLayoutEntryType.createStructArray(
        Aabb.DEFINITION,
        0
    );

    private final BufferObjectLayoutDefinition<OcclusionTestSSBO> definition = BufferObjectLayoutDefinition.create(
        BufferObjectLayoutEntry
            .<Vector2i[], OcclusionTestSSBO>builder(mipLayersType)
            .forGetter(OcclusionTestSSBO::getMipLayers)
            .build(),
        BufferObjectLayoutEntry
            .<Aabb[], OcclusionTestSSBO>builder(aabbsType)
            .forGetter(OcclusionTestSSBO::getAabbs)
            .build()
    );

    private final BufferObjectLayoutDefinition<OcclusionTestSSBO> definitionForSizeCalc = BufferObjectLayoutDefinition.create(
        BufferObjectLayoutEntry
            .<Vector2i[], OcclusionTestSSBO>builder(mipLayersType)
            .forGetter(OcclusionTestSSBO::getMipLayers)
            .build(),
        BufferObjectLayoutEntry
            .<Aabb[], OcclusionTestSSBO>builder(aabbsTypeForSizeCalc)
            .forGetter(OcclusionTestSSBO::getAabbs)
            .build()
    );

    private Vector2i[] mipLayers = createDefaultMipLayers();
    private Aabb[] aabbs = new Aabb[0];

    public OcclusionTestSSBO() {
        super(BufferLayout.STD430, ShaderBufferObjectUsage.SSBO);
    }

    public void setActualSize(int elementCount) {
        this.aabbsType.size(elementCount);
    }

    @Override
    public BufferObjectLayoutDefinition<OcclusionTestSSBO> getDefinition() {
        return this.definition;
    }

    private static Vector2i[] createDefaultMipLayers() {
        Vector2i[] mipLayers = new Vector2i[MIP_LAYER_COUNT];
        for (int i = 0; i < mipLayers.length; i++) {
            mipLayers[i] = new Vector2i();
        }
        return mipLayers;
    }

    /// the returned array pre-allocated all elements
    public @NonNull Aabb @NonNull [] getAabbs(int elementCount) {
        if (this.aabbs.length < elementCount) {
            this.aabbs = new Aabb[elementCount];
            for (int i = 0; i < this.aabbs.length; i++) {
                this.aabbs[i] = new Aabb();
            }
        }
        return this.aabbs;
    }

    public long actualSize(int elementCount) {
        this.aabbsTypeForSizeCalc.size(elementCount);
        return this.definitionForSizeCalc.size(BufferLayout.STD430);
    }
}
