/*
 * Copyright (c) Argon4W
 * SPDX-License-Identifier: MIT
 */
package dev.anvilcraft.lib.v2.rendering.foundation;

import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/// @author Argon4W
@ApiStatus.Internal
public abstract class SimpleResetPool<T, C> {

	@Getter protected	final	C			context;

	@Getter protected			Object[]	pool;
	@Getter protected			int			cursor;
			protected 			int			size;

	public SimpleResetPool(int size, C context) {
		this.size		= size;
		this.pool		= new Object[size];
		this.context	= context;

		this.cursor		= 0;

		for (int i = 0; i < this.size; i++) {
			this.pool[i] = createInstance(this.context, i);
		}
	}

	public 		abstract void 		onAcquire		(T t);

	protected 	abstract T 			createInstance	(C context, int i);

	@Nullable
	protected 	abstract T 			fail			(boolean createInstanceIfAllAcquired);

	protected 	abstract void 		release			(T t);

	protected 	abstract void 		destroy			(T t);

	protected 	abstract boolean 	isAvailable		(T t);

	@Nullable
	public T acquire() {
		return this.acquire(true);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public T acquire(boolean createInstanceIfAllAcquired) {
		if (this.cursor < this.size) {
			T t = (T) this.pool[this.cursor ++];

			if (this.isAvailable(t)) {
				this.onAcquire(t);
				return t;
			}
		}

		return this.fail(createInstanceIfAllAcquired);
	}

	@SuppressWarnings("unchecked")
	public T get(int index) {
		return (T) this.pool[index];
	}

	protected void expand() {
		int old		= this.size;

		this.size	= old * 2;
		this.pool	= Arrays.copyOf(this.pool, this.size);

		for (int i = old; i < size; i ++) {
			this.pool[i] = this.createInstance(context, i);
		}
	}

	@SuppressWarnings("unchecked")
	public void releaseAll() {
		for (int i = 0; i < this.cursor; i++) {
			release((T) this.pool[i]);
		}

		cursor = 0;
	}

	@SuppressWarnings("unchecked")
	public void destroyAll() {
		for (int i = 0; i < this.size; i++) {
			this.destroy((T) this.pool[i]);
		}
	}
}