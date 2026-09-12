package dev.anvilcraft.lib.v2.rendering.foundation.buffers.ubo;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObject;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutDefinition;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutEntry;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.ShaderBufferObjectUsage;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;

/// ```glsl
/// layout(std140) uniform Transforms {
///     mat4 ProjMat;
///     mat4 CameraViewMat;
///     mat4 ModelViewMat;
/// };
/// ```
@Setter
@Getter
@ApiStatus.Internal
public class FullTransformsUbo extends BufferObject<FullTransformsUbo> {

    public static final BufferObjectLayoutDefinition<FullTransformsUbo> DEFINITION = BufferObjectLayoutDefinition.create(
        BufferObjectLayoutEntry.<FullTransformsUbo>ofMat4f().forGetter(FullTransformsUbo::getProjMat).build(),
        BufferObjectLayoutEntry.<FullTransformsUbo>ofMat4f().forGetter(FullTransformsUbo::getCameraViewMat).build(),
        BufferObjectLayoutEntry.<FullTransformsUbo>ofMat4f().forGetter(FullTransformsUbo::getModelViewMat).build()
    );

    public static final int SIZE = DEFINITION.size(BufferLayout.STD140);

    private Matrix4f projMat;
    private Matrix4f cameraViewMat;
    private Matrix4f modelViewMat;

    public FullTransformsUbo() {
        super(BufferLayout.STD140, ShaderBufferObjectUsage.UBO);
        this.projMat = new Matrix4f();
        this.modelViewMat = new Matrix4f();
        this.cameraViewMat = new Matrix4f();
    }

    @Override
    protected BufferObjectLayoutDefinition<FullTransformsUbo> getDefinition() {
        return DEFINITION;
    }
}
