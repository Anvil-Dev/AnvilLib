package dev.anvilcraft.lib.v2.util;

import com.mojang.brigadier.Message;
import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.players.CachedUserNameToIdResolver;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.net.URI;
import java.util.Date;
import java.util.UUID;
import javax.annotation.Nullable;

@UtilityClass
public class ComponentUtil {
    public static final Component TAB = Component.literal("  ");
    public static final Component LF = Component.literal("\n");
    public static final Component SPLITTER = Component.literal(",");
    public static final Component LIST_HEAD = Component.literal("[");
    public static final Component LIST_TAIL = Component.literal("]");
    public static final Component ITEM_HEAD = Component.literal("{");
    public static final Component ITEM_TAIL = Component.literal("}");

    public static Object[] argsValidate(Object... args) {
        for (int i = 0, argsLength = args.length; i < argsLength; i++) {
            args[i] = ComponentUtil.argValidate(args[i]);
        }
        return args;
    }

    public static Component argValidate(@Nullable Object arg) {
        return switch (arg) {
            case Component arg1 -> arg1;
            case String arg1 -> Component.literal(arg1);
            case Number arg1 -> Component.literal(arg1.toString());
            case Boolean arg1 -> Component.literal(arg1.toString());
            case Date date -> Component.translationArg(date);
            case Message msg -> Component.translationArg(msg);
            case UUID id -> Component.translationArg(id);
            case Identifier location -> Component.translationArg(location);
            case ChunkPos pos -> Component.translationArg(pos);
            case URI uri -> Component.translationArg(uri);
            case null -> Component.literal("null");
            default -> Component.literal(arg.toString());
        };
    }

    public static Component dimension(ResourceKey<Level> key) {
        return Component.translatable("dimension." + key.toString().replace(':', '.'));
    }

    public static Component findPlayerName(CachedUserNameToIdResolver cache, UUID id) {
        return cache.get(id)
            .map(NameAndId::name)
            .map(Component::literal)
            .orElse(Component.literal("Unknown[" + id + "]"));
    }
}
