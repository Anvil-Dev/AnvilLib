package dev.anvilcraft.lib.v2.font;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.neoforged.fml.loading.FMLLoader;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Objects;

public class AnvilLibFontConfig {
    @SerializedName("_font_family_comment")
    private final String fontFamilyComment = "Select a font family";
    @Getter
    @SerializedName("font_family")
    private String fontFamily = FontManager.INSTANCE.getDefaultFont().getFamily();
    @SerializedName("_font_comment")
    private final String fontComment = "Select a font";
    @Getter
    @SerializedName("font")
    private String font = FontManager.INSTANCE.getDefaultFontFamily();

    void setValue(AnvilLibFontConfig config) {
        this.fontFamily = config.fontFamily;
        this.font = config.font;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
        AnvilLibFontConfigManager.saveConfig(this);
    }

    public void setFont(String font) {
        this.font = font;
        AnvilLibFontConfigManager.saveConfig(this);
    }

    @ApiStatus.Internal
    @Slf4j
    public static class AnvilLibFontConfigManager {
        public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
        public static @Nullable Path configPath = null;

        public static void readConfig(AnvilLibFontConfig config) {
            Path path = AnvilLibFontConfigManager.getConfigPath();
            File file = path.toFile();
            if (!file.exists()) {
                AnvilLibFontConfigManager.saveConfig(config);
                return;
            }
            try (FileReader fileReader = new FileReader(file)) {
                AnvilLibFontConfig fontConfig = GSON.fromJson(fileReader, AnvilLibFontConfig.class);
                config.setValue(fontConfig);
            } catch (Exception e) {
                log.error(e.getLocalizedMessage(), e);
            }
        }

        public static void saveConfig(AnvilLibFontConfig config) {
            Path path = AnvilLibFontConfigManager.getConfigPath();
            File file = path.toFile();
            if (!file.exists()) {
                //noinspection ResultOfMethodCallIgnored
                file.getParentFile().mkdirs();
            }
            try (FileWriter fileWriter = new FileWriter(file)) {
                GSON.toJson(config, fileWriter);
            } catch (Exception e) {
                log.error(e.getLocalizedMessage(), e);
            }
        }

        public static Path getConfigPath() {
            return Objects.requireNonNullElseGet(
                AnvilLibFontConfigManager.configPath,
                () -> AnvilLibFontConfigManager.configPath = FMLLoader.getCurrent()
                    .getGameDir()
                    .resolve("config")
                    .resolve(AnvilLibFont.MAIN_ID)
                    .resolve("%s-client.json".formatted(AnvilLibFont.MOD_ID))
            );
        }
    }
}
