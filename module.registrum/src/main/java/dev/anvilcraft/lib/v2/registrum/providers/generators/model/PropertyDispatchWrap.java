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

package dev.anvilcraft.lib.v2.registrum.providers.generators.model;

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.client.model.generators.blockstate.UnbakedMutator;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 该类旨在解决 DataGenerator 中包含 {@link PropertyDispatch} 时引发的开发环境 runServer 任务异常崩溃的问题
 * <p>
 * 用法与 {@link PropertyDispatch} 基本一致，在需要还原成为 {@link PropertyDispatch} 时调用 PropertyDispatchWrap.dispatch 即可
 */
@SuppressWarnings("unused")
public abstract class PropertyDispatchWrap<V> {
    public abstract PropertyDispatch<V> dispatch();

    public static <T1 extends Comparable<T1>> PropertyDispatchWrap.C1<MultiVariant, T1> initial(Property<T1> property1) {
        return new C1<>(PropertyDispatch.initial(property1));
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> PropertyDispatchWrap.C2<MultiVariant, T1, T2> initial(
        Property<T1> property1,
        Property<T2> property2
    ) {
        return new PropertyDispatchWrap.C2<>(PropertyDispatch.initial(property1, property2));
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> PropertyDispatchWrap.C3<MultiVariant, T1, T2, T3> initial(
        Property<T1> property1,
        Property<T2> property2,
        Property<T3> property3
    ) {
        return new PropertyDispatchWrap.C3<>(PropertyDispatch.initial(property1, property2, property3));
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> PropertyDispatchWrap.C4<MultiVariant, T1, T2, T3, T4> initial(
        Property<T1> property1,
        Property<T2> property2,
        Property<T3> property3,
        Property<T4> property4
    ) {
        return new PropertyDispatchWrap.C4<>(PropertyDispatch.initial(property1, property2, property3, property4));
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> PropertyDispatchWrap.C5<MultiVariant, T1, T2, T3, T4, T5> initial(
        Property<T1> property1,
        Property<T2> property2,
        Property<T3> property3,
        Property<T4> property4,
        Property<T5> property5
    ) {
        return new PropertyDispatchWrap.C5<>(PropertyDispatch.initial(property1, property2, property3, property4, property5));
    }

    public static <T1 extends Comparable<T1>> PropertyDispatchWrap.C1<VariantMutator, T1> modify(Property<T1> property1) {
        return new PropertyDispatchWrap.C1<>(PropertyDispatch.modify(property1));
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> PropertyDispatchWrap.C2<VariantMutator, T1, T2> modify(
        Property<T1> property1,
        Property<T2> property2
    ) {
        return new PropertyDispatchWrap.C2<>(PropertyDispatch.modify(property1, property2));
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> PropertyDispatchWrap.C3<VariantMutator, T1, T2, T3> modify(
        Property<T1> property1,
        Property<T2> property2,
        Property<T3> property3
    ) {
        return new PropertyDispatchWrap.C3<>(PropertyDispatch.modify(property1, property2, property3));
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> PropertyDispatchWrap.C4<VariantMutator, T1, T2, T3, T4> modify(
        Property<T1> property1,
        Property<T2> property2,
        Property<T3> property3,
        Property<T4> property4
    ) {
        return new PropertyDispatchWrap.C4<>(PropertyDispatch.modify(property1, property2, property3, property4));
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> PropertyDispatchWrap.C5<VariantMutator, T1, T2, T3, T4, T5> modify(
        Property<T1> property1,
        Property<T2> property2,
        Property<T3> property3,
        Property<T4> property4,
        Property<T5> property5
    ) {
        return new PropertyDispatchWrap.C5<>(PropertyDispatch.modify(property1, property2, property3, property4, property5));
    }

    public static <T1 extends Comparable<T1>> PropertyDispatchWrap.C1<UnbakedMutator, T1> modifyUnbaked(Property<T1> p1) {
        return new PropertyDispatchWrap.C1<>(PropertyDispatch.modifyUnbaked(p1));
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> PropertyDispatchWrap.C2<UnbakedMutator, T1, T2> modifyUnbaked(
        Property<T1> p1,
        Property<T2> p2
    ) {
        return new PropertyDispatchWrap.C2<>(PropertyDispatch.modifyUnbaked(p1, p2));
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> PropertyDispatchWrap.C3<UnbakedMutator, T1, T2, T3> modifyUnbaked(
        Property<T1> p1,
        Property<T2> p2,
        Property<T3> p3
    ) {
        return new PropertyDispatchWrap.C3<>(PropertyDispatch.modifyUnbaked(p1, p2, p3));
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> PropertyDispatchWrap.C4<UnbakedMutator, T1, T2, T3, T4> modifyUnbaked(
        Property<T1> p1,
        Property<T2> p2,
        Property<T3> p3,
        Property<T4> p4
    ) {
        return new PropertyDispatchWrap.C4<>(PropertyDispatch.modifyUnbaked(p1, p2, p3, p4));
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> PropertyDispatchWrap.C5<UnbakedMutator, T1, T2, T3, T4, T5> modifyUnbaked(
        Property<T1> p1,
        Property<T2> p2,
        Property<T3> p3,
        Property<T4> p4,
        Property<T5> p5
    ) {
        return new PropertyDispatchWrap.C5<>(PropertyDispatch.modifyUnbaked(p1, p2, p3, p4, p5));
    }


    public static class C1<V, T1 extends Comparable<T1>> extends PropertyDispatchWrap<V> {
        private final PropertyDispatch.C1<V, T1> dispatch;

        public C1(PropertyDispatch.C1<V, T1> dispatch) {
            this.dispatch = dispatch;
        }

        public List<Property<?>> getDefinedProperties() {
            return this.dispatch.getDefinedProperties();
        }

        public PropertyDispatchWrap.C1<V, T1> select(T1 value1, V variants) {
            this.dispatch.select(value1, variants);
            return this;
        }

        public PropertyDispatch<V> generate(Function<T1, V> generator) {
            return this.dispatch.generate(generator);
        }

        public PropertyDispatch<V> dispatch() {
            return this.dispatch;
        }
    }


    public static class C2<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>> extends PropertyDispatchWrap<V> {
        private final PropertyDispatch.C2<V, T1, T2> dispatch;

        public C2(PropertyDispatch.C2<V, T1, T2> dispatch) {
            this.dispatch = dispatch;
        }

        public List<Property<?>> getDefinedProperties() {
            return this.dispatch.getDefinedProperties();
        }

        public PropertyDispatchWrap.C2<V, T1, T2> select(T1 value1, T2 value2, V variants) {
            this.dispatch.select(value1, value2, variants);
            return this;
        }

        public PropertyDispatch<V> generate(BiFunction<T1, T2, V> generator) {
            return this.dispatch.generate(generator);
        }

        public PropertyDispatch<V> dispatch() {
            return this.dispatch;
        }
    }


    public static class C3<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>>
        extends PropertyDispatchWrap<V> {
        private final PropertyDispatch.C3<V, T1, T2, T3> dispatch;

        public C3(PropertyDispatch.C3<V, T1, T2, T3> dispatch) {
            this.dispatch = dispatch;
        }

        public List<Property<?>> getDefinedProperties() {
            return this.dispatch.getDefinedProperties();
        }

        public PropertyDispatchWrap.C3<V, T1, T2, T3> select(T1 value1, T2 value2, T3 value3, V variants) {
            this.dispatch.select(value1, value2, value3, variants);
            return this;
        }

        public PropertyDispatch<V> generate(Function3<T1, T2, T3, V> generator) {
            return this.dispatch.generate(generator);
        }

        public PropertyDispatch<V> dispatch() {
            return this.dispatch;
        }
    }


    public static class C4<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>>
        extends PropertyDispatchWrap<V> {
        private final PropertyDispatch.C4<V, T1, T2, T3, T4> dispatch;

        public C4(PropertyDispatch.C4<V, T1, T2, T3, T4> dispatch) {
            this.dispatch = dispatch;
        }

        public List<Property<?>> getDefinedProperties() {
            return this.dispatch.getDefinedProperties();
        }

        public PropertyDispatchWrap.C4<V, T1, T2, T3, T4> select(T1 value1, T2 value2, T3 value3, T4 value4, V variants) {
            this.dispatch.select(value1, value2, value3, value4, variants);
            return this;
        }

        public PropertyDispatch<V> generate(Function4<T1, T2, T3, T4, V> generator) {
            return this.dispatch.generate(generator);
        }

        public PropertyDispatch<V> dispatch() {
            return this.dispatch;
        }
    }


    public static class C5<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>>
        extends PropertyDispatchWrap<V> {
        private final PropertyDispatch.C5<V, T1, T2, T3, T4, T5> dispatch;

        public C5(PropertyDispatch.C5<V, T1, T2, T3, T4, T5> dispatch) {
            this.dispatch = dispatch;
        }

        public List<Property<?>> getDefinedProperties() {
            return this.dispatch.getDefinedProperties();
        }

        public PropertyDispatchWrap.C5<V, T1, T2, T3, T4, T5> select(T1 value1, T2 value2, T3 value3, T4 value4, T5 value5, V variants) {
            this.dispatch.select(value1, value2, value3, value4, value5, variants);
            return this;
        }

        public PropertyDispatch<V> generate(Function5<T1, T2, T3, T4, T5, V> generator) {
            return this.dispatch.generate(generator);
        }

        public PropertyDispatch<V> dispatch() {
            return this.dispatch;
        }
    }
}
