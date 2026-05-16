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

package dev.anvilcraft.lib.v2.registrum.builders.self;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.builders.BuilderCallback;
import dev.anvilcraft.lib.v2.registrum.builders.SelfBuilder;
import dev.anvilcraft.lib.v2.registrum.util.entry.SoundEventEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

public class SoundEventBuilder<P> extends SelfBuilder<SoundEvent, P, SoundEventBuilder<P>> {
    public SoundEventBuilder(AbstractRegistrum<?> owner,
                             P parent,
                             String name,
                             BuilderCallback callback) {
        super(owner, parent, name, callback, Registries.SOUND_EVENT, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(owner.getModid(), name)));
    }
    public SoundEventBuilder(AbstractRegistrum<?> owner,
                             P parent,
                             String name,
                             BuilderCallback callback,
                             float fix) {
        super(owner, parent, name, callback, Registries.SOUND_EVENT, () -> SoundEvent.createFixedRangeEvent(Identifier.fromNamespaceAndPath(owner.getModid(), name), fix));
    }

    @Override
    public SoundEventEntry register() {
        return (SoundEventEntry) super.register();
    }

    @Override
    protected SoundEventEntry createEntryWrapper(DeferredHolder<SoundEvent, SoundEvent> delegate) {
        return new SoundEventEntry(getOwner(), delegate);
    }
}
