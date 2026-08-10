package dev.anvilcraft.lib.v2.rendering.event;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mod-bus event for registering compute pipelines.
 * <p>
 * Ported from 26.1: the original referenced {@code ALRComputePipeline} directly, which belongs to the
 * #P7e (GPU compute pipeline extension) port batch. To keep this batch independent, the pipeline type is
 * generic here; #P7e instantiates {@code RegisterComputePipelinesEvent<ALRComputePipeline>}.
 */
public class RegisterComputePipelinesEvent<T> extends Event implements IModBusEvent {
    private final List<T> pipelines = new ArrayList<>();

    public void registerPipeline(T pipeline) {
        this.pipelines.add(pipeline);
    }

    public List<T> getPipelines() {
        return Collections.unmodifiableList(this.pipelines);
    }
}
