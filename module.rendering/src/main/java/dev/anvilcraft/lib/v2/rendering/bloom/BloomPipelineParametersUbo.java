package dev.anvilcraft.lib.v2.rendering.bloom;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutDefinition;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutEntry;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObject;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.ShaderBufferObjectUsage;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector2f;

@Getter
@Setter
@ApiStatus.Internal
public class BloomPipelineParametersUbo extends BufferObject<BloomPipelineParametersUbo> {

    public static final BufferObjectLayoutDefinition<BloomPipelineParametersUbo> DEFINITION = BufferObjectLayoutDefinition.create(
            BufferObjectLayoutEntry.<BloomPipelineParametersUbo>ofVec2f().forGetter(BloomPipelineParametersUbo::getResolution).build(),
            BufferObjectLayoutEntry.<BloomPipelineParametersUbo>ofInt().forGetter(BloomPipelineParametersUbo::getFrameIndex).build()
    );

    private final Vector2f      resolution = new Vector2f();
    private       int           frameIndex;

    protected BloomPipelineParametersUbo() {
        super(BufferLayout.STD140, ShaderBufferObjectUsage.UBO);
    }

    public void setResolution(int width, int height) {
        this.resolution.set(
                1.0f / Math.max(width, 1.0f),
                1.0f / Math.max(height, 1.0f)
        );
    }

    @Override
    protected BufferObjectLayoutDefinition<BloomPipelineParametersUbo> getDefinition() {
        return                  DEFINITION;
    }
}
