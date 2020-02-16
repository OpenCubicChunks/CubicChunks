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
package io.github.opencubicchunks.cubicchunks.core.util.world;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import net.minecraft.world.NextTickListEntry;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * When saving chunks, Minecraft needs to filter all of the scheduled ticks by the chunk being saved.
 * Normally this is not a big issue, as there aren't a lot of scheduled ticks. But in some cases
 * (especially on fresh world in the first seconds) there are hundreds of thousands of scheduled ticks.
 *
 * For vanilla-sized chunks, this filtering is still acceptably fast (even if can still cause
 * noticeable delay when saving chunks). For 16x16x16 chunks, it can take minutes to save the
 * whole world.
 *
 * Instead of completely rewriting vanilla handling of scheduled ticks to store them in cubes
 * and also changing the behavior (vanilla "throttles" about of updates per tick), this class
 * implements a collection that keeps track of which entries belong to which cube. This also has the
 * benefit that scheduled ticks are still stored in the same fields, so existing code that
 * relies on it will just work.
 */
public class CubeSplitTickSet implements Set<NextTickListEntry> {

    private final Map<CubePos, NextTickListEntryHashSet> byCube = new HashMap<>();
    private final NextTickListEntryHashSet all = new NextTickListEntryHashSet();

    public Set<NextTickListEntry> getForCube(CubePos pos) {
        Set<NextTickListEntry> val = byCube.get(pos);
        return val == null ? Collections.emptySet() : val;
    }

    @Override public int size() {
        return all.size();
    }

    @Override public boolean isEmpty() {
        return all.isEmpty();
    }

    @Override public boolean contains(Object o) {
        return all.contains(o);
    }

    @SuppressWarnings("Duplicates") @Override public Iterator<NextTickListEntry> iterator() {
        return new Iterator<NextTickListEntry>() {
            private final Iterator<NextTickListEntry> it = all.iterator();
            private NextTickListEntry lastEntry = null;

            @Override public boolean hasNext() {
                return it.hasNext();
            }

            @Override public NextTickListEntry next() {
                return lastEntry = it.next();
            }

            @Override public void remove() {
                it.remove();
                removeByCube(lastEntry);
            }
        };
    }

    private void removeByCube(NextTickListEntry e) {
        CubePos pos = CubePos.fromBlockCoords(e.position);
        Set<NextTickListEntry> set = byCube.get(pos);
        set.remove(e);
        if (set.isEmpty()) {
            byCube.remove(pos);
        }
    }

    @Override public Object[] toArray() {
        return all.toArray();
    }

    @Override public <T> T[] toArray(T[] a) {
        return all.toArray(a);
    }

    @Override public boolean add(NextTickListEntry e) {
        boolean ret = all.add(e);
        byCube.computeIfAbsent(CubePos.fromBlockCoords(e.position), x -> new NextTickListEntryHashSet()).add(e);
        return ret;
    }

    @Override public boolean remove(Object o) {
        boolean ret = all.remove(o);
        if (ret) {
            removeByCube((NextTickListEntry) o);
        }
        return ret;
    }

    @Override public boolean containsAll(Collection<?> c) {
        return all.containsAll(c);
    }

    @Override public boolean addAll(Collection<? extends NextTickListEntry> c) {
        boolean ret = false;
        for (NextTickListEntry entry : c) {
            if (add(entry)) {
                ret = true;
            }
        }
        return ret;
    }

    @Override public boolean retainAll(Collection<?> c) {
        Iterator<NextTickListEntry> it = this.iterator();
        boolean changed = false;
        while (it.hasNext()) {
            if (!c.contains(it.next())) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    @Override public boolean removeAll(Collection<?> c) {
        boolean ret = false;
        for (Object entry : c) {
            if (remove(entry)) {
                ret = true;
            }
        }
        return ret;
    }

    @Override
    public void clear() {
        all.clear();
        byCube.clear();
    }

    // vanilla bug, see https://github.com/SleepyTrousers/EnderCore/issues/105
    // NextTickListEntry equals and compareTo are not consistent,
    // breaking HashMap when there are a lot of hash collisions
    // fix based on https://github.com/gnembon/carpetmod112/blob/a84ad2617ab3c2ca7b10b28264ba325f8adecd3f/patches/net/minecraft/world/NextTickListEntry.java.patch
    // thanks to Earthcomputer for bringing it up

    public static final class EqualsHashCodeWrapper<T extends Comparable<T>> implements Comparable<EqualsHashCodeWrapper<T>> {

        final T entry;

        public EqualsHashCodeWrapper(T entry) {
            this.entry = entry;
        }

        @Override
        public int hashCode() {
            return entry.hashCode();
        }

        @Override
        public boolean equals(Object entry) {
            if (!(entry instanceof EqualsHashCodeWrapper)) {
                return false;
            }
            return this.entry.equals(((EqualsHashCodeWrapper<?>) entry).entry);
        }

        @Override
        public int compareTo(EqualsHashCodeWrapper<T> other) {
            if (this.equals(other)) {
                return 0;
            }
            return this.entry.compareTo(other.entry);
        }
    }

    public static final class NextTickListEntryHashSet extends AbstractSet<NextTickListEntry> {

        private final Set<EqualsHashCodeWrapper<NextTickListEntry>> backingSet = new HashSet<>();

        @Override public Iterator<NextTickListEntry> iterator() {
            return new Iterator<NextTickListEntry>() {
                final Iterator<EqualsHashCodeWrapper<NextTickListEntry>> it = backingSet.iterator();
                @Override public boolean hasNext() {
                    return it.hasNext();
                }

                @Override public NextTickListEntry next() {
                    return it.next().entry;
                }
            };
        }

        @Override
        public int size() {
            return backingSet.size();
        }

        @Override
        public boolean contains(Object entry) {
            if (!(entry instanceof NextTickListEntry)) {
                return false;
            }
            return backingSet.contains(new EqualsHashCodeWrapper<>((NextTickListEntry) entry));
        }

        @Override
        public boolean add(NextTickListEntry entry) {
            return backingSet.add(new EqualsHashCodeWrapper<>(entry));
        }

        @Override
        public boolean remove(Object entry) {
            if (!(entry instanceof NextTickListEntry)) {
                return false;
            }
            return backingSet.remove(new EqualsHashCodeWrapper<>((NextTickListEntry) entry));
        }
    }
}
