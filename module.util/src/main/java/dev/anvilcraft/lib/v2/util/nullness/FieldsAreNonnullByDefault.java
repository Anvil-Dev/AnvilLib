/*
 *
 *  * Original work copyright (c) 2019 tterrag1098 (Registrate)
 *  * Additional modifications copyright (c) 2026 Anvil-Dev (AnvilLib-Registrum)
 *  *
 *  * This Source Code Form is subject to the terms of the Mozilla Public
 *  * License, v. 2.0. If a copy of the MPL was not distributed with this
 *  * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *  *
 *  * Original File: https://github.com/tterrag1098/Registrate/blob/1.21.5/dev/src/main/java/com/tterrag/registrate/util/nullness/FieldsAreNonnullByDefault.java
 *
 */

package dev.anvilcraft.lib.v2.util.nullness;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;

/**
 * This annotation can be applied to a package or class to indicate that the fields in that element are nonnull by default unless there is:
 * <ul>
 * <li>An explicit nullness annotation
 * <li>there is a default parameter annotation applied to a more tightly nested element.
 * </ul>
 */
@Documented
@Nonnull
@TypeQualifierDefault(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Target(
    {
        ElementType.PACKAGE,
        ElementType.TYPE
    }
)
public @interface FieldsAreNonnullByDefault {
}
