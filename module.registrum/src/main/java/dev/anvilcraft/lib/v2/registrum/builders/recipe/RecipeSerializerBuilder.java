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

package dev.anvilcraft.lib.v2.registrum.builders.recipe;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.builders.AbstractBuilder;
import dev.anvilcraft.lib.v2.registrum.builders.BuilderCallback;
import dev.anvilcraft.lib.v2.registrum.util.entry.recipe.RecipeSerializerEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;

public class RecipeSerializerBuilder<T extends Recipe<?>, P>
    extends AbstractBuilder<RecipeSerializer<?>, RecipeSerializer<T>, P, RecipeSerializerBuilder<T, P>> {
    private final MapCodec<T> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

    public RecipeSerializerBuilder(
        AbstractRegistrum<?> owner,
        P parent,
        String name,
        BuilderCallback callback,
        MapCodec<T> codec,
        StreamCodec<RegistryFriendlyByteBuf, T> streamCodec
    ) {
        super(owner, parent, name, callback, Registries.RECIPE_SERIALIZER);
        this.codec = codec;
        this.streamCodec = streamCodec;
    }

    @Override
    public RecipeSerializerEntry<T> register() {
        return (RecipeSerializerEntry<T>) super.register();
    }

    @Override
    protected RecipeSerializerEntry<T> createEntryWrapper(DeferredHolder<RecipeSerializer<?>, RecipeSerializer<T>> delegate) {
        return new RecipeSerializerEntry<>(getOwner(), delegate);
    }

    @Override
    protected RecipeSerializer<T> createEntry() {
        // 1.21.1: RecipeSerializer 是接口（含 codec()/streamCodec()），不能 new RecipeSerializer<>(...)，须匿名类实现
        return new RecipeSerializer<T>() {
            @Override
            public MapCodec<T> codec() {
                return codec;
            }

            @Override
            public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
                return streamCodec;
            }
        };
    }
}
