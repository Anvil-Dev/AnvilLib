package dev.anvilcraft.lib.v2.rpc;

import lombok.extern.slf4j.Slf4j;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;

import java.lang.annotation.ElementType;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局 {@link RemoteCallable} 方法索引表。
 *
 * <p>在首次使用时扫描所有已加载模组中标注了 {@link RemoteCallable} 的方法，按规范键
 * （{@code 全限定类名 + '#' + 方法名 + 方法描述符}）排序后依次分配整数索引。网络包仅传输该整数索引，
 * 而非冗长的「类名 + 方法名 + 描述符」字符串。</p>
 *
 * <p>排序保证收发两端在各自独立扫描后得到一致的索引分配，因此无需额外的索引协商网络包——
 * 前提是两端运行同一套 {@link RemoteCallable} 方法（同一 jar 必然满足）。</p>
 */
@Slf4j
final class RpcRegistry {
    private static final String ANNOTATION_DESCRIPTOR = "L" + RemoteCallable.class.getName().replace('.', '/') + ";";

    private static List<Method> byIndex;
    private static Map<Method, Integer> indexByMethod;

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
        Integer index = indexByMethod.get(method);
        if (index == null) {
            throw new IllegalStateException(
                "Method is not a registered @RemoteCallable: " + method
                + " (is it static and annotated, and present on both sides?)"
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
        if (index < 0 || index >= byIndex.size()) {
            throw new IllegalStateException("Unknown RPC method index: " + index + " (registered: " + byIndex.size() + ")");
        }
        return byIndex.get(index);
    }

    private static void ensureLoaded() {
        if (byIndex != null) return;
        scan();
    }

    @SuppressWarnings("UnstableApiUsage")
    private static void scan() {
        List<Method> methods = new ArrayList<>();
        for (ModFileInfo fileInfo : FMLLoader.getCurrent().getLoadingModList().getModFiles()) {
            for (ModFileScanData.AnnotationData annotation : fileInfo.getFile().getScanResult().getAnnotations()) {
                if (!annotation.annotationType().getDescriptor().equals(ANNOTATION_DESCRIPTOR)) continue;
                if (annotation.targetType() != ElementType.METHOD) continue;
                String className = annotation.clazz().getClassName();
                String memberName = annotation.memberName();
                int parenIndex = memberName.indexOf('(');
                String methodName = memberName.substring(0, parenIndex);
                String descriptor = memberName.substring(parenIndex);
                methods.add(RpcMethods.resolve(className, methodName, descriptor));
            }
        }
        methods.sort(Comparator.comparing(RpcRegistry::canonicalKey));

        List<Method> indexed = List.copyOf(methods);
        Map<Method, Integer> lookup = new HashMap<>();
        for (int i = 0; i < indexed.size(); i++) {
            lookup.put(indexed.get(i), i);
            log.debug("Registered @RemoteCallable [{}] {}", i, canonicalKey(indexed.get(i)));
        }
        byIndex = indexed;
        indexByMethod = lookup;
        log.info("Scan complete - {} @RemoteCallable method(s) registered.", indexed.size());
    }

    private static String canonicalKey(Method method) {
        return method.getDeclaringClass().getName() + "#" + method.getName() + RpcMethods.methodDescriptor(method);
    }
}
