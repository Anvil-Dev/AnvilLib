package dev.anvilcraft.lib.v2.util;

import com.google.common.collect.Multimap;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedList;
import java.util.SequencedCollection;
import java.util.function.Function;
import java.util.function.Predicate;

@UtilityClass
public final class CollectionUtil {
    public static <T> boolean allMatch(Collection<T> collection, Predicate<T> matcher) {
        for (T t : collection) {
            if (!matcher.test(t)) return false;
        }

        return true;
    }

    public static <T> boolean anyMatch(Collection<T> collection, Predicate<T> matcher) {
        for (T t : collection) {
            if (matcher.test(t)) return true;
        }

        return false;
    }

    public static <K, V, M extends Multimap<K, V>> M newMultimap(M emptyMap, Collection<V> values, Function<V, K> keyFactory) {
        for (V value : values) {
            emptyMap.put(keyFactory.apply(value), value);
        }
        return emptyMap;
    }

    public static <T> LinkedList<T> newLinkedList(int ignored) {
        return new LinkedList<>();
    }

    public static <T> @Nullable T get(SequencedCollection<T> collection, int index) {
        int i = 0;
        for (T t : collection) {
            if (i++ == index) return t;
        }
        return null;
    }
}
