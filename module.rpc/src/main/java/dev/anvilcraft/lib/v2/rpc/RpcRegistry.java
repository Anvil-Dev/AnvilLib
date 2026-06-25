package dev.anvilcraft.lib.v2.rpc;

import lombok.extern.slf4j.Slf4j;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.jetbrains.annotations.ApiStatus;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局 {@link RemoteCallable} 方法索引表。
 *
 * <p>索引以「规范键」{@code 全限定类名 + '#' + 方法名 + 方法描述符} 为单位分配，网络包仅传输整数索引。</p>
 *
 * <h2>一致性</h2>
 * <p>服务端为权威方：服务端扫描自身的 {@link RemoteCallable} 方法并分配索引，随后在 Configuration 阶段
 * 通过 {@link dev.anvilcraft.lib.v2.rpc.config.RpcConfigurationPayload} 将 {@code 索引 -> 规范键} 映射下发给客户端；客户端调用
 * {@link #adopt(Map)} 用服务端映射覆盖本地映射。由此两端共享同一套索引，双向 RPC 均按此索引收发，
 * 而不依赖两端各自扫描结果的巧合一致。</p>
 *
 * <p>键到 {@link Method} 的解析按需进行（见 {@link RpcMethods#resolve}），因此客户端采纳一个含有本地
 * 不存在的方法的映射不会立即失败——只有真正调用到该方法时才会报错。</p>
 */
@Slf4j
@ApiStatus.Internal
public final class RpcRegistry {
    private static final String ANNOTATION_DESCRIPTOR = "L" + RemoteCallable.class.getName().replace('.', '/') + ";";

    private static Map<String, Integer> indexByKey;
    private static Map<Integer, String> keyByIndex;

    private RpcRegistry() {
    }

    /**
     * 返回方法对应的整数索引。
     *
     * @param method 已注册的 {@link RemoteCallable} 方法
     * @return 该方法的索引
     */
    static synchronized int index(Method method) {
        ensureLoaded();
        String key = canonicalKey(method);
        Integer index = indexByKey.get(key);
        if (index == null) {
            throw new IllegalStateException(
                "Method is not a registered @RemoteCallable: " + key
                + " (is it present and annotated on the authoritative/server side?)"
            );
        }
        return index;
    }

    /**
     * 返回索引对应的方法。
     *
     * @param index 方法索引
     * @return 对应的 {@link RemoteCallable} 方法
     */
    static synchronized Method byIndex(int index) {
        ensureLoaded();
        String key = keyByIndex.get(index);
        if (key == null) {
            throw new IllegalStateException("Unknown RPC method index: " + index + " (registered: " + keyByIndex.size() + ")");
        }
        return resolve(key);
    }

    /**
     * 返回当前 {@code 索引 -> 规范键} 映射的快照，用于在 Configuration 阶段下发给客户端。
     *
     * @return 索引到规范键的映射
     */
    @ApiStatus.Internal
    public static synchronized Map<Integer, String> snapshot() {
        ensureLoaded();
        return new HashMap<>(keyByIndex);
    }

    /**
     * 用给定（服务端权威）映射覆盖本地索引表。
     *
     * @param map 服务端下发的 {@code 索引 -> 规范键} 映射
     */
    @ApiStatus.Internal
    public static synchronized void adopt(Map<Integer, String> map) {
        Map<Integer, String> byIndex = new HashMap<>(map);
        Map<String, Integer> byKey = new HashMap<>();
        byIndex.forEach((index, key) -> byKey.put(key, index));
        keyByIndex = byIndex;
        indexByKey = byKey;
        log.debug("Adopted {} authoritative @RemoteCallable index mapping(s)", byIndex.size());
    }

    private static void ensureLoaded() {
        if (indexByKey != null) return;
        scan();
    }

    @SuppressWarnings("UnstableApiUsage")
    private static void scan() {
        List<String> keys = new ArrayList<>();
        for (ModFileInfo fileInfo : FMLLoader.getCurrent().getLoadingModList().getModFiles()) {
            for (ModFileScanData.AnnotationData annotation : fileInfo.getFile().getScanResult().getAnnotations()) {
                if (!annotation.annotationType().getDescriptor().equals(ANNOTATION_DESCRIPTOR)) continue;
                if (annotation.targetType() != ElementType.METHOD) continue;
                // memberName 形如 "methodName(Ljava/lang/String;I)V"
                keys.add(annotation.clazz().getClassName() + "#" + annotation.memberName());
            }
        }
        // 排序仅为本地索引分配的确定性与日志可读性；跨端一致性由服务端下发映射保证
        keys.sort(null);

        Map<Integer, String> byIndex = new HashMap<>();
        Map<String, Integer> byKey = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            byIndex.put(i, keys.get(i));
            byKey.put(keys.get(i), i);
            log.debug("Registered @RemoteCallable [{}] {}", i, keys.get(i));
        }
        keyByIndex = byIndex;
        indexByKey = byKey;
        log.info("Scan complete - {} @RemoteCallable method(s) registered.", keys.size());
    }

    private static String canonicalKey(Method method) {
        return method.getDeclaringClass().getName() + "#" + method.getName() + RpcMethods.methodDescriptor(method);
    }

    private static Method resolve(String key) {
        int hash = key.indexOf('#');
        int paren = key.indexOf('(', hash);
        String className = key.substring(0, hash);
        String methodName = key.substring(hash + 1, paren);
        String descriptor = key.substring(paren);
        return RpcMethods.resolve(className, methodName, descriptor);
    }
}
