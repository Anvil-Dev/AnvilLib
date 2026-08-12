package dev.anvilcraft.lib.v2.wheel.client.init;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import dev.anvilcraft.lib.v2.wheel.AnvilLibWheel;
import net.minecraft.client.renderer.DynamicUniformStorage;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.joml.Vector2fc;
import org.jetbrains.annotations.ApiStatus;

import java.nio.ByteBuffer;

@EventBusSubscriber(modid = AnvilLibWheel.MOD_ID, value = Dist.CLIENT)
@ApiStatus.Internal
public class LibDynamicUniforms {
    private final DynamicUniformStorage<RingUniform> ringUbo = new DynamicUniformStorage<>(
        "RingUniform UBO",
        RingUniform.size(),
        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST
    );

    private final DynamicUniformStorage<SelectionUniform> selectionUbo = new DynamicUniformStorage<>(
        "SelectionUniform UBO",
        SelectionUniform.size(),
        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST
    );

    private final DynamicUniformStorage<AnnularSectorUniform> annularSectorUbo = new DynamicUniformStorage<>(
        "AnnularSectorUniform UBO",
        AnnularSectorUniform.size(),
        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST
    );

    private final DynamicUniformStorage<FrostedDiscUniform> frostedDiscUbo = new DynamicUniformStorage<>(
        "FrostedDiscUniform UBO",
        FrostedDiscUniform.size(),
        GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST
    );

    public GpuBufferSlice writeRing(Vector2fc center, float innerDiameter, float outerDiameter, float antiAliasingRadius) {
        return this.ringUbo.writeUniform(new RingUniform(
            center,
            innerDiameter,
            outerDiameter,
            antiAliasingRadius
        ));
    }

    public GpuBufferSlice writeSelection(Vector2fc framebufferSize, Vector2fc center, float radius, float antiAliasingRadius) {
        return this.selectionUbo.writeUniform(new SelectionUniform(
            framebufferSize,
            center,
            radius,
            antiAliasingRadius
        ));
    }

    public GpuBufferSlice writeAnnularSector(
        Vector2fc center,
        float innerRadius,
        float outerRadius,
        float antiAliasingRadius,
        float angleAntiAliasingRad,
        float centerAngleRad,
        float rangeAngleRad
    ) {
        return this.annularSectorUbo.writeUniform(new AnnularSectorUniform(
            center,
            innerRadius,
            outerRadius,
            antiAliasingRadius,
            angleAntiAliasingRad,
            centerAngleRad,
            rangeAngleRad
        ));
    }

    public GpuBufferSlice writeFrostedDisc(
        Vector2fc framebufferSize,
        Vector2fc center,
        float radius,
        float antiAliasingRadius
    ) {
        return this.frostedDiscUbo.writeUniform(new FrostedDiscUniform(
            framebufferSize,
            center,
            radius,
            antiAliasingRadius
        ));
    }

    public void reset() {
        this.ringUbo.endFrame();
        this.selectionUbo.endFrame();
        this.annularSectorUbo.endFrame();
        this.frostedDiscUbo.endFrame();
    }

    @SubscribeEvent
    public static void endFrame(RenderGuiEvent.Post event) {
        AnvilLibWheel.getLibDynamicUniforms().reset();
    }

    public record RingUniform(Vector2fc center, float innerDiameter, float outerDiameter, float antiAliasingRadius)
        implements DynamicUniformStorage.DynamicUniform {

        public static int size() {
            return new Std140SizeCalculator()
                // Center
                .putVec2()
                // InnerDiameter
                .putFloat()
                // OuterDiameter
                .putFloat()
                // AntiAliasingRadius
                .putFloat()
                .get();
        }

        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec2(this.center)
                .putFloat(this.innerDiameter)
                .putFloat(this.outerDiameter)
                .putFloat(this.antiAliasingRadius);
        }
    }

    public record SelectionUniform(Vector2fc framebufferSize, Vector2fc center, float radius, float antiAliasingRadius)
        implements DynamicUniformStorage.DynamicUniform {

        public static int size() {
            return new Std140SizeCalculator()
                // FramebufferSize
                .putVec2()
                // Center
                .putVec2()
                // Radius
                .putFloat()
                // AntiAliasingRadius
                .putFloat()
                .get();
        }

        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec2(this.framebufferSize)
                .putVec2(this.center)
                .putFloat(this.radius)
                .putFloat(this.antiAliasingRadius);
        }
    }

    public record AnnularSectorUniform(
        Vector2fc center,
        float innerRadius,
        float outerRadius,
        float antiAliasingRadius,
        float angleAntiAliasingRad,
        float centerAngleRad,
        float rangeAngleRad
    ) implements DynamicUniformStorage.DynamicUniform {

        public static int size() {
            return new Std140SizeCalculator()
                // Center
                .putVec2()
                // InnerRadius
                .putFloat()
                // OuterRadius
                .putFloat()
                // AntiAliasingRadius
                .putFloat()
                // AngleAntiAliasingRad
                .putFloat()
                // CenterAngleRad
                .putFloat()
                // RangeAngleRad
                .putFloat()
                .get();
        }

        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec2(this.center)
                .putFloat(this.innerRadius)
                .putFloat(this.outerRadius)
                .putFloat(this.antiAliasingRadius)
                .putFloat(this.angleAntiAliasingRad)
                .putFloat(this.centerAngleRad)
                .putFloat(this.rangeAngleRad);
        }
    }

    public record FrostedDiscUniform(
        Vector2fc framebufferSize,
        Vector2fc center,
        float radius,
        float antiAliasingRadius
    ) implements DynamicUniformStorage.DynamicUniform {

        public static int size() {
            return new Std140SizeCalculator()
                // FramebufferSize
                .putVec2()
                // Center
                .putVec2()
                // Radius
                .putFloat()
                // AntiAliasingRadius
                .putFloat()
                .get();
        }

        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                .putVec2(this.framebufferSize)
                .putVec2(this.center)
                .putFloat(this.radius)
                .putFloat(this.antiAliasingRadius);
        }
    }
}
