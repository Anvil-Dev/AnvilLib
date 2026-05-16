package dev.anvilcraft.lib.v2.font.screen;

import dev.anvilcraft.lib.v2.font.AnvilLibFont;
import dev.anvilcraft.lib.v2.font.FontManager;
import dev.anvilcraft.lib.v2.font.screen.widget.Dropdown;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FontConfigScreen extends Screen {
    protected final Screen lastScreen;
    private Dropdown.@Nullable Shielding shielding;
    @SuppressWarnings("FieldCanBeLocal")
    private @Nullable Dropdown familyDropdown;
    private @Nullable Dropdown fontDropdown;
    private @Nullable Button testBtn;
    private Component selectedFamilyText = Component.empty();
    private Component selectedFontText = Component.empty();
    private final Component familyComponent = Component.translatable("screen.anvillib_font.config.family");
    private final Component fontComponent = Component.translatable("screen.anvillib_font.config.font");

    public FontConfigScreen(final ModContainer ignored, final Screen parent) {
        super(Component.translatable("screen.anvillib_font.config"));
        this.lastScreen = parent;
    }

    @Override
    protected void init() {
        int familyComponentWidth = this.font.width(this.familyComponent);
        int fontComponentWidth = this.font.width(this.fontComponent);
        int labelWidth = Math.max(familyComponentWidth, fontComponentWidth);
        int dropdownWidth = Math.clamp(this.width - 40, 180, 320);
        int btnWidth = dropdownWidth;
        int dropdownX = (this.width - dropdownWidth) / 2 + labelWidth + 10;
        int btnX = (this.width - btnWidth) / 2;
        dropdownWidth -= labelWidth + 10;
        int btnY = this.height / 2 - 24;
        int familyDropdownY = btnY + 28;
        int fontDropdownY = familyDropdownY + 28;

        this.shielding = null;

        this.familyDropdown = new Dropdown(dropdownX, familyDropdownY, dropdownWidth, 20, this.width, this.height, this.familyComponent);
        this.fontDropdown = new Dropdown(dropdownX, fontDropdownY, dropdownWidth, 20, this.width, this.height, this.fontComponent);
        this.testBtn = Button
            .builder(
                Component.translatable("screen.anvillib_font.config.test"),
                (_) -> this.minecraft.setScreen(new FontTestScreen(this))
            )
            .size(btnWidth, 20)
            .pos(btnX, btnY)
            .build();

        this.familyDropdown.setOnShieldingAdd(shielding -> this.shielding = shielding);
        this.familyDropdown.setOnShieldingRemove(() -> this.shielding = null);
        this.familyDropdown.setShieldingGetter(() -> this.shielding);

        this.fontDropdown.setOnShieldingAdd(shielding -> this.shielding = shielding);
        this.fontDropdown.setOnShieldingRemove(() -> this.shielding = null);
        this.fontDropdown.setShieldingGetter(() -> this.shielding);

        List<String> families = new ArrayList<>(FontManager.INSTANCE.getFamilyNames());
        families.sort(Comparator.comparing(String::toLowerCase));

        List<Dropdown.DropdownEntry> options = families.stream().map(name -> Dropdown.DropdownEntry.create(name, name)).toList();
        this.familyDropdown.setAllow(options);

        String configuredFamily = AnvilLibFont.CONFIG.getFontFamily();
        options.stream().filter(entry -> entry.id().equals(configuredFamily)).findFirst().ifPresent(this.familyDropdown::setValue);

        this.updateSelectedFamily(this.familyDropdown.getValue());
        this.familyDropdown.setOnValueChanged(entry -> {
            if (entry == null) {
                return;
            }
            AnvilLibFont.CONFIG.setFontFamily(entry.id());
            this.updateSelectedFamily(entry);
            this.refreshFontOptions(entry.id(), null, true);
        });

        this.fontDropdown.setOnValueChanged(entry -> {
            if (entry == null) {
                return;
            }
            AnvilLibFont.CONFIG.setFont(entry.id());
            this.updateSelectedFont(entry);
        });
        this.familyDropdown.setValue(AnvilLibFont.CONFIG.getFontFamily());

        this.refreshFontOptions(this.familyDropdown.getValueId(), AnvilLibFont.CONFIG.getFont(), false);

        this.familyDropdown.setValue(AnvilLibFont.CONFIG.getFont());

        this.addRenderableWidget(this.testBtn);
        this.addRenderableWidget(this.fontDropdown);
        this.addRenderableWidget(this.familyDropdown);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.centeredText(this.font, this.title, this.width / 2, 24, 0xFFFFFFFF);
        guiGraphics.centeredText(this.font, this.selectedFamilyText, this.width / 2, this.height / 2 - 56, 0xFFFFFFFF);
        if (fontDropdown != null) {
            guiGraphics.anvillib$centeredText(
                AnvilLibFont.getSelectFont(),
                this.selectedFontText,
                this.width / 2,
                this.height / 2 - 44,
                0xFFFFFFFF
            );
        }
        int dropdownWidth = Math.clamp(this.width - 40, 180, 320);
        int dropdownLabelX = (this.width - dropdownWidth) / 2;
        int familyDropdownLabelY = this.height / 2 - 24 + this.font.lineHeight / 2 + 28;
        int fontDropdownLabelY = familyDropdownLabelY + 28;
        guiGraphics.text(this.font, this.familyComponent, dropdownLabelX, familyDropdownLabelY, 0xFFFFFFFF);
        guiGraphics.text(this.font, this.fontComponent, dropdownLabelX, fontDropdownLabelY, 0xFFFFFFFF);
    }

    private void updateSelectedFamily(Dropdown.@Nullable DropdownEntry entry) {
        this.selectedFamilyText = entry == null
                                  ? Component.literal("Current family: <none>")
                                  : Component.literal("Current family: " + entry.id());
    }

    private void updateSelectedFont(Dropdown.@Nullable DropdownEntry entry) {
        this.selectedFontText = entry == null
                                ? Component.literal("Current font: <none>")
                                : Component.literal("Current font: " + entry.id());
    }

    private void refreshFontOptions(@Nullable String family, @Nullable String preferredFont, boolean persistSelected) {
        if (this.fontDropdown == null) {
            return;
        }
        if (family == null || family.isBlank() || !FontManager.INSTANCE.getFamilyNames().contains(family)) {
            this.fontDropdown.setAllow(List.of());
            this.updateSelectedFont(null);
            return;
        }

        List<Dropdown.DropdownEntry> fontOptions = FontManager.INSTANCE.getFamilyFontNames(family)
            .stream()
            .sorted(Comparator.comparing(String::toLowerCase))
            .map(name -> Dropdown.DropdownEntry.create(name, name))
            .toList();

        this.fontDropdown.setAllow(fontOptions);

        Dropdown.DropdownEntry selected = fontOptions.stream()
            .filter(entry -> entry.id().equals(preferredFont))
            .findFirst()
            .orElse(this.fontDropdown.getValue());

        this.fontDropdown.setValue(selected);
        this.updateSelectedFont(selected);

        if (persistSelected && selected != null) {
            AnvilLibFont.CONFIG.setFont(selected.id());
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }
}
