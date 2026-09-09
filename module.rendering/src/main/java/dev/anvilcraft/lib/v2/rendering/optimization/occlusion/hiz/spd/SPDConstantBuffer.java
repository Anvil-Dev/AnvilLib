package dev.anvilcraft.lib.v2.rendering.optimization.occlusion.hiz.spd;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObject;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutDefinition;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutEntry;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.ShaderBufferObjectUsage;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector2f;

@Getter
@Setter
@ApiStatus.Internal
public class SPDConstantBuffer extends BufferObject<SPDConstantBuffer> {

    public static final BufferObjectLayoutDefinition<SPDConstantBuffer> DEFINITION = BufferObjectLayoutDefinition.create(
        BufferObjectLayoutEntry.<SPDConstantBuffer>ofInt().forGetter(SPDConstantBuffer::getMips).build(),
        BufferObjectLayoutEntry.<SPDConstantBuffer>ofInt().forGetter(SPDConstantBuffer::getNumWorkGroups).build(),
        BufferObjectLayoutEntry.<SPDConstantBuffer>ofVec2f().forGetter(SPDConstantBuffer::getWorkGroupOffset).build(),
        BufferObjectLayoutEntry.<SPDConstantBuffer>ofVec2f().forGetter(SPDConstantBuffer::getInvInputSize).build()
    );

    public static final int SIZE = DEFINITION.size(BufferLayout.STD140);

    /// The total number of mip levels SPD generates for each input texture slice, excluding input (mip 0)
    private int mips = 12;

    /// number of thread groups per slice
    private int numWorkGroups;

    /// The offset of the first 64x64 input tile in work-group coordinates, normally `(left / 64, top / 64)` for a
    /// downsampled subregion.
    private Vector2f workGroupOffset;

    /// The input texture size is `size = (width, height)`. This field stores
    /// `invInputSize = (1.0 / size.x, 1.0 / size.y)` for normalized UV conversion when linear sampling is enabled.
    private Vector2f invInputSize;

    protected SPDConstantBuffer() {
        super(BufferLayout.STD140, ShaderBufferObjectUsage.UBO);
    }

    @Override
    protected BufferObjectLayoutDefinition<SPDConstantBuffer> getDefinition() {
        return DEFINITION;
    }
}
