package dev.anvilcraft.lib.v2.util;

import com.google.common.collect.AbstractIterator;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

/**
 * 笛卡尔积迭代器工厂。
 *
 * <p>用于替代多层嵌套循环：把若干维度的取值按维度顺序展开为单一序列，最靠前的维度位于最外层。
 * 每维只能整体用数组或整体用列表传入，因此每种维度数量只提供这两类重载。
 * 任一维度为空时不产出任何元素。
 */
public final class FlattenLoop {
    private FlattenLoop() {
    }

    /**
     * 按 2 维顺序展开笛卡尔积，最靠前的维度位于最外层。
     *
     * @param arrayT 第 1 维取值，按数组顺序展开
     * @param arrayU 第 2 维取值，按数组顺序展开
     * @return 按维度顺序依次产出 T、U 的所有组合
     */
    public static <T, U> Iterable<Objs2<T, U>> loop(T[] arrayT, U[] arrayU) {
        return FlattenLoop.loop(Arrays.asList(arrayT), Arrays.asList(arrayU));
    }

    /**
     * 按 2 维顺序展开笛卡尔积，最靠前的维度位于最外层。
     *
     * @param listT 第 1 维取值，按列表顺序展开
     * @param listU 第 2 维取值，按列表顺序展开
     * @return 按维度顺序依次产出 T、U 的所有组合
     */
    public static <T, U> Iterable<Objs2<T, U>> loop(List<T> listT, List<U> listU) {
        int nt = listT.size();
        int nu = listU.size();
        if (nt == 0 || nu == 0) return List.of();

        return () -> new AbstractIterator<>() {
            private int ti = 0;
            private int ui = 0;

            @Override
            protected @Nullable Objs2<T, U> computeNext() {
                if (this.ti == nt) return this.endOfData();

                Objs2<T, U> objs = new Objs2<>(listT.get(this.ti), listU.get(this.ui));

                if (++this.ui < nu) return objs;
                this.ui = 0;
                this.ti++;
                return objs;
            }
        };
    }

    /**
     * 按 3 维顺序展开笛卡尔积，最靠前的维度位于最外层。
     *
     * @param arrayT 第 1 维取值，按数组顺序展开
     * @param arrayU 第 2 维取值，按数组顺序展开
     * @param arrayV 第 3 维取值，按数组顺序展开
     * @return 按维度顺序依次产出 T、U、V 的所有组合
     */
    public static <T, U, V> Iterable<Objs3<T, U, V>> loop(T[] arrayT, U[] arrayU, V[] arrayV) {
        return FlattenLoop.loop(Arrays.asList(arrayT), Arrays.asList(arrayU), Arrays.asList(arrayV));
    }

    /**
     * 按 3 维顺序展开笛卡尔积，最靠前的维度位于最外层。
     *
     * @param listT 第 1 维取值，按列表顺序展开
     * @param listU 第 2 维取值，按列表顺序展开
     * @param listV 第 3 维取值，按列表顺序展开
     * @return 按维度顺序依次产出 T、U、V 的所有组合
     */
    public static <T, U, V> Iterable<Objs3<T, U, V>> loop(List<T> listT, List<U> listU, List<V> listV) {
        int nt = listT.size();
        int nu = listU.size();
        int nv = listV.size();
        if (nt == 0 || nu == 0 || nv == 0) return List.of();

        return () -> new AbstractIterator<>() {
            private int ti = 0;
            private int ui = 0;
            private int vi = 0;

            @Override
            protected @Nullable Objs3<T, U, V> computeNext() {
                if (this.ti == nt) return this.endOfData();

                Objs3<T, U, V> objs = new Objs3<>(listT.get(this.ti), listU.get(this.ui), listV.get(this.vi));

                if (++this.vi < nv) return objs;
                this.vi = 0;
                if (++this.ui < nu) return objs;
                this.ui = 0;
                this.ti++;
                return objs;
            }
        };
    }

    /**
     * 按 4 维顺序展开笛卡尔积，最靠前的维度位于最外层。
     *
     * @param arrayT 第 1 维取值，按数组顺序展开
     * @param arrayU 第 2 维取值，按数组顺序展开
     * @param arrayV 第 3 维取值，按数组顺序展开
     * @param arrayW 第 4 维取值，按数组顺序展开
     * @return 按维度顺序依次产出 T、U、V、W 的所有组合
     */
    public static <T, U, V, W> Iterable<Objs4<T, U, V, W>> loop(T[] arrayT, U[] arrayU, V[] arrayV, W[] arrayW) {
        return FlattenLoop.loop(Arrays.asList(arrayT), Arrays.asList(arrayU), Arrays.asList(arrayV), Arrays.asList(arrayW));
    }

    /**
     * 按 4 维顺序展开笛卡尔积，最靠前的维度位于最外层。
     *
     * @param listT 第 1 维取值，按列表顺序展开
     * @param listU 第 2 维取值，按列表顺序展开
     * @param listV 第 3 维取值，按列表顺序展开
     * @param listW 第 4 维取值，按列表顺序展开
     * @return 按维度顺序依次产出 T、U、V、W 的所有组合
     */
    public static <T, U, V, W> Iterable<Objs4<T, U, V, W>> loop(List<T> listT, List<U> listU, List<V> listV, List<W> listW) {
        int nt = listT.size();
        int nu = listU.size();
        int nv = listV.size();
        int nw = listW.size();
        if (nt == 0 || nu == 0 || nv == 0 || nw == 0) return List.of();

        return () -> new AbstractIterator<>() {
            private int ti = 0;
            private int ui = 0;
            private int vi = 0;
            private int wi = 0;

            @Override
            protected @Nullable Objs4<T, U, V, W> computeNext() {
                if (this.ti == nt) return this.endOfData();

                Objs4<T, U, V, W> objs = new Objs4<>(listT.get(this.ti), listU.get(this.ui), listV.get(this.vi), listW.get(this.wi));

                if (++this.wi < nw) return objs;
                this.wi = 0;
                if (++this.vi < nv) return objs;
                this.vi = 0;
                if (++this.ui < nu) return objs;
                this.ui = 0;
                this.ti++;
                return objs;
            }
        };
    }

    /**
     * 按 5 维顺序展开笛卡尔积，最靠前的维度位于最外层。
     *
     * @param arrayT 第 1 维取值，按数组顺序展开
     * @param arrayU 第 2 维取值，按数组顺序展开
     * @param arrayV 第 3 维取值，按数组顺序展开
     * @param arrayW 第 4 维取值，按数组顺序展开
     * @param arrayX 第 5 维取值，按数组顺序展开
     * @return 按维度顺序依次产出 T、U、V、W、X 的所有组合
     */
    public static <T, U, V, W, X> Iterable<Objs5<T, U, V, W, X>> loop(
        T[] arrayT,
        U[] arrayU,
        V[] arrayV,
        W[] arrayW,
        X[] arrayX
    ) {
        return FlattenLoop.loop(
            Arrays.asList(arrayT),
            Arrays.asList(arrayU),
            Arrays.asList(arrayV),
            Arrays.asList(arrayW),
            Arrays.asList(arrayX)
        );
    }

    /**
     * 按 5 维顺序展开笛卡尔积，最靠前的维度位于最外层。
     *
     * @param listT 第 1 维取值，按列表顺序展开
     * @param listU 第 2 维取值，按列表顺序展开
     * @param listV 第 3 维取值，按列表顺序展开
     * @param listW 第 4 维取值，按列表顺序展开
     * @param listX 第 5 维取值，按列表顺序展开
     * @return 按维度顺序依次产出 T、U、V、W、X 的所有组合
     */
    public static <T, U, V, W, X> Iterable<Objs5<T, U, V, W, X>> loop(
        List<T> listT,
        List<U> listU,
        List<V> listV,
        List<W> listW,
        List<X> listX
    ) {
        int nt = listT.size();
        int nu = listU.size();
        int nv = listV.size();
        int nw = listW.size();
        int nx = listX.size();
        if (nt == 0 || nu == 0 || nv == 0 || nw == 0 || nx == 0) return List.of();

        return () -> new AbstractIterator<>() {
            private int ti = 0;
            private int ui = 0;
            private int vi = 0;
            private int wi = 0;
            private int xi = 0;

            @Override
            protected @Nullable Objs5<T, U, V, W, X> computeNext() {
                if (this.ti == nt) return this.endOfData();

                Objs5<T, U, V, W, X> objs = new Objs5<>(
                    listT.get(this.ti),
                    listU.get(this.ui),
                    listV.get(this.vi),
                    listW.get(this.wi),
                    listX.get(this.xi)
                );

                if (++this.xi < nx) return objs;
                this.xi = 0;
                if (++this.wi < nw) return objs;
                this.wi = 0;
                if (++this.vi < nv) return objs;
                this.vi = 0;
                if (++this.ui < nu) return objs;
                this.ui = 0;
                this.ti++;
                return objs;
            }
        };
    }

    /**
     * 按 6 维顺序展开笛卡尔积，最靠前的维度位于最外层。
     *
     * @param arrayT 第 1 维取值，按数组顺序展开
     * @param arrayU 第 2 维取值，按数组顺序展开
     * @param arrayV 第 3 维取值，按数组顺序展开
     * @param arrayW 第 4 维取值，按数组顺序展开
     * @param arrayX 第 5 维取值，按数组顺序展开
     * @param arrayY 第 6 维取值，按数组顺序展开
     * @return 按维度顺序依次产出 T、U、V、W、X、Y 的所有组合
     */
    public static <T, U, V, W, X, Y> Iterable<Objs6<T, U, V, W, X, Y>> loop(
        T[] arrayT,
        U[] arrayU,
        V[] arrayV,
        W[] arrayW,
        X[] arrayX,
        Y[] arrayY
    ) {
        return FlattenLoop.loop(
            Arrays.asList(arrayT),
            Arrays.asList(arrayU),
            Arrays.asList(arrayV),
            Arrays.asList(arrayW),
            Arrays.asList(arrayX),
            Arrays.asList(arrayY)
        );
    }

    /**
     * 按 6 维顺序展开笛卡尔积，最靠前的维度位于最外层。
     *
     * @param listT 第 1 维取值，按列表顺序展开
     * @param listU 第 2 维取值，按列表顺序展开
     * @param listV 第 3 维取值，按列表顺序展开
     * @param listW 第 4 维取值，按列表顺序展开
     * @param listX 第 5 维取值，按列表顺序展开
     * @param listY 第 6 维取值，按列表顺序展开
     * @return 按维度顺序依次产出 T、U、V、W、X、Y 的所有组合
     */
    public static <T, U, V, W, X, Y> Iterable<Objs6<T, U, V, W, X, Y>> loop(
        List<T> listT,
        List<U> listU,
        List<V> listV,
        List<W> listW,
        List<X> listX,
        List<Y> listY
    ) {
        int nt = listT.size();
        int nu = listU.size();
        int nv = listV.size();
        int nw = listW.size();
        int nx = listX.size();
        int ny = listY.size();
        if (nt == 0 || nu == 0 || nv == 0 || nw == 0 || nx == 0 || ny == 0) return List.of();

        return () -> new AbstractIterator<>() {
            private int ti = 0;
            private int ui = 0;
            private int vi = 0;
            private int wi = 0;
            private int xi = 0;
            private int yi = 0;

            @Override
            protected @Nullable Objs6<T, U, V, W, X, Y> computeNext() {
                if (this.ti == nt) return this.endOfData();

                Objs6<T, U, V, W, X, Y> objs = new Objs6<>(
                    listT.get(this.ti),
                    listU.get(this.ui),
                    listV.get(this.vi),
                    listW.get(this.wi),
                    listX.get(this.xi),
                    listY.get(this.yi)
                );

                if (++this.yi < ny) return objs;
                this.yi = 0;
                if (++this.xi < nx) return objs;
                this.xi = 0;
                if (++this.wi < nw) return objs;
                this.wi = 0;
                if (++this.vi < nv) return objs;
                this.vi = 0;
                if (++this.ui < nu) return objs;
                this.ui = 0;
                this.ti++;
                return objs;
            }
        };
    }

    /**
     * 按 7 维顺序展开笛卡尔积，最靠前的维度位于最外层。
     *
     * @param arrayT 第 1 维取值，按数组顺序展开
     * @param arrayU 第 2 维取值，按数组顺序展开
     * @param arrayV 第 3 维取值，按数组顺序展开
     * @param arrayW 第 4 维取值，按数组顺序展开
     * @param arrayX 第 5 维取值，按数组顺序展开
     * @param arrayY 第 6 维取值，按数组顺序展开
     * @param arrayZ 第 7 维取值，按数组顺序展开
     * @return 按维度顺序依次产出 T、U、V、W、X、Y、Z 的所有组合
     */
    public static <T, U, V, W, X, Y, Z> Iterable<Objs7<T, U, V, W, X, Y, Z>> loop(
        T[] arrayT,
        U[] arrayU,
        V[] arrayV,
        W[] arrayW,
        X[] arrayX,
        Y[] arrayY,
        Z[] arrayZ
    ) {
        return FlattenLoop.loop(
            Arrays.asList(arrayT),
            Arrays.asList(arrayU),
            Arrays.asList(arrayV),
            Arrays.asList(arrayW),
            Arrays.asList(arrayX),
            Arrays.asList(arrayY),
            Arrays.asList(arrayZ)
        );
    }

    /**
     * 按 7 维顺序展开笛卡尔积，最靠前的维度位于最外层。
     *
     * @param listT 第 1 维取值，按列表顺序展开
     * @param listU 第 2 维取值，按列表顺序展开
     * @param listV 第 3 维取值，按列表顺序展开
     * @param listW 第 4 维取值，按列表顺序展开
     * @param listX 第 5 维取值，按列表顺序展开
     * @param listY 第 6 维取值，按列表顺序展开
     * @param listZ 第 7 维取值，按列表顺序展开
     * @return 按维度顺序依次产出 T、U、V、W、X、Y、Z 的所有组合
     */
    public static <T, U, V, W, X, Y, Z> Iterable<Objs7<T, U, V, W, X, Y, Z>> loop(
        List<T> listT,
        List<U> listU,
        List<V> listV,
        List<W> listW,
        List<X> listX,
        List<Y> listY,
        List<Z> listZ
    ) {
        int nt = listT.size();
        int nu = listU.size();
        int nv = listV.size();
        int nw = listW.size();
        int nx = listX.size();
        int ny = listY.size();
        int nz = listZ.size();
        if (nt == 0 || nu == 0 || nv == 0 || nw == 0 || nx == 0 || ny == 0 || nz == 0) return List.of();

        return () -> new AbstractIterator<>() {
            private int ti = 0;
            private int ui = 0;
            private int vi = 0;
            private int wi = 0;
            private int xi = 0;
            private int yi = 0;
            private int zi = 0;

            @Override
            protected @Nullable Objs7<T, U, V, W, X, Y, Z> computeNext() {
                if (this.ti == nt) return this.endOfData();

                Objs7<T, U, V, W, X, Y, Z> objs = new Objs7<>(
                    listT.get(this.ti),
                    listU.get(this.ui),
                    listV.get(this.vi),
                    listW.get(this.wi),
                    listX.get(this.xi),
                    listY.get(this.yi),
                    listZ.get(this.zi)
                );

                if (++this.zi < nz) return objs;
                this.zi = 0;
                if (++this.yi < ny) return objs;
                this.yi = 0;
                if (++this.xi < nx) return objs;
                this.xi = 0;
                if (++this.wi < nw) return objs;
                this.wi = 0;
                if (++this.vi < nv) return objs;
                this.vi = 0;
                if (++this.ui < nu) return objs;
                this.ui = 0;
                this.ti++;
                return objs;
            }
        };
    }

    /**
     * 2 维组合结果。
     *
     * @param t 第 1 维的取值
     * @param u 第 2 维的取值
     */
    public record Objs2<T, U>(T t, U u) {
    }

    /**
     * 3 维组合结果。
     *
     * @param t 第 1 维的取值
     * @param u 第 2 维的取值
     * @param v 第 3 维的取值
     */
    public record Objs3<T, U, V>(T t, U u, V v) {
    }

    /**
     * 4 维组合结果。
     *
     * @param t 第 1 维的取值
     * @param u 第 2 维的取值
     * @param v 第 3 维的取值
     * @param w 第 4 维的取值
     */
    public record Objs4<T, U, V, W>(T t, U u, V v, W w) {
    }

    /**
     * 5 维组合结果。
     *
     * @param t 第 1 维的取值
     * @param u 第 2 维的取值
     * @param v 第 3 维的取值
     * @param w 第 4 维的取值
     * @param x 第 5 维的取值
     */
    public record Objs5<T, U, V, W, X>(T t, U u, V v, W w, X x) {
    }

    /**
     * 6 维组合结果。
     *
     * @param t 第 1 维的取值
     * @param u 第 2 维的取值
     * @param v 第 3 维的取值
     * @param w 第 4 维的取值
     * @param x 第 5 维的取值
     * @param y 第 6 维的取值
     */
    public record Objs6<T, U, V, W, X, Y>(T t, U u, V v, W w, X x, Y y) {
    }

    /**
     * 7 维组合结果。
     *
     * @param t 第 1 维的取值
     * @param u 第 2 维的取值
     * @param v 第 3 维的取值
     * @param w 第 4 维的取值
     * @param x 第 5 维的取值
     * @param y 第 6 维的取值
     * @param z 第 7 维的取值
     */
    public record Objs7<T, U, V, W, X, Y, Z>(T t, U u, V v, W w, X x, Y y, Z z) {
    }
}
