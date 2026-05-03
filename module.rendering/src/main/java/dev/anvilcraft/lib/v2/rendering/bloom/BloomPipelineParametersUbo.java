package dev.anvilcraft.lib.v2.rendering.bloom;

import dev.anvilcraft.lib.v2.rendering.foundation.ubo.UboLayoutDefinition;
import dev.anvilcraft.lib.v2.rendering.foundation.ubo.UboLayoutEntry;
import dev.anvilcraft.lib.v2.rendering.foundation.ubo.UboObject;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2f;

@Getter
@Setter
public class BloomPipelineParametersUbo extends UboObject<BloomPipelineParametersUbo> {

    public static final UboLayoutDefinition<BloomPipelineParametersUbo> DEFINITION = UboLayoutDefinition.create(
            UboLayoutEntry.<BloomPipelineParametersUbo>ofVec2f().forGetter(BloomPipelineParametersUbo::getResolution).build(),
            UboLayoutEntry.<BloomPipelineParametersUbo>ofInt().forGetter(BloomPipelineParametersUbo::getFrameIndex).build()
    );

    private final Vector2f      resolution = new Vector2f();
    private             int     frameIndex;

    public void setResolution(int width, int height) {
        this.resolution.set(
                1.0f / Math.max(width, 1.0f),
                1.0f / Math.max(height, 1.0f)
        );
    }

    @Override
    protected UboLayoutDefinition<BloomPipelineParametersUbo> getDefinition() {
        return                  DEFINITION;
    }
}
