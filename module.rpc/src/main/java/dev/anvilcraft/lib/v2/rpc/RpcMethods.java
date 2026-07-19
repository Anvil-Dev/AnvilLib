package dev.anvilcraft.lib.v2.rpc;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
    /**
     * 返回值编解码器解析缓存。
     */
    private static final Map<Method, StreamCodec<RegistryFriendlyByteBuf, Object>> RETURN_CODEC_CACHE = new ConcurrentHashMap<>();
    /**
     * 方法校验器实例缓存。{@code Optional.empty()} 表示该方法无校验器（始终放行）。
     */
    private static final Map<Method, Optional<IRemoteCallableValidator>> VALIDATOR_CACHE = new ConcurrentHashMap<>();

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
        DEFAULTS.put(UUID.class, UUIDUtil.STREAM_CODEC);
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
        return METHOD_CACHE.computeIfAbsent(className + "#" + methodName + descriptor, _ -> doResolve(className, methodName, descriptor));
    }

    private static Method doResolve(String className, String methodName, String descriptor) {
        Class<?> clazz = loadClass(className);
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.getName().equals(methodName)) continue;
            if (!methodDescriptor(method).equals(descriptor)) continue;
            validate(method);
            method.setAccessible(true);
            return method;
        }
        throw new IllegalStateException("Cannot find method " + methodName + descriptor + " in " + className);
    }

    /**
     * 按类与方法名解析唯一的 {@link RemoteCallable} 方法（用于无方法引用的逃生口调用）。
     *
     * <p>若该名称存在多个重载，视为歧义并抛出异常——此时应改用方法引用形式以借助参数类型消歧。</p>
     *
     * @param clazz      方法所属类
     * @param methodName 方法名
     * @return 已校验通过、可访问的目标方法
     */
    static Method resolveByName(Class<?> clazz, String methodName) {
        Method found = null;
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.getName().equals(methodName)) continue;
            if (!Modifier.isStatic(method.getModifiers())) continue;
            if (!method.isAnnotationPresent(RemoteCallable.class)) continue;
            if (found != null) {
                throw new IllegalStateException("Ambiguous @RemoteCallable method " + clazz.getName() + "#" + methodName + "; use a method reference (RPC.call) to disambiguate overloads");
            }
            method.setAccessible(true);
            found = method;
        }
        if (found == null) {
            throw new IllegalStateException("Cannot find @RemoteCallable static method " + clazz.getName() + "#" + methodName);
        }
        return found;
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot find RPC target class: " + className, e);
        }
    }

    private static void validate(Method method) {
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new IllegalStateException("@RemoteCallable method must be static: " + method);
        }
        if (!method.isAnnotationPresent(RemoteCallable.class)) {
            throw new IllegalStateException("Method is not @RemoteCallable: " + method);
        }
    }

    /**
     * 解析方法各参数的编解码器，顺序与参数声明顺序一致。
     */
    static StreamCodec<RegistryFriendlyByteBuf, Object>[] codecs(Method method) {
        return CODEC_CACHE.computeIfAbsent(method, RpcMethods::resolveCodecs);
    }

    @SuppressWarnings(
        {
            "unchecked",
            "rawtypes"
        }
    )
    private static StreamCodec<RegistryFriendlyByteBuf, Object>[] resolveCodecs(Method method) {
        Parameter[] parameters = method.getParameters();
        StreamCodec[] codecs = new StreamCodec[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            codecs[i] = resolveCodec(parameters[i]);
        }
        return (StreamCodec<RegistryFriendlyByteBuf, Object>[]) codecs;
    }

    private static StreamCodec<?, ?> resolveCodec(Parameter parameter) {
        // 1. 优先使用 @CallableParam 显式指定的编解码器
        CallableParam annotation = parameter.getAnnotation(CallableParam.class);
        if (annotation != null) {
            return readCodecField(annotation.clazz(), annotation.field());
        }
        // 2. 其次按类型解析（默认编解码器 / 类型自身声明的字段）
        StreamCodec<?, ?> codec = codecForType(parameter.getType());
        if (codec != null) {
            return codec;
        }
        throw new IllegalStateException("No StreamCodec for parameter type " + parameter.getType()
            .getName() + "; annotate the parameter with @CallableParam, or declare a public static final StreamCodec field in " + parameter.getType()
                                            .getName());
    }

    /**
     * 解析方法返回值的编解码器（用于有返回值的 {@link RPC#invoke} 调用）。
     *
     * <p>解析顺序：方法上的 {@link CallableParam} 指定的字段 &rarr; 返回类型的默认编解码器 &rarr;
     * 返回类型自身声明的 {@code public static final StreamCodec} 字段。</p>
     *
     * @param method 目标方法（返回类型不得为 {@code void}）
     * @return 返回值编解码器
     */
    static StreamCodec<RegistryFriendlyByteBuf, Object> returnCodec(Method method) {
        return RETURN_CODEC_CACHE.computeIfAbsent(method, RpcMethods::resolveReturnCodec);
    }

    @SuppressWarnings("unchecked")
    private static StreamCodec<RegistryFriendlyByteBuf, Object> resolveReturnCodec(Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType == void.class) {
            throw new IllegalStateException("@RemoteCallable method has no return value: " + method + "; use RPC.call instead");
        }
        StreamCodec<?, ?> codec;
        CallableParam annotation = method.getAnnotation(CallableParam.class);
        if (annotation != null) {
            codec = readCodecField(annotation.clazz(), annotation.field());
        } else {
            codec = codecForType(returnType);
        }
        if (codec == null) {
            throw new IllegalStateException("No StreamCodec for return type " + returnType.getName() + "; annotate the method with @CallableParam, or declare a public static final StreamCodec field in " + returnType.getName());
        }
        return (StreamCodec<RegistryFriendlyByteBuf, Object>) codec;
    }

    /**
     * 按类型解析编解码器：先查 {@link ByteBufCodecs} 默认编解码器，再回退到该类型自身声明的
     * {@code public static final StreamCodec} 字段。
     *
     * @param type 目标类型
     * @return 匹配的编解码器；若都没有则返回 {@code null}
     */
    private static @Nullable StreamCodec<?, ?> codecForType(Class<?> type) {
        StreamCodec<?, ?> codec = DEFAULTS.get(type);
        if (codec != null) {
            return codec;
        }
        return findDeclaredCodec(type);
    }

    /**
     * 在给定类型中查找可用于编码该类型自身的 {@code public static final StreamCodec} 字段。
     *
     * <p>字段的负载类型实参须与参数类型兼容；若存在多个匹配字段则视为歧义并抛出异常。</p>
     *
     * @param type 参数类型
     * @return 匹配的编解码器；若无匹配字段返回 {@code null}
     */
    private static @Nullable StreamCodec<?, ?> findDeclaredCodec(Class<?> type) {
        StreamCodec<?, ?> found = null;
        String foundName = null;
        for (Field field : type.getDeclaredFields()) {
            Set<AccessFlag> flags = field.accessFlags();
            if (!flags.contains(AccessFlag.PUBLIC) || !flags.contains(AccessFlag.STATIC) || !flags.contains(AccessFlag.FINAL)) {
                continue;
            }
            if (!StreamCodec.class.isAssignableFrom(field.getType())) continue;
            if (!isCodecForPayload(field.getGenericType(), type)) continue;
            if (found != null) {
                throw new IllegalStateException("Ambiguous StreamCodec fields in " + type.getName() + ": " + foundName + " and " + field.getName() + "; use @CallableParam to disambiguate");
            }
            try {
                field.setAccessible(true);
                found = (StreamCodec<?, ?>) field.get(null);
                foundName = field.getName();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot read StreamCodec field " + type.getName() + "." + field.getName(), e);
            }
        }
        return found;
    }

    /**
     * 判断 {@code StreamCodec<?, P>} 的负载类型实参 {@code P} 是否可承载 {@code payloadType}。
     */
    private static boolean isCodecForPayload(Type genericType, Class<?> payloadType) {
        if (!(genericType instanceof ParameterizedType pt)) {
            // 原始类型无法校验负载类型，保守地接受
            return true;
        }
        Type[] args = pt.getActualTypeArguments();
        if (args.length < 2) return true;
        Type payloadArg = args[1];
        if (payloadArg instanceof Class<?> payloadClass) {
            return payloadClass.isAssignableFrom(payloadType);
        }
        if (payloadArg instanceof ParameterizedType payloadPt && payloadPt.getRawType() instanceof Class<?> raw) {
            return raw.isAssignableFrom(payloadType);
        }
        // 通配符 / 类型变量等无法静态判定，保守接受
        return true;
    }

    private static StreamCodec<?, ?> readCodecField(Class<?> clazz, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(null);
            if (value instanceof StreamCodec<?, ?> codec) {
                return codec;
            }
            throw new IllegalStateException("Field " + clazz.getName() + "." + fieldName + " is not a StreamCodec");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Cannot read StreamCodec from " + clazz.getName() + "." + fieldName, e);
        }
    }

    /**
     * 运行目标方法的接收端校验器。无校验器（默认哨兵）时始终放行。
     *
     * @param method 目标方法
     * @param ctx    网络包上下文
     * @param args   已解码的实参
     * @return 是否允许执行
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean validate(Method method, IPayloadContext ctx, Object[] args) {
        IRemoteCallableValidator validator = VALIDATOR_CACHE.computeIfAbsent(method, RpcMethods::resolveValidator).orElse(null);
        return validator == null || validator.validate(ctx, method, args);
    }

    private static Optional<IRemoteCallableValidator> resolveValidator(Method method) {
        RemoteCallable annotation = method.getAnnotation(RemoteCallable.class);
        Class<? extends IRemoteCallableValidator> type = annotation.validator();
        // 默认哨兵：注解未指定校验器，等价于始终放行
        if (type == IRemoteCallableValidator.class) {
            return Optional.empty();
        }
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return Optional.of(constructor.newInstance());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot instantiate RPC validator " + type.getName() + " (needs a no-arg constructor)", e);
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
