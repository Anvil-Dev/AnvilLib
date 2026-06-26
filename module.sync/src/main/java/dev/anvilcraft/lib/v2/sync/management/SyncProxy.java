package dev.anvilcraft.lib.v2.sync.management;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.sync.AnvilLibSync;
import dev.anvilcraft.lib.v2.sync.util.SyncDirection;
import dev.anvilcraft.lib.v2.util.Util;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Quaternionfc;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Objects;

@Slf4j
public class SyncProxy<T> {
    @Getter
    @ApiStatus.Internal
    private final StreamCodec<? extends ByteBuf, T> codec;
    @Setter
    @ApiStatus.Internal
    private @Nullable SyncManager manager;
    @Setter
    @ApiStatus.Internal
    private @Nullable Object parent;
    @Setter
    @Getter
    @ApiStatus.Internal
    private @Nullable String fieldName;
    private volatile @Nullable T value;
    @Getter
    private SyncDirection direction = SyncDirection.BOTH;

    public SyncProxy(T value) {
        this.value = value;
        this.codec = Objects.requireNonNull(
            SyncProxy.defaultCodec(Util.cast(value.getClass())),
            "No default codec for type: " + value.getClass().getName()
        );
    }

    public SyncProxy(T value, StreamCodec<? extends ByteBuf, T> codec) {
        this.value = value;
        this.codec = codec;
    }

    public SyncProxy(Class<T> type) {
        this.value = null;
        this.codec = Objects.requireNonNull(SyncProxy.defaultCodec(type), "No default codec for type: " + type.getName());
    }

    public SyncProxy(StreamCodec<? extends ByteBuf, T> codec) {
        this.value = null;
        this.codec = codec;
    }

    public SyncProxy<T> direction(SyncDirection direction) {
        this.direction = direction;
        return this;
    }

    @ApiStatus.Internal
    public void setDirection(SyncDirection direction) {
        if (this.direction == SyncDirection.BOTH) this.direction = direction;
    }

    public <B extends ByteBuf> void setValue(B buf, boolean serverbound) {
        T oldValue = this.value;
        boolean isNull = buf.readBoolean();
        T newValue;
        if (isNull) {
            newValue = null;
        } else {
            newValue = Util.<StreamCodec<B, T>>cast(this.getCodec()).decode(buf);
        }
        this.value = newValue;
        if (!serverbound) return;
        if (this.manager == null) {
            this.manager = AnvilLibSync.SYNC_MANAGER;
        }
        this.manager.setValue(parent, this, oldValue, newValue);
    }

    public @Nullable T getValue() {
        if (this.manager == null) {
            this.manager = AnvilLibSync.SYNC_MANAGER;
        }
        this.manager.getValue(parent, this, this.value);
        return this.value;
    }

    public void setValue(@Nullable T value) {
        T old = this.value;
        this.value = value;
        if (this.manager == null) {
            this.manager = AnvilLibSync.SYNC_MANAGER;
        }
        this.manager.setValue(parent, this, old, this.value);
    }

    public <B extends ByteBuf> void encode(B byteBuf) {
        byteBuf.writeBoolean(this.getValue() == null);
        if (this.getValue() != null) {
            Util.<StreamCodec<B, T>>cast(this.getCodec()).encode(byteBuf, this.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    static <T> StreamCodec<? extends ByteBuf, T> defaultCodec(Class<T> type) {
        if (type == CompoundTag.class) {
            return (StreamCodec<? extends ByteBuf, T>) ByteBufCodecs.TAG;
        }
        if (Tag.class.isAssignableFrom(type)) {
            return (StreamCodec<? extends ByteBuf, T>) ByteBufCodecs.TAG;
        }
        if (Vector3fc.class.isAssignableFrom(type)) {
            return (StreamCodec<? extends ByteBuf, T>) ByteBufCodecs.VECTOR3F;
        }
        if (Quaternionfc.class.isAssignableFrom(type)) {
            return (StreamCodec<? extends ByteBuf, T>) ByteBufCodecs.QUATERNIONF;
        }
        if (type == PropertyMap.class) {
            return (StreamCodec<? extends ByteBuf, T>) ByteBufCodecs.GAME_PROFILE_PROPERTIES;
        }
        if (type == GameProfile.class) {
            return (StreamCodec<? extends ByteBuf, T>) ByteBufCodecs.GAME_PROFILE;
        }
        if (type == BlockPos.class) {
            return (StreamCodec<? extends ByteBuf, T>) BlockPos.STREAM_CODEC;
        }
        if (Component.class.isAssignableFrom(type)) {
            return (StreamCodec<? extends ByteBuf, T>) ComponentSerialization.STREAM_CODEC;
        }
        if (type == ItemStack.class) {
            return (StreamCodec<? extends ByteBuf, T>) ItemStack.OPTIONAL_STREAM_CODEC;
        }
        if (type == ItemStackTemplate.class) {
            return (StreamCodec<? extends ByteBuf, T>) ItemStackTemplate.STREAM_CODEC;
        }
        if (type == String.class) {
            return (StreamCodec<? extends ByteBuf, T>) ByteBufCodecs.STRING_UTF8;
        }
        if (type == Boolean.class || type == boolean.class) {
            return (StreamCodec<? extends ByteBuf, T>) ByteBufCodecs.BOOL;
        }
        if (type == Double.class || type == double.class) {
            return (StreamCodec<? extends ByteBuf, T>) ByteBufCodecs.DOUBLE;
        }
        if (type == Float.class || type == float.class) {
            return (StreamCodec<? extends ByteBuf, T>) ByteBufCodecs.FLOAT;
        }
        if (type == Long.class || type == long.class) {
            return (StreamCodec<? extends ByteBuf, T>) ByteBufCodecs.LONG;
        }
        if (type == Integer.class || type == int.class) {
            return (StreamCodec<? extends ByteBuf, T>) ByteBufCodecs.INT;
        }
        if (type == Short.class || type == short.class) {
            return (StreamCodec<? extends ByteBuf, T>) ByteBufCodecs.SHORT;
        }
        if (type == Byte.class || type == byte.class) {
            return (StreamCodec<? extends ByteBuf, T>) ByteBufCodecs.BYTE;
        }
        if (type == Long[].class || type == long[].class) {
            return (StreamCodec<? extends ByteBuf, T>) ByteBufCodecs.LONG_ARRAY;
        }
        if (type == Byte[].class || type == byte[].class) {
            return (StreamCodec<? extends ByteBuf, T>) ByteBufCodecs.BYTE_ARRAY;
        }
        return SyncProxy.codecFromType(type);
    }

    /**
     * 在目标类型自身声明的 {@code public static final} 字段中查找可用的
     * {@link Codec} 或 {@link MapCodec}，并转换为 {@link StreamCodec}。
     *
     * <p>查找规则：</p>
     * <ul>
     *   <li>仅扫描 {@code type} 自身声明的字段（不含父类），取 {@code public static final} 字段；</li>
     *   <li>字段类型为 {@link Codec Codec&lt;T&gt;} 或 {@link MapCodec MapCodec&lt;T&gt;}，
     *       且其类型实参与 {@code type} 兼容；{@link MapCodec} 经 {@link MapCodec#codec()} 转为 {@link Codec}；</li>
     *   <li>经 {@link ByteBufCodecs#fromCodecWithRegistries(Codec)} 包装为注册表感知的
     *       {@link StreamCodec}（本库的缓冲区在具备注册表访问时即为
     *       {@link net.minecraft.network.RegistryFriendlyByteBuf RegistryFriendlyByteBuf}）。</li>
     * </ul>
     *
     * @return 找到的 {@link StreamCodec}，未找到时返回 {@code null}
     */
    @SuppressWarnings("unchecked")
    @Nullable
    private static <T> StreamCodec<? extends ByteBuf, T> codecFromType(Class<T> type) {
        for (Field field : type.getDeclaredFields()) {
            int mods = field.getModifiers();
            if (!Modifier.isStatic(mods) || !Modifier.isFinal(mods) || !Modifier.isPublic(mods)) continue;

            Class<?> fieldType = field.getType();
            boolean isCodec = Codec.class == fieldType;
            boolean isMapCodec = MapCodec.class == fieldType;
            if (!isCodec && !isMapCodec) continue;
            if (!(field.getGenericType() instanceof ParameterizedType pt)) continue;
            if (pt.getActualTypeArguments().length != 1) continue;
            if (!(pt.getActualTypeArguments()[0] instanceof Class<?> arg)) continue;
            if (!type.isAssignableFrom(arg)) continue;

            try {
                Object value = field.get(null);
                if (value == null) continue;
                Codec<T> codec = isMapCodec ? ((MapCodec<T>) value).codec() : (Codec<T>) value;
                return ByteBufCodecs.fromCodecWithRegistries(codec);
            } catch (IllegalAccessException e) {
                log.warn("Cannot access codec field {}.{}", type.getName(), field.getName(), e);
            }
        }
        return null;
    }
}
