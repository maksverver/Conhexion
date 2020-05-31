package ch.verver.chilab;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.RandomAccess;

/** An immutable list */
public final class ImmutableList<T> extends AbstractList<T> implements List<T>, RandomAccess {

    private static ImmutableList<Object> emptyInstance = new ImmutableList<>(new Object[0]);

    private final Object[] elements;

    @SuppressWarnings("unchecked")
    public static <T> ImmutableList<T> empty() {
        return (ImmutableList<T>) emptyInstance;
    }

    /** Returns an immutable list with the elements passed as arguments. */
    @SafeVarargs
    public static <T> ImmutableList<T> of(T... elements) {
        // Could make this more efficient for small number of arguments by providing overloads which
        // take 0, 1, 2, etc. arguments.
        return new ImmutableList<>(elements.clone());
    }

    /** Returns an immutable list that contains the same values as the given collection. */
    @SuppressWarnings("unchecked")
    public static <T> ImmutableList<T> copyOf(Collection<T> collection) {
        // Could make this more efficient by using a for-loop to extract elements from lists that
        // implement RandomAccess. Could make this more flexible by accepting Iterable instead.
        if (collection instanceof ImmutableList) {
            return (ImmutableList<T>) collection;
        }
        return new ImmutableList<>(collection.toArray());
    }

    // Provided for consistency, though ImmutableList.of(elements) would also work.
    public static <T> ImmutableList<T> copyOf(T[] elements) {
        return new ImmutableList<>(elements.clone());
    }

    public static <T> ImmutableList<T> copyOf(T[] elements, int newLength) {
        return new ImmutableList<>(Arrays.copyOf(elements, newLength));
    }

    private ImmutableList(Object[] values) {
        this.elements = values;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(int index) {
        return (T) elements[index];
    }

    @Override
    public int size() {
        return elements.length;
    }
}
