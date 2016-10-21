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
package cubicchunks;

import com.google.common.collect.Lists;
import cubicchunks.util.Bits;
import cubicchunks.util.Coords;
import cubicchunks.world.ServerHeightMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.spongepowered.test.launch.LaunchWrapperTestRunner;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(LaunchWrapperTestRunner.class)
public class TestOpacityIndex {

	private static Field YminField;
	private static Field YmaxField;
	private static Field SegmentsField;

	static {
		try {
			YminField = ServerHeightMap.class.getDeclaredField("ymin");
			YminField.setAccessible(true);
			YmaxField = ServerHeightMap.class.getDeclaredField("ymax");
			YmaxField.setAccessible(true);
			SegmentsField = ServerHeightMap.class.getDeclaredField("segments");
			SegmentsField.setAccessible(true);
		} catch (NoSuchFieldException | SecurityException ex) {
			throw new Error(ex);
		}
	}

	@Test
	public void getWithAllTransparent() {
		ServerHeightMap index = new ServerHeightMap();
		assertEquals(Coords.NO_HEIGHT, index.getBottomBlockY(0, 0));
		assertEquals(Coords.NO_HEIGHT, index.getTopBlockY(0, 0));
	}

	@Test
	public void getWithoutDataSingleBlock() {
		ServerHeightMap index = makeIndex(10, 10);
		assertEquals(10, index.getBottomBlockY(0, 0));
		assertEquals(10, index.getTopBlockY(0, 0));
	}

	@Test
	public void getWithoutDataMultipleBlocks() {
		ServerHeightMap index = makeIndex(8, 10);
		assertEquals(8, index.getBottomBlockY(0, 0));
		assertEquals(10, index.getTopBlockY(0, 0));
	}

	@Test
	public void getWith1Data() {
		ServerHeightMap index = makeIndex(8, 10,
				8, 1
		);
		assertEquals(8, index.getBottomBlockY(0, 0));
		assertEquals(10, index.getTopBlockY(0, 0));
	}

	@Test
	public void setSingleOpaqueFromEmpty() {
		ServerHeightMap index = new ServerHeightMap();
		index.onOpacityChange(0, 10, 0, 255);
		assertEquals(10, index.getBottomBlockY(0, 0));
		assertEquals(10, index.getTopBlockY(0, 0));
		assertEquals(null, getSegments(index));
	}

	@Test
	public void setSingleTransparentFromSingleOpaque() {
		ServerHeightMap index = makeIndex(10, 10);
		index.onOpacityChange(0, 10, 0, 0);
		assertEquals(Coords.NO_HEIGHT, index.getBottomBlockY(0, 0));
		assertEquals(Coords.NO_HEIGHT, index.getTopBlockY(0, 0));
		assertEquals(null, getSegments(index));
	}

	@Test
	public void setExpandSingleOpaque() {
		ServerHeightMap index = makeIndex(10, 10);

		index.onOpacityChange(0, 11, 0, 255);
		assertEquals(10, index.getBottomBlockY(0, 0));
		assertEquals(11, index.getTopBlockY(0, 0));
		assertEquals(null, getSegments(index));

		index.onOpacityChange(0, 9, 0, 255);
		assertEquals(9, index.getBottomBlockY(0, 0));
		assertEquals(11, index.getTopBlockY(0, 0));
		assertEquals(null, getSegments(index));
	}

	@Test
	public void setShrinkOpaque() {
		ServerHeightMap index = makeIndex(9, 11);

		index.onOpacityChange(0, 9, 0, 0);
		assertEquals(10, index.getBottomBlockY(0, 0));
		assertEquals(11, index.getTopBlockY(0, 0));
		assertEquals(null, getSegments(index));

		index.onOpacityChange(0, 11, 0, 0);
		assertEquals(10, index.getBottomBlockY(0, 0));
		assertEquals(10, index.getTopBlockY(0, 0));
		assertEquals(null, getSegments(index));
	}

	@Test
	public void setDisjointOpaqueAboveOpaue() {
		ServerHeightMap index = makeIndex(9, 11);

		index.onOpacityChange(0, 16, 0, 255);
		assertEquals(9, index.getBottomBlockY(0, 0));
		assertEquals(16, index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
				9, 1,
				12, 0,
				16, 1
		), getSegments(index));
	}

	@Test
	public void setDisjointOpaqueBelowOpaue() {
		ServerHeightMap index = makeIndex(9, 11);

		index.onOpacityChange(0, 4, 0, 255);
		assertEquals(4, index.getBottomBlockY(0, 0));
		assertEquals(11, index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
				4, 1,
				5, 0,
				9, 1
		), getSegments(index));
	}

	@Test
	public void setDisjointOpaqueAboveOpaques() {
		ServerHeightMap index = makeIndex(9, 16,
				9, 1,
				12, 0,
				16, 1
		);

		index.onOpacityChange(0, 20, 0, 255);
		assertEquals(9, index.getBottomBlockY(0, 0));
		assertEquals(20, index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
				9, 1,
				12, 0,
				16, 1,
				17, 0,
				20, 1
		), getSegments(index));
	}

	@Test
	public void setDisjointOpaqueBelowOpaques() {
		ServerHeightMap index = makeIndex(9, 16,
				9, 1,
				12, 0,
				16, 1
		);

		index.onOpacityChange(0, 3, 0, 255);
		assertEquals(3, index.getBottomBlockY(0, 0));
		assertEquals(16, index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
				3, 1,
				4, 0,
				9, 1,
				12, 0,
				16, 1
		), getSegments(index));
	}

	@Test
	public void extendTopOpaqueUp() {
		ServerHeightMap index = makeIndex(9, 16,
				9, 1,
				12, 0,
				16, 1
		);

		index.onOpacityChange(0, 17, 0, 255);
		assertEquals(9, index.getBottomBlockY(0, 0));
		assertEquals(17, index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
				9, 1,
				12, 0,
				16, 1
		), getSegments(index));
	}

	@Test
	public void extendBottomOpaqueDown() {
		ServerHeightMap index = makeIndex(9, 16,
				9, 1,
				12, 0,
				16, 1
		);

		index.onOpacityChange(0, 8, 0, 255);
		assertEquals(8, index.getBottomBlockY(0, 0));
		assertEquals(16, index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
				8, 1,
				12, 0,
				16, 1
		), getSegments(index));
	}

	@Test
	public void setBisectOpaue() {
		ServerHeightMap index = makeIndex(9, 11);

		index.onOpacityChange(0, 10, 0, 0);
		assertEquals(9, index.getBottomBlockY(0, 0));
		assertEquals(11, index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
				9, 1,
				10, 0,
				11, 1
		), getSegments(index));
	}

	@Test
	public void setDataStartSameAsBottomRoomBeforeTop() {
		ServerHeightMap index = makeIndex(4, 7,
				4, 1
		);

		index.onOpacityChange(0, 4, 0, 0);
		assertEquals(5, index.getBottomBlockY(0, 0));
		assertEquals(7, index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
				5, 1
		), getSegments(index));
	}

	@Test
	public void setDataNotStartSameAsNextRoomAfter() {
		ServerHeightMap index = makeIndex(2, 7,
				2, 1
		);

		index.onOpacityChange(0, 4, 0, 0);
		assertEquals(2, index.getBottomBlockY(0, 0));
		assertEquals(7, index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
				2, 1,
				4, 0,
				5, 1
		), getSegments(index));
	}

	@Test
	public void setDataNotStartSameAsTopNoRoomAfter() {
		ServerHeightMap index = makeIndex(2, 4,
				2, 1
		);

		index.onOpacityChange(0, 4, 0, 0);
		assertEquals(2, index.getBottomBlockY(0, 0));
		assertEquals(3, index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
				2, 1
		), getSegments(index));
	}

	@Test
	public void setDataAfterTopSameAsPrevious() {
		ServerHeightMap index = makeIndex(2, 4,
				2, 1
		);

		index.onOpacityChange(0, 4, 0, 0);
		assertEquals(2, index.getBottomBlockY(0, 0));
		assertEquals(3, index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
				2, 1
		), getSegments(index));
	}

	@Test
	public void setTransparentInOpaqueAndClear() {
		ServerHeightMap index = new ServerHeightMap();

		// place blocks
		index.onOpacityChange(0, 0, 0, 255);
		index.onOpacityChange(0, 1, 0, 255);
		index.onOpacityChange(0, 2, 0, 255);

		assertEquals(2, index.getTopBlockY(0, 0));
		assertEquals(0, index.getBottomBlockY(0, 0));

		// remove the middle one
		index.onOpacityChange(0, 1, 0, 0);

		assertEquals(2, index.getTopBlockY(0, 0));
		assertEquals(0, index.getBottomBlockY(0, 0));

		// remove the bottom one
		index.onOpacityChange(0, 0, 0, 0);

		assertEquals(2, index.getTopBlockY(0, 0));
		assertEquals(2, index.getBottomBlockY(0, 0));

		// remove the top one
		index.onOpacityChange(0, 2, 0, 0);

		assertEquals(Coords.NO_HEIGHT, index.getTopBlockY(0, 0));
		assertEquals(Coords.NO_HEIGHT, index.getBottomBlockY(0, 0));
	}

	@Test
	public void checkFloatingIsland() {

		// make an index with a surface
		ServerHeightMap index = makeIndex(100, 102);

		// start setting blocks in the sky
		index.onOpacityChange(0, 200, 0, 255);

		assertEquals(200, index.getTopBlockY(0, 0));
		assertEquals(100, index.getBottomBlockY(0, 0));

		index.onOpacityChange(0, 201, 0, 255);

		assertEquals(201, index.getTopBlockY(0, 0));
		assertEquals(100, index.getBottomBlockY(0, 0));

		index.onOpacityChange(0, 202, 0, 255);

		assertEquals(202, index.getTopBlockY(0, 0));
		assertEquals(100, index.getBottomBlockY(0, 0));
	}

	//test 21011120
	@Test
	public void testMergeSegmentsIntoNoSegmentsAndRemoveTop_generated() {
		ServerHeightMap index = new ServerHeightMap();
		index.onOpacityChange(0, 2, 0, 1);

		assertEquals(2, index.getTopBlockY(0, 0));
		assertEquals(2, index.getBottomBlockY(0, 0));

		index.onOpacityChange(0, 0, 0, 1);

		assertEquals(2, index.getTopBlockY(0, 0));
		assertEquals(0, index.getBottomBlockY(0, 0));

		index.onOpacityChange(0, 1, 0, 1);

		assertEquals(2, index.getTopBlockY(0, 0));
		assertEquals(0, index.getBottomBlockY(0, 0));

		index.onOpacityChange(0, 2, 0, 0);

		assertEquals(1, index.getTopBlockY(0, 0));
		assertEquals(0, index.getBottomBlockY(0, 0));
	}

	@Test
	public void test41210140110000() {
		ServerHeightMap index = new ServerHeightMap();
		index.onOpacityChange(0, 4, 0, 1);

		assertEquals(4, index.getTopBlockY(0, 0));
		assertEquals(4, index.getBottomBlockY(0, 0));

		index.onOpacityChange(0, 2, 0, 1);

		assertEquals(4, index.getTopBlockY(0, 0));
		assertEquals(2, index.getBottomBlockY(0, 0));

		index.onOpacityChange(0, 0, 0, 1);

		assertEquals(4, index.getTopBlockY(0, 0));
		assertEquals(0, index.getBottomBlockY(0, 0));

		index.onOpacityChange(0, 4, 0, 0);

		assertEquals(2, index.getTopBlockY(0, 0));
		assertEquals(0, index.getBottomBlockY(0, 0));

		index.onOpacityChange(0, 1, 0, 1);

		assertEquals(2, index.getTopBlockY(0, 0));
		assertEquals(0, index.getBottomBlockY(0, 0));

		index.onOpacityChange(0, 0, 0, 0);

		assertEquals(2, index.getTopBlockY(0, 0));
		assertEquals(1, index.getBottomBlockY(0, 0));

		index.onOpacityChange(0, 0, 0, 0);

		assertEquals(2, index.getTopBlockY(0, 0));
		assertEquals(1, index.getBottomBlockY(0, 0));
	}

	@Test
	public void testSetAndClear_generated() {
		ServerHeightMap index = new ServerHeightMap();
		index.onOpacityChange(0, 4, 0, 1);

		assertEquals(4, index.getTopBlockY(0, 0));
		assertEquals(4, index.getBottomBlockY(0, 0));

		index.onOpacityChange(0, 2, 0, 1);

		assertEquals(4, index.getTopBlockY(0, 0));
		assertEquals(2, index.getBottomBlockY(0, 0));

		index.onOpacityChange(0, 0, 0, 1);

		assertEquals(4, index.getTopBlockY(0, 0));
		assertEquals(0, index.getBottomBlockY(0, 0));

		index.onOpacityChange(0, 4, 0, 0);

		assertEquals(2, index.getTopBlockY(0, 0));
		assertEquals(0, index.getBottomBlockY(0, 0));

		index.onOpacityChange(0, 2, 0, 0);

		assertEquals(0, index.getTopBlockY(0, 0));
		assertEquals(0, index.getBottomBlockY(0, 0));

		index.onOpacityChange(0, 0, 0, 0);

		assertEquals(Coords.NO_HEIGHT, index.getTopBlockY(0, 0));
		assertEquals(Coords.NO_HEIGHT, index.getBottomBlockY(0, 0));

		index.onOpacityChange(0, 0, 0, 0);

		assertEquals(Coords.NO_HEIGHT, index.getTopBlockY(0, 0));
		assertEquals(Coords.NO_HEIGHT, index.getBottomBlockY(0, 0));
	}

	@Test
	public void test31110000hmaplim_generated() {
		ServerHeightMap index = new ServerHeightMap();
		index.onOpacityChange(0, 3, 0, 1);
		index.onOpacityChange(0, 1, 0, 1);

		int height = index.getTopBlockYBelow(0, 0, 3);
		assertEquals(1, height);
	}
	@Test
	public void allCombinationsTest() {
		//tested with value up to 6 (takes a lot of time)
		final int maxHeight = 5, numBlocks = 4;
		int[] yPosOpacityEncoded = new int[numBlocks];

		while (true) {
			ServerHeightMap index = new ServerHeightMap();
			ArrayOpacityIndexImpl test = new ArrayOpacityIndexImpl();

			StringBuilder msg = new StringBuilder();
			for (int encoded : yPosOpacityEncoded) {
				msg.append("i[").append(encoded / 2).append("]=").append(encoded % 2).append(", ");
			}
			String message = msg.toString();
			try {
				for (int i = 0; i < yPosOpacityEncoded.length; i++) {
					int opacity = yPosOpacityEncoded[i] % 2;
					index.onOpacityChange(0, yPosOpacityEncoded[i] / 2, 0, opacity);
					test.set(yPosOpacityEncoded[i] / 2, opacity);
				}
				//store-read-store test
				byte[] b = index.getData();
				ServerHeightMap newIndex = new ServerHeightMap();
				newIndex.readData(b);
				assertArrayEquals("Got different data after creating index based on read data\n" + message + "\n", b, newIndex.getData());
			} catch (Throwable t) {
				System.out.println(message + "exception");
				throw t;
			}
			for(int i = 0; i < maxHeight; i++) {
				assertEquals(message + ", maxHBelow(" + i + ")", (Integer)test.getMaxYBelow(i), (Integer)index.getTopBlockYBelow(0, 0, i));
			}

			assertEquals(message + "minY", (Integer)test.getMinY(), (Integer)index.getBottomBlockY(0, 0));
			assertEquals(message + "maxY", (Integer)test.getMaxY(), (Integer)index.getTopBlockY(0, 0));


			yPosOpacityEncoded[0]++;
			for (int i = 0; i < numBlocks - 1; i++) {
				if (yPosOpacityEncoded[i] == maxHeight * 2) {
					yPosOpacityEncoded[i] = 0;
					yPosOpacityEncoded[i + 1]++;
				}
			}
			if (yPosOpacityEncoded[numBlocks - 1] == maxHeight * 2) {
				break;
			}
		}
	}

	private ServerHeightMap makeIndex(int ymin, int ymax, int... segments) {
		ServerHeightMap index = new ServerHeightMap();

		// pack the segments
		int[] packedSegments = null;
		if (segments.length > 0) {
			packedSegments = new int[segments.length / 2];
			for (int i = 0; i < segments.length / 2; i++) {
				packedSegments[i] = Bits.packSignedToInt(segments[i * 2 + 0], 24, 0) | Bits.packUnsignedToInt(segments[i * 2 + 1], 8, 24);
			}
		}

		set(index, ymin, ymax, packedSegments);
		return index;
	}

	private void set(ServerHeightMap index, int ymin, int ymax, int[] segments) {
		try {
			YminField.set(index, new int[]{ymin});
			YmaxField.set(index, new int[]{ymax});
			SegmentsField.set(index, new int[][]{segments});
		} catch (IllegalArgumentException | IllegalAccessException ex) {
			throw new Error(ex);
		}
	}

	private List<Integer> getSegments(ServerHeightMap index) {
		try {
			int[] packedSegments = ((int[][]) SegmentsField.get(index))[0];
			if (packedSegments == null) {
				return null;
			}

			final int NoneSegment = 0x7fffff;

			// unpack the segments
			List<Integer> segments = Lists.newArrayList();
			for (int i = 0; i < packedSegments.length && packedSegments[i] != NoneSegment; i++) {
				segments.add(Bits.unpackSigned(packedSegments[i], 24, 0));
				segments.add(Bits.unpackUnsigned(packedSegments[i], 8, 24));
			}
			return segments;

		} catch (IllegalArgumentException | IllegalAccessException ex) {
			throw new Error(ex);
		}
	}

	private static class ArrayOpacityIndexImpl {
		private int[] arr = new int[100];

		private void set(int y, int val) {
			arr[y] = val;
		}

		public int getMinY() {
			for (int i = 0; i < arr.length; i++) {
				if (arr[i] != 0) {
					return i;
				}
			}
			return Coords.NO_HEIGHT;
		}

		public int getMaxY() {
			for (int i = arr.length - 1; i >= 0; i--) {
				if (arr[i] != 0) {
					return i;
				}
			}
			return Coords.NO_HEIGHT;
		}

		public int getMaxYBelow(int y) {
			for(int i = y - 1; i >= 0; i--) {
				if(arr[i] != 0) {
					return i;
				}
			}
			return Coords.NO_HEIGHT;
		}
	}
}
