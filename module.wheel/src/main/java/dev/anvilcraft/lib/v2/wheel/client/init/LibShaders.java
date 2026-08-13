package dev.anvilcraft.lib.v2.wheel.client.init;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.anvilcraft.lib.v2.wheel.AnvilLibWheel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@Slf4j
@EventBusSubscriber(modid = AnvilLibWheel.MOD_ID, value = Dist.CLIENT)
public class LibShaders {
    @Getter
    static @Nullable ShaderInstance ringShader;
    @Getter
    static @Nullable ShaderInstance selectionShader;
    @Getter
    static @Nullable ShaderInstance annularSectorShader;
    @Getter
    static @Nullable ShaderInstance discShader;
    @Getter
    static @Nullable ShaderInstance frostedDiscShader;
    @Getter
    static @Nullable ShaderInstance blurShader;
    @Getter
    static @Nullable ShaderInstance segmentShader;

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) {
        try {
            event.registerShader(
                new ShaderInstance(
                    event.getResourceProvider(),
                    AnvilLibWheel.of("ring"),
                    DefaultVertexFormat.POSITION_COLOR
                ),
                it -> ringShader = it
            );
            event.registerShader(
                new ShaderInstance(
                    event.getResourceProvider(),
                    AnvilLibWheel.of("selection"),
                    DefaultVertexFormat.POSITION_COLOR
                ),
                it -> selectionShader = it
            );
            event.registerShader(
                new ShaderInstance(
                    event.getResourceProvider(),
                    AnvilLibWheel.of("annular_sector"),
                    DefaultVertexFormat.POSITION_COLOR
                ),
                it -> annularSectorShader = it
            );
            event.registerShader(
                new ShaderInstance(
                    event.getResourceProvider(),
                    AnvilLibWheel.of("disc"),
                    DefaultVertexFormat.POSITION_COLOR
                ),
                it -> discShader = it
            );
            event.registerShader(
                new ShaderInstance(
                    event.getResourceProvider(),
                    AnvilLibWheel.of("frosted_disc"),
                    DefaultVertexFormat.POSITION_TEX_COLOR
                ),
                it -> frostedDiscShader = it
            );
            event.registerShader(
                new ShaderInstance(
                    event.getResourceProvider(),
                    AnvilLibWheel.of("blur"),
                    DefaultVertexFormat.POSITION_TEX
                ),
                it -> blurShader = it
            );
            event.registerShader(
                new ShaderInstance(
                    event.getResourceProvider(),
                    AnvilLibWheel.of("segment"),
                    DefaultVertexFormat.POSITION_COLOR
                ),
                it -> segmentShader = it
            );
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }
}
