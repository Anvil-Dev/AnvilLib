package dev.anvilcraft.lib.v2.rendering.optimization.occlusion.hiz;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRGpuDeviceExtension;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.MemoryBarrierFlag;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.compute.pipeline.ALRComputePass;
import dev.anvilcraft.lib.v2.rendering.extension.blaze3d.ALRCommandEncoderExtension;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.GpuBufferConstants;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.StagingSupport;
import dev.anvilcraft.lib.v2.rendering.foundation.buffers.layout.BufferLayout;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.hiz.buffers.Aabb;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.hiz.buffers.OcclusionTestCB;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.hiz.buffers.OcclusionTestSSBO;
import dev.anvilcraft.lib.v2.rendering.ALRComputePipelines;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.CullingStatistics;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionCuller;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.OcclusionKey;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.hiz.converter.DepthTexConverter;
import dev.anvilcraft.lib.v2.rendering.optimization.occlusion.hiz.spd.SinglePassDownsampler;
import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;
import org.joml.Vector2i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HierarchicalZOcclusionCuller implements OcclusionCuller {
    private final Logger logger = LoggerFactory.getLogger("HierarchicalZOcclusionCuller");

    private final Minecraft minecraft;
    private final ALRGpuDeviceExtension gpuDeviceExtension;
    private final GpuDevice gpuDevice;

    private final SinglePassDownsampler downsampler;
    private final DepthTexConverter depthTexConverter;

    private final OcclusionTestCB testCB = new OcclusionTestCB();
    private final OcclusionTestSSBO inputSSBO = new OcclusionTestSSBO();

    private final StagingSupport stagingInputBuffer;

    private final GpuBuffer testCBBuffer;
    private final boolean bindlessTexturing;
    private int[] results;

    private FrameState previousFrameState = null;
    private FrameState currentFrameState = null;
    /// update interval for z-buffer mipmap
    ///
    /// interval = 0 -> update every frame
    ///
    @Setter
    @Getter
    private int mipmapUpdateInterval = 0;

    private int mipmapUpdateCd = mipmapUpdateInterval;
    private int culled = 0;
    private int size = 0;
    private GpuBuffer inputBuffer;
    private GpuBuffer outputBuffer;

    public HierarchicalZOcclusionCuller(ALRGpuDeviceExtension device) {
        this.minecraft = Minecraft.getInstance();
        this.gpuDeviceExtension = device;
        this.gpuDevice = (GpuDevice) device;
        this.testCBBuffer = gpuDevice.createBuffer(
            () -> "Hi-Z Occlusion Test CB",
            GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_UNIFORM,
            OcclusionTestCB.SIZE
        );

        RenderTarget mainRenderTarget = this.minecraft.getMainRenderTarget();

        this.downsampler = new SinglePassDownsampler(
            minecraft,
            gpuDeviceExtension,
            gpuDevice,
            mainRenderTarget
        );
        this.depthTexConverter = new DepthTexConverter(
            gpuDevice,
            gpuDeviceExtension,
            mainRenderTarget,
            this.downsampler.getPaddedWidth(),
            this.downsampler.getPaddedHeight()
        );
        this.stagingInputBuffer = StagingSupport.createInstance(gpuDevice, "HiZ Occlusion Test Staging Buffer");
        this.bindlessTexturing = downsampler.isUseBindlessTexturing();
    }

    @Override
    public void onResize(int width, int height) {
        this.downsampler.onResize(width, height);
        this.depthTexConverter.onResize(
            width,
            height,
            this.downsampler.getPaddedWidth(),
            this.downsampler.getPaddedHeight()
        );
    }

    @Override
    public void beforeExtract() {
    }

    @Override
    public void beginRenderingFrame() {
        if (this.currentFrameState == null) {
            this.currentFrameState = FrameState.create();
        }
        this.previousFrameState = this.currentFrameState;
        this.fetchResults();
        this.currentFrameState = FrameState.create();
    }

    private void fetchResults() {
        if (this.outputBuffer == null) return;
        int size = this.previousFrameState.size();
        if (results == null || results.length < size) {
            results = new int[size];
        }
        CommandEncoder commandEncoder = this.gpuDevice.createCommandEncoder();
        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(outputBuffer, true, false)) {
            ByteBuffer data = mappedView.data();
            IntBuffer intBuffer = data.asIntBuffer();
            intBuffer.get(0, results, 0, size);
        }
        int culled = 0;
        for (int result : results) {
            if (result == 0) {
                culled++;
            }
        }
        this.size = size;
        this.culled = culled;
//        if (flag){
//            logger.error("222222222222222222222222");
//        }
    }

    @Override
    public void submitFeatureKey(OcclusionKey key, List<Object> feature) {
        this.currentFrameState.keys.add(key);
        for (Object o : feature) {
            this.currentFrameState.keyAssociations.put(o, key);
        }
    }

    @Override
    public void processFeatures(CameraRenderState camera) {
        RenderTarget mainRenderTarget = this.minecraft.getMainRenderTarget();
        CommandEncoder commandEncoder = this.gpuDevice.createCommandEncoder();
        if (this.mipmapUpdateCd <= 0) {
            GpuTexture texture = this.depthTexConverter.runConvert(
                commandEncoder,
                mainRenderTarget.getDepthTexture()
            );
            this.downsampler.spdDispatch(commandEncoder, texture);
            this.mipmapUpdateCd = mipmapUpdateInterval;
        } else {
            this.mipmapUpdateCd--;
        }

        this.dispatch(commandEncoder, camera, this.depthTexConverter.getOutput());
    }

    private void dispatch(CommandEncoder commandEncoder, CameraRenderState camera, GpuTexture mip0) {
        if (this.currentFrameState.size() <= 0) return;

        FrameState currentFrameState = this.currentFrameState;
        int elementCount = currentFrameState.keys.size();

        Aabb[] aabbs = this.inputSSBO.getAabbs(elementCount);
        int i = 0;
        for (OcclusionKey key : currentFrameState.keys) {
            int id = i++;
            aabbs[id].set(key.getBoundingBox());
            currentFrameState.keyToIdMap.put(key, id);
        }

        Vector2i[] mipLayers = this.inputSSBO.getMipLayers();
        mipLayers[0] = new Vector2i(downsampler.getPaddedWidth(), downsampler.getPaddedHeight());

        GpuTexture[] mipTextures = downsampler.getMipTextures();

        for (i = 1; i < mipLayers.length; ++i) {
            boolean inRange = (i - 1) < downsampler.getMipLayers().length;
            if (inRange) {
                mipLayers[i].set(downsampler.getMipLayers()[i - 1]);
            } else {
                mipLayers[i].set(0, 0);
            }
        }

        this.inputSSBO.setMipLayers(mipLayers);
        this.inputSSBO.setAabbs(aabbs);
        this.inputSSBO.setActualSize(elementCount);

        this.testCB.setElementCount(elementCount);
        this.testCB.setMipLevels(Math.min(1 + mipTextures.length, OcclusionTestSSBO.MIP_LAYER_COUNT));
        this.testCB.setViewportSize(new Vector2f(
            downsampler.getFramebufferWidth(),
            downsampler.getFramebufferHeight()
        ));
        this.testCB.setCameraPos(new Vector4f((float) camera.pos.x, (float) camera.pos.y, (float) camera.pos.z, 1));
        this.testCB.getProjMat().set(camera.projectionMatrix);
        this.testCB.getCameraMat().set(camera.viewRotationMatrix);

        this.ensureBuffers(elementCount);

        this.testCB.upload(commandEncoder, testCBBuffer.slice());

        long actualRequestedSize = this.inputSSBO.actualSize(elementCount);

        ByteBuffer buffer = this.stagingInputBuffer.getBuffer(this.gpuDevice, commandEncoder, actualRequestedSize);
        this.inputSSBO.write(buffer);
        buffer.position(0);
        // TODO check if mojang added coherent flag
        ((ALRCommandEncoderExtension) commandEncoder).alrMemoryBarrier(MemoryBarrierFlag.BUFFER_UPDATE_BARRIER);
        this.stagingInputBuffer.copyToBuffer(
            commandEncoder,
            0,
            actualRequestedSize,
            inputBuffer.slice(0, actualRequestedSize)
        );

        List<GpuTexture> textures = new ArrayList<>(OcclusionTestSSBO.MIP_LAYER_COUNT);
        textures.add(mip0);
        textures.addAll(Arrays.asList(mipTextures));

        while (textures.size() < OcclusionTestSSBO.MIP_LAYER_COUNT) {
            textures.add(mip0);
        }

        ALRCommandEncoderExtension extension = (ALRCommandEncoderExtension) commandEncoder;

        try (ALRComputePass pass = extension.alrCreateComputePass()) {
            if (this.bindlessTexturing) {
                pass.setPipeline(ALRComputePipelines.HIZ_OCCLUSION_TEST_BINDLESS);
            } else {
                pass.setPipeline(ALRComputePipelines.HIZ_OCCLUSION_TEST);
            }
            pass.bindUniformBlock(0, testCBBuffer.slice());
            pass.bindShaderStorage(0, inputBuffer.slice());
            pass.bindShaderStorage(1, outputBuffer.slice());
            if (this.bindlessTexturing) {
                pass.bindBindlessImageArray("uInputs", textures);
            } else {
                pass.bindArrayOfTexture(0, textures, true, false);
            }
            pass.dispatchWorkgroups(Math.ceilDiv(elementCount, 32), 1, 1);
            pass.memoryBarrier(MemoryBarrierFlag.SHADER_IMAGE_ACCESS_BARRIER, MemoryBarrierFlag.SHADER_STORAGE_BARRIER);
        }
    }

    private void ensureBuffers(int elementCount) {
        long requiredInputSize = inputSSBO.getDefinition().size(BufferLayout.STD430);
        long requiredOutputSize = (long) elementCount * Integer.BYTES;

        if (inputBuffer == null || inputBuffer.size() < requiredInputSize) {
            if (inputBuffer != null) {
                inputBuffer.close();
            }
            inputBuffer = gpuDevice.createBuffer(
                () -> "Hi-Z Occlusion Test Input",
                GpuBuffer.USAGE_COPY_DST | GpuBufferConstants.USAGE_SHADER_STORAGE,
                requiredInputSize
            );
        }

        if (outputBuffer == null || outputBuffer.size() < requiredOutputSize) {
            if (outputBuffer != null) {
                outputBuffer.close();
            }
            outputBuffer = gpuDevice.createBuffer(
                () -> "Hi-Z Occlusion Test Output",
                GpuBuffer.USAGE_COPY_DST | GpuBufferConstants.USAGE_SHADER_STORAGE | GpuBuffer.USAGE_MAP_READ,
                requiredOutputSize
            );
        }
    }

    @Override
    public boolean isEmpty() {
        return this.previousFrameState.keyAssociations.isEmpty();
    }

    @Override
    public @Nullable CullingStatistics collectStatistics() {
        return new CullingStatistics(
            this.size,
            -1,
            -1,
            this.culled,
            this.size - this.culled,
            this.previousFrameState.keyAssociations.size(),
            null
        );
    }

    @Override
    public boolean shouldDraw(Object feature) {
        OcclusionKey key = this.currentFrameState.keyAssociations.get(feature);
        if (key == null || results == null) {
            return true;
        }
        int orDefault = this.previousFrameState.keyToIdMap.getOrDefault(key, -1);
        if (orDefault < 0) {
            return true;
        }
        if (orDefault > results.length) {
            this.logger.warn("Trying to read invalid position {} as it exceeds results.length.", orDefault);
            return true;
        }
        return results[orDefault] > 0;
    }

    @Override
    public void close() throws Exception {
        this.testCBBuffer.close();
        if (this.inputBuffer != null) this.inputBuffer.close();
        if (this.outputBuffer != null) this.outputBuffer.close();
    }

    record FrameState(
        ReferenceSet<OcclusionKey> keys,
        Reference2ObjectMap<Object, OcclusionKey> keyAssociations,
        Int2ReferenceMap<OcclusionKey> idToKeyMap,
        Reference2IntMap<OcclusionKey> keyToIdMap
    ) {
        public static FrameState create() {
            return new FrameState(
                new ReferenceLinkedOpenHashSet<>(),
                new Reference2ObjectLinkedOpenHashMap<>(),
                new Int2ReferenceOpenHashMap<>(),
                new Reference2IntLinkedOpenHashMap<>()
            );
        }

        public int size() {
            return this.keys.size();
        }
    }

}
