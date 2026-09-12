/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/util/Sequence.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.util;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public class Sequence<T> {

    public static <T> Sequence<T> create() {
        return new Sequence<>();
    }

    public Sequence<T> run(Runnable toRun) {
        toRun.run();
        return this;
    }

    public Sequence<T> next(Supplier<T> val) {
        val.get();
        return this;
    }

    public Sequence<T> next(T val) {
        return this;
    }
}
