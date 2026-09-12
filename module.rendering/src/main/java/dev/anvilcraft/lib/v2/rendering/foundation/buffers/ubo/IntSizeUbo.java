package dev.anvilcraft.lib.v2.rendering.foundation.buffers.ubo;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObject;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutDefinition;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutEntry;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.ShaderBufferObjectUsage;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;

/// ```glsl
/// layout(std140, binding = 0) uniform SizeParam {
///     int uWidth;
///     int uHeight;
/// };
/// ```
@Getter
@Setter
@ApiStatus.Internal
public class IntSizeUbo extends BufferObject<IntSizeUbo> {

    public static final BufferObjectLayoutDefinition<IntSizeUbo> DEFINITION = BufferObjectLayoutDefinition.create(
        BufferObjectLayoutEntry.<IntSizeUbo>ofInt().forGetter(IntSizeUbo::getHeight).build(),
        BufferObjectLayoutEntry.<IntSizeUbo>ofInt().forGetter(IntSizeUbo::getWidth).build()
    );

    public static final int SIZE = DEFINITION.size(BufferLayout.STD140);

    private int width;
    private int height;

    protected IntSizeUbo() {
        super(BufferLayout.STD140, ShaderBufferObjectUsage.UBO);
    }

    @Override
    protected BufferObjectLayoutDefinition<IntSizeUbo> getDefinition() {
        return DEFINITION;
    }
}
