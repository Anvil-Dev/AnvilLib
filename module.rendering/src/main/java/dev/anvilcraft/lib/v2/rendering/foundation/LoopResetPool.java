/*
 * Copyright (c) Argon4W
 * SPDX-License-Identifier: MIT
 */
package dev.anvilcraft.lib.v2.rendering.foundation;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/// @author Argon4W
@ApiStatus.Internal
public abstract class LoopResetPool<T, C> extends SimpleResetPool<T, C> {

	public LoopResetPool(int size, C context) {
		super(size, context);
	}

	@Override
	@Nullable
	@SuppressWarnings("unchecked")
	public T acquire(boolean createInstanceIfAllAcquired) {
		for (int i = 0; i < size; i++) {
			T t = (T) pool[i];

			if (this.isAvailable(t)) {
				this.onAcquire(t);
				return t;
			}
		}

		return this.fail(createInstanceIfAllAcquired);
	}
}