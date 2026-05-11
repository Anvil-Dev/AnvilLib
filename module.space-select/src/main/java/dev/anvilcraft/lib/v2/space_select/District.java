package dev.anvilcraft.lib.v2.space_select;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Random;

public record District(
    BlockPos.MutableBlockPos start,
    BlockPos.MutableBlockPos end
) {
    public static District create(BlockPos start, BlockPos end) {
        BlockPos.MutableBlockPos _start = new BlockPos.MutableBlockPos(
            Math.min(start.getX(), end.getX()),
            Math.min(start.getY(), end.getY()),
            Math.min(start.getZ(), end.getZ())
        );
        BlockPos.MutableBlockPos _end = new BlockPos.MutableBlockPos(
            Math.max(start.getX(), end.getX()),
            Math.max(start.getY(), end.getY()),
            Math.max(start.getZ(), end.getZ())
        );
        return new District(_start, _end);
    }

    public void expand(Direction direction, int size) {
        int stepX = direction.getStepX() * size;
        int stepY = direction.getStepY() * size;
        int stepZ = direction.getStepZ() * size;
        if (stepX < 0) {
            this.start().setX(this.start().getX() + stepX);
        } else {
            this.end().setX(this.end().getX() + stepX);
        }
        if (stepY < 0) {
            this.start().setY(this.start().getY() + stepY);
        } else {
            this.end().setY(this.end().getY() + stepY);
        }
        if (stepZ < 0) {
            this.start().setZ(this.start().getZ() + stepZ);
        } else {
            this.end().setZ(this.end().getZ() + stepZ);
        }
    }

    public void contraction(Direction direction, int size) {
        int stepX = direction.getStepX() * size;
        int stepY = direction.getStepY() * size;
        int stepZ = direction.getStepZ() * size;
        if (stepX < 0) {
            this.start().setX(this.start().getX() - stepX);
        } else {
            this.end().setX(this.end().getX() - stepX);
        }
        if (stepY < 0) {
            this.start().setY(this.start().getY() - stepY);
        } else {
            this.end().setY(this.end().getY() - stepY);
        }
        if (stepZ < 0) {
            this.start().setZ(this.start().getZ() - stepZ);
        } else {
            this.end().setZ(this.end().getZ() - stepZ);
        }
    }

    public void move(Direction direction, int step) {
        int stepX = direction.getStepX() * step;
        int stepY = direction.getStepY() * step;
        int stepZ = direction.getStepZ() * step;
        this.start().set(
            this.start().getX() + stepX,
            this.start().getY() + stepY,
            this.start().getZ() + stepZ
        );
        this.end().set(
            this.end().getX() + stepX,
            this.end().getY() + stepY,
            this.end().getZ() + stepZ
        );
    }

    public VoxelShape shape() {
        return Shapes.create(
            0,
            0,
            0,
            this.end().getX() - this.start().getX() + 1,
            this.end().getY() - this.start().getY() + 1,
            this.end().getZ() - this.start().getZ() + 1
        );
    }

    public int color() {
        Random random = new Random(this.hashCode());
        float r = random.nextFloat();
        float g = random.nextFloat();
        float b = random.nextFloat();
        return ARGB.colorFromFloat(102F / 255F, r, g, b);
    }
}
