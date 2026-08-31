package dev.anvilcraft.lib.v2.registrum.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class CreativeTabSections implements CreativeModeTab.Output {
    private static final int COLUMN_COUNT = 9;
    private static final Map<ResourceLocation, Layout> LAYOUTS = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, List<PlacedSection>> PLACEMENTS = new ConcurrentHashMap<>();

    private final CreativeModeTab.ItemDisplayParameters parameters;
    private final CreativeModeTab.Output output;
    private final List<SectionStart> sections = new ArrayList<>();
    private int parentItemCount;

    private CreativeTabSections(
        CreativeModeTab.ItemDisplayParameters parameters,
        CreativeModeTab.Output output
    ) {
        this.parameters = parameters;
        this.output = output;
    }

    public static void build(
        ResourceLocation tabId,
        CreativeModeTab.ItemDisplayParameters parameters,
        CreativeModeTab.Output output,
        Consumer<CreativeTabSections> contents
    ) {
        Objects.requireNonNull(tabId, "tabId");
        Objects.requireNonNull(parameters, "parameters");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(contents, "contents");
        CreativeTabSections sectionOutput = new CreativeTabSections(parameters, output);
        contents.accept(sectionOutput);
        if (sectionOutput.sections.isEmpty()) {
            LAYOUTS.remove(tabId);
            PLACEMENTS.remove(tabId);
            return;
        }
        LAYOUTS.put(tabId, new Layout(List.copyOf(sectionOutput.sections)));
    }

    public void section(CreativeTabSection section, Consumer<CreativeTabSections> contents) {
        this.sections.add(new SectionStart(Objects.requireNonNull(section, "section"), this.parentItemCount));
        Objects.requireNonNull(contents, "contents").accept(this);
    }

    @Override
    public void accept(ItemStack stack, CreativeModeTab.TabVisibility visibility) {
        this.output.accept(stack, visibility);
        if (visibility != CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY
            && stack.isItemEnabled(this.parameters.enabledFeatures())) {
            this.parentItemCount++;
        }
    }

    public static Collection<ItemStack> arrange(ResourceLocation tabId, Collection<ItemStack> items) {
        Layout layout = LAYOUTS.get(tabId);
        if (layout == null) return items;
        List<ItemStack> original = List.copyOf(items);
        List<ItemStack> arranged = new ArrayList<>(original.size());
        List<PlacedSection> placedSections = new ArrayList<>(layout.sections().size());
        int originalIndex = 0;

        for (int index = 0; index < layout.sections().size(); index++) {
            SectionStart sectionStart = layout.sections().get(index);
            int sectionItemIndex = Math.max(
                originalIndex,
                Math.min(sectionStart.itemIndex(), original.size())
            );
            arranged.addAll(original.subList(originalIndex, sectionItemIndex));
            int nextSectionItemIndex = index + 1 < layout.sections().size()
                ? Math.max(
                    sectionItemIndex,
                    Math.min(layout.sections().get(index + 1).itemIndex(), original.size())
                )
                : original.size();
            if (nextSectionItemIndex > sectionItemIndex) {
                while (arranged.size() % COLUMN_COUNT != 0) {
                    arranged.add(ItemStack.EMPTY);
                }
                int bannerIndex = arranged.size();
                for (int cell = 0; cell < sectionStart.section().bannerLength(); cell++) {
                    arranged.add(ItemStack.EMPTY);
                }
                placedSections.add(new PlacedSection(sectionStart.section(), bannerIndex));
            }
            originalIndex = sectionItemIndex;
        }

        arranged.addAll(original.subList(originalIndex, original.size()));
        PLACEMENTS.put(tabId, List.copyOf(placedSections));
        return arranged;
    }

    public static List<PlacedSection> placedSections(ResourceLocation tabId) {
        return PLACEMENTS.getOrDefault(tabId, List.of());
    }

    public record PlacedSection(CreativeTabSection section, int itemIndex) {
    }

    private record Layout(List<SectionStart> sections) {
    }

    private record SectionStart(CreativeTabSection section, int itemIndex) {
    }
}
