package dev.anvilcraft.lib.v2.rendering.preview.level;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 结构预览的方块轮廓选取与只读方块信息。 */
public final class StructurePreviewPicker {
    private StructurePreviewPicker() {
    }

    /** 沿预览相机射线选取最近的可见方块；隐藏层不会挡住下层方块。 */
    public static @Nullable BlockPos pick(SandboxRenderLevel level, Vec3 start, Vec3 end, int minY, int maxY) {
        BlockPos selected = null;
        double nearest = Double.POSITIVE_INFINITY;
        var positions = level.getFilledBlocks().iterator();
        while (positions.hasNext()) {
            BlockPos pos = positions.next();
            if (pos.getY() < minY || pos.getY() >= maxY) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getRenderShape() == RenderShape.INVISIBLE && state.getFluidState().isEmpty()) {
                continue;
            }
            var shape = state.getShape(level, pos);
            // 流体等没有方块轮廓的可见内容也应能查看配置。
            if (shape.isEmpty()) {
                shape = Shapes.block();
            }
            var hit = shape.clip(start, end, pos);
            if (hit == null) {
                continue;
            }
            double distance = start.distanceToSqr(hit.getLocation());
            if (distance < nearest) {
                nearest = distance;
                selected = pos.immutable();
            }
        }
        return selected;
    }

    /** 名称使用游戏翻译，坐标相对于结构原点，属性值保持原版序列化名称。 */
    public static List<Component> tooltip(SandboxRenderLevel level, BlockPos pos, BlockPos placementPos) {
        BlockState state = level.getBlockState(pos);
        List<Component> lines = new ArrayList<>();
        lines.add(state.getBlock().getName().copy().withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()).withStyle(ChatFormatting.GRAY));
        BlockPos relative = pos.subtract(placementPos);
        lines.add(Component.translatable("tooltip.anvillib_rendering.structure_preview.position", relative.getX(), relative.getY(), relative.getZ()));
        if (state.getProperties().isEmpty()) {
            lines.add(Component.translatable("tooltip.anvillib_rendering.structure_preview.empty_properties").withStyle(ChatFormatting.GRAY));
        } else {
            lines.add(Component.translatable("tooltip.anvillib_rendering.structure_preview.properties"));
            state.getProperties().stream().sorted(Comparator.comparing(Property::getName))
                .forEach(property -> lines.add(propertyLine(state, property)));
        }
        return List.copyOf(lines);
    }

    private static <T extends Comparable<T>> Component propertyLine(BlockState state, Property<T> property) {
        return Component.literal(property.getName() + " = " + property.getName(state.getValue(property)))
            .withStyle(ChatFormatting.GRAY);
    }
}
