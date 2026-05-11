/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/util/DebugMarkers.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.util;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

@SuppressWarnings("null")
public class DebugMarkers {

    private static final String PREFIX = "REGISTRATE.";

    private static Marker marker(String name) {
        return MarkerManager.getMarker(PREFIX + name);
    }

    public static final Marker REGISTER = marker("REGISTER");
    public static final Marker DATA = marker("DATA");
}