package dev.anvilcraft.lib.v2.config;

import com.google.common.collect.ImmutableList;
import dev.anvilcraft.lib.v2.config.util.FormattingUtil;
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
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import javax.annotation.Nullable;

@Slf4j
public class ConfigManager {
    private static final Map<String, ConfigManager> INSTANCES = new ConcurrentHashMap<>();

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
        if (FMLLoader.getCurrent().getDist().isClient()) manager.registerScreen(container);
        return config;
    }

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

    public void register(IEventBus bus) {
        bus.register(this);
    }

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

    @SubscribeEvent
    public void loading(ModConfigEvent event) {
        this.configSpecMap.values().forEach(ConfigRecord::load);
    }

    public void registerScreen(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
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
            Object object = field.get(configObj);
            if (field.isAnnotationPresent(Comment.class)) {
                Comment comment = field.getAnnotation(Comment.class);
                String name = fieldName;
                if (parent != null) name = parent + "." + name;
                builder.translation(ConfigData.OPTION_STRING.formatted(modId, name));
                builder.comment(comment.value());
            }
            if (field.isAnnotationPresent(CollapsibleObject.class)) {
                builder.push(fieldName);
                ConfigManager.register(builder, modId, fieldName, object, values);
                builder.pop();
                continue;
            }
            ModConfigSpec.ConfigValue<?> value = ConfigManager.define(builder, fieldName, field, object);
            values.add(new ConfigField(configObj, field, value));
        }
    }

    public static ModConfigSpec.ConfigValue<?> define(ModConfigSpec.Builder builder, String name, Field field, Object object) {
        return switch (object) {
            case Number num -> ConfigManager.defineInRange(builder, name, field, num);
            case Enum<?> enumValue -> ConfigManager.defineEnum(builder, name, enumValue);
            case Boolean bool -> builder.define(name, bool.booleanValue());
            default -> builder.define(name, object);
        };
    }

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> ModConfigSpec.EnumValue<E> defineEnum(ModConfigSpec.Builder builder, String name, Enum<?> enumValue) {
        return builder.defineEnum(name, (E) enumValue);
    }

    public static ModConfigSpec.ConfigValue<?> defineInRange(ModConfigSpec.Builder builder, String name, Field field, Number number) {
        if (field.isAnnotationPresent(BoundedDiscrete.class)) {
            BoundedDiscrete discrete = field.getAnnotation(BoundedDiscrete.class);
            return switch (number) {
                case Byte value ->
                    builder.defineInRange(name, value, BoundedDiscrete.Util.minByte(discrete), BoundedDiscrete.Util.maxByte(discrete));
                case Short value ->
                    builder.defineInRange(name, value, BoundedDiscrete.Util.minShort(discrete), BoundedDiscrete.Util.maxShort(discrete));
                case Integer value ->
                    builder.defineInRange(name, value, BoundedDiscrete.Util.minInt(discrete), BoundedDiscrete.Util.maxInt(discrete));
                case Long value ->
                    builder.defineInRange(name, value, BoundedDiscrete.Util.minLong(discrete), BoundedDiscrete.Util.maxLong(discrete));
                case Float value ->
                    builder.defineInRange(name, value, BoundedDiscrete.Util.minFloat(discrete), BoundedDiscrete.Util.maxFloat(discrete));
                case Double value ->
                    builder.defineInRange(name, value, BoundedDiscrete.Util.minDouble(discrete), BoundedDiscrete.Util.maxDouble(discrete));
                default -> builder.define(name, number);
            };
        } else {
            return builder.define(name, number);
        }
    }
}
