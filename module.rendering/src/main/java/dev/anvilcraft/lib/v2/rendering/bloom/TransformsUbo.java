package dev.anvilcraft.lib.v2.rendering.bloom;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutDefinition;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObjectLayoutEntry;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.BufferObject;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.object.ShaderBufferObjectUsage;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;

@Setter
@Getter
@ApiStatus.Internal
public class TransformsUbo extends BufferObject<TransformsUbo> {

    public static final BufferObjectLayoutDefinition<TransformsUbo> DEFINITION = BufferObjectLayoutDefinition.create(
            BufferObjectLayoutEntry.<TransformsUbo>ofMat4f().forGetter(TransformsUbo::getProjMat).build()
    );

    private Matrix4f projMat;

    public TransformsUbo(Matrix4f projMat) {
        super(BufferLayout.STD140, ShaderBufferObjectUsage.UBO);
        this.projMat = projMat;
    }

    @Override
    protected BufferObjectLayoutDefinition<TransformsUbo> getDefinition() {
        return DEFINITION;
    }
}
