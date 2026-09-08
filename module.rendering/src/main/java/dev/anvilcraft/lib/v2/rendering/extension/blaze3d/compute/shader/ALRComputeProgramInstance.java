package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader;

import dev.anvilcraft.lib.v2.rendering.AnvilLibRendering;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceBackendExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.NamedUniformAccess;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePipeline;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.Getter;
import net.minecraft.client.renderer.ShaderDefines;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

@ApiStatus.Internal
public final class ALRComputeProgramInstance implements NamedUniformAccess {
    public static final ALRComputeProgramInstance INVALID = new ALRComputeProgramInstance(
        0,
        new ALRComputeProgramInstanceKey(
            AnvilLibRendering.location("compute/invalid"),
            ShaderDefines.builder().build()
        ),
        null
    );

    private final int id;
    private final ALRComputeProgramInstanceKey key;
    @Getter
    private final ALRComputePipeline owner;

    private final Object2IntMap<String> uniformLocationCache = new Object2IntLinkedOpenHashMap<>();


    public ALRComputeProgramInstance(
        int id,
        ALRComputeProgramInstanceKey key,
        ALRComputePipeline owner
    ) {
        this.id = id;
        this.key = key;
        this.owner = owner;
    }

    @Override
    public int getUniformLocation(String name, ALRGpuDeviceBackendExtension device) {
        int orDefault = this.uniformLocationCache.getOrDefault(name, -2);
        if (orDefault == -2){
            orDefault = device.alrGetUniformLocation(this, owner, name);
            this.uniformLocationCache.put(name, orDefault);
        }
        return orDefault;
    }

    public int id() {
        return id;
    }

    public ALRComputeProgramInstanceKey key() {
        return key;
    }

    public ALRComputePipeline owner() {
        return owner;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ALRComputeProgramInstance) obj;
        return this.id == that.id &&
            Objects.equals(this.key, that.key) &&
            Objects.equals(this.owner, that.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, key, owner);
    }

    @Override
    public String toString() {
        return "ALRComputeProgramInstance[" +
            "id=" + id + ", " +
            "key=" + key + ", " +
            "owner=" + owner + ']';
    }

}
