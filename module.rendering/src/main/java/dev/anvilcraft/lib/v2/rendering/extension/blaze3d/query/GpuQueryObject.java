package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.query;

import dev.anvilcraft.lib.v2.rendering.foundation.GpuReusableResource;

public interface GpuQueryObject extends GpuReusableResource {

    void begin();

    void end();

    long getValue();

    void close();
}
