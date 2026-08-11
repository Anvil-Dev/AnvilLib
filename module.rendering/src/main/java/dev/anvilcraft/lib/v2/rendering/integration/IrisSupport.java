package dev.anvilcraft.lib.v2.rendering.integration;

import net.neoforged.fml.ModList;
import javax.annotation.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Stack;

/**
 * Optional integration with Iris (shader mod) for the cached BER pipeline.
 * <p>
 * Ported from 26.1: the API shape ({@code net.irisshaders.iris.api.v0.IrisApi} +
 * {@code net.irisshaders.iris.vertices.ImmediateState}) is identical in 1.21.1-era Iris (verified
 * against 1.8.12+1.21.1-neoforge). The dev branch used a {@code compileOnly} dependency from the
 * Modrinth maven, which has been shut down (the 1.21.1 Iris coordinate can no longer be resolved), so
 * the Iris classes are accessed through reflection instead: the integration compiles without Iris and
 * activates at runtime only when Iris is actually loaded. The "oculus" branch does not exist on 1.21.1
 * (kept for API parity, always false).
 */
public class IrisSupport {
    private static final boolean IRIS_PRESENT;
    private static final Stack<IrisState> irisStateStack = new Stack<>();

    @Nullable private static Method isShaderPackInUseMethod;
    @Nullable private static Object irisApiInstance;
    @Nullable private static Field isRenderingLevelField;
    @Nullable private static Field skipExtensionField;

    static {
        boolean present = ModList.get().isLoaded("iris") || ModList.get().isLoaded("oculus");
        if (present) {
            try {
                Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Method getInstance = irisApiClass.getMethod("getInstance");
                irisApiInstance = getInstance.invoke(null);
                isShaderPackInUseMethod = irisApiClass.getMethod("isShaderPackInUse");

                Class<?> immediateStateClass = Class.forName("net.irisshaders.iris.vertices.ImmediateState");
                isRenderingLevelField = immediateStateClass.getField("isRenderingLevel");
                skipExtensionField = immediateStateClass.getField("skipExtension");
            } catch (ReflectiveOperationException e) {
                // The loaded Iris version does not match the expected class shape: disable the
                // integration instead of breaking the cached BER pipeline.
                present = false;
                irisApiInstance = null;
                isShaderPackInUseMethod = null;
                isRenderingLevelField = null;
                skipExtensionField = null;
            }
        }
        IRIS_PRESENT = present;
    }

    public static boolean isIrisPresent() {
        return IRIS_PRESENT;
    }

    public static void pushIrisGlobalState() {
        if (IRIS_PRESENT) {
            _pushIrisGlobalState();
        }
    }

    public static void popIrisGlobalState() {
        if (IRIS_PRESENT) {
            _popIrisGlobalState();
        }
    }

    public static boolean isShaderEnabled() {
        if (IRIS_PRESENT) {
            return isShaderEnabledInternal();
        }
        return false;
    }

    private static void _pushIrisGlobalState() {
        irisStateStack.push(
            new IrisState(
                getIsRenderingLevel(),
                getSkipExtension()
            )
        );
        setIsRenderingLevel(true);
        setSkipExtension(false);
    }

    private static void _popIrisGlobalState() {
        IrisState peek = irisStateStack.peek();
        if (peek != null) {
            irisStateStack.pop();
            setIsRenderingLevel(peek.isRenderingLevel);
            setSkipExtension(peek.skipExtension);
        }
    }

    private static boolean isShaderEnabledInternal() {
        try {
            return (boolean) isShaderPackInUseMethod.invoke(irisApiInstance);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static boolean getIsRenderingLevel() {
        try {
            return isRenderingLevelField != null && (boolean) isRenderingLevelField.get(null);
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    private static void setIsRenderingLevel(boolean value) {
        try {
            if (isRenderingLevelField != null) {
                isRenderingLevelField.set(null, value);
            }
        } catch (IllegalAccessException e) {
            // ignore
        }
    }

    private static boolean getSkipExtension() {
        try {
            if (skipExtensionField != null) {
                @SuppressWarnings("unchecked")
                ThreadLocal<Boolean> threadLocal = (ThreadLocal<Boolean>) skipExtensionField.get(null);
                return threadLocal != null && threadLocal.get();
            }
        } catch (IllegalAccessException e) {
            // ignore
        }
        return false;
    }

    private static void setSkipExtension(boolean value) {
        try {
            if (skipExtensionField != null) {
                @SuppressWarnings("unchecked")
                ThreadLocal<Boolean> threadLocal = (ThreadLocal<Boolean>) skipExtensionField.get(null);
                if (threadLocal != null) {
                    threadLocal.set(value);
                }
            }
        } catch (IllegalAccessException e) {
            // ignore
        }
    }

    private record IrisState(
        boolean isRenderingLevel,
        boolean skipExtension
    ) {
    }
}
