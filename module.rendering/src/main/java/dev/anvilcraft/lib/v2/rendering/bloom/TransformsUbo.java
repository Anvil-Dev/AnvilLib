package dev.anvilcraft.lib.v2.rendering.bloom;

import dev.anvilcraft.lib.v2.rendering.foundation.buffers.ubo.UboLayoutDefinition;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.ubo.UboLayoutEntry;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.ubo.UboObject;
import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4f;

@Setter
@Getter
public class TransformsUbo extends UboObject<TransformsUbo> {

    public static final UboLayoutDefinition<TransformsUbo> DEFINITION = UboLayoutDefinition.create(
            UboLayoutEntry.<TransformsUbo>ofMat4f().forGetter(TransformsUbo::getProjMat).build()
    );

    private Matrix4f projMat;

    public TransformsUbo(Matrix4f projMat) {
        this.projMat = projMat;
    }

    @Override
    protected UboLayoutDefinition<TransformsUbo> getDefinition() {
        return DEFINITION;
    }
}
