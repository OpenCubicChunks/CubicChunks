/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2018 @PhiPro95 and @Mathe172
 *  Copyright (c) 2021 @jellysquid_
 *  Copyright (c) 2021 OpenCubicChunks
 *  Copyright (c) 2021 contributors
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
package io.github.opencubicchunks.cubicchunks.core.lighting.phosphor;

import java.util.ArrayDeque;
import java.util.Deque;

//Implement own queue with pooled segments to reduce allocation costs and reduce idle memory footprint
public class PooledLightUpdateQueue {

    private static final int CACHED_QUEUE_SEGMENTS_COUNT = 1 << 12; // 4096
    private static final int QUEUE_SEGMENT_SIZE = 1 << 10; // 1024

    private final Pool pool;

    private Segment cur, last;

    private int size = 0;

    // Stores whether or not the queue is empty. Updates to this field will be seen by all threads immediately. Writes
    // to volatile fields are generally quite a bit more expensive, so we avoid repeatedly setting this flag to true.
    private volatile boolean empty;

    public PooledLightUpdateQueue(Pool pool) {
        this.pool = pool;
    }

    /**
     * Not thread-safe! If you must know whether or not the queue is empty, please use {@link PooledLightUpdateQueue#isEmpty()}.
     *
     * @return The number of encoded values present in this queue
     */
    public int size() {
        return this.size;
    }

    /**
     * Thread-safe method to check whether or not this queue has work to do. Significantly cheaper than acquiring a lock.
     * @return True if the queue is empty, otherwise false
     */
    public boolean isEmpty() {
        return this.empty;
    }

    /**
     * Not thread-safe! Adds an encoded long value into this queue.
     * @param x x coordinate
     * @param y y coordinate
     * @param z z coordinate
     * @param val The encoded value to add
     */
    public void add(final int x, final int y, final int z, final int val) {
        if (this.cur == null) {
            this.empty = false;
            this.cur = this.last = this.pool.acquire();
        }

        if (this.last.index == QUEUE_SEGMENT_SIZE) {
            Segment ret = this.last.next = this.last.pool.acquire();
            int i = ret.index++ << 2;
            ret.dataArray[i] = x;
            ret.dataArray[i | 1] = y;
            ret.dataArray[i | 2] = z;
            ret.dataArray[i | 3] = val;

            this.last = ret;
        } else {
            int i = this.last.index++ << 2;
            this.last.dataArray[i] = x;
            this.last.dataArray[i | 1] = y;
            this.last.dataArray[i | 2] = z;
            this.last.dataArray[i | 3] = val;
        }

        ++this.size;
    }

    /**
     * Not thread safe! Creates an iterator over the values in this queue. Values will be returned in a FIFO fashion.
     * @return The iterator
     */
    public LightUpdateQueueIterator iterator() {
        return new LightUpdateQueueIterator(this.cur);
    }

    private void clear() {
        Segment segment = this.cur;

        while (segment != null) {
            Segment next = segment.next;
            segment.release();
            segment = next;
        }

        this.size = 0;
        this.cur = null;
        this.last = null;
        this.empty = true;
    }

    public class LightUpdateQueueIterator {

        private Segment cur;
        private int[] curArray;

        private int index, capacity;

        private LightUpdateQueueIterator(Segment cur) {
            this.cur = cur;

            if (this.cur != null) {
                this.curArray = cur.dataArray;
                this.capacity = cur.index;
            }
        }

        public boolean hasNext() {
            return this.cur != null;
        }

        public int x() {
            return this.curArray[this.index << 2];
        }

        public int y() {
            return this.curArray[this.index << 2 | 1];
        }

        public int z() {
            return this.curArray[this.index << 2 | 2];
        }

        public int val() {
            return this.curArray[this.index << 2 | 3];
        }

        public void next() {
            this.index++;

            if (this.index == this.capacity) {
                this.index = 0;

                this.cur = this.cur.next;

                if (this.cur != null) {
                    this.curArray = this.cur.dataArray;
                    this.capacity = this.cur.index;
                }
            }
        }

        public void finish() {
            PooledLightUpdateQueue.this.clear();
        }
    }

    public static class Pool {

        private final Deque<Segment> segmentPool = new ArrayDeque<>();

        private Segment acquire() {
            if (this.segmentPool.isEmpty()) {
                return new Segment(this);
            }

            return this.segmentPool.pop();
        }

        private void release(Segment segment) {
            if (this.segmentPool.size() < CACHED_QUEUE_SEGMENTS_COUNT) {
                this.segmentPool.push(segment);
            }
        }
    }

    private static class Segment {

        private final int[] dataArray = new int[QUEUE_SEGMENT_SIZE << 2];
        private int index = 0;
        private Segment next;
        private final Pool pool;

        private Segment(Pool pool) {
            this.pool = pool;
        }

        private void release() {
            this.index = 0;
            this.next = null;

            this.pool.release(this);
        }
    }

}