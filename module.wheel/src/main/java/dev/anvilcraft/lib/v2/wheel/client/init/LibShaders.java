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
        } catch (IOException e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }
}
