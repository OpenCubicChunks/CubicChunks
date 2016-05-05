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
package cubicchunks.world;

import cubicchunks.util.Bits;

import java.io.*;

public class OpacityIndex implements IOpacityIndex {

	private static int None = Integer.MIN_VALUE;

	// it's hard to find a segment value we can't use
	// this is the best I can do
	// a 1-block segment at the very top of the world with opacity zero
	// seems a bit redundant =P
	private static int NoneSegment = packSegment(0x7fffff, 0);

	private int[] m_ymin;
	private int[] m_ymax;
	/**
	 * The array of segments for each x/z position stores Y positions at which the opaque/transparent state changes.
	 */
	private int[][] m_segments;

	private int heightMapLowest = None;

	private int m_hash;
	private boolean m_needsHash;

	public OpacityIndex() {
		m_ymin = new int[16 * 16];
		m_ymax = new int[16 * 16];
		m_segments = new int[16 * 16][];

		// init to empty
		for (int i = 0; i < 16 * 16; i++) {
			m_ymin[i] = None;
			m_ymax[i] = None;
		}

		m_hash = 0;
		m_needsHash = true;
	}

	@Override
	public Integer getTopBlockYBelow(int localX, int localZ, int blockY) {
		int i = getIndex(localX, localZ);
		if (blockY > m_ymax[i]) {
			return getTopBlockY(localX, localZ);
		}
		//below or at the minimum height, so no blocks below
		if (blockY <= m_ymin[i]) {
			return null;
		}
		int[] segments = m_segments[i];
		if (segments == null) {
			//there are no opacity changes, it's solid opaque column from ymin to ymax
			//and blockY is between ymin and ymax, so the next opaque block below blockY
			//is blockY - 1.
			return blockY - 1;
		}
		int mini = 0;
		int maxi = getLastSegmentIndex(segments);

		//binary search
		while (mini <= maxi) {
			int midi = (mini + maxi) >>> 1;
			int midPos = unpackPos(segments[midi]);

			if (midPos < blockY) {
				mini = midi + 1;
			} else if (midPos > blockY) {
				maxi = midi - 1;
			} else {
				// hit a segment start exactly
				mini = midi + 1;
				break;
			}
		}
		// didn't hit a segment start, mini is the correct answer + 1
		assert (mini > 0) : String.format("can't find %d in %s", blockY, dump(i));
		//now mini - 1 is index that contains blockY
		//so subtract 1
		int segmentIndex = mini - 1;
		int blockYSegment = segments[segmentIndex];
		int blockYSegmentOpacity = unpackOpacity(blockYSegment);
		/*
		 [X]
		 ---
		 [X] <-- if it's anywhere here or above in this segment - there is opaque block just below
		 ---
		 [X] <-- blockY can't be here, excluded by checking ymin
		 ---
		 [ ]
		  ^ going up
		 */
		if (segmentIndex == 0) {
			assert blockYSegmentOpacity != 0 : "The bottom opacity segment is transparent!";
			return blockY - 1;
		}
		//there is a segment below current segment
		int belowYSegment = segments[segmentIndex - 1];
		int belowYSegmentHeight = unpackPos(belowYSegment);

		int blockYSegmentHeight = unpackPos(blockYSegment);
		/*
		 If code reaches this point, it means that there is segment below blockY segment.
		 Now there may be a few cases:
		  * segment with blockY is fully transparent:
		    '-> so the segment below must be opaque.
		        Next opaque block Y below is the top of the next segment (bottom of blockY segment - 1)
		  * segment with blockY is opaque:
		    '-> so there are a few cases possible
		    * blockY is not at the bottom of it
		      '-> next opaque block below is at blockY - 1
		    * blockY is at the bottom of it
		      '-> the next opaque block must be in some segment below.
		          The segment below must be transparent, and the segment below it is opaque.
		          So the block we want if 1 block below the bottom of the segment below blockYSegment
		 */
		if(blockYSegmentOpacity == 0) {
			return blockYSegmentHeight - 1;
		}
		if(blockY != blockYSegmentHeight) {
			return blockY - 1;
		}
		return belowYSegmentHeight - 1;
	}

	@Override
	public boolean isOpaque(int localX, int blockY, int localZ) {

		// are we out of range?
		int i = getIndex(localX, localZ);
		if (blockY > m_ymax[i] || blockY < m_ymin[i]) {
			return false;
		}

		// are there segments for this column?
		int[] segments = m_segments[i];
		if (segments == null) {
			// this column is black or white
			// there are no shades of grey =P
			return true;
		}

		// scan the shades of grey with binary search
		int mini = 0;
		int maxi = getLastSegmentIndex(segments);
		while (mini <= maxi) {
			int midi = (mini + maxi) >>> 1;
			int midPos = unpackPos(segments[midi]);

			if (midPos < blockY) {
				mini = midi + 1;
			} else if (midPos > blockY) {
				maxi = midi - 1;
			} else {
				// hit a segment start exactly
				return unpackOpacity(segments[midi]) != 0;
			}
		}

		// didn't hit a segment start, mini is the correct answer + 1
		assert (mini > 0) : String.format("can't find %d in %s", blockY, dump(i));

		return unpackOpacity(segments[mini - 1]) != 0;
	}

	@Override
	public void onOpacityChange(int localX, int blockY, int localZ, int opacity) {
		// what's the range?
		int xzIndex = getIndex(localX, localZ);
		boolean isOpaque = opacity != 0;
		// try to stay in no-segments mode as long as we can, this is the simple case
		if (m_segments[xzIndex] == null) {
			setNoSegments(xzIndex, blockY, isOpaque);
		} else {
			setOpacityWithSegments(xzIndex, blockY, isOpaque);
		}
		heightMapLowest = None;
		m_needsHash = true;
	}

	private void setNoSegments(int xzIndex, int blockY, boolean isOpaque) {
		if (isOpaque) {
			setNoSegmentsOpaque(xzIndex, blockY);
		} else {
			setNoSegmentsTransparent(xzIndex, blockY);
		}
	}

	private void setNoSegmentsOpaque(int xzIndex, int blockY) {
		// something from nothing?
		if (m_ymin[xzIndex] == None && m_ymax[xzIndex] == None) {
			m_ymin[xzIndex] = blockY;
			m_ymax[xzIndex] = blockY;
			return;
		}

		// extending the range?
		if (blockY == m_ymin[xzIndex] - 1) {
			m_ymin[xzIndex]--;
			return;
		} else if (blockY == m_ymax[xzIndex] + 1) {
			m_ymax[xzIndex]++;
			return;
		}

		// making a new section

		//more than one block above ymax?
		if (blockY > m_ymax[xzIndex] + 1) {
			/*
			 A visualization of what happens:

			 X - already opaque, within min-max
			 # - newly set opaque

			 [ ]
			 --- --> no segment here, new_ymax=blockY
			 [#] (block at blockY)
			 --- --> segment=2, height=blockY, isOpaque=1
			 [ ]
			 ---
			 [ ]
			 --- --> segment=1, height=old_ymax + 1, isOpaque=0
			 [X] (block at old_ymax)
			 ---
			 [X]
			 ---
			 [X] (block at ymin)
			 --- --> segment=0, height=ymin, isOpaque=1
			 [ ]
			  ^ going up from there
			 */
			m_segments[xzIndex] = new int[]{
					packSegment(m_ymin[xzIndex], 1),
					packSegment(m_ymax[xzIndex] + 1, 0),
					packSegment(blockY, 1)
			};
			m_ymax[xzIndex] = blockY;
			return;
			//more than one block below ymin?
		} else if (blockY < m_ymin[xzIndex] - 1) {
			/*
			 X - already opaque, within min-max
			 # - newly set opaque

			 ---
			 [ ]
			 --- --> no segment, limited by ymax
			 [X] (block at ymax)
			 ---
			 [X]
			 ---
			 [X] (block at old_ymin)
			 --- --> segment=2, height=old_ymin, isOpaque=1
			 [ ]
			 ---
			 [ ]
			 --- --> segment=1, height=blockY + 1, isOpaque=0
			 [#] (block at blockY)
			 --- --> segment=0, height=blockY, isOpaque=1
			 [ ]
			  ^ going up from there
			 */
			m_segments[xzIndex] = new int[]{
					packSegment(blockY, 1),
					packSegment(blockY + 1, 0),
					packSegment(m_ymin[xzIndex], 1)
			};
			m_ymin[xzIndex] = blockY;
			return;
		}

		// must already be in range
		assert (blockY >= m_ymin[xzIndex] && blockY <= m_ymax[xzIndex]);
	}

	private void setNoSegmentsTransparent(int i, int blockY) {
		// nothing into nothing?
		if (m_ymin[i] == None && m_ymax[i] == None) {
			return;
		}
		assert !(m_ymin[i] == None || m_ymax[i] == None) : "Only one of ymin and ymax is None! This is not possible";

		// only one block left?
		if (m_ymax[i] == m_ymin[i]) {

			// something into nothing?
			if (blockY == m_ymin[i]) {
				m_ymin[i] = None;
				m_ymax[i] = None;
			}

			// if setting to transparent somewhere else - nothing changes
			return;
		}

		// out of range?
		if (blockY < m_ymin[i] || blockY > m_ymax[i]) {
			return;
		}

		// shrinking the range?
		if (blockY == m_ymin[i]) {
			m_ymin[i]++;
			return;
		} else if (blockY == m_ymax[i]) {
			m_ymax[i]--;
			return;
		}

		// we must be bisecting the range, need to make segments
		assert (blockY > m_ymin[i] && blockY < m_ymax[i]) : String.format("blockY outside of ymin/ymax range: %d -> [%d,%d]", blockY, m_ymin[i], m_ymax[i]);
		/*
		 Example:
		 ---
		 [ ]
		 --- --> no segment, limited by ymax
		 [X] (block at ymax)
		 ---
		 [X]
		 --- --> segment=2, height=blockY + 1, isOpaque=1
		 [-] <--removing this, at y=blockY
		 --- --> segment=1, height=blockY, isOpaque=0
		 [X]
		 ---
		 [X] (block ay ymin)
		 --- --> segment=0, height=ymin, isOpaque=1
		 [ ]
		  ^ going up
		*/
		m_segments[i] = new int[]{
				packSegment(m_ymin[i], 1),
				packSegment(blockY, 0),
				packSegment(blockY + 1, 1)
		};
	}

	private void setOpacityWithSegments(int i, int blockY, boolean isOpaque) {
		// binary search to find the insertion point
		int[] segments = m_segments[i];
		int minj = 0;
		int maxj = getLastSegmentIndex(segments);
		while (minj <= maxj) {
			int midj = (minj + maxj) >>> 1;
			int midPos = unpackPos(segments[midj]);

			if (midPos < blockY) {
				minj = midj + 1;
			} else if (midPos > blockY) {
				maxj = midj - 1;
			} else {
				minj = midj + 1;
				break;
			}
		}

		// minj-1 is the containing segment, or -1 if we're off the bottom
		int j = minj - 1;
		if (j < 0) {
			setOpacityWithSegmentsBelowBottom(i, blockY, isOpaque);
		} else if (blockY > m_ymax[i]) {
			setOpacityWithSegmentsAboveTop(i, blockY, isOpaque);
		} else {
			// j is the containing segment, blockY may be at the start
			setOpacityWithSegmentsFor(i, blockY, j, isOpaque);
		}
	}

	private void setOpacityWithSegmentsBelowBottom(int xzIndex, int blockY, boolean isOpaque) {
		// will the opacity even change?
		if (!isOpaque) {
			return;
		}

		int[] segments = m_segments[xzIndex];

		boolean extendsBottomSegmentByOne = blockY == m_ymin[xzIndex] - 1;
		if (extendsBottomSegmentByOne) {
			/*
			 ---
			 [X]
			 ---
			 [X]
			 --- <-- the current bottom segment starts here
			 [#] <-- inserting here
			 --- <-- new bottom segment start
			 [ ]
			  ^ going up
			 */
			assert unpackOpacity(segments[0]) == 1 : "The bottom segment is transparent!";
			moveSegmentStartDownAndUpdateMinY(xzIndex, 0);
		} else {
			/*
			 ---
			 [X]
			 ---
			 [X]
			 --- <-- the current bottom segment starts here, now segment 2
			 [ ]
			 --- <-- new segment 1, height=blockY + 1, isOpaque=0
			 [#] <-- inserting here
			 --- <-- new segment 0, height=blockY, isOpaque=1
			 [ ]
			  ^ going up
			 */
			int segment0 = packSegment(blockY, 1);
			int segment1 = packSegment(blockY + 1, 0);
			insertSegmentsBelow(xzIndex, 0, segment0, segment1);
			m_ymin[xzIndex] = blockY;
		}
	}

	private void setOpacityWithSegmentsAboveTop(int xzIndex, int blockY, boolean isOpaque) {
		// will the opacity even change?
		if (!isOpaque) {
			return;
		}

		int[] segments = m_segments[xzIndex];
		int lastIndex = getLastSegmentIndex(segments);

		boolean extendsTopSegmentByOne = blockY == m_ymax[xzIndex] + 1;
		if (extendsTopSegmentByOne) {
			/*
			 [ ]
			 --- <-- new ymax
			 [#] <-- inserting here
			 --- <-- current top segment ends here, limited by ymax
			 [X]
			 ---
			 [X]
			  ^ going up
			 */
			assert unpackOpacity(segments[lastIndex]) == 1 : "The top segment is transparent!";
			m_ymax[xzIndex] = blockY;
		} else {
			/*
			 [ ]
			 --- <-- limited by newMaxY
			 [#] <-- inserting here
			 --- <-- new segment [previousLastSegment+2], height=blockY, isOpaque=1
			 [ ] <-- possibly many blocks here
			 ---
			 [ ] <-- block at prevMaxY+1
			 --- <-- previously limited by ymax, add segment=[previousLastSegment+1], height=prevMaxY+1, isOpaque=0
			 [X] <-- block at prevMaxY
			 ---
			 [X]
			  ^ going up
			 */
			int segmentPrevLastPlus1 = packSegment(m_ymax[xzIndex] + 1, 0);
			int segmentPrevLastPlus2 = packSegment(blockY, 1);
			//insert below the segment above the last segment, so above the last segment
			insertSegmentsBelow(xzIndex, lastIndex + 1, segmentPrevLastPlus1, segmentPrevLastPlus2);
			m_ymax[xzIndex] = blockY;
		}
	}

	private void setOpacityWithSegmentsFor(int xzIndex, int blockY, int segmentIndexWithBlockY, boolean isOpaque) {
		int[] segments = m_segments[xzIndex];
		int isOpaqueInt = isOpaque ? 1 : 0;

		int segmentWithBlockY = segments[segmentIndexWithBlockY];

		//does it even change anything?
		if (unpackOpacity(segmentWithBlockY) == isOpaqueInt) {
			return;
		}

		int segmentBottom = unpackPos(segmentWithBlockY);
		int segmentTop = getSegmentTopBlockY(xzIndex, segmentIndexWithBlockY);

		if (segmentTop == segmentBottom) {
			assert segmentBottom == blockY;
			negateOneBlockSegment(xzIndex, segmentIndexWithBlockY);
			return;
		}
		/*
		 3 possible cases:
		  * change at the top of segment
		  * change at the bottom of segment
		  * change in the middle of segment
		*/
		int lastSegment = getLastSegmentIndex(segments);
		if (blockY == segmentTop) {
			//if it's the top of the top segment - just change ymax
			if (segmentIndexWithBlockY == lastSegment) {
				assert unpackOpacity(segments[lastSegment]) == 1 : "The top segment is transparent!";
				m_ymax[xzIndex]--;
				return;
			}
			/*
			 [-]
			 ---
			 [#] <-- changing this from [X] to [-]
			 ---
			 [X] <-- segmentWithBlockY
			  ^ going up
			 */
			moveSegmentStartDownAndUpdateMinY(xzIndex, segmentIndexWithBlockY + 1);
			return;
		}
		if (blockY == segmentBottom) {
			moveSegmentStartUpAndUpdateMinY(xzIndex, segmentIndexWithBlockY);
			return;
		}
		/*
		 ---
		 [X]
		 ---
		 [X]
		 --- <-- insert this (newSegment2), height=blockY + 1, opacity=!isOpaque
		 [#] <-- changing this
		 --- <-- insert this (newSegment1), height=blockY, opacity=isOpaque
		 [X]
		 ---
		 [X]
		 --- <-- segmentWithBlockY
		 [-]
		 */
		int newSegment1 = packSegment(blockY, isOpaqueInt);
		int newSegment2 = packSegment(blockY + 1, 1 - isOpaqueInt);
		insertSegmentsBelow(xzIndex, segmentIndexWithBlockY + 1, newSegment1, newSegment2);
	}

	private void negateOneBlockSegment(int xzIndex, int segmentIndexWithBlockY) {

		int[] segments = m_segments[xzIndex];
		int lastSegmentIndex = getLastSegmentIndex(segments);

		assert lastSegmentIndex >= 2 : "Less than 3 segments in array!";
		if (segmentIndexWithBlockY == lastSegmentIndex) {

			assert unpackOpacity(segments[segmentIndexWithBlockY]) == 1 : "The top segment is transparent!";
			//the top segment must be opaque, so we set it to transparent
			//and the segment below it is also transparent.
			//set both of them to None and decrease maxY
			int segmentBelow = segments[segmentIndexWithBlockY - 1];
			m_ymax[xzIndex] = unpackPos(segmentBelow) - 1;
			if (segmentIndexWithBlockY == 2) {
				//after removing top 2 segments we will be left with 1 segment
				//remove them entirely to guarantee at least 3 segments and use min/maxY
				m_segments[xzIndex] = null;
				return;
			}
			segments[segmentIndexWithBlockY] = NoneSegment;
			segments[segmentIndexWithBlockY - 1] = NoneSegment;
			return;
		}
		if (segmentIndexWithBlockY == 0) {
			assert unpackOpacity(segments[segmentIndexWithBlockY]) == 1 : "The top segment is transparent!";
			//same logic as for top segment applies
			int segmentAbove = segments[1];
			m_ymin[xzIndex] = unpackPos(segments[2]);
			if (lastSegmentIndex == 2) {
				m_segments[xzIndex] = null;
				return;
			}
			removeTwoSegments(xzIndex, 0);
			return;
		}
		/*
		 The situation:
		 # - opacity to set
		 - - opposite opacity

		 ---
		 [#]
		 ---
		 [#]
		 --- <-- old segment=segmentIndexWithBlockY+1, height=blockY+1
		 [-]
		 --- <-- old segment=segmentIndexWithBlockY, height=blockY, opacity=(-)
		 [#]
		 ---
		 [#]
		  ^ going up

		  Since this is not the top/bottom segment - we can remove it.
		  And to avoid 2 identical segments in a row - remove the one above too
		 */
		removeTwoSegments(xzIndex, segmentIndexWithBlockY);
		//but in case after the removal there are less than 3 segments
		//remove them entirely and rely only on min/maxY
		if (lastSegmentIndex == 2) {
			m_segments[xzIndex] = null;
			return;
		}
	}

	private void moveSegmentStartUpAndUpdateMinY(int xzIndex, int segmentIndex) {

		int segment = m_segments[xzIndex][segmentIndex];
		int pos = unpackPos(segment);
		int opacity = unpackOpacity(segment);

		// move the segment
		m_segments[xzIndex][segmentIndex] = packSegment(pos + 1, opacity);

		// move the bottom if needed
		if (segmentIndex == 0) {
			m_ymin[xzIndex]++;
		}
	}

	private void moveSegmentStartDownAndUpdateMinY(int xzIndex, int segmentIndex) {

		int segment = m_segments[xzIndex][segmentIndex];
		int pos = unpackPos(segment);
		int opacity = unpackOpacity(segment);

		// move the segment
		m_segments[xzIndex][segmentIndex] = packSegment(pos - 1, opacity);

		// move the bottom if needed
		if (segmentIndex == 0) {
			m_ymin[xzIndex]--;
		}
	}

	private void removeTwoSegments(int xzIndex, int firstSegmentToRemove) {

		int[] segments = m_segments[xzIndex];
		int jmax = getLastSegmentIndex(segments);

		// remove the segment
		for (int n = firstSegmentToRemove; n < jmax - 1; n++) {
			segments[n] = segments[n + 2];
		}
		segments[jmax] = NoneSegment;
		segments[jmax - 1] = NoneSegment;

		if (segments[0] == NoneSegment) {
			m_segments[xzIndex] = null;
		}
	}

	//is theIndex = lastSegmentIndex+1, it will be inserted after last segment
	private void insertSegmentsBelow(int xzIndex, int theIndex, int... newSegments) {
		int lastIndex = getLastSegmentIndex(m_segments[xzIndex]);
		int expandSize = newSegments.length;
		//will it fit in current array?
		if (m_segments[xzIndex].length >= lastIndex + expandSize) {
			//shift all segments up
			for (int i = lastIndex; i >= theIndex; i--) {
				m_segments[xzIndex][i + expandSize] = m_segments[xzIndex][i];
			}
			for (int i = 0; i < expandSize; i++) {
				m_segments[xzIndex][theIndex + i] = newSegments[i];
			}
		} else {
			//need to expand the array
			int[] newSegmentArr = new int[(lastIndex + 1) + expandSize];
			int newArrIndex = 0;
			int oldArrIndex = 0;
			//copy all index up to before theIndex
			for (int i = 0; i < theIndex; i++) {
				newSegmentArr[newArrIndex] = m_segments[xzIndex][oldArrIndex];
				newArrIndex++;
				oldArrIndex++;
			}
			//copy new elements
			for (int i = 0; i < expandSize; i++) {
				newSegmentArr[newArrIndex] = newSegments[i];
				newArrIndex++;
			}
			//copy everything else
			while (newArrIndex < newSegmentArr.length) {
				newSegmentArr[newArrIndex] = m_segments[xzIndex][oldArrIndex];
				newArrIndex++;
				oldArrIndex++;
			}
			m_segments[xzIndex] = newSegmentArr;
		}
	}

	private int getSegmentTopBlockY(int xzIndex, int segmentIndex) {
		int[] segments = m_segments[xzIndex];
		//if it's the last segment in the array, or the one above is NoneSegment
		if (segments.length - 1 == segmentIndex || segments[segmentIndex + 1] == NoneSegment) {
			return m_ymax[xzIndex];
		}
		return unpackPos(segments[segmentIndex + 1]) - 1;
	}

	@Override
	public Integer getTopBlockY(int localX, int localZ) {
		int i = getIndex(localX, localZ);
		int pos = m_ymax[i];
		if (pos == None) {
			return null;
		}
		return pos;
	}

	@Override
	public Integer getBottomBlockY(int localX, int localZ) {
		int i = getIndex(localX, localZ);
		int pos = m_ymin[i];
		if (pos == None) {
			return null;
		}
		return pos;
	}

	public int getLowestTopBlockY() {
		if (heightMapLowest == None) {
			heightMapLowest = Integer.MAX_VALUE;
			for (int i = 0; i < m_ymax.length; i++) {
				if (m_ymax[i] < heightMapLowest) {
					heightMapLowest = m_ymax[i];
				}
			}
		}
		return heightMapLowest;
	}

	public byte[] getData() {
		try {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(buf);
			writeData(out);
			out.close();
			return buf.toByteArray();
		} catch (IOException ex) {
			throw new Error(ex);
		}
	}

	public byte[] getDataForClient() {
		try {
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(buf);

			for (int v : m_ymin) {
				out.writeInt(v);
			}
			for (int v : m_ymax) {
				out.writeInt(v);
			}

			out.close();
			return buf.toByteArray();
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	public void readData(byte[] data) {
		try {
			ByteArrayInputStream buf = new ByteArrayInputStream(data);
			DataInputStream in = new DataInputStream(buf);
			readData(in);
			in.close();
		} catch (IOException ex) {
			throw new Error(ex);
		}
	}

	public void readData(DataInputStream in)
			throws IOException {
		for (int i = 0; i < m_segments.length; i++) {
			m_ymin[i] = in.readInt();
			m_ymax[i] = in.readInt();
			int[] segments = new int[in.readUnsignedShort()];
			if (segments.length == 0) {
				continue;
			}
			for (int j = 0; j < segments.length; j++) {
				segments[j] = in.readInt();
			}
			m_segments[i] = segments;
		}
	}

	public void writeData(DataOutputStream out)
			throws IOException {
		for (int i = 0; i < m_segments.length; i++) {
			out.writeInt(m_ymin[i]);
			out.writeInt(m_ymax[i]);
			int[] segments = m_segments[i];
			if (segments == null || segments.length == 0) {
				out.writeShort(0);
			} else {
				int lastSegmentIndex = getLastSegmentIndex(segments);
				out.writeShort(lastSegmentIndex + 1);
				for (int j = 0; j <= lastSegmentIndex; j++) {
					out.writeInt(segments[j]);
				}
			}
		}
	}

	public String dump(int localX, int localZ) {
		return dump(getIndex(localX, localZ));
	}

	private String dump(int i) {
		StringBuilder buf = new StringBuilder();
		buf.append("range=[");
		buf.append(m_ymin[i]);
		buf.append(",");
		buf.append(m_ymax[i]);
		buf.append("], segments(p,o)=");
		if (m_segments[i] != null) {
			for (int packed : m_segments[i]) {
				int pos = unpackPos(packed);
				int opacity = unpackOpacity(packed);
				buf.append("(");
				buf.append(pos);
				buf.append(",");
				buf.append(opacity);
				buf.append(")");
			}
		}
		return buf.toString();
	}

	private static int getIndex(int localX, int localZ) {
		return (localZ << 4) | localX;
	}

	private static int packSegment(int pos, int opacity) {
		return Bits.packUnsignedToInt(opacity, 8, 24) | Bits.packSignedToInt(pos, 24, 0);
	}

	private static int unpackOpacity(int packed) {
		return Bits.unpackUnsigned(packed, 8, 24);
	}

	private static int unpackPos(int packed) {
		return Bits.unpackSigned(packed, 24, 0);
	}

	private static int getLastSegmentIndex(int[] segments) {
		for (int i = segments.length - 1; i >= 0; i--) {
			if (segments[i] != NoneSegment) {
				return i;
			}
		}
		throw new Error("Invalid segments state");
	}

	@Override
	public int hashCode() {
		if (m_needsHash) {
			m_hash = computeHash();
			m_needsHash = false;
		}
		return m_hash;
	}

	private int computeHash() {
		final int MyFavoritePrime = 37;
		int hash = 1;
		for (int i = 0; i < m_segments.length; i++) {
			hash *= MyFavoritePrime;
			hash += m_ymin[i];
			hash *= MyFavoritePrime;
			hash += m_ymax[i];
			if (m_segments[i] == null) {
				hash *= MyFavoritePrime;
			} else {
				for (int n : m_segments[i]) {
					hash *= MyFavoritePrime;
					hash += n;
				}
			}
		}
		return hash;
	}
}
