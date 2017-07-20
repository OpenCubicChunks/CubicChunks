/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
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
package cubicchunks.lighting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TestLightUpdateQueue {

    @Test public void testMultipleBeginEndNoError() {
        //success if no error
        LightUpdateQueue q = new LightUpdateQueue();
        for (int i = 0; i < 10; i++) {
            q.begin(new BlockPos(0, 0, 0));
            q.end();
        }
    }

    @Test(expected = IllegalStateException.class) public void testEndWithoutBeginError() {
        LightUpdateQueue q = new LightUpdateQueue();
        q.end();
    }

    @Test(expected = IllegalStateException.class) public void testBeginTwiceError() {
        LightUpdateQueue q = new LightUpdateQueue();
        q.begin(new BlockPos(0, 0, 0));
        q.begin(new BlockPos(0, 0, 0));
    }

    @Test public void testNoNextWhenStartingMultipleAttempts() {
        LightUpdateQueue q = new LightUpdateQueue();
        for (int i = 0; i < 10; i++) {
            q.begin(new BlockPos(0, 0, 0));
            assertFalse(q.next());
            q.end();
        }
    }

    @Test public void testGetEqualToPutChangedOrigin() {
        BlockPos origin = new BlockPos(41325, -43224, 32432);
        BlockPos expectedPos = origin.add(1, 2, 3);
        int expectedValue = 10;
        int expectedDistance = 5;

        doTestPutEqual(origin, expectedPos, expectedValue, expectedDistance);
    }

    @Test public void testGetEqualToPutMinMaxIntOrigin() {
        //this will cause integer overflow, it's fine
        BlockPos origin = new BlockPos(Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
        BlockPos expectedPos = origin.add(1, 2, 3);
        int expectedValue = 10;
        int expectedDistance = 5;

        doTestPutEqual(origin, expectedPos, expectedValue, expectedDistance);
    }

    private void doTestPutEqual(BlockPos origin, BlockPos expectedPos, int expectedValue, int expectedDistance) {
        LightUpdateQueue q = new LightUpdateQueue();
        q.begin(origin);
        {
            putNextCheckEqual(expectedPos, expectedValue, expectedDistance, q);
        }
        q.end();
    }

    private void putNextCheckEqual(BlockPos expectedPos, int expectedValue, int expectedDistance, LightUpdateQueue q) {
        q.put(expectedPos, expectedValue, expectedDistance);
        checkNextEqual(expectedPos, expectedValue, expectedDistance, q);
    }

    private void checkNextEqual(BlockPos expectedPos, int expectedValue, int expectedDistance, LightUpdateQueue q) {
        q.next();
        BlockPos foundPos = q.getPos();
        int foundValue = q.getValue();
        int foundDistance = q.getDistance();
        assertEquals("Position after next() is different than position in put()", expectedPos, foundPos);
        assertEquals("Value after next() is different than value in put()", expectedValue, foundValue);
        assertEquals("Distance after next() is different than distance in put()", expectedDistance, foundDistance);
    }

    @Test public void testResetSingleEntry() {
        BlockPos origin = new BlockPos(3252, 543624, 352);
        BlockPos expectedPos = origin.add(1, 2, 3);
        int expectedValue = 10;

        LightUpdateQueue q = new LightUpdateQueue();
        q.begin(origin);
        {
            putNextCheckEqual(expectedPos, expectedValue, expectedValue, q);
            putNextCheckEqual(expectedPos, expectedValue, expectedValue, q);
            putNextCheckEqual(expectedPos, expectedValue, expectedValue, q);

            assertFalse(q.next());

            q.resetIndex();

            q.put(expectedPos, expectedValue - 1, expectedValue - 1);
            checkNextEqual(expectedPos, expectedValue, expectedValue, q);
            checkNextEqual(expectedPos, expectedValue, expectedValue, q);
            checkNextEqual(expectedPos, expectedValue, expectedValue, q);
            checkNextEqual(expectedPos, expectedValue - 1, expectedValue - 1, q);

            assertFalse(q.next());
        }
        q.end();
    }

    @Test public void testResetFlag() {
        BlockPos origin = new BlockPos(3252, 543624, 352);
        BlockPos expectedPos = origin.add(1, 2, 3);
        int expectedValue = 10;

        LightUpdateQueue q = new LightUpdateQueue();
        q.begin(origin);
        {
            putNextCheckEqual(expectedPos, expectedValue, expectedValue, q);
            putNextCheckEqual(expectedPos, expectedValue, expectedValue, q);
            putNextCheckEqual(expectedPos, expectedValue, expectedValue, q);

            assertFalse(q.next());

            q.resetIndex();

            q.put(expectedPos, expectedValue, expectedValue);

            checkNextEqual(expectedPos, expectedValue, expectedValue, q);
            assertTrue(q.isBeforeReset());

            checkNextEqual(expectedPos, expectedValue, expectedValue, q);
            assertTrue(q.isBeforeReset());

            checkNextEqual(expectedPos, expectedValue, expectedValue, q);
            assertTrue(q.isBeforeReset());

            checkNextEqual(expectedPos, expectedValue, expectedValue, q);
            assertFalse(q.isBeforeReset());

            assertFalse(q.next());
        }
        q.end();
    }

    @Test public void testManyEntriesEqualValuesMultiple() {
        final int amount = 100000;

        Random rand = new Random(42);

        Queue<BlockPos> expectedCoordsQueue = new ArrayDeque<>();
        Queue<Integer> expectedValueQueue = new ArrayDeque<>();
        Queue<Integer> expectedDistanceQueue = new ArrayDeque<>();

        BlockPos origin = new BlockPos(432534, 2352, -4325);

        LightUpdateQueue q = new LightUpdateQueue();
        for (int i = 0; i < 10; i++) {
            testManyEntriesEqualValue_do(amount, rand, expectedCoordsQueue, expectedValueQueue, expectedDistanceQueue, origin, q);
        }
    }

    private void testManyEntriesEqualValue_do(int amount, Random rand,
            Queue<BlockPos> expectedCoordsQueue, Queue<Integer> expectedValueQueue,
            Queue<Integer> expectedDistanceQueue, BlockPos origin, LightUpdateQueue q) {
        q.begin(origin);
        {
            for (int i = 0; i < amount; i++) {
                BlockPos pos = randPos(rand).add(origin);
                int value = randValue(rand);
                int dist = randValue(rand);
                q.put(pos, value, dist);
                expectedCoordsQueue.add(pos);
                expectedValueQueue.add(value);
                expectedDistanceQueue.add(dist);
            }
            for (int i = 0; i < amount; i++) {
                q.next();
                assertEquals(expectedCoordsQueue.remove(), q.getPos());
                assertEquals(expectedValueQueue.remove(), Integer.valueOf(q.getValue()));
                assertEquals(expectedDistanceQueue.remove(), Integer.valueOf(q.getDistance()));
            }
            assertFalse("Queue has more elements than added", q.next());
        }
        q.end();
    }

    private BlockPos randPos(Random r) {
        return new BlockPos(r.nextInt(LightUpdateQueue.MAX_POS - LightUpdateQueue.MIN_POS + 1) + LightUpdateQueue.MIN_POS,
                r.nextInt(LightUpdateQueue.MAX_POS - LightUpdateQueue.MIN_POS + 1) + LightUpdateQueue.MIN_POS,
                r.nextInt(LightUpdateQueue.MAX_POS - LightUpdateQueue.MIN_POS + 1) + LightUpdateQueue.MIN_POS);
    }

    private int randValue(Random r) {
        return r.nextInt(LightUpdateQueue.MAX_VALUE - LightUpdateQueue.MIN_VALUE + 1) + LightUpdateQueue.MIN_VALUE;
    }
}
