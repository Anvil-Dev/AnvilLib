package dev.anvilcraft.lib.v2.rendering.event;

import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePipeline;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RegisterComputePipelinesEvent extends Event implements IModBusEvent {
    private final List<ALRComputePipeline> pipelines = new ArrayList<>();

    public void registerPipeline(ALRComputePipeline pipeline) {
        this.pipelines.add(pipeline);
    }

    public List<ALRComputePipeline> getPipelines() {
        return Collections.unmodifiableList(this.pipelines);
    }
}
