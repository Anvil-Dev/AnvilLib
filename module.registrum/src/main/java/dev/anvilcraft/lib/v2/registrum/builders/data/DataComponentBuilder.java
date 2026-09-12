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

package dev.anvilcraft.lib.v2.registrum.builders.data;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.builders.AbstractBuilder;
import dev.anvilcraft.lib.v2.registrum.builders.BuilderCallback;
import dev.anvilcraft.lib.v2.registrum.util.entry.data.DataComponentEntry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;

@SuppressWarnings("unused")
public class DataComponentBuilder<E, P> extends AbstractBuilder<DataComponentType<?>, DataComponentType<E>, P, DataComponentBuilder<E, P>> {
    final DataComponentType.Builder<E> builder;

    public DataComponentBuilder(AbstractRegistrum<?> owner, P parent, String name, BuilderCallback callback) {
        super(owner, parent, name, callback, Registries.DATA_COMPONENT_TYPE);
        builder = DataComponentType.builder();
    }

    public DataComponentBuilder<E, P> persistent(Codec<E> codec) {
        builder.persistent(codec);
        return this;
    }

    public DataComponentBuilder<E, P> networkSynchronized(StreamCodec<? super RegistryFriendlyByteBuf, E> streamCodec) {
        builder.networkSynchronized(streamCodec);
        return this;
    }

    public DataComponentBuilder<E, P> cacheEncoding() {
        builder.cacheEncoding();
        return this;
    }

    public DataComponentBuilder<E, P> ignoreSwapAnimation() {
        builder.ignoreSwapAnimation();
        return this;
    }

    @Override
    protected DataComponentType<E> createEntry() {
        return builder.build();
    }

    @Override
    public DataComponentEntry<E> register() {
        return (DataComponentEntry<E>) super.register();
    }

    @Override
    protected DataComponentEntry<E> createEntryWrapper(DeferredHolder<DataComponentType<?>, DataComponentType<E>> delegate) {
        return new DataComponentEntry<>(getOwner(), delegate);
    }
}
