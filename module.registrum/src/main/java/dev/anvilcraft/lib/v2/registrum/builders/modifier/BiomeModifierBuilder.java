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
import dev.anvilcraft.lib.v2.registrum.util.entry.modifier.BiomeModifierEntry;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class BiomeModifierBuilder<T extends BiomeModifier, P>
    extends AbstractBuilder<MapCodec<? extends BiomeModifier>, MapCodec<T>, P, BiomeModifierBuilder<T, P>> {
    private final MapCodec<T> mapCodec;

    public BiomeModifierBuilder(AbstractRegistrum<?> owner, P parent, String name, BuilderCallback callback, MapCodec<T> mapCodec) {
        super(owner, parent, name, callback, NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS);
        this.mapCodec = mapCodec;
    }

    @Override
    protected MapCodec<T> createEntry() {
        return mapCodec;
    }

    @Override
    public BiomeModifierEntry<T> register() {
        return (BiomeModifierEntry<T>) super.register();
    }

    @Override
    protected BiomeModifierEntry<T> createEntryWrapper(DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<T>> delegate) {
        return new BiomeModifierEntry<>(getOwner(), delegate);
    }
}
