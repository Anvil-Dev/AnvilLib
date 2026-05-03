/*
 *
 *  * Original work copyright (c) 2019 tterrag1098 (Registrate)
 *  * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *  *
 *  * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/util/OneTimeEventReceiver.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.util;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.util.nullness.NonnullType;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@RequiredArgsConstructor
@Log4j2
public class OneTimeEventReceiver<T extends Event> implements Consumer<@NonnullType T> {


    public static <T extends Event & IModBusEvent> void addModListener(
        AbstractRegistrum<?> owner,
        Class<? super T> evtClass,
        Consumer<? super T> listener
    ) {
        OneTimeEventReceiver.<T>addModListener(owner, EventPriority.NORMAL, evtClass, listener);
    }

    public static <T extends Event & IModBusEvent> void addModListener(
        AbstractRegistrum<?> owner,
        EventPriority priority,
        Class<? super T> evtClass,
        Consumer<? super T> listener
    ) {
        if (owner.getModEventBus() == null) {
            if (!waitingModListeners.contains(owner, evtClass)) {
                waitingModListeners.put(owner, evtClass, new ArrayList<>());
            }
            waitingModListeners.get(owner, evtClass).add(Pair.of(priority, listener));
            return;
        }
        if (!seenModBus) {
            seenModBus = true;
            for (var waitingListener : waitingModListeners.row(owner).entrySet()) {
                for (var pair : waitingListener.getValue()) {
                    //noinspection unchecked
                    OneTimeEventReceiver.<T>addListener(
                        owner.getModEventBus(),
                        pair.getKey(),
                        (Class<? super T>) waitingListener.getKey(),
                        (Consumer<? super T>) pair.getValue()
                    );
                }
            }
            addModListener(owner, FMLLoadCompleteEvent.class, OneTimeEventReceiver::onLoadComplete);
        }
        OneTimeEventReceiver.<T>addListener(owner.getModEventBus(), priority, evtClass, listener);
    }

    public static <T extends Event> void addForgeListener(Class<? super T> evtClass, Consumer<? super T> listener) {
        OneTimeEventReceiver.<T>addForgeListener(EventPriority.NORMAL, evtClass, listener);
    }

    public static <T extends Event> void addForgeListener(EventPriority priority, Class<? super T> evtClass, Consumer<? super T> listener) {
        OneTimeEventReceiver.<T>addListener(NeoForge.EVENT_BUS, priority, evtClass, listener);
    }

    @Deprecated
    public static <T extends Event> void addListener(IEventBus bus, Class<? super T> evtClass, Consumer<? super T> listener) {
        OneTimeEventReceiver.<T>addListener(bus, EventPriority.NORMAL, evtClass, listener);
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    public static <T extends Event> void addListener(
        IEventBus bus,
        EventPriority priority,
        Class<? super T> evtClass,
        Consumer<? super T> listener
    ) {
        bus.addListener(priority, false, (Class<T>) evtClass, new OneTimeEventReceiver<>(bus, listener));
    }

    private static boolean seenModBus = false;
    private static final Table<AbstractRegistrum<?>, Class<?>, List<Pair<EventPriority, Consumer<?>>>> waitingModListeners = HashBasedTable.create();

    private final IEventBus bus;
    private final Consumer<? super T> listener;
    private final AtomicBoolean consumed = new AtomicBoolean();

    @Override
    public void accept(T event) {
        if (consumed.compareAndSet(false, true)) {
            listener.accept(event);
            unregister(bus, this, event);
        }
    }

    private static final List<Triple<IEventBus, Object, Class<? extends Event>>> toUnregister = new ArrayList<>();

    private static synchronized void unregister(IEventBus bus, Object listener, Event event) {
        unregister(bus, listener, event.getClass());
    }

    public static synchronized void unregister(AbstractRegistrum<?> owner, Object listener, Class<? extends Event> event) {
        unregister(owner.getModEventBus(), listener, event);
    }

    private static synchronized void unregister(IEventBus bus, Object listener, Class<? extends Event> event) {
        toUnregister.add(Triple.of(bus, listener, event));
    }

    private static void onLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            toUnregister.forEach(t -> t.getLeft().unregister(t.getMiddle()));
            toUnregister.clear();
        });
    }
}
