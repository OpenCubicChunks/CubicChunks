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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * See {@link io.github.opencubicchunks.cubicchunks.core.util.world.CubeSplitTickSet}
 */
public class CubeSplitTickList extends AbstractList<NextTickListEntry> {

    // byCube Lists are not ordered, it's only a list to allow the unlikely case of duplicates
    private final Map<CubePos, List<NextTickListEntry>> byCube = new HashMap<>();
    private final List<NextTickListEntry> all = new ArrayList<>();

    public List<NextTickListEntry> getForCube(CubePos pos) {
        List<NextTickListEntry> val = byCube.get(pos);
        return val == null ? Collections.emptyList() : val;
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
        List<NextTickListEntry> list = byCube.get(pos);
        list.remove(e);
        if (list.isEmpty()) {
            byCube.remove(pos);
        }
    }

    private void addByCube(NextTickListEntry e) {
        byCube.computeIfAbsent(CubePos.fromBlockCoords(e.position), x -> new ArrayList<>()).add(e);
    }

    @Override public Object[] toArray() {
        return all.toArray();
    }

    @Override public <T> T[] toArray(T[] a) {
        return all.toArray(a);
    }

    @Override public boolean add(NextTickListEntry e) {
        all.add(e);
        addByCube(e);
        return true;
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

    @Override public void clear() {
        all.clear();
        byCube.clear();
    }

    @Override public NextTickListEntry get(int index) {
        return all.get(index);
    }

    @Override public NextTickListEntry set(int index, NextTickListEntry e) {
        NextTickListEntry old = all.set(index, e);
        removeByCube(old);
        addByCube(e);
        return null;
    }

    @Override public void add(int index, NextTickListEntry element) {
        all.add(index, element);
        addByCube(element);
    }

    @Override public NextTickListEntry remove(int index) {
        NextTickListEntry old = all.remove(index);
        removeByCube(old);
        return old;
    }

    @Override public int indexOf(Object o) {
        return all.indexOf(o);
    }

    @Override public int lastIndexOf(Object o) {
        return all.lastIndexOf(o);
    }
}
