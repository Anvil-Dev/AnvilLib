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

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.builders.AbstractBuilder;
import dev.anvilcraft.lib.v2.registrum.builders.BuilderCallback;
import dev.anvilcraft.lib.v2.registrum.util.entry.recipe.RecipeTypeEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;

public class RecipeTypeBuilder<T extends Recipe<?>, P> extends AbstractBuilder<RecipeType<?>, RecipeType<T>, P, RecipeTypeBuilder<T, P>> {
    public RecipeTypeBuilder(AbstractRegistrum<?> owner, P parent, String name, BuilderCallback callback) {
        super(owner, parent, name, callback, Registries.RECIPE_TYPE);
    }

    @Override
    protected RecipeType<T> createEntry() {
        return new RecipeType<T>() {
            @Override
            public String toString() {
                return getOwner().getModid() + ":" + getName();
            }
        };
    }

    @Override
    public RecipeTypeEntry<T> register() {
        return (RecipeTypeEntry<T>) super.register();
    }

    @Override
    protected RecipeTypeEntry<T> createEntryWrapper(DeferredHolder<RecipeType<?>, RecipeType<T>> delegate) {
        return new RecipeTypeEntry<>(getOwner(), delegate);
    }
}
