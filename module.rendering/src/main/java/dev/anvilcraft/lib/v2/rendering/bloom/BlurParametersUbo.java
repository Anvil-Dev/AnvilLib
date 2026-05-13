package dev.anvilcraft.lib.v2.rendering.bloom;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.ubo.UboLayoutDefinition;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.ubo.UboLayoutEntry;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.ubo.UboObject;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2f;

@Setter
@Getter
public class BlurParametersUbo extends UboObject<BlurParametersUbo> {

    public static final UboLayoutDefinition<BlurParametersUbo> DEFINITION = UboLayoutDefinition.create(
        UboLayoutEntry.<BlurParametersUbo>ofVec2f().forGetter(BlurParametersUbo::getDirection).build(),
        UboLayoutEntry.<BlurParametersUbo>ofFloat().forGetter(BlurParametersUbo::getSampleStepLength).build(),
        UboLayoutEntry.<BlurParametersUbo>ofFloat().forGetter(BlurParametersUbo::getColorMultiplier).build()
    );

    private float sampleStepLength;

    private float colorMultiplier;
    private Vector2f direction;

    public BlurParametersUbo(float sampleStepLength, float colorMultiplier, Vector2f direction) {
        this.sampleStepLength = sampleStepLength;
        this.colorMultiplier = colorMultiplier;
        this.direction = direction;
    }

    @Override
    protected UboLayoutDefinition<BlurParametersUbo> getDefinition() {
        return DEFINITION;
    }
}
