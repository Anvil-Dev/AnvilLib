package dev.anvilcraft.lib.v2.rendering.bloom;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.ubo.UboLayoutDefinition;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.ubo.UboLayoutEntry;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.ubo.UboObject;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BloomParametersUbo extends UboObject<BloomParametersUbo> {

    public static final UboLayoutDefinition<BloomParametersUbo> DEFINITION = UboLayoutDefinition.create(
            UboLayoutEntry.<BloomParametersUbo>ofFloat().forGetter(BloomParametersUbo::getBloomIntensity).build(),
            UboLayoutEntry.<BloomParametersUbo>ofFloat().forGetter(BloomParametersUbo::getBloomBlendThreshold).build(),
            UboLayoutEntry.<BloomParametersUbo>ofFloat().forGetter(BloomParametersUbo::getLuminanceSensitivity).build()
    );

    private float bloomIntensity;
    private float bloomBlendThreshold;
    private float luminanceSensitivity;

    public BloomParametersUbo(float bloomIntensity, float bloomBlendThreshold, float luminanceSensitivity) {
        this.bloomIntensity = bloomIntensity;
        this.bloomBlendThreshold = bloomBlendThreshold;
        this.luminanceSensitivity = luminanceSensitivity;
    }

    @Override
    protected UboLayoutDefinition<BloomParametersUbo> getDefinition() {
        return DEFINITION;
    }
}
