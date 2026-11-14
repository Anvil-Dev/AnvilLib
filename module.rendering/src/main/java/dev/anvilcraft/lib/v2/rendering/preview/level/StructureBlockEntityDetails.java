package dev.anvilcraft.lib.v2.rendering.preview.level;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 从结构快照读取方块实体配置，不依赖模组的客户端同步数据或预览加载回调。 */
public final class StructureBlockEntityDetails {
    private static final String PREFIX = "tooltip.anvillib_rendering.structure_preview.";
    private static final Set<String> METADATA = Set.of("id", "x", "y", "z", "keepPacked");
    private static final int MAX_LINES = 4096;
    private static final int MAX_DEPTH = 32;

    private StructureBlockEntityDetails() {
    }

    /** 在放置结构前复制数据；坐标转换与渲染使用同一原点。 */
    public static Map<BlockPos, CompoundTag> capture(CompoundTag template, BlockPos placementPos) {
        Map<BlockPos, CompoundTag> result = new HashMap<>();
        for (Tag tag : template.getList("blocks", Tag.TAG_COMPOUND)) {
            if (!(tag instanceof CompoundTag block) || !(block.get("nbt") instanceof CompoundTag data)) {
                continue;
            }
            ListTag pos = block.getList("pos", Tag.TAG_INT);
            if (pos.size() != 3) {
                continue;
            }
            result.put(placementPos.offset(pos.getInt(0), pos.getInt(1), pos.getInt(2)), data.copy());
        }
        return result;
    }

    /** 未提供翻译的模组字段保留完整路径和值，嵌套库存、过滤器及组件也可查看。 */
    public static List<Component> describe(String blockId, CompoundTag data) {
        List<Component> lines = new ArrayList<>();
        String blockKey = PREFIX + "nbt." + blockId.replace(':', '.');
        for (String key : data.getAllKeys().stream().sorted().toList()) {
            if (!METADATA.contains(key)) {
                append(lines, blockKey, key, data.get(key), 0);
            }
        }
        if (lines.isEmpty()) {
            lines.add(Component.translatable(PREFIX + "empty_block_entity"));
        }
        return List.copyOf(lines);
    }

    private static void append(List<Component> lines, String blockKey, String path, Tag tag, int depth) {
        if (lines.size() >= MAX_LINES) {
            if (lines.size() == MAX_LINES) {
                lines.add(Component.translatable(PREFIX + "data_limit"));
            }
            return;
        }
        if (depth >= MAX_DEPTH) {
            lines.add(Component.literal(path + ": ").append(Component.translatable(PREFIX + "data_limit")));
            return;
        }
        if (tag instanceof CompoundTag compound && !compound.isEmpty()) {
            for (String key : compound.getAllKeys().stream().sorted().toList()) {
                append(lines, blockKey, path + "." + key, compound.get(key), depth + 1);
                if (lines.size() > MAX_LINES) {
                    break;
                }
            }
        } else if (tag instanceof ListTag list && !list.isEmpty()) {
            for (int i = 0; i < list.size() && lines.size() <= MAX_LINES; i++) {
                append(lines, blockKey, path + "[" + i + "]", list.get(i), depth + 1);
            }
        } else if (tag instanceof ByteArrayTag array) {
            appendArray(lines, blockKey, path, array.getAsByteArray().length, i -> Byte.toString(array.getAsByteArray()[i]));
        } else if (tag instanceof IntArrayTag array) {
            appendArray(lines, blockKey, path, array.getAsIntArray().length, i -> Integer.toString(array.getAsIntArray()[i]));
        } else if (tag instanceof LongArrayTag array) {
            appendArray(lines, blockKey, path, array.getAsLongArray().length, i -> Long.toString(array.getAsLongArray()[i]));
        } else {
            String value = tag instanceof StringTag string ? string.getAsString() : tag.toString();
            if (tag instanceof NumericTag) {
                value = value.replaceFirst("[bBsSlLfFdD]$", "");
            }
            lines.add(field(blockKey, path, value));
        }
    }

    private static void appendArray(List<Component> lines, String blockKey, String path, int size,
                                    java.util.function.IntFunction<String> value) {
        if (size == 0) {
            lines.add(field(blockKey, path, "[]"));
        }
        for (int i = 0; i < size; i++) {
            if (lines.size() >= MAX_LINES) {
                lines.add(Component.translatable(PREFIX + "data_limit"));
                break;
            }
            lines.add(field(blockKey, path + "[" + i + "]", value.apply(i)));
        }
    }

    private static Component field(String blockKey, String path, String value) {
        String fieldKey = blockKey + "." + path;
        Language language = Language.getInstance();
        MutableComponent label = Component.empty();
        for (String segment : path.split("\\.")) {
            if (!label.getSiblings().isEmpty()) {
                label.append(".");
            }
            int index = segment.indexOf('[');
            String name = index < 0 ? segment : segment.substring(0, index);
            label.append(Component.translatableWithFallback(PREFIX + "nbt.field." + name, name));
            if (index >= 0) {
                label.append(segment.substring(index));
            }
        }
        if (language.has(fieldKey)) {
            label = Component.translatable(fieldKey);
        }
        String leaf = path.substring(path.lastIndexOf('.') + 1);
        String valueKey = fieldKey + ".value." + value;
        if (!language.has(valueKey)) {
            valueKey = PREFIX + "nbt.field." + leaf + ".value." + value;
        }
        Component displayValue = Component.translatableWithFallback(valueKey, value);
        if (language.has(fieldKey + ".format")) {
            displayValue = Component.translatable(fieldKey + ".format", displayValue);
        }
        return label.copy().append(": ").append(displayValue).withStyle(ChatFormatting.GRAY);
    }
}
