package dev.anvilcraft.lib.v2.cube.client.model;

import dev.anvilcraft.lib.v2.cube.client.CubeSelection;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.anvilcraft.lib.v2.cube.geometry.ConvexShape;
import dev.anvilcraft.lib.v2.cube.geometry.SelectionGeometry;
import dev.anvilcraft.lib.v2.cube.mixin.client.SingleVariantAccessor;
import dev.anvilcraft.lib.v2.cube.mixin.client.MultipartModelAccessor;
import dev.anvilcraft.lib.v2.cube.mixin.client.WeightedModelAccessor;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class ModelSelections {
    public static final long MAX_GEOMETRY_BYTES = 16L * 1024 * 1024;
    public static final int MAX_STATES = 32768;
    private final Map<BlockModelPart, ModelCapture.Source> sources;
    private final Map<BlockModelPart, ModelSelection> leaves = new IdentityHashMap<>();
    private final Map<List<ConvexShape>, SelectionGeometry> shared = new HashMap<>();
    private long bytes;
    private int unsupported;

    private ModelSelections(Map<BlockModelPart, ModelCapture.Source> sources) { this.sources = sources; }

    public static Snapshot bake(Map<BlockState, BlockStateModel> models) {
        ModelSelections builder = new ModelSelections(ModelCapture.take());
        Map<BlockState, ModelSelection> states = new IdentityHashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!CubeSelection.isEnabled(block)) continue;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                if (states.size() >= MAX_STATES) { builder.unsupported++; continue; }
                BlockStateModel baked = models.get(state);
                if (baked == null) continue;
                ModelSelection selection = builder.resolve(baked, state, 0);
                if (selection == null || !CubeSelection.supportedBounds(state.hasOffsetFunction()
                    ? selection.bounds().inflate(0.5) : selection.bounds())) builder.unsupported++;
                else states.put(state, selection);
            }
        }
        return new Snapshot(Map.copyOf(states), builder.bytes, builder.unsupported);
    }

    private @Nullable ModelSelection resolve(BlockStateModel model, BlockState state, int depth) {
        if (depth > 16) return null;
        BlockModelPart leaf = model instanceof SingleVariantAccessor single ? single.anvillib_cube$model() : null;
        ModelCapture.Source source = this.sources.get(leaf);
        if (source != null) {
            if (this.leaves.containsKey(leaf)) return this.leaves.get(leaf);
            ModelSelection result = null;
            try {
                SelectionGeometry geometry = this.intern(CubeModelDecoder.decode(source.elements(), BlockModelRotation.IDENTITY));
                if (geometry != null) {
                    SelectionPart part = source.state().transformation().isIdentity() ? new SelectionPart(geometry)
                        : new SelectionPart(geometry, new Matrix4f().translation(0.5F, 0.5F, 0.5F)
                            .mul(source.state().transformation().getMatrix()).translate(-0.5F, -0.5F, -0.5F));
                    result = new ModelSelection.Fixed(part);
                }
            } catch (IllegalArgumentException exception) {
                // 平面或非凸自定义元素不伪装成体积，整项保留原版选择行为。
            }
            this.leaves.put(leaf, result);
            return result;
        }
        if (model instanceof WeightedModelAccessor weighted) {
            List<ModelSelection> variants = new ArrayList<>();
            List<Integer> weights = new ArrayList<>();
            AABB bounds = null;
            for (Weighted<BlockStateModel> entry : weighted.anvillib_cube$variants().unwrap()) {
                ModelSelection variant = this.resolve(entry.value(), state, depth + 1);
                if (variant == null) return null;
                variants.add(variant);
                weights.add(entry.weight());
                bounds = bounds == null ? variant.bounds() : bounds.minmax(variant.bounds());
            }
            return bounds == null ? null : new ModelSelection.Weighted(variants, weights, weights.stream().mapToInt(Integer::intValue).sum(), bounds);
        }
        if (model instanceof MultipartModelAccessor multipart) {
            List<ModelSelection> parts = new ArrayList<>();
            List<ConvexShape> fixed = new ArrayList<>();
            AABB bounds = null;
            boolean allFixed = true;
            for (BlockStateModel entry : multipart.anvillib_cube$shared().selectModels(state)) {
                ModelSelection part = this.resolve(entry, state, depth + 1);
                if (part == null || parts.size() >= CubeSelection.MAX_PARTS) return null;
                parts.add(part);
                bounds = bounds == null ? part.bounds() : bounds.minmax(part.bounds());
                if (part instanceof ModelSelection.Fixed simple) fixed.addAll(simple.part().blockSpaceShapes());
                else allFixed = false;
            }
            if (bounds == null) return null;
            if (allFixed) {
                SelectionGeometry joined = this.intern(fixed);
                if (joined != null) return new ModelSelection.Fixed(new SelectionPart(joined));
                return null;
            }
            return new ModelSelection.Multipart(parts, bounds);
        }
        return null;
    }

    private @Nullable SelectionGeometry intern(List<ConvexShape> shapes) {
        SelectionGeometry existing = this.shared.get(shapes);
        if (existing != null) return existing;
        if (shapes.isEmpty() || shapes.size() > SelectionGeometry.MAX_SHAPES) return null;
        long lowerBound = shapes.stream().mapToLong(ConvexShape::estimatedBytes).sum();
        if (this.bytes + lowerBound > MAX_GEOMETRY_BYTES) return null;
        SelectionGeometry geometry = new SelectionGeometry(shapes);
        if (this.bytes + geometry.estimatedBytes() > MAX_GEOMETRY_BYTES) return null;
        this.shared.put(geometry.shapes(), geometry);
        this.bytes += geometry.estimatedBytes();
        return geometry;
    }

    public record Snapshot(Map<BlockState, ModelSelection> states, long geometryBytes, int unsupportedStates) { }
}
