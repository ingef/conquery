/*
 *                                     //
 * Copyright 2016 Karlis Zigurs (http://zigurs.com)
 *                                   //
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bakdata.conquery.util.search;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

import static java.lang.System.arraycopy;
import static java.util.Arrays.binarySearch;
import static java.util.Arrays.parallelSort;

/**
 * Immutable, array backed, {@link Set} of unique (as determined by  their
 * respective {@link Object#equals(Object)} method), non-{@code null} elements.
 * <p>
 * The rationale is to have as lightweight as possible memory profile and iteration cost
 * while retaining {@link Set} semantics - in this case at cost of modifying operations.
 * <p>
 * Use provided helper functions {@link ImmutableSet#createInstanceByAdding(Comparable)}
 * and {@link ImmutableSet#createInstanceByRemoving(Comparable)} to create a new
 * {@link ImmutableSet} instance after applying requested modification using
 * supplied set as a base (leaving the original {@link ImmutableSet} intact).
 * <p>
 * Calls to any modifying operations on an instance will throw an exception as per
 * {@link AbstractCollection} implementation.
 * <p>
 * This implementation does not permit {@code null} elements.
 * <p>
 * This implementation is thread-safe.
 *
 * @author Karlis Zigurs, 2016
 */
public final class ImmutableSet<T extends Comparable<? super T>> extends AbstractSet<T> {

    /** Reusable empty set instance. */
    private static final ImmutableSet EMPTY = new ImmutableSet<>(newArraySizeOf(0));

    /** Array with elements in this set. */
    private final T[] elements;

    /** Cached hashcode. */
    private int cachedHashCode; // = 0 by default

    private ImmutableSet(final T[] uniqueNonNullElements) {
        this(uniqueNonNullElements, false);
    }

    private ImmutableSet(final T[] uniqueNonNullElements,
                         final boolean alreadySorted) {
        Objects.requireNonNull(uniqueNonNullElements);

        /* Private constructor, so no need to worry
           about duplicating the input array */

        if (!alreadySorted)
            parallelSort(uniqueNonNullElements);

        elements = uniqueNonNullElements;
    }

    @Override
    public int size() {
        return elements.length;
    }

    @Override
    public boolean contains(final Object o) {
        return (o instanceof Comparable)
                && (binarySearch(elements, o) > -1);
    }

    @Override
    public Iterator<T> iterator() {
        return new ArrayIterator<>(elements);
    }

    @Override
    public void forEach(final Consumer<? super T> action) {
        Objects.requireNonNull(action);

        for (final T element : elements)
            action.accept(element);
    }

    /**
     * Follows {@link AbstractSet#hashCode()} semantics - hash code
     * is calculated from the elements in the set and will be equal
     * to other sets that contain the same (hash codes of) elements.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        if (cachedHashCode == 0) {
            int newHashCode = 0;

            for (final T element : elements)
                newHashCode += element.hashCode();

            cachedHashCode = newHashCode;
        }

        return cachedHashCode;
    }

    /**
     * Follows {@link AbstractSet#equals(Object)} semantics.
     *
     * @param o object to compare to
     *
     * @return {@code true} if compared to set consisting of equal elements
     */
    @Override
    public boolean equals(final Object o) {
        if (o == this)
            return true;

        if (!(o instanceof Set))
            return false;

        final Set<?> set = (Set<?>) o;

        return (set.size() == size())
                && containsAll(set);
    }

    @Override
    public Spliterator<T> spliterator() {
        return Spliterators.spliterator(elements,
                Spliterator.ORDERED
                        | Spliterator.DISTINCT
                        | Spliterator.SORTED
                        | Spliterator.SIZED
                        | Spliterator.SUBSIZED
                        | Spliterator.NONNULL
                        | Spliterator.IMMUTABLE
        );
    }

    /*
     * Extended functionality
     */

    /**
     * Create a new {@link ImmutableSet} instance by adding an item
     * to contents of this set. This set will remain unmodified.
     *
     * @param newElement element to add
     *
     * @return new instance with the element added or current instance if element was already present
     */
    public ImmutableSet<T> createInstanceByAdding(final T newElement) {
        Objects.requireNonNull(newElement);

        final int newElementIndex = binarySearch(elements, newElement);

        if (newElementIndex > -1) // already contains
            return this;

        /* newElementIndex becomes insertionPoint thanks to contract of binarySearch */
        final int insertionPoint = (-newElementIndex) - 1;

        final T[] newElements = newArraySizeOf(elements.length + 1);

        arraycopy(elements, 0, newElements, 0, insertionPoint);
        arraycopy(elements, insertionPoint,
                newElements, insertionPoint + 1, elements.length - insertionPoint);
        newElements[insertionPoint] = newElement;

        return new ImmutableSet<>(newElements, true);
    }

    /**
     * Create a new {@link ImmutableSet} instance by removing an element
     * from this set. This set will remain unmodified.
     *
     * @param elementToRemove element to remove
     *
     * @return new instance with element removed or current instance if specified item is not present
     */
    public ImmutableSet<T> createInstanceByRemoving(final T elementToRemove) {
        Objects.requireNonNull(elementToRemove);

        if (isEmpty())
            return this;

        final int existingItemIndex = binarySearch(elements, elementToRemove);

        if (existingItemIndex < 0) // not present
            return this;

        if (size() == 1) // present, but the only element? Ok.
            return emptySet();

        /*
         * Remove from a known position
         */

        final T[] newElements = newArraySizeOf(elements.length - 1);

        arraycopy(elements, 0, newElements, 0, existingItemIndex);
        arraycopy(elements, existingItemIndex + 1,
                newElements, existingItemIndex, newElements.length - existingItemIndex);

        return new ImmutableSet<>(newElements, true);
    }

    /*
     * Helpers
     */

    /**
     * Empty set of given type.
     *
     * @param <T> type
     *
     * @return empty set
     */
    public static <T extends Comparable<? super T>> ImmutableSet<T> emptySet() {
        return (ImmutableSet<T>) EMPTY;
    }

    /**
     * Set of single element.
     *
     * @param element element to wrap
     * @param <T>     type
     *
     * @return set containing single element
     */
    public static <T extends Comparable<? super T>> ImmutableSet<T> singletonSet(final T element) {
        Objects.requireNonNull(element);

        final T[] newArray = newArraySizeOf(1);
        newArray[0] = element;

        return new ImmutableSet<>(newArray);
    }

    /**
     * Create a {@link ImmutableSet} of unique, non-{@code null} elements
     * from the specified {@link Collection}.
     *
     * @param source source collection
     * @param <T>    type
     *
     * @return immutable set of unique, non-null elements
     */
    public static <T extends Comparable<? super T>> ImmutableSet<T> fromCollection(final Collection<? extends T> source) {
        Objects.requireNonNull(source);

        if (source.isEmpty())
            return emptySet();

        if (source instanceof ImmutableSet)
            return (ImmutableSet<T>) source;

        if (source instanceof Set) {
            final Set set = (Set) source;
            set.remove(null);
            return new ImmutableSet<>(arrayFromCollection(set));
        }

        /* and brute force fallback */
        final Set<T> set = new HashSet<>(source.size());

        set.addAll(source);
        set.remove(null);

        return new ImmutableSet<>(arrayFromCollection(set));
    }

    /**
     * Create an {@link ImmutableSet} instance consisting of union of two {@link Collection}s.
     *
     * @param left  left source collection
     * @param right right source collection
     * @param <T>   type
     *
     * @return set of all unique, non-null elements from both collections
     */
    public static <T extends Comparable<? super T>> ImmutableSet<T> fromCollections(final Collection<? extends T> left,
                                                                                    final Collection<? extends T> right) {
        Objects.requireNonNull(left);
        Objects.requireNonNull(right);

        if (left.isEmpty())
            return fromCollection(right);

        if (right.isEmpty())
            return fromCollection(left);

        /* brute force it is */
        final Set<T> set = new HashSet<>(left.size() + right.size());

        set.addAll(left);
        set.addAll(right);
        set.remove(null);

        return new ImmutableSet<>(arrayFromCollection(set));
    }

    /** Helper to consolidate toArray(T[]) calls. */
    private static <T extends Comparable<? super T>> T[] arrayFromCollection(final Collection<? extends T> collection) {
        return collection.toArray(newArraySizeOf(collection.size()));
    }

    /**
     * Helper to supply new arrays of erasure type from a single location.
     */
    private static <T extends Comparable<? super T>> T[] newArraySizeOf(final int size) {
        return (T[]) new Comparable[size];
    }

    /**
     * Simple array iterator.
     *
     * @param <T> type of array elements
     */
    private static final class ArrayIterator<T> implements Iterator<T> {

        private final T[] array;
        private int index;

        ArrayIterator(final T[] elements) {
            Objects.requireNonNull(elements);
            array = elements;
        }

        @Override
        public boolean hasNext() {
            return index < array.length;
        }

        @Override
        public T next() {
            if (index >= array.length)
                throw new NoSuchElementException("all items already retrieved");

            return array[index++];
        }
    }
}
