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
package cubicchunks;

import static org.junit.Assert.*;

import io.github.opencubicchunks.cubicchunks.core.util.WatchersSortingList;
import mcp.MethodsReturnNonnullByDefault;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TestWatchersSortingList {

    List<Integer> added = new ArrayList<>();
    List<Integer> removed = new ArrayList<>();
    
    private void setup(){
        added.clear();
        removed.clear();
    }
    
    @Test
    public void testIterator() {
        this.setup();
        WatchersSortingList<Integer> list = new WatchersSortingList<>(Integer::compare);
        this.fillList(list);
        this.checkList(list);
    }

    @Test
    public void testSort() {
        this.setup();
        WatchersSortingList<Integer> list = new WatchersSortingList<>(Integer::compare);
        this.fillList(list);
        list.sort();
        this.checkList(list);
        Integer prev = null;
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (Integer e : list) {
            sb.append(e+",");
            if (prev != null)
                assertTrue(sb.toString()+"]",e.compareTo(prev) > 0);
            prev = e;
        }
    }

    private void fillList(WatchersSortingList<Integer> list) {
        Random random = new Random(42);
        for (int i=0;i<64;i++) {
            int e = random.nextInt(64);
            if(!list.contains(e)) {
                list.appendToEnd(e);
                added.add(e);
            }
        }
        for (int i=0;i<64;i++) {
            int e = random.nextInt(64);
            if(!list.contains(e)) {
                list.appendToStart(e);
                added.add(e);
            }
        }
        for (int i=0;i<64;i++) {
            int e = random.nextInt(64);
            if(list.contains(e)) {
                list.remove(e);
                removed.add(e);
                assertFalse(list.contains(e));
            }
        }
    }

    private void checkList(WatchersSortingList<Integer> list) {
        int i = 0;
        for (Integer e : list) {
            e.intValue();
        }
        for (Integer e : list) {
            i++;
            assertTrue(added.toString(),added.contains(e));
            assertTrue(list.toString() + removed.toString(),!removed.contains(e));
        }
        assertEquals(i, added.size() - removed.size());
    }
}
