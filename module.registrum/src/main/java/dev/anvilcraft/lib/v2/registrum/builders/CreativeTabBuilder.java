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

package dev.anvilcraft.lib.v2.registrum.builders;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings(
    {
        "unused",
        "UnusedReturnValue"
    }
)
public class CreativeTabBuilder<P> extends AbstractBuilder<CreativeModeTab, CreativeModeTab, P, CreativeTabBuilder<P>> {
    private CreativeModeTab.Builder builder = CreativeModeTab.builder();

    public CreativeTabBuilder(AbstractRegistrum<?> owner, P parent, String name, BuilderCallback callback) {
        super(owner, parent, name, callback, Registries.CREATIVE_MODE_TAB);
    }

    public static <P> CreativeTabBuilder<P> create(
        AbstractRegistrum<?> owner,
        P parent,
        String name,
        BuilderCallback callback,
        Supplier<ItemLike> icon
    ) {
        return new CreativeTabBuilder<>(owner, parent, name, callback).defaultTitle().icon(() -> icon.get().asItem().getDefaultInstance());
    }

    public CreativeTabBuilder<P> defaultTitle() {
        String descriptionId = Util.makeDescriptionId("itemGroup", this.getResourceKey().identifier());
        lang((_) -> descriptionId);
        this.title(Component.translatable(descriptionId));
        return this;
    }

    public CreativeTabBuilder<P> title(Component displayName) {
        this.builder = this.builder.title(displayName);
        return this;
    }

    public CreativeTabBuilder<P> icon(Supplier<ItemStack> iconGenerator) {
        this.builder = this.builder.icon(iconGenerator);
        return this;
    }

    public CreativeTabBuilder<P> displayItems(CreativeModeTab.DisplayItemsGenerator displayItemsGenerator) {
        this.builder = this.builder.displayItems(displayItemsGenerator);
        return this;
    }

    public CreativeTabBuilder<P> alignedRight() {
        this.builder = this.builder.alignedRight();
        return this;
    }

    public CreativeTabBuilder<P> hideTitle() {
        this.builder = this.builder.hideTitle();
        return this;
    }

    public CreativeTabBuilder<P> noScrollBar() {
        this.builder = this.builder.noScrollBar();
        return this;
    }

    public CreativeTabBuilder<P> backgroundTexture(Identifier backgroundTexture) {
        this.builder = this.builder.backgroundTexture(backgroundTexture);
        return this;
    }

    public CreativeTabBuilder<P> withSearchBar() {
        this.builder = this.builder.withSearchBar();
        return this;
    }

    public CreativeTabBuilder<P> withSearchBar(int searchBarWidth) {
        this.builder = this.builder.withSearchBar(searchBarWidth);
        return this;
    }

    public CreativeTabBuilder<P> withScrollBarSpriteLocation(Identifier scrollBarSpriteLocation) {
        this.builder = this.builder.withScrollBarSpriteLocation(scrollBarSpriteLocation);
        return this;
    }

    public CreativeTabBuilder<P> withTabsImage(Identifier tabsImage) {
        this.builder = this.builder.withTabsImage(tabsImage);
        return this;
    }

    public CreativeTabBuilder<P> withLabelColor(int labelColor) {
        this.builder = this.builder.withLabelColor(labelColor);
        return this;
    }

    public CreativeTabBuilder<P> withTabFactory(Function<CreativeModeTab.Builder, CreativeModeTab> tabFactory) {
        this.builder = this.builder.withTabFactory(tabFactory);
        return this;
    }

    public CreativeTabBuilder<P> withTabsBefore(Identifier... tabs) {
        this.builder = this.builder.withTabsBefore(tabs);
        return this;
    }

    public CreativeTabBuilder<P> withTabsAfter(Identifier... tabs) {
        this.builder = this.builder.withTabsAfter(tabs);
        return this;
    }

    @SafeVarargs
    public final CreativeTabBuilder<P> withTabsBefore(DeferredHolder<CreativeModeTab, CreativeModeTab>... tabs) {
        @SuppressWarnings("unchecked") ResourceKey<CreativeModeTab>[] array = (ResourceKey<CreativeModeTab>[]) new ResourceKey[tabs.length];
        for (int i = 0; i < tabs.length; i++) {
            array[i] = tabs[i].getKey();
        }
        this.withTabsBefore(array);
        return this;
    }

    @SafeVarargs
    public final CreativeTabBuilder<P> withTabsAfter(DeferredHolder<CreativeModeTab, CreativeModeTab>... tabs) {
        @SuppressWarnings("unchecked") ResourceKey<CreativeModeTab>[] array = (ResourceKey<CreativeModeTab>[]) new ResourceKey[tabs.length];
        for (int i = 0; i < tabs.length; i++) {
            array[i] = tabs[i].getKey();
        }
        this.withTabsAfter(array);
        return this;
    }

    @SafeVarargs
    public final CreativeTabBuilder<P> withTabsBefore(ResourceKey<CreativeModeTab>... tabs) {
        this.builder = this.builder.withTabsBefore(tabs);
        return this;
    }

    @SafeVarargs
    public final CreativeTabBuilder<P> withTabsAfter(ResourceKey<CreativeModeTab>... tabs) {
        this.builder = this.builder.withTabsAfter(tabs);
        return this;
    }

    public CreativeTabBuilder<P> displayItems(Collection<? extends Holder<? extends ItemLike>> collection) {
        this.builder = this.builder.displayItems(collection);
        return this;
    }


    public CreativeTabBuilder<P> displayItems(ItemLike... itemLikes) {
        this.displayItems((parameters, output) -> {
            for (ItemLike itemLike : itemLikes) {
                output.accept(itemLike);
            }
        });
        return this;
    }

    @Override
    protected CreativeModeTab createEntry() {
        return this.builder.build();
    }
}
