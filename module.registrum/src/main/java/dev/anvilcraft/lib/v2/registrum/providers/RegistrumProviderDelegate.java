/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/providers/RegistrateProviderDelegate.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.providers;

import net.minecraft.data.DataProvider;
import net.minecraft.resources.Identifier;

public interface RegistrumProviderDelegate<R, T extends R> extends DataProvider {

    String getName();

    Identifier getId();

    T getEntry();
}