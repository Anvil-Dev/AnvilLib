package dev.anvilcraft.lib.v2.yukkuri.api.vapor;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Global registry for large-cauldron vaporization methods. */
public final class VaporizationSources {
    private static final Map<ResourceLocation, VaporizationSource> SOURCES = new LinkedHashMap<>();

    private VaporizationSources() {
    }

    public static synchronized void register(VaporizationSource source) {
        Objects.requireNonNull(source, "source");
        ResourceLocation id = Objects.requireNonNull(source.id(), "source.id()");
        if (SOURCES.putIfAbsent(id, source) != null) {
            throw new IllegalArgumentException("A vaporization source is already registered as " + id);
        }
    }

    public static synchronized List<VaporizationSource> getSources() {
        List<VaporizationSource> result = new ArrayList<>(SOURCES.values());
        result.sort(Comparator.comparingInt(VaporizationSource::priority).reversed()
            .thenComparing(source -> source.id().toString()));
        return List.copyOf(result);
    }
}
