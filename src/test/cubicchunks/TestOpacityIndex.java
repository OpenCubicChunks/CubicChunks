/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2014 Tall Worlds
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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

import cubicchunks.util.Bits;
import cubicchunks.world.OpacityIndex;


public class TestOpacityIndex {
	
	private static Field YminField;
	private static Field YmaxField;
	private static Field SegmentsField;
	
	static {
		try {
			YminField = OpacityIndex.class.getDeclaredField("m_ymin");
			YminField.setAccessible(true);
			YmaxField = OpacityIndex.class.getDeclaredField("m_ymax");
			YmaxField.setAccessible(true);
			SegmentsField = OpacityIndex.class.getDeclaredField("m_segments");
			SegmentsField.setAccessible(true);
		} catch (NoSuchFieldException | SecurityException ex) {
			throw new Error(ex);
		}
	}

	@Test
	public void getWithAllTransparent() {
		OpacityIndex index = new OpacityIndex();
		assertEquals(null, index.getBottomBlockY(0, 0));
		assertEquals(null, index.getTopBlockY(0, 0));
		assertEquals(0, index.getOpacity(0, -100, 0));
		assertEquals(0, index.getOpacity(0, -10, 0));
		assertEquals(0, index.getOpacity(0, 0, 0));
		assertEquals(0, index.getOpacity(0, 10, 0));
		assertEquals(0, index.getOpacity(0, 100, 0));
	}
	
	@Test
	public void getWithoutDataSingleBlock() {
		OpacityIndex index = makeIndex(10, 10);
		assertEquals(10, (int)index.getBottomBlockY(0, 0));
		assertEquals(10, (int)index.getTopBlockY(0, 0));
		assertEquals(0, index.getOpacity(0, 9, 0));
		assertEquals(255, index.getOpacity(0, 10, 0));
		assertEquals(0, index.getOpacity(0, 11, 0));
	}
	
	@Test
	public void getWithoutDataMultipleBlocks() {
		OpacityIndex index = makeIndex(8, 10);
		assertEquals(8, (int)index.getBottomBlockY(0, 0));
		assertEquals(10, (int)index.getTopBlockY(0, 0));
		assertEquals(0, index.getOpacity(0, 7, 0));
		assertEquals(255, index.getOpacity(0, 8, 0));
		assertEquals(255, index.getOpacity(0, 9, 0));
		assertEquals(255, index.getOpacity(0, 10, 0));
		assertEquals(0, index.getOpacity(0, 11, 0));
	}
	
	@Test
	public void getWith1Data() {
		OpacityIndex index = makeIndex(8, 10,
			5, 10
		);
		assertEquals(8, (int)index.getBottomBlockY(0, 0));
		assertEquals(10, (int)index.getTopBlockY(0, 0));
		assertEquals(0, index.getOpacity(0, 7, 0));
		assertEquals(10, index.getOpacity(0, 8, 0));
		assertEquals(10, index.getOpacity(0, 9, 0));
		assertEquals(10, index.getOpacity(0, 10, 0));
		assertEquals(0, index.getOpacity(0, 11, 0));
	}
	
	@Test
	public void getWith2Data() {
		OpacityIndex index = makeIndex(6, 10,
			6, 10,
			9, 20
		);
		assertEquals(6, (int)index.getBottomBlockY(0, 0));
		assertEquals(10, (int)index.getTopBlockY(0, 0));
		assertEquals(0, index.getOpacity(0, 5, 0));
		assertEquals(10, index.getOpacity(0, 6, 0));
		assertEquals(10, index.getOpacity(0, 7, 0));
		assertEquals(10, index.getOpacity(0, 8, 0));
		assertEquals(20, index.getOpacity(0, 9, 0));
		assertEquals(20, index.getOpacity(0, 10, 0));
		assertEquals(0, index.getOpacity(0, 11, 0));
	}
	
	@Test
	public void setSingleOpaqueFromEmpty() {
		OpacityIndex index = new OpacityIndex();
		index.setOpacity(0, 10, 0, 255);
		assertEquals(10, (int)index.getBottomBlockY(0, 0));
		assertEquals(10, (int)index.getTopBlockY(0, 0));
		assertEquals(null, getSegments(index));
	}
	
	@Test
	public void setSingleTransparentFromSingleOpaque() {
		OpacityIndex index = makeIndex(10, 10);
		index.setOpacity(0, 10, 0, 0);
		assertEquals(null, index.getBottomBlockY(0, 0));
		assertEquals(null, index.getTopBlockY(0, 0));
		assertEquals(null, getSegments(index));
	}
	
	@Test
	public void setExpandSingleOpaque() {
		OpacityIndex index = makeIndex(10, 10);
		
		index.setOpacity(0, 11, 0, 255);
		assertEquals(10, (int)index.getBottomBlockY(0, 0));
		assertEquals(11, (int)index.getTopBlockY(0, 0));
		assertEquals(null, getSegments(index));
		
		index.setOpacity(0, 9, 0, 255);
		assertEquals(9, (int)index.getBottomBlockY(0, 0));
		assertEquals(11, (int)index.getTopBlockY(0, 0));
		assertEquals(null, getSegments(index));
	}
	
	@Test
	public void setShrinkOpaque() {
		OpacityIndex index = makeIndex(9, 11);
		
		index.setOpacity(0, 9, 0, 0);
		assertEquals(10, (int)index.getBottomBlockY(0, 0));
		assertEquals(11, (int)index.getTopBlockY(0, 0));
		assertEquals(null, getSegments(index));
		
		index.setOpacity(0, 11, 0, 0);
		assertEquals(10, (int)index.getBottomBlockY(0, 0));
		assertEquals(10, (int)index.getTopBlockY(0, 0));
		assertEquals(null, getSegments(index));
	}
	
	@Test
	public void setDisjointOpaqueAboveOpaue() {
		OpacityIndex index = makeIndex(9, 11);
		
		index.setOpacity(0, 16, 0, 255);
		assertEquals(9, (int)index.getBottomBlockY(0, 0));
		assertEquals(16, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			9, 255,
			12, 0,
			16, 255
		), getSegments(index));
	}
	
	@Test
	public void setDisjointOpaqueBelowOpaue() {
		OpacityIndex index = makeIndex(9, 11);
		
		index.setOpacity(0, 4, 0, 255);
		assertEquals(4, (int)index.getBottomBlockY(0, 0));
		assertEquals(11, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			4, 255,
			5, 0,
			9, 255
		), getSegments(index));
	}
	
	@Test
	public void setBisectOpaue() {
		OpacityIndex index = makeIndex(9, 11);
		
		index.setOpacity(0, 10, 0, 0);
		assertEquals(9, (int)index.getBottomBlockY(0, 0));
		assertEquals(11, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			9, 255,
			10, 0,
			11, 255
		), getSegments(index));
	}
	
	@Test
	public void setDataStartSame() {
		OpacityIndex index = makeIndex(2, 7,
			2, 1,
			4, 2,
			6, 3
		);
		
		index.setOpacity(0, 4, 0, 2);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(7, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 1,
			4, 2,
			6, 3
		), getSegments(index));
	}

	@Test
	public void setDataStartSameAsPrevRoomBeforeSegment() {
		OpacityIndex index = makeIndex(2, 7,
			2, 1,
			4, 2,
			6, 3
		);
		
		index.setOpacity(0, 4, 0, 1);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(7, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 1,
			5, 2,
			6, 3
		), getSegments(index));
	}

	@Test
	public void setDataStartSameAsPrevNoRoomBeforeSegment() {
		OpacityIndex index = makeIndex(2, 7,
			2, 1,
			4, 2,
			5, 3
		);
		
		index.setOpacity(0, 4, 0, 1);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(7, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 1,
			5, 3
		), getSegments(index));
	}
	
	@Test
	public void setDataStartSameAsPrevRoomBeforeTop() {
		OpacityIndex index = makeIndex(2, 5,
			2, 1,
			4, 2
		);
		
		index.setOpacity(0, 4, 0, 1);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(5, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 1,
			5, 2
		), getSegments(index));
	}

	@Test
	public void setDataStartSameAsPrevNoRoomBeforeTop() {
		OpacityIndex index = makeIndex(2, 4,
			2, 1,
			4, 2
		);
		
		index.setOpacity(0, 4, 0, 1);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(4, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 1
		), getSegments(index));
	}

	@Test
	public void setDataStartSameAsBottomRoomBeforeNext() {
		OpacityIndex index = makeIndex(4, 7,
			4, 2,
			6, 3
		);
		
		index.setOpacity(0, 4, 0, 0);
		assertEquals(5, (int)index.getBottomBlockY(0, 0));
		assertEquals(7, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			5, 2,
			6, 3
		), getSegments(index));
	}

	@Test
	public void setDataStartSameAsBottomNoRoomBeforeNext() {
		OpacityIndex index = makeIndex(4, 7,
			4, 2,
			5, 3
		);
		
		index.setOpacity(0, 4, 0, 0);
		assertEquals(5, (int)index.getBottomBlockY(0, 0));
		assertEquals(7, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			5, 3
		), getSegments(index));
	}

	@Test
	public void setDataStartSameAsBottomRoomBeforeTop() {
		OpacityIndex index = makeIndex(4, 7,
			4, 2
		);
		
		index.setOpacity(0, 4, 0, 0);
		assertEquals(5, (int)index.getBottomBlockY(0, 0));
		assertEquals(7, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			5, 2
		), getSegments(index));
	}

	@Test
	public void setDataStartSameAsBottomNoRoomBeforeTop() {
		OpacityIndex index = makeIndex(4, 4,
			4, 2
		);
		
		index.setOpacity(0, 4, 0, 0);
		assertEquals(null, index.getBottomBlockY(0, 0));
		assertEquals(null, index.getTopBlockY(0, 0));
		assertEquals(null, getSegments(index));
	}

	@Test
	public void setDataStartSameAsNextRoomBeforeNext() {
		OpacityIndex index = makeIndex(2, 7,
			2, 1,
			4, 2,
			6, 3
		);
		
		index.setOpacity(0, 4, 0, 3);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(7, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 1,
			4, 3,
			5, 2,
			6, 3
		), getSegments(index));
	}

	@Test
	public void setDataStartSameAsNextNoRoomBeforeNext() {
		OpacityIndex index = makeIndex(2, 7,
			2, 1,
			4, 2,
			5, 3
		);
		
		index.setOpacity(0, 4, 0, 3);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(7, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 1,
			4, 3
		), getSegments(index));
	}

	@Test
	public void setDataStartSameAsTopRoomBeforeTop() {
		OpacityIndex index = makeIndex(2, 5,
			2, 1,
			4, 2
		);
		
		index.setOpacity(0, 4, 0, 0);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(5, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 1,
			4, 0,
			5, 2
		), getSegments(index));
	}

	@Test
	public void setDataStartSameAsTopNoRoomBeforeTop() {
		OpacityIndex index = makeIndex(2, 4,
			2, 1,
			4, 2
		);
		
		index.setOpacity(0, 4, 0, 0);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(3, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 1
		), getSegments(index));
	}

	@Test
	public void setDataStartSameAsBothNoRoomBeforeNext() {
		OpacityIndex index = makeIndex(2, 7,
			2, 1,
			4, 2,
			5, 1
		);
		
		index.setOpacity(0, 4, 0, 1);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(7, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 1
		), getSegments(index));
	}
	
	@Test
	public void setDataStartUniqueNoRoomAfter() {
		OpacityIndex index = makeIndex(2, 7,
			2, 1,
			4, 2,
			5, 1
		);
		
		index.setOpacity(0, 4, 0, 3);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(7, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 1,
			4, 3,
			5, 1
		), getSegments(index));
	}
	
	@Test
	public void setDataStartUniqueRoomAfter() {
		OpacityIndex index = makeIndex(2, 7,
			2, 1,
			4, 2,
			6, 1
		);
		
		index.setOpacity(0, 4, 0, 3);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(7, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 1,
			4, 3,
			5, 2,
			6, 1
		), getSegments(index));
	}
	
	@Test
	public void setDataNotStartSameAsNextNoRoomAfter() {
		OpacityIndex index = makeIndex(2, 7,
			2, 1,
			5, 2
		);
		
		index.setOpacity(0, 4, 0, 3);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(7, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 1,
			4, 3,
			5, 2
		), getSegments(index));
	}
	
	@Test
	public void setDataNotStartSameAsNextRoomAfter() {
		OpacityIndex index = makeIndex(2, 7,
			2, 1,
			6, 2
		);
		
		index.setOpacity(0, 4, 0, 3);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(7, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 1,
			4, 3,
			5, 1,
			6, 2
		), getSegments(index));
	}
	
	@Test
	public void setDataNotStartSameAsTopNoRoomAfter() {
		OpacityIndex index = makeIndex(2, 4,
			2, 1
		);
		
		index.setOpacity(0, 4, 0, 0);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(3, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 1
		), getSegments(index));
	}
	
	@Test
	public void setDataNotStartUniqueNoRoomAfter() {
		OpacityIndex index = makeIndex(2, 7,
			2, 1,
			5, 2
		);
		
		index.setOpacity(0, 4, 0, 3);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(7, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 1,
			4, 3,
			5, 2
		), getSegments(index));
	}
	
	@Test
	public void setDataNotStartUniqueRoomAfter() {
		OpacityIndex index = makeIndex(2, 7,
			2, 1,
			6, 2
		);
		
		index.setOpacity(0, 4, 0, 3);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(7, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 1,
			4, 3,
			5, 1,
			6, 2
		), getSegments(index));
	}
	
	@Test
	public void setTranslucentFromEmpty() {
		OpacityIndex index = new OpacityIndex();
		index.setOpacity(0, 10, 0, 5);
		assertEquals(10, (int)index.getBottomBlockY(0, 0));
		assertEquals(10, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			10, 5
		), getSegments(index));
	}
	
	@Test
	public void setTranslucentFromOpaqueBlock() {
		OpacityIndex index = makeIndex(2, 2);
		index.setOpacity(0, 2, 0, 5);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(2, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 5
		), getSegments(index));
	}
	
	@Test
	public void setTranslucentTopFromOpaqueRange() {
		OpacityIndex index = makeIndex(2, 6);
		index.setOpacity(0, 6, 0, 5);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(6, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 255,
			6, 5
		), getSegments(index));
	}
	
	@Test
	public void setTranslucentBottomFromOpaqueRange() {
		OpacityIndex index = makeIndex(2, 6);
		index.setOpacity(0, 2, 0, 5);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(6, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 5,
			3, 255
		), getSegments(index));
	}
	
	@Test
	public void setTranslucentMiddleFromOpaqueRange() {
		OpacityIndex index = makeIndex(2, 6);
		index.setOpacity(0, 4, 0, 5);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(6, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 255,
			4, 5,
			5, 255
		), getSegments(index));
	}
	
	@Test
	public void setTranslucentAboveOpaqueRange() {
		OpacityIndex index = makeIndex(2, 6);
		index.setOpacity(0, 12, 0, 5);
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		assertEquals(12, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			2, 255,
			7, 0,
			12, 5
		), getSegments(index));
	}
	
	@Test
	public void setTranslucentBelowOpaqueRange() {
		OpacityIndex index = makeIndex(2, 6);
		index.setOpacity(0, -2, 0, 5);
		assertEquals(-2, (int)index.getBottomBlockY(0, 0));
		assertEquals(6, (int)index.getTopBlockY(0, 0));
		assertEquals(Arrays.asList(
			-2, 5,
			-1, 0,
			2, 255
		), getSegments(index));
	}
        
	@Test
	public void setTransparentInOpaqueAndClear() {
		OpacityIndex index = new OpacityIndex();
		
		// place blocks
		index.setOpacity(0, 0, 0, 255);
		index.setOpacity(0, 1, 0, 255);
		index.setOpacity(0, 2, 0, 255);
		
		// check
		assertEquals(0, index.getOpacity(0, -1, 0));
		assertEquals(255, index.getOpacity(0, 0, 0));
		assertEquals(255, index.getOpacity(0, 1, 0));
		assertEquals(255, index.getOpacity(0, 2, 0));
		assertEquals(0, index.getOpacity(0, 3, 0));
		assertEquals(2, (int)index.getTopBlockY(0, 0));
		assertEquals(0, (int)index.getBottomBlockY(0, 0));
		
		// remove the middle one
		index.setOpacity(0, 1, 0, 0);
		
		assertEquals(0, index.getOpacity(0, -1, 0));
		assertEquals(255, index.getOpacity(0, 0, 0));
		assertEquals(0, index.getOpacity(0, 1, 0));
		assertEquals(255, index.getOpacity(0, 2, 0));
		assertEquals(0, index.getOpacity(0, 3, 0));
		assertEquals(2, (int)index.getTopBlockY(0, 0));
		assertEquals(0, (int)index.getBottomBlockY(0, 0));
		
		// remove the bottom one
		index.setOpacity(0, 0, 0, 0);
		
		assertEquals(0, index.getOpacity(0, -1, 0));
		assertEquals(0, index.getOpacity(0, 0, 0));
		assertEquals(0, index.getOpacity(0, 1, 0));
		assertEquals(255, index.getOpacity(0, 2, 0));
		assertEquals(0, index.getOpacity(0, 3, 0));
		assertEquals(2, (int)index.getTopBlockY(0, 0));
		assertEquals(2, (int)index.getBottomBlockY(0, 0));
		
		// remove the top one
		index.setOpacity(0, 2, 0, 0);
		
		assertEquals(0, index.getOpacity(0, -1, 0));
		assertEquals(0, index.getOpacity(0, 0, 0));
		assertEquals(0, index.getOpacity(0, 1, 0));
		assertEquals(0, index.getOpacity(0, 2, 0));
		assertEquals(0, index.getOpacity(0, 3, 0));
		assertEquals(null, index.getTopBlockY(0, 0));
		assertEquals(null, index.getBottomBlockY(0, 0));
	}
	
	private OpacityIndex makeIndex(int ymin, int ymax, int ... segments) {
		OpacityIndex index = new OpacityIndex();
		
		// pack the segments
		int[] packedSegments = null;
		if (segments.length > 0) {
			packedSegments = new int[segments.length/2];
			for (int i=0; i<segments.length/2; i++) {
				packedSegments[i] = Bits.packSignedToInt(segments[i*2+0], 24, 0) | Bits.packUnsignedToInt(segments[i*2+1], 8, 24);
			}
		}
		
		set(index, ymin, ymax, packedSegments);
		return index;
	}
	
	private void set(OpacityIndex index, int ymin, int ymax, int[] segments) {
		try {
			YminField.set(index, new int[] { ymin });
			YmaxField.set(index, new int[] { ymax });
			SegmentsField.set(index, new int[][] { segments });
		} catch (IllegalArgumentException | IllegalAccessException ex) {
			throw new Error(ex);
		}
	}
	
	private List<Integer> getSegments(OpacityIndex index) {
		try {
			int[] packedSegments = ((int[][])SegmentsField.get(index))[0];
			if (packedSegments == null) {
				return null;
			}
			
			final int NoneSegment = 0x7fffff;
			
			// unpack the segments
			List<Integer> segments = Lists.newArrayList();
			for (int i=0; i<packedSegments.length && packedSegments[i] != NoneSegment; i++) {
				segments.add(Bits.unpackSigned(packedSegments[i], 24, 0));
				segments.add(Bits.unpackUnsigned(packedSegments[i], 8, 24));
			}
			return segments;
			
		} catch (IllegalArgumentException | IllegalAccessException ex) {
			throw new Error(ex);
		}
	}
}
