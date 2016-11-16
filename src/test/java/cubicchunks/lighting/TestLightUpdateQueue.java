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

import net.minecraft.util.math.BlockPos;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestLightUpdateQueue {
	@Test public void testBeginEndNoError() {
		//success if no error
		LightUpdateQueue q = new LightUpdateQueue();
		q.begin(new BlockPos(0, 0, 0));
		q.end();
	}

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

	@Test public void testNoNextWhenStarting() {
		LightUpdateQueue q = new LightUpdateQueue();
		q.begin(new BlockPos(0, 0, 0));
		assertFalse(q.next());
		q.end();
	}

	@Test public void testNoNextWhenStartingMultipleAttempts() {
		LightUpdateQueue q = new LightUpdateQueue();
		for (int i = 0; i < 10; i++) {
			q.begin(new BlockPos(0, 0, 0));
			assertFalse(q.next());
			q.end();
		}
	}

	@Test public void testHasNextAfterPut() {
		LightUpdateQueue q = new LightUpdateQueue();
		q.begin(new BlockPos(0, 0, 0));
		q.put(0, 0, 0, 0);
		assertTrue("No next after put()", q.next());
		q.end();
	}

	@Test public void testHasNextAfterPutMultipleAttempts() {
		LightUpdateQueue q = new LightUpdateQueue();
		for (int i = 0; i < 10; i++) {
			q.begin(new BlockPos(0, 0, 0));
			q.put(0, 0, 0, 0);
			assertTrue("No next after put()", q.next());
			q.end();
		}
	}

	@Test public void testGetEqualToPut() {
		BlockPos expectedPos = new BlockPos(1, 2, 3);
		int expectedValue = 42;

		LightUpdateQueue q = new LightUpdateQueue();
		q.begin(new BlockPos(0, 0, 0));
		{
			q.put(expectedPos, expectedValue);
			q.next();
			BlockPos foundPos = q.getPos();
			int foundValue = q.getValue();
			assertEquals("Position after next() is different than position in put()", expectedPos, foundPos);
			assertEquals("Value after next() is different than value in put()", expectedValue, foundValue);
		}
		q.end();
	}

	@Test public void testGetEqualToPutChangedOrigin() {
		BlockPos origin = new BlockPos(41325, -43224, 32432);
		BlockPos expectedPos = origin.add(1, 2, 3);
		int expectedValue = 42;

		LightUpdateQueue q = new LightUpdateQueue();
		q.begin(origin);
		{
			q.put(expectedPos, expectedValue);
			q.next();
			BlockPos foundPos = q.getPos();
			int foundValue = q.getValue();
			assertEquals("Position after next() is different than position in put()", expectedPos, foundPos);
			assertEquals("Value after next() is different than value in put()", expectedValue, foundValue);
		}
		q.end();
	}

	@Test public void testGetEqualToPutMinMaxIntOrigin() {
		//this will cause integer overflow, it's fine
		BlockPos origin = new BlockPos(Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
		BlockPos expectedPos = origin.add(1, 2, 3);
		int expectedValue = 42;

		LightUpdateQueue q = new LightUpdateQueue();
		q.begin(origin);
		{
			q.put(expectedPos, expectedValue);
			q.next();
			BlockPos foundPos = q.getPos();
			int foundValue = q.getValue();
			assertEquals("Position after next() is different than position in put()", expectedPos, foundPos);
			assertEquals("Value after next() is different than value in put()", expectedValue, foundValue);
		}
		q.end();
	}

	@Test public void testResetSingleEntry() {
		BlockPos origin = new BlockPos(3252, 543624, 352);
		BlockPos expectedPos = origin.add(1, 2, 3);
		int expectedValue = 42;

		LightUpdateQueue q = new LightUpdateQueue();
		q.begin(origin);
		{
			q.put(expectedPos, expectedValue);
			assertTrue(q.next());
			assertEquals(expectedPos, q.getPos());
			assertEquals(expectedValue, q.getValue());

			assertFalse(q.next());

			q.resetIndex();

			assertTrue(q.next());
			assertEquals(expectedPos, q.getPos());
			assertEquals(expectedValue, q.getValue());

			assertFalse(q.next());
		}
		q.end();
	}

	@Test public void testManyEntriesEqualAmount() {
		final int amount = 100000;
		//this will cause integer overflow, it's fine
		BlockPos origin = new BlockPos(Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
		BlockPos expectedPos = origin.add(1, 2, 3);
		int expectedValue = 42;

		LightUpdateQueue q = new LightUpdateQueue();
		testManyEntriesEqualAmount_do(amount, origin, expectedPos, expectedValue, q);
	}


	@Test public void testManyEntriesEqualAmountMultiple() {
		final int amount = 100000;
		//this will cause integer overflow, it's fine
		BlockPos origin = new BlockPos(Integer.MIN_VALUE, Integer.MAX_VALUE, 0);
		BlockPos expectedPos = origin.add(1, 2, 3);
		int expectedValue = 42;

		LightUpdateQueue q = new LightUpdateQueue();
		for (int i = 0; i < 10; i++) {
			testManyEntriesEqualAmount_do(amount, origin, expectedPos, expectedValue, q);
		}
	}

	private void testManyEntriesEqualAmount_do(int amount, BlockPos origin, BlockPos expectedPos, int expectedValue, LightUpdateQueue q) {
		q.begin(origin);
		{
			for (int i = 0; i < amount; i++) {
				q.put(expectedPos, expectedValue);
			}
			for (int i = 0; i < amount; i++) {
				assertTrue("Queue has less elements that added", q.next());
			}
			assertFalse("Queue has more elements than added", q.next());
		}
		q.end();
	}

	@Test public void testManyEntriesEqualValues() {
		final int amount = 100000;

		Random rand = new Random(42);

		Queue<BlockPos> expectedCoordsQueue = new ArrayDeque<>();
		Queue<Integer> expectedValueQueue = new ArrayDeque<>();

		BlockPos origin = new BlockPos(432534, 2352, -4325);

		LightUpdateQueue q = new LightUpdateQueue();
		testManyEntriesEqualValue_do(amount, rand, expectedCoordsQueue, expectedValueQueue, origin, q);
	}


	@Test public void testManyEntriesEqualValuesMultiple() {
		final int amount = 100000;

		Random rand = new Random(42);

		Queue<BlockPos> expectedCoordsQueue = new ArrayDeque<>();
		Queue<Integer> expectedValueQueue = new ArrayDeque<>();

		BlockPos origin = new BlockPos(432534, 2352, -4325);

		LightUpdateQueue q = new LightUpdateQueue();
		for (int i = 0; i < 10; i++) {
			testManyEntriesEqualValue_do(amount, rand, expectedCoordsQueue, expectedValueQueue, origin, q);
		}
	}

	private void testManyEntriesEqualValue_do(int amount, Random rand, Queue<BlockPos> expectedCoordsQueue, Queue<Integer> expectedValueQueue, BlockPos origin, LightUpdateQueue q) {
		q.begin(origin);
		{
			for (int i = 0; i < amount; i++) {
				BlockPos pos = randPos(rand).add(origin);
				int value = randValue(rand);
				q.put(pos, value);
				expectedCoordsQueue.add(pos);
				expectedValueQueue.add(value);
			}
			for (int i = 0; i < amount; i++) {
				q.next();
				assertEquals(expectedCoordsQueue.remove(), q.getPos());
				assertEquals(expectedValueQueue.remove(), Integer.valueOf(q.getValue()));
			}
			assertFalse("Queue has more elements than added", q.next());
		}
		q.end();
	}

	@Test public void testAddRemoveInterleaved() {
		BlockPos origin = new BlockPos(-214324, 8545634, -324674);

		BlockPos pos1 = new BlockPos(0, 1, 2).add(origin);
		BlockPos pos2 = new BlockPos(3, 4, 5).add(origin);
		BlockPos pos3 = new BlockPos(6, 7, 8).add(origin);
		BlockPos pos4 = new BlockPos(9, 10, 11).add(origin);

		int value1 = 1, value2 = 2, value3 = 3, value4 = 4;

		//success if no error
		LightUpdateQueue q = new LightUpdateQueue();
		q.begin(origin);
		{
			q.put(pos1, value1);
			q.next();
			assertEquals(pos1, q.getPos());
			assertEquals(value1, q.getValue());

			q.put(pos2, value2);
			q.put(pos3, value3);

			q.next();
			assertEquals(pos2, q.getPos());
			assertEquals(value2, q.getValue());

			q.put(pos4, value4);

			q.next();
			assertEquals(pos3, q.getPos());
			assertEquals(value3, q.getValue());

			q.next();
			assertEquals(pos4, q.getPos());
			assertEquals(value4, q.getValue());
		}
		assertFalse(q.next());
		q.end();
	}

	@Test public void testManyEntriesEqualValuesInterleavedGetAdd() {
		final int amount = 100000;

		Random rand = new Random(42);

		Queue<BlockPos> expectedCoordsQueue = new ArrayDeque<>();
		Queue<Integer> expectedValueQueue = new ArrayDeque<>();

		BlockPos origin = new BlockPos(432534, 2352, -4325);

		//success if no error
		LightUpdateQueue q = new LightUpdateQueue();
		q.begin(origin);
		{
			for (int i = 0; i < amount; i++) {
				BlockPos pos = randPos(rand).add(origin);
				int value = randValue(rand);
				q.put(pos, value);
				expectedCoordsQueue.add(pos);
				expectedValueQueue.add(value);

				q.next();
				assertEquals(expectedCoordsQueue.remove(), q.getPos());
				assertEquals(expectedValueQueue.remove(), Integer.valueOf(q.getValue()));
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
