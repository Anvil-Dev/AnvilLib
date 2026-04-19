/*
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Modified work copyright (c) 2025 IThundxr (Registrate fork)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/IThundxr/Registrate/blob/1.21/dev/src/main/java/com/tterrag/registrate/util/nullness/NonNullUnaryOperator.java
 */

package dev.anvilcraft.lib.v2.util.nullness;

import java.util.Objects;

@FunctionalInterface
public interface NonNullUnaryOperator<T> extends NonNullFunction<T, T> {

    static <T> NonNullUnaryOperator<T> identity() {
        return t -> t;
    }

    default <V> NonNullUnaryOperator<T> andThen(NonNullUnaryOperator<T> after) {
        Objects.requireNonNull(after);
        return t -> after.apply(apply(t));
    }
}
