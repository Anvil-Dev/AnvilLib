/*
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Modified work copyright (c) 2025 IThundxr (Registrate fork)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/IThundxr/Registrate/blob/1.21/dev/src/main/java/com/tterrag/registrate/util/nullness/NonNullConsumer.java
 */

package dev.anvilcraft.lib.v2.util.nullness;

import java.util.Objects;
import java.util.function.Consumer;

@FunctionalInterface
public interface NonNullConsumer<@NonnullType T> extends Consumer<T> {
    
    @Override
    void accept(T t);
    
    default NonNullConsumer<T> andThen(NonNullConsumer<? super T> after) {
        Objects.requireNonNull(after);
        return (T t) -> { accept(t); after.accept(t); };
    }
    
    static <T> NonNullConsumer<T> noop() {
        return t -> {};
    }
}
