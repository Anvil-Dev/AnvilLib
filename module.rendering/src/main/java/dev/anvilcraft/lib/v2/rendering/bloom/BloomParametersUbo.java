package dev.anvilcraft.lib.v2.rendering.bloom;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutDefinition;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutEntry;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObject;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.ShaderBufferObjectUsage;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;

@Setter
@Getter
@ApiStatus.Internal
public class BloomParametersUbo extends BufferObject<BloomParametersUbo> {

    public static final BufferObjectLayoutDefinition<BloomParametersUbo> DEFINITION = BufferObjectLayoutDefinition.create(
            BufferObjectLayoutEntry.<BloomParametersUbo>ofFloat().forGetter(BloomParametersUbo::getBloomIntensity).build(),
            BufferObjectLayoutEntry.<BloomParametersUbo>ofFloat().forGetter(BloomParametersUbo::getBloomBlendThreshold).build(),
            BufferObjectLayoutEntry.<BloomParametersUbo>ofFloat().forGetter(BloomParametersUbo::getLuminanceSensitivity).build()
    );

    private float bloomIntensity;
    private float bloomBlendThreshold;
    private float luminanceSensitivity;

    public BloomParametersUbo(float bloomIntensity, float bloomBlendThreshold, float luminanceSensitivity) {
        super(BufferLayout.STD140, ShaderBufferObjectUsage.UBO);
        this.bloomIntensity = bloomIntensity;
        this.bloomBlendThreshold = bloomBlendThreshold;
        this.luminanceSensitivity = luminanceSensitivity;
    }

    @Override
    protected BufferObjectLayoutDefinition<BloomParametersUbo> getDefinition() {
        return DEFINITION;
    }
}
