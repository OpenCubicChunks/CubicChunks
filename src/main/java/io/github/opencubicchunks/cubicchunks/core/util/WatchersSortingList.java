/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package io.github.opencubicchunks.cubicchunks.core.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

/**
 * Helper class to delay removing of an elements. Created and used to reduce CPU
 * load during removing elements from this list on event of player movement as
 * replacement of {@code ArrayList}.
 */
@SuppressWarnings({"unchecked"})
public class WatchersSortingList<T> implements Iterable<T> {

    /** Order in which list should be sorted during tick event */
    private final Comparator<T> order;

    /** Contain all data in sorting order */
    private Object[] data = new Object[32768];
    /**
     * First used element of a data array. Leave some unused space to append
     * elements with high priority.
     */
    private int start = data.length / 4;
    /**
     * Size of a list. Used to limit accessible members of data array during
     * sorting and iteration.
     */
    private int size = 0, removed = 0;
    /**
     * Contain all data without certain order. Used to detect if element is
     * already added
     */
    private final Object2IntMap<T> indexMap = new Object2IntOpenHashMap<>();

    public WatchersSortingList(Comparator<T> orderIn) {
        indexMap.defaultReturnValue(-1);
        // With this trick we can drop removed elements to the end of a list.
        order = (o1, o2) -> {
            boolean o1Removed = o1 == null;
            boolean o2Removed = o2 == null;
            if(o1Removed && o2Removed)
                return 0;
            if(o1Removed)
                return Integer.MAX_VALUE;
            if(o2Removed)
                return Integer.MIN_VALUE;
            return orderIn.compare(o1, o2);
        };
    }

    /** Sort and remove dead elements */
    public void sort() {
        Arrays.sort((T[]) data, start, start + size, order);
        int newSize = Integer.MIN_VALUE;
        for (int i = start; i <= start + size; i++) {
            if (data[i] == null) {
                newSize = i - start;
                break;
            }
            indexMap.put((T) data[i], i);
        }
        assert newSize != Integer.MIN_VALUE;
        size = newSize;
        removed = 0;
    }

    /**
     * Check if list is empty. Even when it return true list still could contain
     * data, but it will be removed in attempt to access it via iterator.
     * 
     * @return if list contain any accessible data
     */
    public boolean isEmpty() {
        return size - removed == 0;
    }

    /**
     * Return iterator over elements of list. If iterator meet any element which
     * is scheduled to be removed by {@code remove(T element)} function, it will
     * be removed from data.
     * 
     * @return iterator over elements
     */
    @Nonnull @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {

            int i = start;
            T prev = null;
            T next = null;

            private void peekNext() {
                while (next == null && i < start + size) {
                    T e = (T) data[i++];
                    if (e != null) {
                        next = e;
                    }
                }
            }

            @Override
            public boolean hasNext() {
                peekNext();
                return next != null;
            }

            @Override
            public T next() {
                peekNext();
                if (next == null)
                    throw new ArrayIndexOutOfBoundsException();
                prev = next;
                next = null;
                return prev;
            }

            @Override
            public void remove() {
                WatchersSortingList.this.remove(prev);
            }
        };
    }

    /**
     * Schedule element to be removed if it is contained in list.
     *
     * @param entry entry to remove
     */
    public void remove(T entry) {
        int idx = indexMap.removeInt(entry);
        if (idx >= 0) {
            data[idx] = null;
            removed++;
        }
    }

    /**
     * Remove such elements {@code a} whom return {@code true} on call
     * {@code predicate.test(a)} from that list immediately.
     *
     * @param predicate a predicate matching entries to remove
     */
    public void removeIf(Predicate<T> predicate) {
        for (int i = start; i < start + size; i++) {
            T a = (T) data[i];
            if (a == null) {
                continue;
            }
            if (predicate.test(a)) {
                indexMap.remove(a);
                data[i] = null;
                removed++;
            }
        }
    }

    /**
     * Append element to start of a list.
     *
     * @param element element to add
     * @throws NullPointerException in attempt to add {@code null}.
     * @throws IllegalArgumentException if list already contain such element.
     */
    public void appendToStart(T element) {
        if (element == null)
            throw new NullPointerException("This list does not allow null elements.");
        if (start <= 0)
            grow();
        --start;
        data[start] = element;
        indexMap.put(element, start);
        size++;
    }

    /**
     * Add element to an end of list.
     *
     * @param element element to add
     * @throws NullPointerException in attempt to add {@code null}.
     * @throws IllegalArgumentException if list already contain such element.
     */
    public void appendToEnd(T element) {
        if (element == null)
            throw new NullPointerException("This list does not allow null elements.");
        if (start + size >= this.data.length)
            grow();
        data[start + size] = element;
        indexMap.put(element, start + size);
        size++;
    }
    
    /**
     * @param element element to check
     * @return {@code true} if list contains element.
     */
    public boolean contains(T element) {
        return indexMap.containsKey(element);
    }

    private void grow() {
        Object[] newData = new Object[data.length * 2];
        int newStart = newData.length / 4;
        System.arraycopy(data, start, newData, newStart, size);
        data = newData;
        start = newStart;
        for (int i = start; i < start + size; i++) {
            if (data[i] != null) {
                indexMap.put((T) data[i], i);
            }
        }
    }
    
    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Object datum : data) {
            sb.append(datum).append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
