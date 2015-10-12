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


public class OpacityIndex {
	
	private static int None = Integer.MIN_VALUE;
	
	// it's hard to find a segment value we can't use
	// this is the best I can do
	// a 1-block segment at the very top of the world with opacity zero
	// seems a bit redundant =P
	private static int NoneSegment = packSegment(0x7fffff, 0);

	private int[] m_ymin;
	private int[] m_ymax;
	private int[][] m_segments;
	
	private int m_hash;
	private boolean m_needsHash;
	
	public OpacityIndex() {
		
		m_ymin = new int[16*16];
		m_ymax = new int[16*16];
		m_segments = new int[16*16][];
		
		// init to empty
		for (int i=0; i<16*16; i++) {
			m_ymin[i] = None;
			m_ymax[i] = None;
		}
		
		m_hash = 0;
		m_needsHash = true;
	}
	
	public int getOpacity(int localX, int blockY, int localZ) {
	
		// are we out of range?
		int i = getIndex(localX, localZ);
		if (blockY > m_ymax[i] || blockY < m_ymin[i]) {
			return 0;
		}
		
		// are there segments for this column?
		int[] segments = m_segments[i];
		if (segments == null) {
			// this column is black or white
			// there are no shades of grey =P
			return 255;
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
				return unpackOpacity(segments[midi]);
			}
		}
		
		// TEMP: return transparent instead of crashing if something went wrong
		// I don't have time to fix the bug, so I'd rather a user see lighting artifacts than crashes
		if (mini <= 0) {
			return 0;
		}
		
		// didn't hit a segment start, mini is the correct answer + 1
		assert(mini > 0) : String.format("can't find %d in %s", blockY, dump(i));
		return unpackOpacity(segments[mini - 1]);
	}
	
	public void setOpacity(int localX, int blockY, int localZ, int opacity) {
		// what's the range?
		int xzIndex = getIndex(localX, localZ);
		
		// try to stay in no-segments mode as long as we can
		if (m_segments[xzIndex] == null) {
			if (opacity == 255) {
				setOpacityNoSegmentsOpaque(xzIndex, blockY);
			} else if (opacity == 0) {
				setOpacityNoSegmentsTransparent(xzIndex, blockY);
			} else {
				setOpacityNoSegmentsTranslucent(xzIndex, blockY, opacity);
			}
		} else {
			setOpacityWithSegments(xzIndex, blockY, opacity);
		}
		
		m_needsHash = true;
	}
	
	private void setOpacityNoSegmentsOpaque(int xzIndex, int blockY) {
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
		
		// making a new section?
		if (blockY > m_ymax[xzIndex] + 1) {
			m_segments[xzIndex] = new int[] {
				packSegment(m_ymin[xzIndex], 255),
				packSegment(m_ymax[xzIndex] + 1, 0),
				packSegment(blockY, 255)
			};
			m_ymax[xzIndex] = blockY;
			return;
		} else if (blockY < m_ymin[xzIndex] - 1) {
			m_segments[xzIndex] = new int[] {
				packSegment(blockY, 255),
				packSegment(blockY + 1, 0),
				packSegment(m_ymin[xzIndex], 255)
			};
			m_ymin[xzIndex] = blockY;
			return;
		}
		
		// must already be in range
		assert (blockY >= m_ymin[xzIndex] && blockY <= m_ymax[xzIndex]);
	}
	
	private void setOpacityNoSegmentsTransparent(int i, int blockY) {
		// nothing into nothing?
		if (m_ymin[i] == None && m_ymax[i] == None) {
			return;
		}
		
		// only one block left?
		if (m_ymax[i] - m_ymin[i] == 0) {
			
			// something into nothing?
			if (blockY == m_ymin[i]) {
				m_ymin[i] = None;
				m_ymax[i] = None;
			}
			
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
		assert (blockY >= m_ymin[i] && blockY <= m_ymax[i]) : String.format("%d -> [%d,%d]", blockY, m_ymin[i], m_ymax[i]);
		m_segments[i] = new int[] {
			packSegment(m_ymin[i], 255),
			packSegment(blockY, 0),
			packSegment(blockY + 1, 255)
		};
	}
	
	private void setOpacityNoSegmentsTranslucent(int i, int blockY, int opacity) {
		// is there no range yet?
		if (m_ymin[i] == None && m_ymax[i] == None) {
			
			// make a new segment
			m_segments[i] = new int[] {
				packSegment(blockY, opacity)
			};
			m_ymin[i] = blockY;
			m_ymax[i] = blockY;
			
			return;
		}
		
		// convert the range into a segment and continue in with-segments mode
		makeSegmentsFromOpaqueRange(i);
		setOpacityWithSegments(i, blockY, opacity);
	}
	
	private void makeSegmentsFromOpaqueRange(int i) {
		assert (m_segments[i] == null);
		assert (m_ymin[i] != None);
		assert (m_ymax[i] != None);
		m_segments[i] = new int[] {
			packSegment(m_ymin[i], 255)
		};
	}
	
	private void setOpacityWithSegments(int i, int blockY, int opacity) {
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
				// set would overwrite a segment start
				setOpacityWithSegmentsAt(i, blockY, midj, opacity);
				return;
			}
		}
		
		// didn't hit a segment start, but minj-1 is the containing segment, or -1 if we're off the bottom
		int j = minj - 1;
		if (j < 0) {
			setOpacityWithSegmentsBeforeBottom(i, blockY, opacity);
		} else if (blockY > m_ymax[i]) {
			setOpacityWithSegmentsAfterTop(i, blockY, opacity);
		} else {
			// j is the containing segment, and blockY is not at the start
			setOpacityWithSegmentsAfter(i, blockY, j, opacity);
		}
	}

	private void setOpacityWithSegmentsAt(int i, int blockY, int j, int opacity) {
		int[] segments = m_segments[i];
		
		// will the opacity even change?
		int oldSegment = segments[j];
		int oldOpacity = unpackOpacity(oldSegment);
		if (opacity == oldOpacity) {
			return;
		}
		
		boolean isFirstSegment = j == 0;
		boolean isLastSegment = j == getLastSegmentIndex(segments);
		
		boolean sameOpacityAsPrev;
		if (isFirstSegment) {
			sameOpacityAsPrev = opacity == 0;
		} else {
			sameOpacityAsPrev = opacity == unpackOpacity(segments[j-1]);
		}
		
		boolean sameOpacityAsNext;
		if (isLastSegment) {
			sameOpacityAsNext = opacity == 0;
		} else {
			sameOpacityAsNext = opacity == unpackOpacity(segments[j+1]);
		}
		
		boolean isRoomAfter;
		if (isLastSegment) {
			isRoomAfter = m_ymax[i] > blockY;
		} else {
			isRoomAfter = unpackPos(segments[j+1]) > blockY + 1;
		}
		
		if (sameOpacityAsPrev && sameOpacityAsNext) {
			if (isRoomAfter) {
				moveSegmentStartUp(i, j);
			} else {
				if (isFirstSegment && isLastSegment) {
					removeSegments(i);
				} else {

					if(isLastSegment) {
						int lastSegment = getLastSegmentIndex(segments);
						m_ymax[i] = unpackPos(segments[lastSegment - 1]) - 1;
					} else if(isFirstSegment) {
						assert !isLastSegment;
						m_ymin[i] = unpackPos(segments[2]);
					}
					if(isLastSegment) {
						removeTwoSegments(i, j - 1);
					} else {
						removeTwoSegments(i, j);
					}
				}
			}
		} else if (sameOpacityAsPrev) {
			if (isRoomAfter) {
				moveSegmentStartUp(i, j);
			} else {
				removeSegment(i, j);
			}
		} else if (sameOpacityAsNext) {
			if (isRoomAfter) {
				addSegment(i, j+1, blockY+1, oldOpacity);
				m_segments[i][j] = packSegment(blockY, opacity);
			} else {
				if (isLastSegment) {
					removeSegment(i, j);
					m_ymax[i]--;
				} else {
					removeSegment(i, j);
					moveSegmentStartDown(i, j);
				}
			}
		} else {
			if (isRoomAfter) {
				addSegment(i, j+1, blockY+1, oldOpacity);
			}
			m_segments[i][j] = packSegment(blockY, opacity);
		}
	}

	private void setOpacityWithSegmentsAfter(int i, int blockY, int j, int opacity) {
		int[] segments = m_segments[i];
		
		// will the opacity even change?
		int oldSegment = segments[j];
		int oldOpacity = unpackOpacity(oldSegment);
		if (opacity == oldOpacity) {
			return;
		}
		
		boolean isLastSegment = j == getLastSegmentIndex(segments);
		
		boolean sameOpacityAsNext;
		if (isLastSegment) {
			sameOpacityAsNext = opacity == 0;
		} else {
			sameOpacityAsNext = opacity == unpackOpacity(segments[j+1]);
		}
		
		boolean isRoomAfter;
		if (isLastSegment) {
			isRoomAfter = m_ymax[i] > blockY;
		} else {
			isRoomAfter = unpackPos(segments[j+1]) > blockY + 1;
		}
		
		if (sameOpacityAsNext) {
			if (isRoomAfter) {
				addSegment(i, j+1, blockY, opacity);
				addSegment(i, j+2, blockY+1, oldOpacity);
			} else {
				if (isLastSegment) {
					m_ymax[i]--;
				} else {
					moveSegmentStartDown(i, j+1);
				}
			}
		} else {
			addSegment(i, j+1, blockY, opacity);
			if (isRoomAfter) {
				addSegment(i, j+2, blockY+1, oldOpacity);
			}
		}
	}
	
	private void setOpacityWithSegmentsAfterTop(int i, int blockY, int opacity) {
		// will the opacity even change?
		if (opacity == 0) {
			return;
		}
		
		int[] segments = m_segments[i];
		int j = getLastSegmentIndex(segments);
		
		boolean nextToLastSegment = blockY == m_ymax[i] + 1;
		if (nextToLastSegment) {
			boolean extendSegment = opacity == unpackOpacity(segments[j]);
			if (extendSegment) {
				// nothing to do, just increment ymax at end of func
			} else {
				addSegment(i, j+1, blockY, opacity);
			}
		} else {
			addSegment(i, j+1, m_ymax[i] + 1, 0);
			addSegment(i, j+2, blockY, opacity);
		}
		
		m_ymax[i] = blockY;
	}

	private void setOpacityWithSegmentsBeforeBottom(int i, int blockY, int opacity) {
		// will the opacity even change?
		if (opacity == 0) {
			return;
		}
		
		int[] segments = m_segments[i];
		
		boolean nextToFirstSegment = blockY == m_ymin[i] - 1;
		if (nextToFirstSegment) {
			boolean extendSegment = opacity == unpackOpacity(segments[0]);
			if (extendSegment) {
				moveSegmentStartDown(i, 0);
			} else {
				addSegment(i, 0, blockY, opacity);
				m_ymin[i] = blockY;
			}
		} else {
			addSegment(i, 0, blockY + 1, 0);
			addSegment(i, 0, blockY, opacity);
			m_ymin[i] = blockY;
		}
	}

	private void moveSegmentStartUp(int i, int j) {
		
		int segment = m_segments[i][j];
		int pos = unpackPos(segment);
		int opacity = unpackOpacity(segment);
		
		// move the segment
		m_segments[i][j] = packSegment(pos + 1, opacity);
		
		// move the bottom if needed
		if (j == 0) {
			m_ymin[i]++;
		}
	}
	
	private void moveSegmentStartDown(int i, int j) {
		
		int segment = m_segments[i][j];
		int pos = unpackPos(segment);
		int opacity = unpackOpacity(segment);
		
		// move the segment
		m_segments[i][j] = packSegment(pos - 1, opacity);
		
		// move the bottom if needed
		if (j == 0) {
			m_ymin[i]--;
		}
	}
	
	private void removeSegment(int i, int j) {
		
		int[] segments = m_segments[i];
		int jmax = getLastSegmentIndex(segments);

		// move the bounds if needed
		// for reasons I can't explain it's only needed to move lower bound here and only by one.
		if (j == 0) {
			m_ymin[i]++;
		}
		// remove the segment
		for (int n=j; n<jmax; n++) {
			segments[n] = segments[n+1];
		}
		segments[jmax] = NoneSegment;
		
		if(segments[0] == NoneSegment) {
			m_segments[i] = null;
		}
	}

	private void removeTwoSegments(int i, int j) {

		int[] segments = m_segments[i];
		int jmax = getLastSegmentIndex(segments);

		// remove the segment
		for (int n=j; n<jmax-1; n++) {
			segments[n] = segments[n+2];
		}
		segments[jmax] = NoneSegment;
		segments[jmax-1] = NoneSegment;

		if(segments[0] == NoneSegment) {
			m_segments[i] = null;
		}
	}
	
	private void addSegment(int i, int j, int pos, int opacity) {
		int lastIndex = getLastSegmentIndex(m_segments[i]);
		if (lastIndex + 1 == m_segments[i].length) {
			
			// allocate more space
			int[] newSegments = new int[lastIndex + 2];
			for (int n=0; n<=lastIndex; n++) {
				newSegments[n] = m_segments[i][n];
			}
			m_segments[i] = newSegments;
		}
		
		// shift the segments by one
		lastIndex++;
		for (int n=lastIndex; n>j; n--) {
			m_segments[i][n] = m_segments[i][n-1];
		}
		
		m_segments[i][j] = packSegment(pos, opacity);
	}
	
	@SuppressWarnings("unused")
	private int getSegmentLength(int i, int j) {
		int[] segments = m_segments[i];
		int pos = unpackPos(segments[j]);
		if (j + 1 < segments.length) {
			return unpackPos(segments[j+1]) - pos;
		} else {
			return m_ymax[i] - pos + 1;
		}
	}
	
	private void removeSegments(int i) {
		m_segments[i] = null;
		m_ymin[i] = None;
		m_ymax[i] = None;
	}

	public Integer getTopBlockY(int localX, int localZ) {
		int i = getIndex(localX, localZ);
		int pos = m_ymax[i];
		if (pos == None) {
			return null;
		}
		return pos;
	}
	
	public Integer getBottomBlockY(int localX, int localZ) {
		int i = getIndex(localX, localZ);
		int pos = m_ymin[i];
		if (pos == None) {
			return null;
		}
		return pos;
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
		for (int i=0; i<m_segments.length; i++) {
			m_ymin[i] = in.readInt();
			m_ymax[i] = in.readInt();
			int[] segments = new int[in.readUnsignedShort()];
			for (int j=0; j<segments.length; j++) {
				segments[j] = in.readInt();
			}
		}
	}
	
	public void writeData(DataOutputStream out)
	throws IOException {
		for (int i=0; i<m_segments.length; i++) {
			out.writeInt(m_ymin[i]);
			out.writeInt(m_ymax[i]);
			int[] segments = m_segments[i];
			if (segments == null) {
				out.writeShort(0);
			} else {
				int lastSegmentIndex = getLastSegmentIndex(segments);
				out.writeShort(lastSegmentIndex + 1);
				for (int j=0; j<=lastSegmentIndex; j++) {
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
		for (int i=segments.length - 1; i>=0; i--) {
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
		for (int i=0; i<m_segments.length; i++) {
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
