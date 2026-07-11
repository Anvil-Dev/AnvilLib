package dev.anvilcraft.lib.v2.rendering.integration;

import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.vertices.ImmediateState;
import net.neoforged.fml.ModList;

import java.util.Stack;

public class IrisSupport {
    private static final boolean IRIS_PRESENT;
    private static final Stack<IrisState> irisStateStack = new Stack<>();

    static {
        IRIS_PRESENT = ModList.get().isLoaded("iris") || ModList.get().isLoaded("oculus");
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
                ImmediateState.isRenderingLevel,
                ImmediateState.skipExtension.get()
            )
        );
    }

    private static void _popIrisGlobalState() {
        IrisState peek = irisStateStack.peek();
        if (peek != null) {
            irisStateStack.pop();
            ImmediateState.isRenderingLevel = peek.isRenderingLevel;
            ImmediateState.skipExtension.set(peek.skipExtension);
        }
    }

    private static boolean isShaderEnabledInternal() {
        return IrisApi.getInstance().isShaderPackInUse();
    }

    private record IrisState(
        boolean isRenderingLevel,
        boolean skipExtension
    ) {

    }
}
