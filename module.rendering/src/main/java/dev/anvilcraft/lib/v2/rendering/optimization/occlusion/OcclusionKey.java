package dev.anvilcraft.lib.v2.rendering.optimization.occlusion;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.phys.AABB;

import java.util.function.Supplier;

/// Stable handle for associating one logical render feature with occlusion
/// state across frames. Reuse the same instance for the feature and update
/// its bounding box as needed; identity, not bounding-box equality, defines
/// the key.
public class OcclusionKey {

    @Getter
    @Setter
    private AABB boundingBox;

    @Getter
    private final Supplier<String> name;

    public OcclusionKey(AABB boundingBox) {
        this.boundingBox = boundingBox;
        this.name = OcclusionKey::defaultName;
    }

    public OcclusionKey(Supplier<String> name, AABB boundingBox) {
        this.name = name;
        this.boundingBox = boundingBox;
    }

    @Override
    public final boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public final int hashCode() {
        return System.identityHashCode(this);
    }

    private static String defaultName() {
        return "OcclusionKey";
    }
}
