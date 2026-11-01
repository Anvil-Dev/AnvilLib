package dev.anvilcraft.lib.v2.config;

import com.google.common.collect.ImmutableList;
import dev.anvilcraft.lib.v2.config.util.FormattingUtil;
import dev.anvilcraft.lib.v2.config.client.ConfigScreens;
import lombok.extern.slf4j.Slf4j;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import javax.annotation.Nullable;

@Slf4j
public class ConfigManager {
    private static final Map<String, ConfigManager> INSTANCES = new ConcurrentHashMap<>();

    private static final Map<ModConfigSpec.ConfigValue<?>, Slider.Range> SLIDERS = new ConcurrentHashMap<>();

    private final Map<Object, ConfigRecord> configSpecMap = new HashMap<>();
    private final String modId;

    private ConfigManager(String modId) {
        this.modId = modId;
    }

    public static <T> T register(String modId, Supplier<T> configFactory) {
        Optional<? extends ModContainer> byId = Optional.of(ModList.get()).flatMap(list -> list.getModContainerById(modId));
        ModContainer container;
        if (byId.isPresent()) {
            container = byId.get();
        } else {
            log.warn("Mod container not found for mod id {}", modId);
            return configFactory.get();
        }
        IEventBus bus = container.getEventBus();
        ConfigManager manager = INSTANCES.computeIfAbsent(
            modId, id -> {
                ConfigManager configManager = new ConfigManager(id);
                configManager.register(Objects.requireNonNull(bus));
                return configManager;
            }
        );
        T config = manager.register(configFactory.get());
        if (FMLLoader.getDist().isClient()) manager.registerScreen(container);
        return config;
    }

    @ApiStatus.Internal
    public <T> T register(T configObj) {
        Class<?> configClass = configObj.getClass();
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        if (configClass.isAnnotationPresent(Config.class)) {
            Config config = configClass.getAnnotation(Config.class);
            String name = config.name();
            String group = config.group();
            ModConfig.Type type = config.type();
            ImmutableList.Builder<ConfigField> valuesBuilder = ImmutableList.builder();
            try {
                ConfigManager.register(builder, name, null, configObj, valuesBuilder);
            } catch (IllegalAccessException e) {
                log.error(e.getMessage(), e);
            }
            ModConfigSpec spec = builder.build();
            this.configSpecMap.put(configObj, new ConfigRecord(group, name, type, spec, configObj, valuesBuilder.build()));
        }
        return configObj;
    }

    @ApiStatus.Internal
    public void register(IEventBus bus) {
        bus.register(this);
    }

    @ApiStatus.Internal
    @SubscribeEvent
    public void onModConstruct(FMLConstructModEvent event) {
        ModContainer container = ModLoadingContext.get().getActiveContainer();
        if (!this.modId.equals(container.getModId())) return;
        for (Map.Entry<Object, ConfigRecord> entry : this.configSpecMap.entrySet()) {
            ConfigRecord value = entry.getValue();
            if (value.registered().get()) return;
            value.registered().set(true);
            log.info("Registering {} for {}", value.getFileName(), container.getModId());
            container.registerConfig(value.type(), value.spec(), value.getFileName());
        }
    }

    @ApiStatus.Internal
    @SubscribeEvent
    public void loading(ModConfigEvent event) {
        this.configSpecMap.values().forEach(ConfigRecord::load);
    }

    @ApiStatus.Internal
    public void registerScreen(ModContainer container) {
        ConfigScreens.register(container, spec -> this.configSpecMap.values().stream()
            .filter(record -> record.spec() == spec)
            .map(record -> record.object().getClass().getAnnotation(Config.class).screenStyle())
            .findFirst().orElse(ConfigScreenStyle.DEFAULT));
    }

    private static void register(
        ModConfigSpec.Builder builder,
        String modId,
        @Nullable String parent,
        Object configObj,
        ImmutableList.Builder<ConfigField> values
    ) throws IllegalAccessException {
        Class<?> configClass = configObj.getClass();
        Field[] fields = configClass.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = FormattingUtil.toLowerCaseUnder(field.getName());
            String name = fieldName;
            if (parent != null) name = parent + "." + name;
            Object object = field.get(configObj);
            builder.translation(ConfigData.OPTION_STRING.formatted(modId, name));
            if (field.isAnnotationPresent(Comment.class)) {
                Comment comment = field.getAnnotation(Comment.class);
                builder.comment(comment.value());
            }
            if (field.isAnnotationPresent(CollapsibleObject.class)) {
                builder.push(fieldName);
                ConfigManager.register(builder, modId, name, object, values);
                builder.pop();
                continue;
            }
            ModConfigSpec.ConfigValue<?> value = ConfigManager.define(builder, fieldName, field, object);
            values.add(new ConfigField(configObj, field, value));
        }
    }

    @ApiStatus.Internal
    public static ModConfigSpec.ConfigValue<?> define(ModConfigSpec.Builder builder, String name, Field field, Object object) {
        Slider slider = field.getAnnotation(Slider.class);
        Slider.Range range = null;
        if (slider != null) {
            BoundedDiscrete bounds = field.getAnnotation(BoundedDiscrete.class);
            if (!(object instanceof Double || object instanceof Float) || bounds == null) {
                throw new IllegalArgumentException("@Slider requires a float/double field with @BoundedDiscrete: " + field);
            }
            range = new Slider.Range(bounds.min(), bounds.max(), slider.step());
        }
        ModConfigSpec.ConfigValue<?> value = switch (object) {
            case Number num -> ConfigManager.defineInRange(builder, name, field, num);
            case Enum<?> enumValue -> ConfigManager.defineEnum(builder, name, enumValue);
            case Boolean bool -> builder.define(name, bool.booleanValue());
            default -> builder.define(name, object);
        };
        if (range != null) SLIDERS.put(value, range);
        return value;
    }

    @ApiStatus.Internal
    public static @Nullable Slider.Range slider(ModConfigSpec.ConfigValue<?> value) {
        return SLIDERS.get(value);
    }

    /** Applies edited client values without writing the file or reloading server configuration. */
    @ApiStatus.Internal
    public static void previewClientValue(ModConfigSpec spec, List<String> path) {
        for (ConfigManager manager : INSTANCES.values()) {
            for (ConfigRecord record : manager.configSpecMap.values()) {
                if (record.spec() != spec || record.type() != ModConfig.Type.CLIENT || !spec.isLoaded()) continue;
                for (ConfigField field : record.values()) {
                    if (field.value().getPath().equals(path)) field.load();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @ApiStatus.Internal
    public static <E extends Enum<E>> ModConfigSpec.EnumValue<E> defineEnum(ModConfigSpec.Builder builder, String name, Enum<?> enumValue) {
        return builder.defineEnum(name, (E) enumValue);
    }

    @ApiStatus.Internal
    public static ModConfigSpec.ConfigValue<?> defineInRange(ModConfigSpec.Builder builder, String name, Field field, Number number) {
        BoundedDiscrete discrete = field.getAnnotation(BoundedDiscrete.class);
        // TOML 不保留 Byte/Short/Float 等 Java 包装类型。使用 NeoForge 的数值
        // ConfigValue 进行归一化，避免保存后反复校正，也让原生编辑控件识别这些字段。
        return switch (number) {
            case Byte value -> builder.defineInRange(name, value.intValue(),
                discrete == null ? Byte.MIN_VALUE : (int) BoundedDiscrete.Util.minByte(discrete),
                discrete == null ? Byte.MAX_VALUE : (int) BoundedDiscrete.Util.maxByte(discrete));
            case Short value -> builder.defineInRange(name, value.intValue(),
                discrete == null ? Short.MIN_VALUE : (int) BoundedDiscrete.Util.minShort(discrete),
                discrete == null ? Short.MAX_VALUE : (int) BoundedDiscrete.Util.maxShort(discrete));
            case Integer value -> builder.defineInRange(name, value.intValue(),
                discrete == null ? Integer.MIN_VALUE : BoundedDiscrete.Util.minInt(discrete),
                discrete == null ? Integer.MAX_VALUE : BoundedDiscrete.Util.maxInt(discrete));
            case Long value -> builder.defineInRange(name, value.longValue(),
                discrete == null ? Long.MIN_VALUE : BoundedDiscrete.Util.minLong(discrete),
                discrete == null ? Long.MAX_VALUE : BoundedDiscrete.Util.maxLong(discrete));
            case Float value -> builder.defineInRange(name, Double.parseDouble(value.toString()),
                discrete == null ? Double.NEGATIVE_INFINITY : BoundedDiscrete.Util.minDouble(discrete),
                discrete == null ? Double.POSITIVE_INFINITY : BoundedDiscrete.Util.maxDouble(discrete));
            case Double value -> builder.defineInRange(name, value.doubleValue(),
                discrete == null ? Double.NEGATIVE_INFINITY : BoundedDiscrete.Util.minDouble(discrete),
                discrete == null ? Double.POSITIVE_INFINITY : BoundedDiscrete.Util.maxDouble(discrete));
            default -> builder.define(name, number);
        };
    }
}
