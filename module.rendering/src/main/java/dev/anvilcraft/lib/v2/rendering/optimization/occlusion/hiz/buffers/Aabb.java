package dev.anvilcraft.lib.v2.rendering.optimization.occlusion.hiz.buffers;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObject;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutDefinition;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutEntry;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.ShaderBufferObjectUsage;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector4f;

/// One element of the shader's std430 `AABB` array.
///
/// ```glsl
/// struct AABB {
///     vec4 minPos;
///     vec4 maxPos;
/// };
/// ```
@Getter
@Setter
@ApiStatus.Internal
public class Aabb extends BufferObject<Aabb> {

    public static final BufferObjectLayoutDefinition<Aabb> DEFINITION = BufferObjectLayoutDefinition.create(
        BufferObjectLayoutEntry.<Aabb>ofVec4f().forGetter(Aabb::getMinPos).build(),
        BufferObjectLayoutEntry.<Aabb>ofVec4f().forGetter(Aabb::getMaxPos).build()
    );

    public static final int SIZE = DEFINITION.size(BufferLayout.STD430);

    private Vector4f minPos = new Vector4f(0, 0, 0, 0);
    private Vector4f maxPos = new Vector4f(0, 0, 0, 0);

    public Aabb() {
        super(BufferLayout.STD430, ShaderBufferObjectUsage.SSBO);
    }

    public void set(AABB aabb) {
        this.minPos.x = (float) aabb.minX;
        this.minPos.y = (float) aabb.minY;
        this.minPos.z = (float) aabb.minZ;
        this.minPos.w = 0;

        this.maxPos.x = (float) aabb.maxX;
        this.maxPos.y = (float) aabb.maxY;
        this.maxPos.z = (float) aabb.maxZ;
        this.maxPos.w = 0;
    }

    @Override
    protected BufferObjectLayoutDefinition<Aabb> getDefinition() {
        return DEFINITION;
    }

    public static Aabb of(AABB aabb) {
        Aabb a = new Aabb();
        a.set(aabb);
        return a;
    }
}
