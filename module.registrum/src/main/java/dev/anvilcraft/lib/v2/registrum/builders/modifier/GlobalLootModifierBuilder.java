/*
 *
 * Original work copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package dev.anvilcraft.lib.v2.registrum.builders.modifier;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.builders.AbstractBuilder;
import dev.anvilcraft.lib.v2.registrum.builders.BuilderCallback;
import dev.anvilcraft.lib.v2.registrum.util.entry.modifier.GlobalLootModifierEntry;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class GlobalLootModifierBuilder<T extends IGlobalLootModifier, P>
    extends AbstractBuilder<MapCodec<? extends IGlobalLootModifier>, MapCodec<T>, P, GlobalLootModifierBuilder<T, P>> {

    final MapCodec<T> codec;

    public GlobalLootModifierBuilder(AbstractRegistrum<?> owner, P parent, String name, BuilderCallback callback, MapCodec<T> codec) {
        super(owner, parent, name, callback, NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS);
        this.codec = codec;
    }

    @Override
    protected MapCodec<T> createEntry() {
        return codec;
    }

    @Override
    public GlobalLootModifierEntry<T> register() {
        return (GlobalLootModifierEntry<T>) super.register();
    }

    @Override
    protected GlobalLootModifierEntry<T> createEntryWrapper(DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<T>> delegate) {
        return new GlobalLootModifierEntry<>(getOwner(), delegate);
    }
}
