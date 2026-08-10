package dev.anvilcraft.lib.v2.rendering.glitch;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObject;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutDefinition;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutEntry;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.ShaderBufferObjectUsage;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2f;

@Getter
@Setter
public class GlitchParametersUbo extends BufferObject<GlitchParametersUbo> {

    public static final BufferObjectLayoutDefinition<GlitchParametersUbo> DEFINITION = BufferObjectLayoutDefinition.create(
        BufferObjectLayoutEntry.<GlitchParametersUbo>ofVec2f().forGetter(GlitchParametersUbo::getInSize).build(),
        BufferObjectLayoutEntry.<GlitchParametersUbo>ofFloat().forGetter(GlitchParametersUbo::getGameTime).build()
    );

    private Vector2f inSize = new Vector2f();
    private float gameTime = 0;

    protected GlitchParametersUbo() {
        super(BufferLayout.STD140, ShaderBufferObjectUsage.UBO);
    }

    @Override
    protected BufferObjectLayoutDefinition<GlitchParametersUbo> getDefinition() {
        return DEFINITION;
    }
}
