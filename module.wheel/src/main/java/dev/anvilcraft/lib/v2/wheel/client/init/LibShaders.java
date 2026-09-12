package dev.anvilcraft.lib.v2.wheel.client.init;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.anvilcraft.lib.v2.wheel.AnvilLibWheel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.client.renderer.ShaderDefines;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = AnvilLibWheel.MOD_ID, value = Dist.CLIENT)
public class LibShaders {
    private static final ShaderProgram ringShader = new ShaderProgram(AnvilLibWheel.of("core/ring"), DefaultVertexFormat.POSITION_COLOR, ShaderDefines.EMPTY);
    public static @Nullable CompiledShaderProgram getRingShader() { return Minecraft.getInstance().getShaderManager().getProgram(ringShader); }
    private static final ShaderProgram selectionShader = new ShaderProgram(AnvilLibWheel.of("core/selection"), DefaultVertexFormat.POSITION_COLOR, ShaderDefines.EMPTY);
    public static @Nullable CompiledShaderProgram getSelectionShader() { return Minecraft.getInstance().getShaderManager().getProgram(selectionShader); }
    private static final ShaderProgram annularSectorShader = new ShaderProgram(AnvilLibWheel.of("core/annular_sector"), DefaultVertexFormat.POSITION_COLOR, ShaderDefines.EMPTY);
    public static @Nullable CompiledShaderProgram getAnnularSectorShader() { return Minecraft.getInstance().getShaderManager().getProgram(annularSectorShader); }
    private static final ShaderProgram discShader = new ShaderProgram(AnvilLibWheel.of("core/disc"), DefaultVertexFormat.POSITION_COLOR, ShaderDefines.EMPTY);
    public static @Nullable CompiledShaderProgram getDiscShader() { return Minecraft.getInstance().getShaderManager().getProgram(discShader); }
    private static final ShaderProgram frostedDiscShader = new ShaderProgram(AnvilLibWheel.of("core/frosted_disc"), DefaultVertexFormat.POSITION_TEX_COLOR, ShaderDefines.EMPTY);
    public static @Nullable CompiledShaderProgram getFrostedDiscShader() { return Minecraft.getInstance().getShaderManager().getProgram(frostedDiscShader); }
    private static final ShaderProgram blurShader = new ShaderProgram(AnvilLibWheel.of("core/blur"), DefaultVertexFormat.POSITION_TEX, ShaderDefines.EMPTY);
    public static @Nullable CompiledShaderProgram getBlurShader() { return Minecraft.getInstance().getShaderManager().getProgram(blurShader); }
    private static final ShaderProgram segmentShader = new ShaderProgram(AnvilLibWheel.of("core/segment"), DefaultVertexFormat.POSITION_COLOR, ShaderDefines.EMPTY);
    public static @Nullable CompiledShaderProgram getSegmentShader() { return Minecraft.getInstance().getShaderManager().getProgram(segmentShader); }
    @SubscribeEvent
    public static void register(RegisterShadersEvent event) {
        event.registerShader(ringShader);
        event.registerShader(selectionShader);
        event.registerShader(annularSectorShader);
        event.registerShader(discShader);
        event.registerShader(frostedDiscShader);
        event.registerShader(blurShader);
        event.registerShader(segmentShader);
    }
}
