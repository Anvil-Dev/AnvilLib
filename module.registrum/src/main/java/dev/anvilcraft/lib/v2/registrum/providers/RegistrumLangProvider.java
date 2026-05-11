/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/providers/RegistrateLangProvider.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.providers;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.util.nullness.NonNullSupplier;
import dev.anvilcraft.lib.v2.util.nullness.NonnullType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.data.LanguageProvider;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

public class RegistrumLangProvider extends LanguageProvider implements RegistrumProvider {

    private static class AccessibleLanguageProvider extends LanguageProvider {

        public AccessibleLanguageProvider(PackOutput packOutput, String modid, String locale) {
            super(packOutput, modid, locale);
        }

        @Override
        public void add(@Nullable String key, @Nullable String value) {
            super.add(key, value);
        }

        @Override
        protected void addTranslations() {
        }
    }

    private final AbstractRegistrum<?> owner;

    private final AccessibleLanguageProvider upsideDown;

    public RegistrumLangProvider(AbstractRegistrum<?> owner, PackOutput packOutput) {
        super(packOutput, owner.getModid(), "en_us");
        this.owner = owner;
        this.upsideDown = new AccessibleLanguageProvider(packOutput, owner.getModid(), "en_ud");
    }

    @Override
    public LogicalSide getSide() {
        return LogicalSide.CLIENT;
    }

    @Override
    public String getName() {
        return "Lang (en_us/en_ud)";
    }

    @Override
    protected void addTranslations() {
        owner.genData(ProviderType.LANG, this);
    }

    public static final String toEnglishName(String internalName) {
        return Arrays.stream(internalName.toLowerCase(Locale.ROOT).split("_"))
            .map(StringUtils::capitalize)
            .collect(Collectors.joining(" "));
    }

    @SuppressWarnings(
        {
            "unchecked",
            "ConstantConditions"
        }
    )
    public <T> String getAutomaticName(NonNullSupplier<? extends T> sup, ResourceKey<? extends Registry<T>> registry) {
        return toEnglishName(((Registry<Registry<T>>) BuiltInRegistries.REGISTRY).getValue(registry.identifier())
            .getKey(sup.get())
            .getPath());
    }

    public void addBlock(NonNullSupplier<? extends Block> block) {
        addBlock(block, getAutomaticName(block, Registries.BLOCK));
    }

    public void addBlockWithTooltip(NonNullSupplier<? extends Block> block, String tooltip) {
        addBlock(block);
        addTooltip(block, tooltip);
    }

    public void addBlockWithTooltip(NonNullSupplier<? extends Block> block, String name, String tooltip) {
        addBlock(block, name);
        addTooltip(block, tooltip);
    }

    public void addItem(NonNullSupplier<? extends Item> item) {
        addItem(item, getAutomaticName(item, Registries.ITEM));
    }

    public void addItemWithTooltip(NonNullSupplier<? extends Item> block, String name, List<@NonnullType String> tooltip) {
        addItem(block, name);
        addTooltip(block, tooltip);
    }

    public void addTooltip(NonNullSupplier<? extends ItemLike> item, String tooltip) {
        add(item.get().asItem().getDescriptionId() + ".desc", tooltip);
    }

    public void addTooltip(NonNullSupplier<? extends ItemLike> item, List<@NonnullType String> tooltip) {
        for (int i = 0; i < tooltip.size(); i++) {
            add(item.get().asItem().getDescriptionId() + ".desc." + i, tooltip.get(i));
        }
    }

    public void add(CreativeModeTab tab, String name) {
        var contents = tab.getDisplayName().getContents();
        if (contents instanceof TranslatableContents lang) {
            add(lang.getKey(), name);
        } else {
            throw new IllegalArgumentException("Creative tab does not have a translatable name: " + tab.getDisplayName());
        }
    }

    public void addEntityType(NonNullSupplier<? extends EntityType<?>> entity) {
        addEntityType(entity, getAutomaticName(entity, Registries.ENTITY_TYPE));
    }

    // Automatic en_ud generation

    private static final String NORMAL_CHARS =
        /* lowercase */ "abcdefghijklmnñopqrstuvwxyz" +
        /* uppercase */ "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
        /*  numbers  */ "0123456789" +
        /*  special  */ "_,;.?!/\\'\"()[]{}<>";
    private static final String UPSIDE_DOWN_CHARS =
        /* lowercase */ "ɐqɔpǝɟᵷɥᴉɾʞꞁɯuuodbɹsʇnʌʍxʎz" +
        /* uppercase */ "ⱯᗺƆᗡƎℲ⅁HIՐꞰꞀWNOԀꝹᴚS⟘∩ɅMX⅄Z" +
        /*  numbers  */ "0⥝ᘔƐ߈ϛ9ㄥ86" +
        /*  special  */ "‾'⸵˙¿¡/\\,„)(][}{><";

    private static final Pattern PLACEHOLDER_REGEX = Pattern.compile(
        "%(\\d+\\$)?([-#+ 0,(<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])"
    );

    static {
        if (NORMAL_CHARS.length() != UPSIDE_DOWN_CHARS.length()) {
            throw new AssertionError("Char maps do not match in length!");
        }
    }

    private String toUpsideDown(String normal) {
        if (normal.isEmpty()) return normal;

        Matcher matcher = PLACEHOLDER_REGEX.matcher(normal);

        List<int[]> placeholders = new ArrayList<>();
        while (matcher.find()) {
            placeholders.add(new int[]{matcher.start(), matcher.end()});
        }

        List<String> segments = new ArrayList<>();
        int lastEnd = 0;

        for (int[] ph : placeholders) {
            if (lastEnd <= ph[0]) {
                String text = normal.substring(lastEnd, ph[0]);
                segments.add(upsideDown(text));
            }
            segments.add(normal.substring(ph[0], ph[1]));
            lastEnd = ph[1];
        }

        if (lastEnd < normal.length()) {
            segments.add(upsideDown(normal.substring(lastEnd)));
        } else if (lastEnd == normal.length()) {
            segments.add("");
        }

        Collections.reverse(segments);

        StringBuilder result = new StringBuilder();
        for (String seg : segments) {
            result.append(seg);
        }
        return result.toString();
    }

    private static String upsideDown(String normal) {
        char[] ud = new char[normal.length()];
        for (int i = normal.length() - 1; i >= 0; i--) {
            char c = normal.charAt(i);
            int lookup = NORMAL_CHARS.indexOf(c);
            if (lookup >= 0) {
                c = UPSIDE_DOWN_CHARS.charAt(lookup);
            }
            ud[normal.length() - 1 - i] = c;
        }
        return new String(ud);
    }

    @Override
    public void add(String key, String value) {
        super.add(key, value);
        upsideDown.add(key, toUpsideDown(value));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(super.run(cache), upsideDown.run(cache));
    }
}
