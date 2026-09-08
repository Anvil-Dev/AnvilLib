package dev.anvilcraft.lib.v2.rendering;

import dev.anvilcraft.lib.v2.rendering.event.RegisterComputePipelinesEvent;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePipeline;
import net.minecraft.client.renderer.ShaderDefines;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(Dist.CLIENT)
public class ALRComputePipelines {
    public static final ALRComputePipeline FFX_SPD_DOWNSAMPLE_PASS = ALRComputePipeline.builder()
        .withName(AnvilLibRendering.location("ffx_spd_downsample_pass"))
        .withShader(AnvilLibRendering.location("compute/ffx_spd_downsample_pass.csh"))
        .withDefines(
            ShaderDefines.builder()
//                .define("SPD_MAX_MIP_LEVELS", 5) // minium required value in spec of GL_MAX_IMAGE_UNITS is 8, spd use one for input tex, one for mid tex
                .define("FFX_SPD_OPTION_DOWNSAMPLE_FILTER", "2") // use max for HZB
                .define("FFX_SPD_OPTION_WAVE_INTEROP_LDS", 0) // weird inverted
                .build()
        )
        .withUniformBlock("cbFSR1")
        .withTexture("r_input_downsample_src")
        .withShaderStorage("rw_internal_global_atomic")
        .withReadWriteImage("rw_input_downsample_src_mid_mip")
        .withArrayOfImage("rw_input_downsample_src_mips", true, true, 13)
        .build();

    public static final ALRComputePipeline FFX_SPD_DOWNSAMPLE_PASS_BINDLESS = ALRComputePipeline.builder()
        .withName(AnvilLibRendering.location("ffx_spd_downsample_pass_bindless"))
        .withShader(AnvilLibRendering.location("compute/ffx_spd_downsample_pass_bindless.csh"))
        .withDefines(
            ShaderDefines.builder()
//                .define("SPD_MAX_MIP_LEVELS", 4) // minium required value in spec of GL_MAX_IMAGE_UNITS is 8, spd use one for input tex
                .define("FFX_SPD_OPTION_DOWNSAMPLE_FILTER", "2") // use max for HZB
                .define("FFX_SPD_OPTION_WAVE_INTEROP_LDS", 0) // weird inverted
                .build()
        )
        .withUniformBlock("cbFSR1")
        .withTexture("r_input_downsample_src")
        .withShaderStorage("rw_internal_global_atomic")
        .withReadWriteImage("rw_input_downsample_src_mid_mip")
        .withBindlessArrayOfImage("rw_input_downsample_src_mips", true, true, 13)
        .build();

    public static final ALRComputePipeline FFX_SPD_DOWNSAMPLE_PASS_NO_LDS = ALRComputePipeline.builder()
        .withName(AnvilLibRendering.location("ffx_spd_downsample_pass"))
        .withShader(AnvilLibRendering.location("compute/ffx_spd_downsample_pass.csh"))
        .withDefines(
            ShaderDefines.builder()
//                .define("SPD_MAX_MIP_LEVELS", 5) // minium required value in spec of GL_MAX_IMAGE_UNITS is 8, spd use one for input tex, one for mid tex
                .define("FFX_SPD_OPTION_DOWNSAMPLE_FILTER", "2") // use max for HZB
                .define("FFX_SPD_OPTION_WAVE_INTEROP_LDS", 1) // weird inverted
                .build()
        )
        .withUniformBlock("cbFSR1")
        .withTexture("r_input_downsample_src")
        .withShaderStorage("rw_internal_global_atomic")
        .withReadWriteImage("rw_input_downsample_src_mid_mip")
        .withArrayOfImage("rw_input_downsample_src_mips", true, true, 13)
        .build();

    public static final ALRComputePipeline FFX_SPD_DOWNSAMPLE_PASS_BINDLESS_NO_LDS = ALRComputePipeline.builder()
        .withName(AnvilLibRendering.location("ffx_spd_downsample_pass_bindless"))
        .withShader(AnvilLibRendering.location("compute/ffx_spd_downsample_pass_bindless.csh"))
        .withDefines(
            ShaderDefines.builder()
//                .define("SPD_MAX_MIP_LEVELS", 4) // minium required value in spec of GL_MAX_IMAGE_UNITS is 8, spd use one for input tex
                .define("FFX_SPD_OPTION_DOWNSAMPLE_FILTER", "2") // use max for HZB
                .define("FFX_SPD_OPTION_WAVE_INTEROP_LDS", 1) // weird inverted
                .build()
        )
        .withUniformBlock("cbFSR1")
        .withTexture("r_input_downsample_src")
        .withShaderStorage("rw_internal_global_atomic")
        .withReadWriteImage("rw_input_downsample_src_mid_mip")
        .withBindlessArrayOfImage("rw_input_downsample_src_mips", true, true, 13)
        .build();

    public static final ALRComputePipeline DEPTH_CONVERT = ALRComputePipeline.builder()
        .withName(AnvilLibRendering.location("depth_convert"))
        .withShader(AnvilLibRendering.location("compute/depth_convert.csh"))
        .withUniformBlock("ConvertParam")
        .withTexture("Input")
        .withWriteOnlyImage("Output")
        .build();

    public static final ALRComputePipeline HIZ_OCCLUSION_TEST = ALRComputePipeline.builder()
        .withName(AnvilLibRendering.location("hiz_occlusion_test"))
        .withShader(AnvilLibRendering.location("compute/hiz_occlusion_test.csh"))
        .withUniformBlock("cbOcclusionTest")
        .withShaderStorage("ShaderInput")
        .withShaderStorage("ShaderOutput")
        .withArrayOfImage("uInputs", true, false, 13)
        .build();

    public static final ALRComputePipeline HIZ_OCCLUSION_TEST_BINDLESS = ALRComputePipeline.builder()
        .withName(AnvilLibRendering.location("hiz_occlusion_test_bindless"))
        .withShader(AnvilLibRendering.location("compute/hiz_occlusion_test_bindless.csh"))
        .withUniformBlock("cbOcclusionTest")
        .withShaderStorage("ShaderInput")
        .withShaderStorage("ShaderOutput")
        .withBindlessArrayOfImage("uInputs", true, false, 13)
        .build();

    @SubscribeEvent
    public static void on(RegisterComputePipelinesEvent event) {
        event.registerPipeline(FFX_SPD_DOWNSAMPLE_PASS);
        event.registerPipeline(FFX_SPD_DOWNSAMPLE_PASS_BINDLESS);
        event.registerPipeline(FFX_SPD_DOWNSAMPLE_PASS_BINDLESS_NO_LDS);
        event.registerPipeline(FFX_SPD_DOWNSAMPLE_PASS_NO_LDS);

        event.registerPipeline(DEPTH_CONVERT);
        event.registerPipeline(HIZ_OCCLUSION_TEST);
        event.registerPipeline(HIZ_OCCLUSION_TEST_BINDLESS);
    }
}
