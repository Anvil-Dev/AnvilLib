package dev.anvilcraft.lib.v2.rendering.blur;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutDefinition;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutEntry;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObject;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.ShaderBufferObjectUsage;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2f;

@Setter
@Getter
public class BlurParametersUbo extends BufferObject<BlurParametersUbo> {

    public static final BufferObjectLayoutDefinition<BlurParametersUbo> DEFINITION = BufferObjectLayoutDefinition.create(
        BufferObjectLayoutEntry.<BlurParametersUbo>ofVec2f().forGetter(BlurParametersUbo::getDirection).build(),
        BufferObjectLayoutEntry.<BlurParametersUbo>ofFloat().forGetter(BlurParametersUbo::getSampleStepLength).build(),
        BufferObjectLayoutEntry.<BlurParametersUbo>ofFloat().forGetter(BlurParametersUbo::getColorMultiplier).build()
    );

    private float sampleStepLength;

    private float colorMultiplier;
    private Vector2f direction;

    public BlurParametersUbo(float sampleStepLength, float colorMultiplier, Vector2f direction) {
        super(BufferLayout.STD140, ShaderBufferObjectUsage.UBO);
        this.sampleStepLength = sampleStepLength;
        this.colorMultiplier = colorMultiplier;
        this.direction = direction;
    }

    @Override
    protected BufferObjectLayoutDefinition<BlurParametersUbo> getDefinition() {
        return DEFINITION;
    }
}
