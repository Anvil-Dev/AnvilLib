/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/util/RegistrateDistExecutor.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.util;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;

@ApiStatus.Internal
public class RegistrumDistExecutor {
    public static void unsafeRunWhenOn(Dist dist, Supplier<Runnable> toRun) {
        if (dist == FMLLoader.getCurrent().getDist()) {
            toRun.get().run();
        }
    }
}
