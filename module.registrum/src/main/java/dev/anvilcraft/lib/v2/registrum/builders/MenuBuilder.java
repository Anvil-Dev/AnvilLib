/*
 *
 * Original work copyright (c) 2019 tterrag1098 (Registrate)
 * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/builders/MenuBuilder.java
 *
 */

package dev.anvilcraft.lib.v2.registrum.builders;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.util.OneTimeEventReceiver;
import dev.anvilcraft.lib.v2.registrum.util.RegistrumDistExecutor;
import dev.anvilcraft.lib.v2.registrum.util.entry.MenuEntry;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import dev.anvilcraft.lib.v2.util.nullness.NonNullSupplier;
import dev.anvilcraft.lib.v2.util.nullness.NonnullType;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;

import org.jspecify.annotations.Nullable;

@SuppressWarnings("unused")
public class MenuBuilder<T extends AbstractContainerMenu, S extends Screen & MenuAccess<T>, P>
    extends AbstractBuilder<MenuType<?>, MenuType<T>, P, MenuBuilder<T, S, P>> {

    public interface MenuFactory<T extends AbstractContainerMenu> {

        T create(MenuType<T> type, int windowId, Inventory inv);
    }

    public interface ForgeMenuFactory<T extends AbstractContainerMenu> {

        T create(MenuType<T> type, int windowId, Inventory inv, @Nullable RegistryFriendlyByteBuf buffer);
    }

    public interface ScreenFactory<M extends AbstractContainerMenu, T extends Screen & MenuAccess<M>> {

        T create(M menu, Inventory inv, Component displayName);
    }

    private final ForgeMenuFactory<T> factory;
    private final NonNullSupplier<ScreenFactory<T, S>> screenFactory;

    public MenuBuilder(
        AbstractRegistrum<?> owner,
        P parent,
        String name,
        BuilderCallback callback,
        MenuFactory<T> factory,
        NonNullSupplier<ScreenFactory<T, S>> screenFactory
    ) {
        this(owner, parent, name, callback, (type, windowId, inv, $) -> factory.create(type, windowId, inv), screenFactory);
    }

    public MenuBuilder(
        AbstractRegistrum<?> owner,
        P parent,
        String name,
        BuilderCallback callback,
        ForgeMenuFactory<T> factory,
        NonNullSupplier<ScreenFactory<T, S>> screenFactory
    ) {
        super(owner, parent, name, callback, Registries.MENU);
        this.factory = factory;
        this.screenFactory = screenFactory;
    }

    @Override
    protected @NonnullType MenuType<T> createEntry() {
        ForgeMenuFactory<T> factory = this.factory;
        final var supplier = this.asSupplier();
        MenuType<T> ret = IMenuTypeExtension.create((windowId, inv, buf) -> factory.create(supplier.get(), windowId, inv, buf));
        RegistrumDistExecutor.unsafeRunWhenOn(
            Dist.CLIENT, () -> () -> {
                ScreenFactory<T, S> screenFactory = this.screenFactory.get();
                OneTimeEventReceiver.addModListener(
                    this.getOwner(), RegisterMenuScreensEvent.class, event -> {
                        event.register(ret, screenFactory::create);
                    }
                );
            }
        );
        return ret;
    }

    @Override
    protected RegistryEntry<MenuType<?>, MenuType<T>> createEntryWrapper(DeferredHolder<MenuType<?>, MenuType<T>> delegate) {
        return new MenuEntry<>(getOwner(), delegate);
    }

    @Override
    public MenuEntry<T> register() {
        return (MenuEntry<T>) super.register();
    }
}
