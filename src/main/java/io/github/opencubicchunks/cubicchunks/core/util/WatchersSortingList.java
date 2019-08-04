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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

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
    private int size = 0;
    /**
     * Contain all data without certain order. Used to detect if element is
     * already added
     */
    private final Set<T> dataAsSet = new HashSet<T>();
    /** Contain elements which will be removed during tick event */
    private final Set<T> toRemove = new HashSet<T>();

    public WatchersSortingList(Comparator<T> orderIn) {
        order = new Comparator<T>() {

            // With this trick we can drop removed elements to the end of a list.
            @Override
            public int compare(T o1, T o2) {
                boolean o1Removed = toRemove.contains(o1);
                boolean o2Removed = toRemove.contains(o2);
                if(o1Removed && o2Removed)
                    return 0;
                if(o1Removed)
                    return Integer.MAX_VALUE;
                if(o2Removed)
                    return Integer.MIN_VALUE;
                return orderIn.compare(o1, o2);
            }};
    }

    /** Sort and remove dead elements */
    public void sort() {
        Arrays.sort((T[]) data, start, start + size, order);
        size-=toRemove.size();
        dataAsSet.removeAll(toRemove);
        toRemove.clear();
    }

    /**
     * Check if list is empty. Even when it return true list still could contain
     * data, but it will be removed in attempt to access it via iterator.
     * 
     * @return if list contain any accessible data
     */
    public boolean isEmpty() {
        int amount = size - toRemove.size();
        assert (amount >= 0);
        return amount == 0;
    }

    /**
     * Return iterator over elements of list. If iterator meet any element which
     * is scheduled to be removed by {@code remove(T element)} function, it will
     * be removed from data.
     * 
     * @return iterator over elements
     */
    public Iterator<T> iterator() {
        return new Iterator<T>() {

            int i = start;
            T prev = null;
            T next = null;

            private void peekNext() {
                while (next == null && i < start + size) {
                    T e = (T) data[i++];
                    if (!toRemove.contains(e)) {
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
                toRemove.add(prev);
            }
        };
    }

    /**
     * Schedule element to be removed if it is contained in list.
     */
    public void remove(T entry) {
        if(dataAsSet.contains(entry))
            toRemove.add(entry);
    }

    /**
     * Remove such elements {@code a} whom return {@code true} on call
     * {@code predicate.test(a)} from that list immediately.
     */
    public void removeIf(Predicate<T> predicate) {
        for (int i = start; i < start + size; i++) {
            T a = (T) data[i];
            if (predicate.test(a)) {
                toRemove.add(a);
            }
        }
    }

    /**
     * Append element to start of a list.
     * 
     * @throws NullPointerException in attempt to add {@code null}.
     * @throws IllegalArgumentException if list already contain such element.
     */
    public void appendToStart(T element) {
        if (element == null)
            throw new NullPointerException("This list does not allow null elements.");
        // A rare case when we should use &. We need to remove element from list
        // of a dead AND we need to add it to data set and check both sets before throwing exception.
        if(!toRemove.remove(element) & !dataAsSet.add(element))
            throw new IllegalArgumentException("List already contain element " + element.toString());
        if (start <= 0)
            grow();
        --start;
        data[start] = element;
        size++;
    }

    /**
     * Add element to an end of list.
     * 
     * @throws NullPointerException in attempt to add {@code null}.
     * @throws IllegalArgumentException if list already contain such element.
     */
    public void appendToEnd(T element) {
        if (element == null)
            throw new NullPointerException("This list does not allow null elements.");
        if(!toRemove.remove(element) & !dataAsSet.add(element))
            throw new IllegalArgumentException("List already contain element " + element.toString());
        if (start + size >= this.data.length)
            grow();
        data[start + size] = element;
        size++;
    }
    
    /** @return {@code true} if list contains element. */
    public boolean contains(T element) {
        return dataAsSet.contains(element) && !toRemove.contains(element);
    }

    private void grow() {
        Object[] newData = new Object[data.length * 2];
        int newStart = newData.length / 4;
        System.arraycopy(data, start, newData, newStart, size);
        data = newData;
        start = newStart;
    }
    
    @Override
    public String toString(){
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (int i = 0; i < data.length; i++) {
            sb.append(data[i]+",");
        }
        sb.append("]");
        return sb.toString();
    }
}
