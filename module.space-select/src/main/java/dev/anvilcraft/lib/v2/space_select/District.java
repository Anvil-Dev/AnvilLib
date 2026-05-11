package dev.anvilcraft.lib.v2.space_select;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

    public boolean contains(double x, double y, double z) {
        return x >= this.start.getX() && x <= this.end.getX()
               && y >= this.start.getY() && y <= this.end.getY()
               && z >= this.start.getZ() && z <= this.end.getZ();
    }

    public static Direction.Axis getPrimaryAxis(Vec3 lookAngle) {
        double absX = Math.abs(lookAngle.x);
        double absY = Math.abs(lookAngle.y);
        double absZ = Math.abs(lookAngle.z);
        if (absX >= absY && absX >= absZ) return Direction.Axis.X;
        if (absY >= absX && absY >= absZ) return Direction.Axis.Y;
        return Direction.Axis.Z;
    }

    public void scaleOnAxis(Direction.Axis axis, int scrollAmount, Vec3 playerPos, AABB boundingBox, Vec3 lookAngle) {
        double playerCoord = axis.choose(playerPos.x, playerPos.y, playerPos.z);
        double minPlayerCoord = axis.choose(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
        double maxPlayerCoord = axis.choose(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
        double minCoord = axis.choose(this.start.getX(), this.start.getY(), this.start.getZ());
        double maxCoord = axis.choose(this.end.getX(), this.end.getY(), this.end.getZ());
        double lookComp = axis.choose(lookAngle.x, lookAngle.y, lookAngle.z);

        boolean inside = maxPlayerCoord >= minCoord && minPlayerCoord <= maxCoord + 1;

        int faceSign;
        if (inside) {
            faceSign = lookComp > 0 ? 1 : -1;
        } else {
            double distToMin = Math.abs(playerCoord - minCoord);
            double distToMax = Math.abs(playerCoord - maxCoord + 1);
            faceSign = distToMin < distToMax ? -1 : 1;
        }

        int delta = (lookComp > 0 ? 1 : -1) * scrollAmount;

        if (faceSign > 0) {
            int newEnd = (int) maxCoord + delta;
            if (newEnd >= (int) minCoord) {
                setAxisCoord(this.end, axis, newEnd);
            }
        } else {
            int newStart = (int) minCoord + delta;
            if (newStart <= (int) maxCoord) {
                setAxisCoord(this.start, axis, newStart);
            }
        }
    }

    private static void setAxisCoord(BlockPos.MutableBlockPos pos, Direction.Axis axis, int value) {
        switch (axis) {
            case X -> pos.setX(value);
            case Y -> pos.setY(value);
            case Z -> pos.setZ(value);
        }
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
