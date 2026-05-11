package dev.anvilcraft.lib.renderdoc.loader;

import java.lang.instrument.Instrumentation;

/**
 * Java Agent for dynamically loading RenderDoc DLL into JVM.
 * <p>
 * This agent allows RenderDoc to be attached to a running JVM process
 * for graphics debugging purposes.
 * </p>
 */
public class RenderDocAgent {

    /**
     * System property key for specifying the RenderDoc library path.
     */
    public static final String RENDERDOC_LIBRARY_PATH = "renderdoc.library.path";

    /**
     * System property key for specifying the RenderDoc library path (alternative).
     */
    public static final String RENDERDOC_LIBRARY_PATH_ALT = "neoforge.rendernurse.renderdoc.library";

    /**
     * JVM entry point for Java Agent when started with -javaagent flag.
     *
     * @param agentArgs agent arguments passed via -javaagent:jarpath=args
     * @param inst      the instrumentation instance
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        loadRenderDoc();
    }

    /**
     * JVM entry point for Java Agent when attached to a running JVM.
     *
     * @param agentArgs agent arguments
     * @param inst      the instrumentation instance
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        loadRenderDoc();
    }

    /**
     * Attempts to load the RenderDoc native library.
     * <p>
     * The library path can be specified via:
     * <ul>
     *   <li>System property {@link #RENDERDOC_LIBRARY_PATH}</li>
     *   <li>System property {@link #RENDERDOC_LIBRARY_PATH_ALT}</li>
     *   <li>Agent arguments (as a path)</li>
     * </ul>
     * </p>
     */
    private static void loadRenderDoc() {
        String libraryPath = getLibraryPath();

        if (libraryPath == null || libraryPath.isEmpty()) {
            System.err.println("[RenderDoc-Loader] No RenderDoc library path specified. " +
                "Set system property '" + RENDERDOC_LIBRARY_PATH + "' or '" + RENDERDOC_LIBRARY_PATH_ALT + "'");
            return;
        }

        try {
            System.load(libraryPath);
            System.out.println("[RenderDoc-Loader] Successfully loaded RenderDoc from: " + libraryPath);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("[RenderDoc-Loader] Failed to load RenderDoc from: " + libraryPath);
            e.printStackTrace();
        } catch (SecurityException e) {
            System.err.println("[RenderDoc-Loader] Security exception when loading RenderDoc from: " + libraryPath);
            e.printStackTrace();
        }
    }

    /**
     * Gets the RenderDoc library path from system properties or agent arguments.
     *
     * @return the library path, or null if not specified
     */
    private static String getLibraryPath() {
        // Try primary system property first
        String path = System.getProperty(RENDERDOC_LIBRARY_PATH);
        if (path != null && !path.isEmpty()) {
            return path;
        }

        // Try alternative system property
        path = System.getProperty(RENDERDOC_LIBRARY_PATH_ALT);
        if (path != null && !path.isEmpty()) {
            return path;
        }

        return null;
    }
}
