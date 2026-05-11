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

package dev.anvilcraft.lib.v2.registrum.util.entry.data;

import dev.anvilcraft.lib.v2.registrum.AbstractRegistrum;
import dev.anvilcraft.lib.v2.registrum.util.entry.RegistryEntry;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;

public class AttachmentEntry<E> extends RegistryEntry<AttachmentType<?>, AttachmentType<E>> {

    public AttachmentEntry(AbstractRegistrum<?> owner, DeferredHolder<AttachmentType<?>, AttachmentType<E>> key) {
        super(owner, key);
    }

    public static <E> AttachmentEntry<E> cast(RegistryEntry<AttachmentType<?>, AttachmentType<E>> entry) {
        return RegistryEntry.cast(AttachmentEntry.class, entry);
    }
}
