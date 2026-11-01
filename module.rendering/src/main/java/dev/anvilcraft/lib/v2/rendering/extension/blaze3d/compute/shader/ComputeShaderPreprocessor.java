package dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.shader;

import com.mojang.renderpearl.api.pipeline.ShaderSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.util.shaderc.ShadercIncludeResolve;
import org.lwjgl.util.shaderc.ShadercIncludeResultRelease;

/** Uses the game's shaderc preprocessor while retaining the compute extension's OpenGL backend. */
final class ComputeShaderPreprocessor {
    private ComputeShaderPreprocessor() {}

    static String process(Map<Identifier, String> sources, Identifier id, ShaderDefines defines) {
        String source = sources.get(id);
        if (source == null) throw new IllegalArgumentException("Missing compute shader: " + id);
        List<ShaderSource.CachedIncludeSource> allocated = new ArrayList<>();
        long compiler = Shaderc.shaderc_compiler_initialize();
        long options = Shaderc.shaderc_compile_options_initialize();
        try (ShadercIncludeResolve resolver = ShadercIncludeResolve.create((user, requested, kind, requesting, depth) -> {
            String path = MemoryUtil.memUTF8(requested);
            Identifier include;
            if (kind == Shaderc.shaderc_include_type_relative && !path.contains(":")) {
                Identifier parent = Identifier.parse(MemoryUtil.memUTF8(requesting));
                String parentPath = parent.getPath();
                include = parent.withPath(parentPath.substring(0, parentPath.lastIndexOf('/') + 1) + path);
            } else {
                include = Identifier.parse(path).withPrefix("shaders/include/");
            }
            String text = sources.get(include);
            var result = text == null ? ShaderSource.CachedIncludeSource.createError("Missing include: " + include)
                : ShaderSource.CachedIncludeSource.create(include, normalizeIncludes(text));
            allocated.add(result);
            return result.includeResultPtr();
        }); ShadercIncludeResultRelease release = ShadercIncludeResultRelease.create((user, result) -> {})) {
            Shaderc.shaderc_compile_options_set_target_env(options, Shaderc.shaderc_target_env_opengl, Shaderc.shaderc_env_version_opengl_4_5);
            defines.values().forEach((name, value) -> Shaderc.shaderc_compile_options_add_macro_definition(options, name, value));
            defines.flags().forEach(name -> Shaderc.shaderc_compile_options_add_macro_definition(options, name, ""));
            Shaderc.shaderc_compile_options_set_include_callbacks(options, resolver, release, 0L);
            long result = Shaderc.shaderc_compile_into_preprocessed_text(compiler, normalizeIncludes(source),
                Shaderc.shaderc_compute_shader, id.toString(), "main", options);
            try {
                if (Shaderc.shaderc_result_get_compilation_status(result) != Shaderc.shaderc_compilation_status_success) {
                    throw new IllegalStateException(Shaderc.shaderc_result_get_error_message(result));
                }
                return MemoryUtil.memUTF8(Shaderc.shaderc_result_get_bytes(result));
            } finally {
                Shaderc.shaderc_result_release(result);
            }
        } finally {
            allocated.forEach(ShaderSource.CachedIncludeSource::close);
            Shaderc.shaderc_compile_options_release(options);
            Shaderc.shaderc_compiler_release(compiler);
        }
    }

    private static String normalizeIncludes(String source) {
        return source.replace("#moj_import", "#include");
    }
}
