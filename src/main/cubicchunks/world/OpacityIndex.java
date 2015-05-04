package cubicchunks.world;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import cubicchunks.util.Bits;


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
	
	public OpacityIndex() {
		
		m_ymin = new int[16*16];
		m_ymax = new int[16*16];
		m_segments = new int[16*16][];
		
		// init to empty
		for (int i=0; i<16*16; i++) {
			m_ymin[i] = None;
			m_ymax[i] = None;
		}
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
		
		// didn't hit a segment start, mini is the correct answer + 1
		assert(mini > 0);
		return unpackOpacity(segments[mini - 1]);
	}
	
	public void setOpacity(int localX, int blockY, int localZ, int opacity) {
		
		// what's the range?
		int i = getIndex(localX, localZ);
		
		// try to stay in no-segments mode as long as we can
		if (m_segments[i] == null) {
			if (opacity == 255) {
				setOpacityNoSegmentsOpaque(i, blockY);
			} else if (opacity == 0) {
				setOpacityNoSegmentsTransparent(i, blockY);
			} else {
				makeSegmentsFromOpaqueRange(i);
				setOpacityWithSegments(i, blockY, opacity);
			}
		} else {
			setOpacityWithSegments(i, blockY, opacity);
		}
	}
	
	public void setOpacityNoSegmentsOpaque(int i, int blockY) {
		
		// something from nothing?
		if (m_ymin[i] == None && m_ymax[i] == None) {
			m_ymin[i] = blockY;
			m_ymax[i] = blockY;
			return;
		}
		
		// extending the range?
		if (blockY == m_ymin[i] - 1) {
			m_ymin[i]--;
			return;
		} else if (blockY == m_ymax[i] + 1) {
			m_ymax[i]++;
			return;
		}
		
		// making a new section?
		if (blockY > m_ymax[i] + 1) {
			m_segments[i] = new int[] {
				packSegment(m_ymin[i], 255),
				packSegment(m_ymax[i] + 1, 0),
				packSegment(blockY, 255)
			};
			m_ymax[i] = blockY;
			return;
		} else if (blockY < m_ymin[i] - 1) {
			m_segments[i] = new int[] {
				packSegment(blockY, 255),
				packSegment(blockY + 1, 0),
				packSegment(m_ymin[i], 255)
			};
			m_ymin[i] = blockY;
			return;
		}
		
		// must already be in range
		assert (blockY >= m_ymin[i] && blockY <= m_ymax[i]);
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
		
		// shrinking the range?
		if (blockY == m_ymin[i]) {
			m_ymin[i]++;
			return;
		} else if (blockY == m_ymax[i]) {
			m_ymax[i]--;
			return;
		}
		
		// we must be bisecting the range, need to make segments
		assert (blockY >= m_ymin[i] && blockY <= m_ymax[i]);
		m_segments[i] = new int[] {
			packSegment(m_ymin[i], 255),
			packSegment(blockY, 0),
			packSegment(blockY + 1, 255)
		};
	}
	
	private void makeSegmentsFromOpaqueRange(int i) {
		assert (m_segments[i] == null);
		assert (m_ymin[i] == None);
		assert (m_ymax[i] == None);
		m_segments[i] = new int[] {
			packSegment(m_ymin[i], 255)
		};
	}
	
	private void setOpacityWithSegments(int i, int blockY, int opacity) {

		// binary search to find the insertion point
		int[] segments = m_segments[i];
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
				// set would overwrite a segment start
				setOpacityWithSegmentsAt(i, blockY, midi, opacity);
				return;
			}
		}
		
		// didn't hit a segment start, but mini-1 is the lower segment start index
		setOpacityWithDataAfter(i, blockY, mini - 1, opacity);
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
		boolean isLastSegment = j == segments.length - 1;
		
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
		if (j + 1 < segments.length) {
			isRoomAfter = unpackPos(segments[j+1]) > blockY + 1;
		} else {
			isRoomAfter = m_ymax[i] > blockY;
		}
		
		if (sameOpacityAsPrev && sameOpacityAsNext) {
			if (isRoomAfter) {
				moveSegmentStartUp(i, j);
			} else {
				if (isFirstSegment && isLastSegment) {
					removeSegments(i);
				} else {
					// TODO
					throw new Error("Both without room not implemented yet!");
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
				// TODO
				throw new Error("Adding segments not implemented yet!");
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
			// TODO
			throw new Error("Neither not implemented yet!");
		}
	}

	private void setOpacityWithDataAfter(int i, int blockY, int j, int opacity) {
		// TODO Auto-generated method stub
		
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
		
		// can only remove one-block segments
		assert(getSegmentLength(i, j) == 1);
		
		// remove the segment
		for (int n=j; n<jmax; n++) {
			segments[n] = segments[n+1];
		}
		segments[jmax] = NoneSegment;
		
		// move the bounds if needed
		if (j == 0) {
			m_ymin[i]++;
		}
	}
	
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
		// TODO
	}
	
	public void writeData(DataOutputStream out)
	throws IOException {
		// TODO
	}
	
	public String dump(int localX, int localZ) {
		int index = getIndex(localX, localZ);
		StringBuilder buf = new StringBuilder();
		buf.append("t=");
		buf.append(m_ymax[index]);
		buf.append(", d(p,o)=");
		for (int packed : m_segments[index]) {
			int pos = unpackPos(packed);
			int opacity = unpackOpacity(packed);
			buf.append("(");
			buf.append(pos);
			buf.append(",");
			buf.append(opacity);
			buf.append(")");
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

}
