/**
 * Java Agent for dynamically loading RenderDoc DLL into JVM.
 * <p>
 * Usage:
 * <pre>
 * java -javaagent:renderdoc-loader.jar -Drenderdoc.library.path=/path/to/renderdoc.dll -jar app.jar
 * </pre>
 * </p>
 * <p>
 * Or use the alternative property name:
 * <pre>
 * java -javaagent:renderdoc-loader.jar -Dneoforge.rendernurse.renderdoc.library=/path/to/renderdoc.dll -jar app.jar
 * </pre>
 * </p>
 */
package dev.anvilcraft.lib.renderdoc.loader;
