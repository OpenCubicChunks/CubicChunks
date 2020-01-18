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
package io.github.opencubicchunks.cubicchunks.api.util;

import mcp.MethodsReturnNonnullByDefault;

import java.util.Arrays;
import java.util.Iterator;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Hash table implementation for objects in a 2-dimensional cartesian coordinate system.
 *
 * @param <T> class of the objects to be contained in this map
 *
 * @see XZAddressable
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class XZMap<T extends XZAddressable> implements Iterable<T> {

    /**
     * A larger prime number used as seed for hash calculation.
     */
    private static final int HASH_SEED = 1183822147;


    /**
     * backing array containing all elements of this map
     */
    private XZAddressable[] buckets;

    /**
     * the current number of elements in this map
     */
    private int size;

    /**
     * the maximum permissible load of the backing array, after reaching it the array will be resized
     */
    private float loadFactor;

    /**
     * the load threshold of the backing array, after reaching it the array will be resized
     */
    private int loadThreshold;

    /**
     * binary mask used to wrap indices
     */
    private int mask;


    /**
     * Creates a new XZMap with the given load factor and initial capacity. The map will automatically grow if
     * the specified load is surpassed.
     *
     * @param loadFactor the load factor
     * @param capacity the initial capacity
     */
    public XZMap(float loadFactor, int capacity) {

        if (loadFactor > 1.0) {
            throw new IllegalArgumentException("You really dont want to be using a " + loadFactor + " load loadFactor with this hash table!");
        }

        this.loadFactor = loadFactor;

        int tCapacity = 1;
        while (tCapacity < capacity) {
            tCapacity <<= 1;
        }
        this.buckets = new XZAddressable[tCapacity];

        this.refreshFields();
    }


    /**
     * Returns the number of elements in this map
     *
     * @return the number of elements in this map
     */
    public int getSize() {
        return this.size;
    }


    /**
     * Computes a 32b hash based on the given coordinates.
     *
     * @param x the x-coordinate
     * @param z the z-coordinate
     *
     * @return a 32b hash based on the given coordinates
     */
    private static int hash(int x, int z) {
        int hash = HASH_SEED;
        hash += x;
        hash *= HASH_SEED;
        hash += z;
        hash *= HASH_SEED;
        return hash;
    }

    /**
     * Computes the desired bucket's index for the given coordinates, based on the map's current capacity.
     *
     * @param x the x-coordinate
     * @param z the z-coordinate
     *
     * @return the desired bucket's index for the given coordinates
     */
    private int getIndex(int x, int z) {
        return hash(x, z) & this.mask;
    }

    /**
     * Computes the next index to the right of the given index, wrapping around if necessary.
     *
     * @param index the previous index
     *
     * @return the next index
     */
    private int getNextIndex(int index) {
        return (index + 1) & this.mask;
    }

    /**
     * Removes all elements from the map.
     */
    public void clear() {
        Arrays.fill(this.buckets, null);
        this.size = 0;
    }

    /**
     * Associates the given value with its xz-coordinates. If the map previously contained a mapping for these
     * coordinates, the old value is replaced.
     *
     * @param value value to be associated with its coordinates
     *
     * @return the previous value associated with the given value's coordinates or null if no such value exists
     */
    @Nullable @SuppressWarnings("unchecked")
    public T put(T value) {

        int x = value.getX();
        int z = value.getZ();
        int index = getIndex(x, z);

        // find the closest empty space or the element to be replaced
        XZAddressable bucket = this.buckets[index];
        while (bucket != null) {

            // If there exists an element at the given element's position, overwrite it.
            if (bucket.getX() == x && bucket.getZ() == z) {
                this.buckets[index] = value;
                return (T) bucket;
            }

            index = getNextIndex(index);
            bucket = this.buckets[index];
        }

        // Insert the element into the empty bucket.
        this.buckets[index] = value;

        // If the load threshold has been reached, increase the map's size.
        ++this.size;
        if (this.size > this.loadThreshold) {
            grow();
        }

        return null;
    }

    /**
     * Removes and returns the entry associated with the given coordinates.
     *
     * @param x the x-coordinate
     * @param z the z-coordinate
     *
     * @return the entry associated with the specified coordinates or null if no such value exists
     */
    @Nullable @SuppressWarnings("unchecked")
    public T remove(int x, int z) {

        int index = getIndex(x, z);

        // Search for the element. Only the buckets from the element's supposed index up to the next free slot must
        // be checked.
        XZAddressable bucket = this.buckets[index];
        while (bucket != null) {

            // If the correct bucket was found, remove it.
            if (bucket.getX() == x && bucket.getZ() == z) {
                this.collapseBucket(index);
                return (T) bucket;
            }

            index = getNextIndex(index);
            bucket = this.buckets[index];
        }

        // nothing was removed
        return null;
    }

    /**
     * Removes and returns the given value from this map. More specifically, removes the entry whose xz-coordinates
     * equal the given value's coordinates.
     *
     * @param value the value to be removed
     *
     * @return the entry associated with the given value's coordinates or null if no such entry exists
     */
    @Nullable public T remove(T value) {
        return this.remove(value.getX(), value.getZ());
    }

    /**
     * Returns the value associated with the given coordinates or null if no such value exists.
     *
     * @param x the x-coordinate
     * @param z the z-coordinate
     *
     * @return the entry associated with the specified coordinates or null if no such value exists
     */
    @Nullable @SuppressWarnings("unchecked")
    public T get(int x, int z) {

        int index = getIndex(x, z);

        XZAddressable bucket = this.buckets[index];
        while (bucket != null) {

            // If the correct bucket was found, return it.
            if (bucket.getX() == x && bucket.getZ() == z) {
                return (T) bucket;
            }

            index = getNextIndex(index);
            bucket = this.buckets[index];
        }

        // nothing was found
        return null;
    }

    /**
     * Returns true if there exists an entry associated with the given xz-coordinates in this map.
     *
     * @param x the x-coordinate
     * @param z the y-coordinate
     *
     * @return true if there exists an entry associated with the given coordinates in this map
     */
    public boolean contains(int x, int z) {

        int index = getIndex(x, z);

        XZAddressable bucket = this.buckets[index];
        while (bucket != null) {

            // If the correct bucket was found, return true.
            if (bucket.getX() == x && bucket.getZ() == z) {
                return true;
            }

            index = getNextIndex(index);
            bucket = this.buckets[index];
        }

        // nothing was found
        return false;
    }

    /**
     * Returns true if the given value is contained within this map. More specifically, returns true if there exists
     * an entry in this map whose xz-coordinates equal the given value's coordinates.
     *
     * @param value the value
     *
     * @return true if the given value is contained within this map
     */
    public boolean contains(T value) {
        return this.contains(value.getX(), value.getZ());
    }

    /**
     * Doubles the size of the backing array and redistributes all contained values accordingly.
     */
    private void grow() {

        XZAddressable[] oldBuckets = this.buckets;

        // double the size!
        this.buckets = new XZAddressable[this.buckets.length * 2];
        this.refreshFields();

        // Move the old entries to the new array.
        for (XZAddressable oldBucket : oldBuckets) {

            // Skip empty buckets.
            if (oldBucket == null) {
                continue;
            }

            // Get the desired index of the old bucket and insert it into the first available slot.
            int index = getIndex(oldBucket.getX(), oldBucket.getZ());
            XZAddressable bucket = this.buckets[index];
            while (bucket != null) {
                bucket = this.buckets[index = getNextIndex(index)];
            }
            this.buckets[index] = oldBucket;
        }
    }

    /**
     * Removes the value contained at the given index by shifting suitable values on its right to the left.
     *
     * @param hole the index of the bucket to be collapsed
     */
    private void collapseBucket(int hole) {

        // This method must not be called on empty buckets.
        assert this.buckets[hole] != null;
        --this.size;

        int currentIndex = hole;
        while (true) {
            currentIndex = getNextIndex(currentIndex);

            // If there exists no element at the given index, there is nothing to fill the hole with.
            XZAddressable bucket = this.buckets[currentIndex];
            if (bucket == null) {
                this.buckets[hole] = null;
                return;
            }

            // If the hole lies to the left of the currentIndex and to the right of the targetIndex, move the current
            // element. These if conditions are necessary due to the bucket array wrapping around.
            int targetIndex = getIndex(bucket.getX(), bucket.getZ());

            // normal
            if (hole < currentIndex) {
                if (targetIndex <= hole || currentIndex < targetIndex) {
                    this.buckets[hole] = bucket;
                    hole = currentIndex;
                }
            }

            // wrap around!
            else {
                if (hole >= targetIndex && targetIndex > currentIndex) {
                    this.buckets[hole] = bucket;
                    hole = currentIndex;
                }
            }
        }
    }

    /**
     * Updates the load threshold and the index mask based on the backing array's current size.
     */
    private void refreshFields() {
        // we need that 1 extra space, make shore it will be there
        this.loadThreshold = Math.min(this.buckets.length - 1, (int) (this.buckets.length * this.loadFactor));
        this.mask = this.buckets.length - 1;
    }


    // Interface: Iterable<T> ------------------------------------------------------------------------------------------

    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int at = -1;
            int next = -1;

            @Override
            public boolean hasNext() {
                if (next > at) {
                    return true;
                }
                for (next++; next < buckets.length; next++) {
                    if (buckets[next] != null) {
                        return true;
                    }
                }
                return false;
            }

            @Nullable @Override
            @SuppressWarnings("unchecked")
            public T next() {
                if (next > at) {
                    at = next;
                    return (T) buckets[at];
                }
                for (next++; next < buckets.length; next++) {
                    if (buckets[next] != null) {
                        at = next;
                        return (T) buckets[at];
                    }
                }
                return null;
            }

            //TODO: WARNING: risk of iterating over the same item more than once if this is used
            //               do to items wrapping back around form the front of the buckets array
            @Override
            public void remove() {
                collapseBucket(at);
                next = at = at - 1; // There could be a new item in the removed bucket
            }
        };
    }
}
