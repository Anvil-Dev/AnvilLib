package dev.anvilcraft.lib.v2.piston.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.piston.IMoveableEntityBlock;
import dev.anvilcraft.lib.v2.piston.injection.IPistonMovingBlockEntityExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(PistonMovingBlockEntity.class)
abstract class PistonMovingBlockEntityMixin extends BlockEntity implements IPistonMovingBlockEntityExtension {
    @Shadow
    private BlockState movedState;
    @Unique
    private static final Codec<BlockEntityType<?>> anvillib$TYPE_CODEC = BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec();
    @Unique
    private static final String anvillib$MOVEABLE_BLOCK_ENTITY = "anvillib:moveable_block_entity";
    @Unique
    private @Nullable BlockEntity anvillib$blockEntity = null;

    public PistonMovingBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public void anvillib$setBlockEntity(@Nullable BlockEntity blockEntity) {
        this.anvillib$blockEntity = blockEntity;
    }

    @Override
    public @Nullable BlockEntity anvillib$clearBlockEntity() {
        BlockEntity blockEntity = this.anvillib$blockEntity;
        this.anvillib$blockEntity = null;
        return blockEntity;
    }

    @Override
    public @Nullable BlockEntity anvillib$getBlockEntity() {
        return this.anvillib$blockEntity;
    }

    @Inject(
        method = "loadAdditional", at = @At("TAIL")
    )
    private void loadAdditional(ValueInput input, CallbackInfo ci) {
        Optional<ValueInput> child = input.child(anvillib$MOVEABLE_BLOCK_ENTITY);
        if (child.isEmpty()) return;
        ValueInput valueInput = child.get();
        Optional<BlockEntityType<?>> entityType = valueInput.read("id", anvillib$TYPE_CODEC);
        if (entityType.isEmpty()) return;
        int x = valueInput.getIntOr("x", 0);
        int y = valueInput.getIntOr("y", 0);
        int z = valueInput.getIntOr("z", 0);
        BlockEntityType<?> blockEntityType = entityType.get();
        this.anvillib$blockEntity = blockEntityType.create(new BlockPos(x, y, z), this.movedState);
        this.anvillib$blockEntity.loadWithComponents(valueInput);
    }

    @Inject(
        method = "saveAdditional", at = @At("TAIL")
    )
    private void saveAdditional(ValueOutput output, CallbackInfo ci) {
        if (this.anvillib$blockEntity == null) return;
        ValueOutput child = output.child(anvillib$MOVEABLE_BLOCK_ENTITY);
        this.anvillib$blockEntity.saveWithFullMetadata(child);
    }

    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;" + "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
            shift = At.Shift.AFTER,
            ordinal = 1
        )
    )
    private static void tick(
        Level level,
        BlockPos pos,
        BlockState state,
        PistonMovingBlockEntity entity,
        CallbackInfo ci,
        @Local(name = "newState") BlockState newState
    ) {
        if (level.isClientSide()) return;
        if (!(newState.getBlock() instanceof IMoveableEntityBlock block)) return;
        BlockEntity blockEntity = entity.anvillib$clearBlockEntity();
        if (blockEntity == null) return;
        blockEntity.worldPosition = pos;
        blockEntity.clearRemoved();
        level.removeBlockEntity(pos);
        level.setBlockEntity(blockEntity);
        block.notifyMoved(level, pos, newState, blockEntity);
    }

    @Inject(
        method = "finalTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
            shift = At.Shift.AFTER
        )
    )
    private void finalTick(CallbackInfo ci, @Local(name = "newState") BlockState newState) {
        if (this.level == null || this.level.isClientSide()) return;
        // noinspection ConstantValue
        if (!(this instanceof IPistonMovingBlockEntityExtension blockEntity1)) return;
        if (!(newState.getBlock() instanceof IMoveableEntityBlock block)) return;
        BlockEntity blockEntity = blockEntity1.anvillib$clearBlockEntity();
        if (blockEntity == null) return;
        blockEntity.worldPosition = this.worldPosition;
        blockEntity.clearRemoved();
        this.level.removeBlockEntity(this.worldPosition);
        this.level.setBlockEntity(blockEntity);
        block.notifyMoved(this.level, this.worldPosition, this.movedState, blockEntity);
    }
}
