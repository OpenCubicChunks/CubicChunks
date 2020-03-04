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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Hash table implementation for objects in a 3-dimensional cartesian coordinate
 * system.
 *
 * @param <T> class of the objects to be contained in this map
 *
 * @see XYZAddressable
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class XYZMap<T extends XYZAddressable> implements Iterable<T> {

    private static final Logger LOGGER = LogManager.getLogger("cubicchunks");

    private static final boolean CHECK_THREADED_WRITES = "true".equalsIgnoreCase(System.getProperty("cubicchunks.debug.checkThreadedXYZMapWrites"));

    /**
     * A larger prime number used as seed for hash calculation.
     */
    private static final int HASH_SEED = 1183822147;

    /**
     * Backing array containing all elements of this map, accessed by pointers.
     * This array contain no gaps with {@code null}, therefore it is possible to
     * iterate thru elements using simple index increment. Added to optimize map
     * iterator.
     */
    @Nonnull private XYZAddressable[] bucketsByPointer;
    /**
     * Backing array containing all elements of this map, accessed by hash.
     * Elements of this array accessed a same way as regular Java HashSet and
     * HashMap items, fastest way possible. It is used to optimize
     * {@code get(III)} function.
     */
    @Nonnull private XYZAddressable[] bucketsByHash;
    @Nonnull private int[] pointers;
    /**
     * the current number of elements in this map
     */
    private int size = 0;

    /**
     * the maximum permissible load of the backing array, after reaching it the
     * array will be resized
     */
    private float loadFactor;

    /**
     * the load threshold of the backing array, after reaching it the array will
     * be resized
     */
    private int loadThreshold;

    /**
     * binary mask used to wrap indices
     */
    private int mask;

    private final Thread debugStartThreadRef = Thread.currentThread();

    /**
     * Creates a new XYZMap with the given load factor and initial capacity. The
     * map will automatically grow if the specified load is surpassed.
     *
     * @param loadFactor the load factor
     * @param capacity the initial capacity
     */
    public XYZMap(float loadFactor, int capacity) {

        if (loadFactor > 1.0) {
            throw new IllegalArgumentException("You really dont want to be using a " + loadFactor + " load loadFactor with this hash table!");
        }

        this.loadFactor = loadFactor;

        int tCapacity = 1;
        while (tCapacity < capacity) {
            tCapacity <<= 1;
        }
        this.bucketsByPointer = new XYZAddressable[tCapacity];
        this.bucketsByHash = new XYZAddressable[tCapacity];
        this.pointers = new int[tCapacity];

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
     * @param y the y-coordinate
     * @param z the z-coordinate
     *
     * @return a 32b hash based on the given coordinates
     */
    private static int hash(int x, int y, int z) {
        int hash = HASH_SEED;
        hash += x;
        hash *= HASH_SEED;
        hash += y;
        hash *= HASH_SEED;
        hash += z;
        hash *= HASH_SEED;
        return hash;
    }

    /**
     * Computes the desired pointer index for the given coordinates, based on
     * the map's current capacity.
     *
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @param z the z-coordinate
     *
     * @return the desired pointer index for the given coordinates
     */
    private int getPointerIndex(int x, int y, int z) {
        return hash(x, y, z) & this.mask;
    }

    /**
     * Computes the next index to the right of the given index, wrapping around
     * if necessary.
     *
     * @param pointerIndex the previous index
     *
     * @return the next index
     */
    private int getNextPointerIndex(int pointerIndex) {
        return ++pointerIndex & this.mask;
    }

    /**
     * Removes all elements from the map.
     */
    public void clear() {
        checkThreadedWrite();
        Arrays.fill(this.bucketsByHash, null);
        Arrays.fill(this.bucketsByPointer, null);
        Arrays.fill(this.pointers, 0);
        this.size = 0;
    }

    /**
     * Associates the given value with its xyz-coordinates. If the map
     * previously contained a mapping for these coordinates, the old value is
     * replaced.
     *
     * @param value value to be associated with its coordinates
     *
     * @return the previous value associated with the given value's coordinates
     *         or null if no such value exists
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public T put(T value) {
        checkThreadedWrite();
        int x = value.getX();
        int y = value.getY();
        int z = value.getZ();
        int pointerIndex = this.getPointerIndex(x, y, z);
        int index = pointers[pointerIndex];

        while (index != 0) {
            XYZAddressable bucket = this.bucketsByPointer[index];
            if (bucket.getX() == x && bucket.getY() == y && bucket.getZ() == z) {
                this.bucketsByPointer[index] = value;
                this.bucketsByHash[pointerIndex] = value;
                return (T) bucket;
            }
            pointerIndex = this.getNextPointerIndex(pointerIndex);
            index = pointers[pointerIndex];
        }
        this.bucketsByPointer[++size] = value;
        this.bucketsByHash[pointerIndex] = value;
        this.pointers[pointerIndex] = size;

        // If the load threshold has been reached, increase the map's size.
        if (this.size > this.loadThreshold)
            grow();
        return null;
    }

    /**
     * Removes and returns the entry associated with the given coordinates.
     *
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @param z the z-coordinate
     *
     * @return the entry associated with the specified coordinates or null if no
     *         such entry exists
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public T remove(int x, int y, int z) {
        checkThreadedWrite();

        int pointerIndex = this.getPointerIndex(x, y, z);
        int index = pointers[pointerIndex];

        // Search for the element. Only the buckets from the element's supposed
        // index up to the next free slot must
        // be checked.
        while (index != 0) {
            XYZAddressable bucket = this.bucketsByPointer[index];
            // If the correct bucket was found, remove it.
            if (bucket.getX() == x && bucket.getY() == y && bucket.getZ() == z) {
                this.collapseBucket(pointerIndex, index);
                return (T) bucket;
            }
            pointerIndex = this.getNextPointerIndex(pointerIndex);
            index = pointers[pointerIndex];
        }

        // nothing was removed
        return null;
    }

    /**
     * Removes and returns the given value from this map. More specifically,
     * removes the entry whose xyz-coordinates equal the given value's
     * coordinates.
     *
     * @param value the value to be removed
     *
     * @return the entry associated with the given value's coordinates or null
     *         if no such entry exists
     */
    @Nullable
    public T remove(T value) {
        return this.remove(value.getX(), value.getY(), value.getZ());
    }

    /**
     * Returns the value associated with the given coordinates or null if no
     * such value exists.
     *
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @param z the z-coordinate
     *
     * @return the entry associated with the specified coordinates or null if no
     *         such value exists
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public T get(int x, int y, int z) {
        int index = this.getPointerIndex(x, y, z);
        XYZAddressable bucket = this.bucketsByHash[index];
        while (bucket != null) {

            // If the correct bucket was found, return it.
            if (bucket.getX() == x && bucket.getY() == y && bucket.getZ() == z) {
                return (T) bucket;
            }

            index = getNextPointerIndex(index);
            bucket = this.bucketsByHash[index];
        }

        // nothing was found
        return null;
    }

    /**
     * Returns true if there exists an entry associated with the given
     * xyz-coordinates in this map.
     *
     * @param x the x-coordinate
     * @param y the z-coordinate
     * @param z the y-coordinate
     *
     * @return true if there exists an entry associated with the given
     *         coordinates in this map
     */
    public boolean contains(int x, int y, int z) {
        int index = this.getPointerIndex(x, y, z);
        XYZAddressable bucket = this.bucketsByHash[index];
        while (bucket != null) {

            // If the correct bucket was found, return it.
            if (bucket.getX() == x && bucket.getY() == y && bucket.getZ() == z) {
                return true;
            }

            index = getNextPointerIndex(index);
            bucket = this.bucketsByHash[index];
        }

        // nothing was found
        return false;
    }

    /**
     * Returns true if the given value is contained within this map. More
     * specifically, returns true if there exists an entry in this map whose
     * xyz-coordinates equal the given value's coordinates.
     *
     * @param value the value
     *
     * @return true if the given value is contained within this map
     */
    public boolean contains(T value) {
        return this.contains(value.getX(), value.getY(), value.getZ());
    }

    /**
     * Doubles the size of the backing array and redistributes all contained
     * values accordingly.
     */
    private void grow() {
        int newLength = this.bucketsByPointer.length * 2;
        int newMask = newLength - 1;
        XYZAddressable[] newBucketsByPointer = new XYZAddressable[newLength];
        XYZAddressable[] newBucketsByHash = new XYZAddressable[newLength];
        int[] newPointers = new int[newLength];
        for (int i = 1; i <= size; i++) {
            XYZAddressable bucket = bucketsByPointer[i];
            newBucketsByPointer[i] = bucket;
            int pointerIndex = hash(bucket.getX(), bucket.getY(), bucket.getZ()) & newMask;
            while (newPointers[pointerIndex] != 0)
                pointerIndex = ++pointerIndex & newMask;
            newPointers[pointerIndex] = i;
            newBucketsByHash[pointerIndex] = bucket;
        }
        bucketsByPointer = newBucketsByPointer;
        bucketsByHash = newBucketsByHash;
        pointers = newPointers;
        mask=newMask;
        loadThreshold = (int) (newLength * this.loadFactor) - 2;
    }

    /**
     * Removes the value contained at the given index by shifting suitable
     * values on its right to the left.
     *
     * @param holePointerIndex the index of the ponter to be collapsed
     * @param holeIndex an index of the bucket to be collapsed
     */
    private void collapseBucket(final int holePointerIndex, final int holeIndex) {
        final int lastElement = size;
        final int oldLastPointerIndex = getElementPointerIndex(lastElement);

        List<XYZAddressable> nextPointersBuckets = new ArrayList<XYZAddressable>(10);
        List<Integer> nextBucketIndexes = new ArrayList<Integer>(10);

        this.pointers[oldLastPointerIndex] = holeIndex;
        this.pointers[holePointerIndex] = 0;
        this.bucketsByPointer[holeIndex] = this.bucketsByPointer[lastElement];
        this.bucketsByPointer[lastElement] = null;
        this.bucketsByHash[holePointerIndex] = null;
        this.size--;

        int pointerIndex = this.getNextPointerIndex(holePointerIndex);
        int index = pointers[pointerIndex];
        while (index != 0) {
            XYZAddressable bucket = this.bucketsByPointer[index];
            nextPointersBuckets.add(bucket);
            nextBucketIndexes.add(index);
            this.pointers[pointerIndex] = 0;
            this.bucketsByHash[pointerIndex] = null;
            pointerIndex = this.getNextPointerIndex(pointerIndex);
            index = pointers[pointerIndex];
        }

        for (int i = 0; i < nextPointersBuckets.size(); i++) {
            XYZAddressable bucket = nextPointersBuckets.get(i);
            int x = bucket.getX();
            int y = bucket.getY();
            int z = bucket.getZ();
            int newBucketPointerIndex = this.getPointerIndex(x, y, z);
            int newIndex = pointers[newBucketPointerIndex];
            while (newIndex != 0) {
                newBucketPointerIndex = this.getNextPointerIndex(newBucketPointerIndex);
                newIndex = pointers[newBucketPointerIndex];
            }
            this.pointers[newBucketPointerIndex] = nextBucketIndexes.get(i);
            this.bucketsByHash[newBucketPointerIndex] = bucket;
        }

    }

    private int getElementPointerIndex(int index) {
        XYZAddressable lastElement = this.bucketsByPointer[index];
        int pointerIndex = this.getPointerIndex(lastElement.getX(), lastElement.getY(), lastElement.getZ());
        while (pointers[pointerIndex] != index) {
            pointerIndex = this.getNextPointerIndex(pointerIndex);
        }
        return pointerIndex;
    }

    /**
     * Updates the load threshold and the index mask based on the backing
     * array's current size.
     */
    private void refreshFields() {
        // we need that 1 extra space, make sure it will be there
        this.loadThreshold = (int) (this.bucketsByPointer.length * this.loadFactor) - 2;
        this.mask = this.bucketsByPointer.length - 1;
    }

    private void checkThreadedWrite() {
        if (CHECK_THREADED_WRITES) {
            if (Thread.currentThread() != debugStartThreadRef) {
                LOGGER.error("Invalid threaded write access", new RuntimeException("Detected XYZ map write access from unexpected thread!"));
            }
        }
    }

    // Interface: Iterable<T>
    // ------------------------------------------------------------------------------------------

    public Iterator<T> iterator() {
        return new Iterator<T>() {

            int at = 1;

            @Override
            public boolean hasNext() {
                return at <= size;
            }

            @Nullable
            @Override
            @SuppressWarnings("unchecked")
            public T next() {
                return (T) bucketsByPointer[at++];
            }

            @Override
            public void remove() {
                checkThreadedWrite();
                int pointerIndex = getElementPointerIndex(--at);
                collapseBucket(pointerIndex, at);
            }
        };
    }

    /**
     * Return iterator over elements started from random position defined by
     * seed
     *
     * @param seed defines start position
     * @return An iterator that starts at randomized position based on seed
     **/
    public Iterator<T> randomWrappedIterator(int seed) {
        return new Iterator<T>() {

            // Start point: 1. Shall not be larger that array length
            // (obviously).
            // 2. Shall not, in most cases, be larger than allocated buckets
            // zone (because it must be uniformly random).
            // 3. Shall not be zero (because zero element always null in this
            // implementation).
            boolean start = size > 0;
            int startFrom = start ? (getNextPointerIndex(seed) % size | 1) : 0;
            int at = startFrom;

            @Override
            public boolean hasNext() {
                // 'at' equal to 'startFrom' allowed until first iteration.
                return at != startFrom || start;
            }

            @Override
            @SuppressWarnings("unchecked")
            public T next() {
                start = false;
                T toReturn = (T) bucketsByPointer[at++];
                if (at > size)
                    at = 1;
                return toReturn;
            }

            @Override
            public void remove() {
                checkThreadedWrite();
                int pointerIndex = getElementPointerIndex(--at);
                collapseBucket(pointerIndex, at);
            }
        };
    }
}
