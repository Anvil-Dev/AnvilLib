/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/Registrate.java
 *
 */

package dev.anvilcraft.lib.v2.registrum;

import lombok.extern.log4j.Log4j2;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;

import java.util.Optional;

@Log4j2
public class Registrum extends AbstractRegistrum<Registrum> {
    /**
     * Create a new {@link Registrum} and register event listeners for registration and data generation. Used in lieu of adding side-effects to constructor, so that alternate initialization
     * strategies can be done in subclasses.
     *
     * @param modid The mod ID for which objects will be registered
     * @return The {@link Registrum} instance
     */
    public static Registrum create(String modid) {
        var ret = new Registrum(modid);

        Optional<IEventBus> modEventBus = ModList.get().getModContainerById(modid)
            .map(ModContainer::getEventBus);

        modEventBus.ifPresentOrElse(
            ret::registerEventListeners, () -> {
                String message = "# [Registrum] Failed to register eventListeners for mod " + modid + ", This should be reported to this mod's dev #";


                StringBuilder hashtags = new StringBuilder().repeat("#", message.length());

                log.fatal(hashtags.toString());
                log.fatal(message);
                log.fatal(hashtags.toString());
            }
        );

        return ret;
    }

    protected Registrum(String modid) {
        super(modid);
    }
}
