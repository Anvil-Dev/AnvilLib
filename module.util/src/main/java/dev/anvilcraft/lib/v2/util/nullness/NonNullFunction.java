/*
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Modified work copyright (c) 2025 IThundxr (Registrate fork)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/IThundxr/Registrate/blob/1.21/dev/src/main/java/com/tterrag/registrate/util/nullness/NonNullFunction.java
 */

package dev.anvilcraft.lib.v2.util.nullness;

import java.util.Objects;
import java.util.function.Function;

@FunctionalInterface
public interface NonNullFunction<@NonnullType T, @NonnullType R> extends Function<T, R> {

    @Override
    R apply(T t);

    default <V> NonNullFunction<T, V> andThen(NonNullFunction<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return t -> after.apply(apply(t));
    }
}
