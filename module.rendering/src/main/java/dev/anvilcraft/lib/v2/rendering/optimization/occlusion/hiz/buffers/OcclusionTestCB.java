package dev.anvilcraft.lib.v2.rendering.optimization.occlusion.hiz.buffers;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObject;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutDefinition;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutEntry;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.ShaderBufferObjectUsage;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

/// ```glsl
/// layout(std430, binding = 0) uniform CB {
///     int elementCount;
///     int mipLevels;
///     vec2 viewportSize;
///     vec4 cameraPos;
///     mat4 ProjMat;
///     mat4 CameraMat;
/// } cbOcclusionTest;
/// ```
@Getter
@Setter
@ApiStatus.Internal
public class OcclusionTestCB extends BufferObject<OcclusionTestCB> {

    public static final BufferObjectLayoutDefinition<OcclusionTestCB> DEFINITION = BufferObjectLayoutDefinition.create(
        BufferObjectLayoutEntry.<OcclusionTestCB>ofInt().forGetter(OcclusionTestCB::getElementCount).build(),
        BufferObjectLayoutEntry.<OcclusionTestCB>ofInt().forGetter(OcclusionTestCB::getMipLevels).build(),
        BufferObjectLayoutEntry.<OcclusionTestCB>ofVec2f().forGetter(OcclusionTestCB::getViewportSize).build(),
        BufferObjectLayoutEntry.<OcclusionTestCB>ofVec4f().forGetter(OcclusionTestCB::getCameraPos).build(),
        BufferObjectLayoutEntry.<OcclusionTestCB>ofMat4f().forGetter(OcclusionTestCB::getProjMat).build(),
        BufferObjectLayoutEntry.<OcclusionTestCB>ofMat4f().forGetter(OcclusionTestCB::getCameraMat).build()
    );

    public static final int SIZE = DEFINITION.size(BufferLayout.STD430);

    private int elementCount;
    private int mipLevels;
    private Vector2f viewportSize = new Vector2f();
    private Vector4f cameraPos = new Vector4f(0, 0, 0, 1);
    private Matrix4f projMat = new Matrix4f();
    private Matrix4f cameraMat = new Matrix4f();
    public OcclusionTestCB() {
        super(BufferLayout.STD140, ShaderBufferObjectUsage.UBO);
    }

    @Override
    protected BufferObjectLayoutDefinition<OcclusionTestCB> getDefinition() {
        return DEFINITION;
    }

}
