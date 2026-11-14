package dev.anvilcraft.lib.v2.cube.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 规则与本轮烘焙几何一起发布，避免资源重载过程中使用不同批次的数据。 */
record CubeSelectionRules(Map<String, Mode> namespaces, Map<ResourceLocation, Mode> blocks) {
    static final CubeSelectionRules EMPTY = new CubeSelectionRules(Map.of(), Map.of());
    private static final String PATH = "anvillib/cube_selection.json";

    CubeSelectionRules {
        namespaces = Map.copyOf(namespaces);
        blocks = Map.copyOf(blocks);
    }

    Mode mode(ResourceLocation block, Mode fallback) {
        return this.blocks.getOrDefault(block, this.namespaces.getOrDefault(block.getNamespace(), fallback));
    }

    static CubeSelectionRules load(ResourceManager resources) {
        List<String> packs = resources.listPacks().map(PackResources::packId).toList();
        List<Source> sources = new ArrayList<>();
        resources.listResourceStacks("anvillib", id -> id.getPath().equals(PATH)).forEach((id, stack) -> {
            for (Resource resource : stack) {
                sources.add(new Source(id, resource, packs.indexOf(resource.sourcePackId())));
            }
        });
        // 先按资源包排序，保证不同配置命名空间也遵守资源包优先级；同一包内按资源 ID 排序。
        sources.sort(Comparator.comparingInt(Source::priority).thenComparing(source -> source.id().toString()));
        CubeSelectionRules rules = EMPTY;
        for (Source source : sources) {
            try (Reader reader = source.resource().openAsReader()) {
                rules = rules.merge(JsonParser.parseReader(reader).getAsJsonObject());
            } catch (IOException | RuntimeException exception) {
                LogUtils.getLogger().warn("Unable to load cube selection rules {} from {}; keeping lower-priority rules",
                    source.id(), source.resource().sourcePackId(), exception);
            }
        }
        return rules;
    }

    CubeSelectionRules merge(JsonObject json) {
        for (String key : json.keySet()) {
            if (!key.equals("namespaces") && !key.equals("blocks")) {
                throw new IllegalArgumentException("Unknown cube selection field: " + key);
            }
        }
        Map<String, Mode> namespaces = new HashMap<>(this.namespaces);
        Map<ResourceLocation, Mode> blocks = new HashMap<>(this.blocks);
        if (json.has("namespaces")) {
            for (var entry : json.getAsJsonObject("namespaces").entrySet()) {
                ResourceLocation.fromNamespaceAndPath(entry.getKey(), "cube_selection");
                namespaces.put(entry.getKey(), Mode.parse(entry.getValue()));
            }
        }
        if (json.has("blocks")) {
            for (var entry : json.getAsJsonObject("blocks").entrySet()) {
                blocks.put(ResourceLocation.parse(entry.getKey()), Mode.parse(entry.getValue()));
            }
        }
        // 整份文件校验成功后才替换，非法规则不会只应用一半。
        return new CubeSelectionRules(namespaces, blocks);
    }

    enum Mode {
        MODEL, OUTLINE, VANILLA;

        static Mode parse(JsonElement value) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Cube selection mode must be a string");
            }
            return switch (value.getAsString()) {
                case "model" -> MODEL;
                case "outline" -> OUTLINE;
                case "vanilla" -> VANILLA;
                default -> throw new IllegalArgumentException("Unknown cube selection mode: " + value);
            };
        }
    }

    private record Source(ResourceLocation id, Resource resource, int priority) { }
}
