/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2021 OpenCubicChunks
 *  Copyright (c) 2015-2021 contributors
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

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.XZAddressable;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

/**
 * Helper class to delay removing of elements. Created and used to reduce CPU
 * load during removing elements from this list on event of player movement as
 * replacement of {@code ArrayList}.
 */
@SuppressWarnings({"unchecked"})
public class WatchersSortingList2D<T extends BucketSorterEntry & XZAddressable> implements Iterable<T> {

    private static final int BUCKET_COUNT = (int) (CubicChunks.MAX_RENDER_DISTANCE * Math.sqrt(2)) + 1;

    private final ObjectArrayList<T>[] buckets = new ObjectArrayList[BUCKET_COUNT];
    private final int[] bucketSizes = new int[BUCKET_COUNT];

    private final int intrusiveCollectionId;
    private final Supplier<Collection<EntityPlayer>> playersSupplier;
    private int[] playerPositions = new int[0];
    private int distributingBucket = 0;

    public WatchersSortingList2D(int intrusiveCollectionId, Supplier<Collection<EntityPlayer>> playersSupplier) {
        this.intrusiveCollectionId = intrusiveCollectionId;
        this.playersSupplier = playersSupplier;
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = new ObjectArrayList<>();
        }
        updatePlayerPositions();
    }

    public void tick() {
        updatePlayerPositions();
        for (int i = 0, j = 0; i < 10 && j < BUCKET_COUNT; j++) {
            if (bucketSizes[distributingBucket] == 0) {
                distributingBucket++;
                if (distributingBucket >= BUCKET_COUNT) {
                    distributingBucket = 0;
                }
                continue;
            }
            redistributeBucket(distributingBucket++);
            if (distributingBucket >= BUCKET_COUNT) {
                distributingBucket = 0;
            }
            i++;
        }
    }

    private void updatePlayerPositions() {
        Collection<EntityPlayer> players = playersSupplier.get();
        int newSize = players.size() * 2;
        if (playerPositions.length != newSize) {
            playerPositions = new int[newSize];
        }
        int i = 0;
        for (EntityPlayer player : players) {
            playerPositions[i++] = Coords.blockToCube(player.posX);
            playerPositions[i++] = Coords.blockToCube(player.posZ);
        }
    }

    public void redistributeBucket(int bucket) {
        ObjectArrayList<T> list = buckets[bucket];
        for (int i = 0; i < list.size();) {
            T element = list.get(i);
            int newBucket = computeBucketIdx(element);
            if (newBucket == bucket) {
                i++;
                continue;
            }
            long oldElementNewData = 1 | ((long) newBucket << 1);
            bucketSizes[bucket]--;
            if (i != list.size() - 1) {
                T replacement = list.pop();
                list.set(i, replacement);
                long replacementElementData = replacement.getSorterStorage(intrusiveCollectionId) & 0x00000000FFFFFFFFL;
                replacement.setSorterStorage(intrusiveCollectionId, replacementElementData | ((long) i << 32));
            } else {
                list.pop();
            }
            ObjectArrayList<T> newList = buckets[newBucket];
            bucketSizes[newBucket]++;
            element.setSorterStorage(intrusiveCollectionId, oldElementNewData | ((long) newList.size() << 32));
            newList.add(element);
        }
    }

    /**
     * Check if list is empty. Even when it return true list still could contain
     * data, but it will be removed in attempt to access it via iterator.
     *
     * @return if list contain any accessible data
     */
    public boolean isEmpty() {
        return false; // TODO
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
        return iteratorUpToDistance(BUCKET_COUNT - 1);
    }

    public Iterator<T> iteratorUpToDistance(int maxDistance) {
        return new Iterator<T>() {
            int bucket = 0;
            int idx = 0;

            T next;

            private T peekNext() {
                if (next != null) {
                    return next;
                }
                if (bucket > maxDistance) {
                    return null;
                }
                while (idx >= bucketSizes[bucket]) {
                    bucket++;
                    idx = 0;
                    if (bucket > maxDistance) {
                        return null;
                    }
                }

                return next = buckets[bucket].get(idx++);
            }
            @Override
            public boolean hasNext() {
                return peekNext() != null;
            }

            @Override
            public T next() {
                T ret = peekNext();
                next = null;
                return ret;
            }

            @Override
            public void remove() {
                next = null;
                idx--;
                // TODO: this is wrong and can crash
                if (idx < 0) {
                    bucket--;
                    idx = bucketSizes[bucket] - 1;
                }
                WatchersSortingList2D.this.remove(buckets[bucket].get(idx));
                // if we removed the last element from this bucket, there is nothing to replace it with, so actually go to the next bucket
                if (idx >= bucketSizes[bucket]) {
                    bucket++;
                    idx = 0;
                }
            }
        };
    }

    public void remove(T entry) {
        long sorterStorage = entry.getSorterStorage(intrusiveCollectionId);
        if (sorterStorage == 0) {
            return;
        }
        entry.setSorterStorage(intrusiveCollectionId, 0);
        int bucket = ((int) sorterStorage) >> 1;
        bucketSizes[bucket]--;
        int index = (int) (sorterStorage >>> 32);
        ObjectArrayList<T> list = buckets[bucket];
        if (index == list.size() - 1) {
            list.pop();
            return;
        }
        // remove the last element, and put it in place of the old one we are removing
        // then fix up the index map to point to the new location
        T replacementElement = list.pop();
        list.set(index, replacementElement);
        long replacementData = replacementElement.getSorterStorage(intrusiveCollectionId) & 0x00000000FFFFFFFFL;
        replacementElement.setSorterStorage(intrusiveCollectionId, replacementData | ((long) index << 32));
    }


    /**
     * Remove such elements {@code a} whom return {@code true} on call
     * {@code predicate.test(a)} from that list immediately.
     *
     * @param predicate a predicate matching entries to remove
     */
    public void removeIf(Predicate<T> predicate) {
        // TODO: optimize
        for (Iterator<T> iterator = this.iterator(); iterator.hasNext(); ) {
            T t = iterator.next();
            if (predicate.test(t)) {
                iterator.remove();
            }
        }
    }

    public void add(T element) {
        if (this.contains(element)) {
            return; // already added
        }
        int bucket = computeBucketIdx(element);
        ObjectArrayList<T> list = buckets[bucket];
        bucketSizes[bucket]++;
        int indexInBucket = list.size();
        list.add(element);
        element.setSorterStorage(intrusiveCollectionId, (long) indexInBucket << 32 | (long) bucket << 1 | 1L);
    }

    private int computeBucketIdx(T element) {
        if (playerPositions.length == 0) {
            return BUCKET_COUNT - 1;
        }
        int x = element.getX();
        int z = element.getZ();

        int dx = x - playerPositions[0];
        int dz = z - playerPositions[1];
        int distSqMin = dx*dx + dz*dz;
        for (int i = 2; i < playerPositions.length; i += 2) {
            dx = x - playerPositions[i];
            dz = z - playerPositions[i+1];
            int distSq = dx*dx + dz*dz;
            if (distSq < distSqMin) {
                distSqMin = distSq;
            }
        }
        // fast very approximate square root
        int log2dist = 32 - Integer.numberOfLeadingZeros(distSqMin);
        int bitsToCutOff = log2dist >> 1;
        int approxDist = distSqMin >> bitsToCutOff;
        return Math.min(approxDist, BUCKET_COUNT - 1);
    }

    /**
     * @param element element to check
     * @return {@code true} if list contains element.
     */
    public boolean contains(T element) {
        return element.getSorterStorage(intrusiveCollectionId) != 0;
    }
}
