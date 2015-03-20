/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 * 
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package cubicchunks.world;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import cubicchunks.util.ValueCache;

public class LightIndex {
	
	private LightIndexColumn[] m_columns;
	private ValueCache<Integer> m_topNonTransparentBlockY;
	
	public LightIndex(int seaLevel) {
		m_columns = new LightIndexColumn[16 * 16];
		for (int i = 0; i < m_columns.length; i++) {
			m_columns[i] = new LightIndexColumn(seaLevel);
		}
		
		m_topNonTransparentBlockY = new ValueCache<Integer>();
	}
	
	public int getOpacity(int localX, int blockY, int localZ) {
		int xzCoord = localZ << 4 | localX;
		if (m_columns[xzCoord] == null) {
			return 0;
		}
		return m_columns[xzCoord].getOpacity(blockY);
	}
	
	public void setOpacity(int localX, int blockY, int localZ, int opacity) {
		int xzCoord = localZ << 4 | localX;
		m_columns[xzCoord].setOpacity(blockY, opacity);
		
		m_topNonTransparentBlockY.clear();
	}
	
	public Integer getTopNonTransparentBlockY(int localX, int localZ) {
		int xzCoord = localZ << 4 | localX;
		return m_columns[xzCoord].getTopNonTransparentBlockY();
	}
	
	public Integer getTopNonTransparentBlockY() {
		// do we need to update the cache?
		if (!m_topNonTransparentBlockY.hasValue()) {
			m_topNonTransparentBlockY.set(null);
			
			for (int i = 0; i < m_columns.length; i++) {
				// get the top y from the column
				Integer blockY = m_columns[i].getTopNonTransparentBlockY();
				if (blockY == null) {
					continue;
				}
				
				// does it beat our current top y?
				if (m_topNonTransparentBlockY.get() == null || blockY > m_topNonTransparentBlockY.get()) {
					m_topNonTransparentBlockY.set(blockY);
				}
			}
		}
		return m_topNonTransparentBlockY.get();
	}
	
	public Integer getTopOpaqueBlockBelowSeaLevel(int localX, int localZ) {
		int xzCoord = localZ << 4 | localX;
		return m_columns[xzCoord].getTopOpaqueBlockBelowSeaLevel();
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
	
	public void readData(DataInputStream in) throws IOException {
		for (int i = 0; i < m_columns.length; i++) {
			m_columns[i].readData(in);
		}
	}
	
	public void writeData(DataOutputStream out) throws IOException {
		for (int i = 0; i < m_columns.length; i++) {
			m_columns[i].writeData(out);
		}
	}
	
	public String dump(int localX, int localZ) {
		int xzCoord = localZ << 4 | localX;
		return m_columns[xzCoord].dump();
	}
}
