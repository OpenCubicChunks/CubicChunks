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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Hash table implementation for objects in a 3-dimensional cartesian coordinate
 * system.
 *
 * @param <T> class of the objects to be contained in this map
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
     * Backing array containing all elements of this map, accessed by hash.
     * Elements of this array accessed a same way as regular Java HashSet and
     * HashMap items, fastest way possible. It is used to optimize
     * {@code get(III)} function.
     */
    @Nonnull
    private Node<T>[] bucketsByHash;
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
     * @param capacity   the initial capacity
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
        this.bucketsByHash = this.nodeArray(tCapacity);

        this.refreshFields();
    }

    @SuppressWarnings("unchecked")
    private Node<T>[] nodeArray(int size) {
        return (Node<T>[]) new Node[size];
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
     * @return the next index
     */
    private int getNextPointerIndex(int pointerIndex) {
        return ++pointerIndex & this.mask;
    }

    /**
     * Removes all elements from the map.
     */
    public void clear() {
        this.checkThreadedWrite();
        Arrays.fill(this.bucketsByHash, null);
        this.size = 0;
    }

    /**
     * Associates the given value with its xyz-coordinates. If the map
     * previously contained a mapping for these coordinates, the old value is
     * replaced.
     *
     * @param value value to be associated with its coordinates
     * @return the previous value associated with the given value's coordinates
     * or null if no such value exists
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public T put(T value) {
        this.checkThreadedWrite();
        int x = value.getX();
        int y = value.getY();
        int z = value.getZ();
        int hash = hash(x, y, z);
        Node<T> node = this.bucketsByHash[hash & this.mask];
        if (node != null) {
            Node prev;
            do {
                if (node.hash == hash && node.x == x && node.y == y && node.z == z) {
                    //value already existed
                    T old = node.value;
                    node.value = value;
                    return old;
                }
                prev = node;
                node = node.next;
            } while (node != null);

            //node wasn't there before
            node = new Node<>(hash, x, y, z, value);
            prev.next = node;
            node.prev = prev;
        } else {
            this.bucketsByHash[hash & this.mask] = new Node<>(hash, x, y, z, value);
        }

        if (++this.size > this.loadThreshold) {
            this.grow();
        }
        return null;
    }

    /**
     * Removes and returns the entry associated with the given coordinates.
     *
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @param z the z-coordinate
     * @return the entry associated with the specified coordinates or null if no
     * such entry exists
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public T remove(int x, int y, int z) {
        this.checkThreadedWrite();

        int hash = hash(x, y, z);
        Node<T> node = this.bucketsByHash[hash & this.mask];
        while (node != null) {
            if (node.hash == hash && node.x == x && node.y == y && node.z == z) {
                this.size--;
                if (node.next != null) {
                    node.next.prev = node.prev;
                }
                if (node.prev != null) {
                    node.prev.next = node.next;
                } else {
                    //if the previous node is null, this node is a root node
                    this.bucketsByHash[hash & this.mask] = node.next;
                }
                return node.value;
            }
            node = node.next;
        }
        return null;
    }

    /**
     * Removes and returns the given value from this map. More specifically,
     * removes the entry whose xyz-coordinates equal the given value's
     * coordinates.
     *
     * @param value the value to be removed
     * @return the entry associated with the given value's coordinates or null
     * if no such entry exists
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
     * @return the entry associated with the specified coordinates or null if no
     * such value exists
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public T get(int x, int y, int z) {
        int hash = hash(x, y, z);
        Node<T> node = this.bucketsByHash[hash & this.mask];
        while (node != null) {
            if (node.hash == hash && node.x == x && node.y == y && node.z == z) {
                return node.value;
            }
            node = node.next;
        }
        return null;
    }

    /**
     * Returns true if there exists an entry associated with the given
     * xyz-coordinates in this map.
     *
     * @param x the x-coordinate
     * @param y the z-coordinate
     * @param z the y-coordinate
     * @return true if there exists an entry associated with the given
     * coordinates in this map
     */
    public boolean contains(int x, int y, int z) {
        int hash = hash(x, y, z);
        Node<T> node = this.bucketsByHash[hash & this.mask];
        while (node != null) {
            if (node.hash == hash && node.x == x && node.y == y && node.z == z) {
                return true;
            }
            node = node.next;
        }
        return false;
    }

    /**
     * Returns true if the given value is contained within this map. More
     * specifically, returns true if there exists an entry in this map whose
     * xyz-coordinates equal the given value's coordinates.
     *
     * @param value the value
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
        int newLength = this.bucketsByHash.length << 1;
        int newMask = newLength - 1;
        Node<T>[] newBucketsByHash = this.nodeArray(newLength);
        for (Node<T> node : this.bucketsByHash) {
            while (node != null) {
                Node<T> inTable = newBucketsByHash[node.hash & newMask];
                newBucketsByHash[node.hash & newMask] = node;
                Node<T> oldNext = node.next;
                node.prev = node.next = null;

                if (inTable != null) {
                    //hash collision
                    inTable.prev = node;
                    node.next = inTable;
                }
                node = oldNext;
            }
        }
        this.bucketsByHash = newBucketsByHash;
        this.mask = newMask;
        this.loadThreshold = (int) (newLength * this.loadFactor) - 2;
    }

    /**
     * Updates the load threshold and the index mask based on the backing
     * array's current size.
     */
    private void refreshFields() {
        // we need that 1 extra space, make sure it will be there
        this.loadThreshold = (int) (this.bucketsByHash.length * this.loadFactor) - 2;
        this.mask = this.bucketsByHash.length - 1;
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
            private int index = 0;
            private Node<T> node;
            private Node<T> next = this.seek();

            @Override
            public boolean hasNext() {
                return this.next != null;
            }

            @Override
            public T next() {
                if (!this.hasNext())    {
                    throw new NoSuchElementException();
                }
                this.node = this.next;
                this.next = this.seek();
                return this.node.value;
            }

            private Node<T> seek() {
                if (this.node != null && this.node.next != null) {
                    return this.node.next;
                }

                while (this.index < XYZMap.this.bucketsByHash.length) {
                    Node<T> node = XYZMap.this.bucketsByHash[this.index++];
                    if (node != null) {
                        return node;
                    }
                }
                return null;
            }

            @Override
            public void remove() {
                XYZMap.this.checkThreadedWrite();
                if (this.node.next != null) {
                    this.node.next.prev = this.node.prev;
                }
                if (this.node.prev != null) {
                    this.node.prev.next = this.node.next;
                } else {
                    XYZMap.this.bucketsByHash[this.node.hash & XYZMap.this.mask] = this.node.next;
                }
                XYZMap.this.size--;
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
        throw new UnsupportedOperationException();
    }

    private static final class Node<T extends XYZAddressable> {
        private final int hash;
        private final int x;
        private final int y;
        private final int z;
        private T value;

        private Node<T> next;
        private Node<T> prev;

        public Node(int hash, int x, int y, int z, T value) {
            this.hash = hash;
            this.x = x;
            this.y = y;
            this.z = z;
            this.value = value;
        }
    }
}
