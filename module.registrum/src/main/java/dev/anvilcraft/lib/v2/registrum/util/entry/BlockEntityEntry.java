/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/util/entry/BlockEntityEntry.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.util.entry;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

public class BlockEntityEntry<T extends BlockEntity> extends RegistryEntry<BlockEntityType<?>, BlockEntityType<T>> {

    public BlockEntityEntry(AbstractRegistrum<?> owner, DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> delegate) {
        super(owner, delegate);
    }

    /**
     * Create a "default" instance of this {@link BlockEntity} via the {@link BlockEntityType}.
     *
     * @return The instance
     */
    public T create(BlockPos pos, BlockState state) {
        return get().create(pos, state);
    }

    /**
     * Check that the given {@link BlockEntity} is an instance of this type.
     *
     * @param t The {@link BlockEntity} instance
     * @return {@code true} if the type matches, {@code false} otherwise.
     */
    public boolean is(@Nullable BlockEntity t) {
        return t != null && t.getType() == get();
    }

    /**
     * Get an instance of this {@link BlockEntity} from the world.
     *
     * @param world The world to look for the instance in
     * @param pos   The position of the instance
     * @return An {@link Optional} containing the instance, if it exists and matches this type. Otherwise, {@link Optional#empty()}.
     */
    public Optional<T> get(BlockGetter world, BlockPos pos) {
        return Optional.ofNullable(getNullable(world, pos));
    }

    /**
     * Get an instance of this {@link BlockEntity} from the world.
     *
     * @param world The world to look for the instance in
     * @param pos   The position of the instance
     * @return The instance, if it exists and matches this type. Otherwise, {@code null}.
     */
    @SuppressWarnings("unchecked")
    public @Nullable T getNullable(BlockGetter world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        return is(be) ? (T) be : null;
    }

    public static <T extends BlockEntity> BlockEntityEntry<T> cast(RegistryEntry<BlockEntityType<?>, BlockEntityType<T>> entry) {
        return RegistryEntry.cast(BlockEntityEntry.class, entry);
    }
}
