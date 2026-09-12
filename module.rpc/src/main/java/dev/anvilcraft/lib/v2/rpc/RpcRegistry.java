package dev.anvilcraft.lib.v2.rpc;

import lombok.extern.slf4j.Slf4j;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link RemoteCallable} 方法的索引表。索引以「规范键」{@code 全限定类名 + '#' + 方法名 + 方法描述符}
 * 为单位分配，网络包仅传输整数索引。
 *
 * <h2>双实例与一致性</h2>
 * <p>本类按服务端 / 客户端各维护一个实例：</p>
 * <ul>
 *     <li><b>权威实例</b>（{@link AnvilLibRpc#REGISTRY}）：由本地扫描填充，<em>从不</em>被
 *     {@link #adopt(Map)} 覆盖。服务端在 Configuration 阶段用 {@link #snapshot()} 将其映射下发给客户端。</li>
 *     <li><b>客户端实例</b>（{@code AnvilLibRpcClient.REGISTRY}）：仅通过 {@link #adopt(Map)} 采纳服务端
 *     下发的映射。</li>
 * </ul>
 *
 * <p>由此即便客户端断开某服务器后再开局域网，权威实例仍是本地扫描结果（未被污染），不会把上一个服务器的
 * 映射当作自己的下发出去。编解码时按方向选择实例（发送方向决定编码端、{@code ctx.flow()} 决定解码端），
 * 双向 RPC 的索引始终一致。</p>
 */
@Slf4j
@ApiStatus.Internal
public final class RpcRegistry {
    private static final String ANNOTATION_DESCRIPTOR = "L" + RemoteCallable.class.getName().replace('.', '/') + ";";

    /**
     * 是否为权威实例：权威实例在未初始化时执行本地扫描，非权威（客户端）实例则要求先经
     * {@link #adopt(Map)} 采纳服务端映射。
     */
    private final boolean authoritative;
    private @Nullable Map<String, Integer> indexByKey;
    private @Nullable Map<Integer, String> keyByIndex;

    /**
     * @param authoritative {@code true} 为权威（服务端）实例，按需本地扫描；{@code false} 为客户端实例，仅采纳下发
     */
    public RpcRegistry(boolean authoritative) {
        this.authoritative = authoritative;
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

    /**
     * 返回方法对应的整数索引。
     *
     * @param method 已注册的 {@link RemoteCallable} 方法
     * @return 该方法的索引
     */
    public synchronized int index(Method method) {
        ensureLoaded();
        String key = canonicalKey(method);
        assert indexByKey != null;
        Integer index = indexByKey.get(key);
        if (index == null) {
            throw new IllegalStateException("Method is not a registered @RemoteCallable: " + key);
        }
        return index;
    }

    /**
     * 返回索引对应的方法。
     *
     * @param index 方法索引
     * @return 对应的 {@link RemoteCallable} 方法
     */
    public synchronized Method byIndex(int index) {
        ensureLoaded();
        assert keyByIndex != null;
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
    public synchronized Map<Integer, String> snapshot() {
        ensureLoaded();
        assert keyByIndex != null;
        return new HashMap<>(keyByIndex);
    }

    /**
     * 用给定（服务端权威）映射覆盖本实例的索引表。
     *
     * @param map 服务端下发的 {@code 索引 -> 规范键} 映射
     */
    public synchronized void adopt(Map<Integer, String> map) {
        Map<Integer, String> byIndex = new HashMap<>(map);
        Map<String, Integer> byKey = new HashMap<>();
        byIndex.forEach((index, key) -> byKey.put(key, index));
        this.keyByIndex = byIndex;
        this.indexByKey = byKey;
        log.debug("Adopted {} authoritative @RemoteCallable index mapping(s)", byIndex.size());
    }

    private void ensureLoaded() {
        if (indexByKey != null) return;
        if (!authoritative) {
            throw new IllegalStateException("RPC index mapping has not been received from the server yet");
        }
        scan();
    }

    private void scan() {
        List<String> keys = getKeys();
        // 排序仅为本地索引分配的确定性与日志可读性；跨端一致性由服务端下发映射保证
        keys.sort(null);

        Map<Integer, String> byIndex = new HashMap<>();
        Map<String, Integer> byKey = new HashMap<>();
        for (int i = 0; i < keys.size(); i++) {
            byIndex.put(i, keys.get(i));
            byKey.put(keys.get(i), i);
            log.debug("Registered @RemoteCallable [{}] {}", i, keys.get(i));
        }
        this.keyByIndex = byIndex;
        this.indexByKey = byKey;
        log.info("Scan complete - {} @RemoteCallable method(s) registered.", keys.size());
    }

    @SuppressWarnings("UnstableApiUsage")
    private static List<String> getKeys() {
        List<String> keys = new ArrayList<>();
        for (ModFileInfo fileInfo : FMLLoader.getCurrent().getLoadingModList().getModFiles()) {
            for (ModFileScanData.AnnotationData annotation : fileInfo.getFile().getScanResult().getAnnotations()) {
                if (!annotation.annotationType().getDescriptor().equals(ANNOTATION_DESCRIPTOR)) continue;
                if (annotation.targetType() != ElementType.METHOD) continue;
                // memberName 形如 "methodName(Ljava/lang/String;I)V"
                keys.add(annotation.clazz().getClassName() + "#" + annotation.memberName());
            }
        }
        return keys;
    }
}
