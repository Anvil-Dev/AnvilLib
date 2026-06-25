package dev.anvilcraft.lib.v2.rpc;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC 方法解析与参数编解码器解析的内部工具。
 *
 * <p>负责：</p>
 * <ul>
 *     <li>根据类名、方法名、方法描述符定位 {@link RemoteCallable} 静态方法（并做安全校验）；</li>
 *     <li>为方法的每个参数解析 {@link StreamCodec}：优先使用 {@link CallableParam} 指定的编解码器，
 *     否则回退到 {@link ByteBufCodecs} 中的默认编解码器。</li>
 * </ul>
 */
final class RpcMethods {
    /**
     * {@link ByteBufCodecs} 中按参数类型提供的默认编解码器。使用这些类型的参数无需 {@link CallableParam}。
     */
    private static final Map<Class<?>, StreamCodec<?, ?>> DEFAULTS = new HashMap<>();
    /**
     * 方法解析缓存，键为 {@code className#methodName + descriptor}。
     */
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();
    /**
     * 参数编解码器解析缓存。
     */
    private static final Map<Method, StreamCodec<RegistryFriendlyByteBuf, Object>[]> CODEC_CACHE = new ConcurrentHashMap<>();

    static {
        // int / long 默认使用 VarInt / VarLong，与原版网络包惯例一致
        register(boolean.class, Boolean.class, ByteBufCodecs.BOOL);
        register(byte.class, Byte.class, ByteBufCodecs.BYTE);
        register(short.class, Short.class, ByteBufCodecs.SHORT);
        register(int.class, Integer.class, ByteBufCodecs.VAR_INT);
        register(long.class, Long.class, ByteBufCodecs.VAR_LONG);
        register(float.class, Float.class, ByteBufCodecs.FLOAT);
        register(double.class, Double.class, ByteBufCodecs.DOUBLE);
        DEFAULTS.put(String.class, ByteBufCodecs.STRING_UTF8);
        DEFAULTS.put(byte[].class, ByteBufCodecs.BYTE_ARRAY);
        DEFAULTS.put(long[].class, ByteBufCodecs.LONG_ARRAY);
        DEFAULTS.put(Tag.class, ByteBufCodecs.TAG);
        DEFAULTS.put(CompoundTag.class, ByteBufCodecs.COMPOUND_TAG);
    }

    private RpcMethods() {
    }

    private static void register(Class<?> primitive, Class<?> boxed, StreamCodec<? super ByteBuf, ?> codec) {
        DEFAULTS.put(primitive, codec);
        DEFAULTS.put(boxed, codec);
    }

    /**
     * 解析并校验目标方法。
     *
     * <p>该方法是接收端的安全闸口：仅当目标方法是 {@code static} 且标注了 {@link RemoteCallable} 时才会返回，
     * 否则抛出异常拒绝调用。</p>
     *
     * @param className  方法所属类的全限定名
     * @param methodName 方法名
     * @param descriptor 方法描述符（JVM 字节码格式），用于区分重载
     * @return 已校验通过、可访问的目标方法
     */
    static Method resolve(String className, String methodName, String descriptor) {
        return METHOD_CACHE.computeIfAbsent(
            className + "#" + methodName + descriptor,
            key -> doResolve(className, methodName, descriptor)
        );
    }

    private static Method doResolve(String className, String methodName, String descriptor) {
        Class<?> clazz;
        try {
            clazz = Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot find RPC target class: " + className, e);
        }
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.getName().equals(methodName)) continue;
            if (!methodDescriptor(method).equals(descriptor)) continue;
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new IllegalStateException("@RemoteCallable method must be static: " + className + "#" + methodName);
            }
            if (!method.isAnnotationPresent(RemoteCallable.class)) {
                throw new IllegalStateException("Method is not @RemoteCallable: " + className + "#" + methodName);
            }
            method.setAccessible(true);
            return method;
        }
        throw new IllegalStateException("Cannot find method " + methodName + descriptor + " in " + className);
    }

    /**
     * 解析方法各参数的编解码器，顺序与参数声明顺序一致。
     */
    static StreamCodec<RegistryFriendlyByteBuf, Object>[] codecs(Method method) {
        return CODEC_CACHE.computeIfAbsent(method, RpcMethods::resolveCodecs);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static StreamCodec<RegistryFriendlyByteBuf, Object>[] resolveCodecs(Method method) {
        Parameter[] parameters = method.getParameters();
        StreamCodec[] codecs = new StreamCodec[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            codecs[i] = resolveCodec(parameters[i]);
        }
        return (StreamCodec<RegistryFriendlyByteBuf, Object>[]) codecs;
    }

    private static StreamCodec<?, ?> resolveCodec(Parameter parameter) {
        CallableParam annotation = parameter.getAnnotation(CallableParam.class);
        if (annotation != null) {
            return readCodecField(annotation);
        }
        StreamCodec<?, ?> codec = DEFAULTS.get(parameter.getType());
        if (codec == null) {
            throw new IllegalStateException(
                "No default StreamCodec for parameter type " + parameter.getType().getName()
                + "; annotate the parameter with @CallableParam to provide one"
            );
        }
        return codec;
    }

    private static StreamCodec<?, ?> readCodecField(CallableParam annotation) {
        try {
            Field field = annotation.clazz().getDeclaredField(annotation.field());
            field.setAccessible(true);
            Object value = field.get(null);
            if (value instanceof StreamCodec<?, ?> codec) {
                return codec;
            }
            throw new IllegalStateException(
                "Field " + annotation.clazz().getName() + "." + annotation.field() + " is not a StreamCodec"
            );
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(
                "Cannot read StreamCodec from " + annotation.clazz().getName() + "." + annotation.field(), e
            );
        }
    }

    /**
     * 计算方法的 JVM 字节码描述符，例如 {@code (Ljava/lang/String;I)V}。
     */
    static String methodDescriptor(Method method) {
        StringBuilder builder = new StringBuilder("(");
        for (Class<?> parameterType : method.getParameterTypes()) {
            builder.append(typeDescriptor(parameterType));
        }
        return builder.append(')').append(typeDescriptor(method.getReturnType())).toString();
    }

    private static String typeDescriptor(Class<?> type) {
        if (type == void.class) return "V";
        if (type == boolean.class) return "Z";
        if (type == byte.class) return "B";
        if (type == char.class) return "C";
        if (type == short.class) return "S";
        if (type == int.class) return "I";
        if (type == long.class) return "J";
        if (type == float.class) return "F";
        if (type == double.class) return "D";
        if (type.isArray()) return "[" + typeDescriptor(type.getComponentType());
        return "L" + type.getName().replace('.', '/') + ";";
    }
}
