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
package cubicchunks;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.junit.Test;

import cubicchunks.world.LightIndexColumn;

public class TestLightIndexColumn {
	
	private static final int SeaLevel = 0;
	
	@Test
	public void readZero() {
		LightIndexColumn index = new LightIndexColumn(SeaLevel);
		for (int i = -16; i <= 16; i++) {
			assertEquals(0, index.getOpacity(i));
		}
	}
	
	@Test
	public void writeBottomDiffAbove() {
		for (int i = -16; i <= 16; i++) {
			LightIndexColumn index = buildColumn(i + 0, 0);
			
			index.setOpacity(i + 0, 1);
			
			assertEquals(1, index.getOpacity(i));
			assertEquals(0, index.getOpacity(i + 1));
		}
	}
	
	@Test
	public void writeBottomSameAbove() {
		for (int i = -16; i <= 16; i++) {
			LightIndexColumn index = buildColumn(i + 0, 0, i + 1, 1);
			
			index.setOpacity(i + 0, 1);
			
			assertEquals(1, index.getOpacity(i + 0));
			assertEquals(1, index.getOpacity(i + 1));
		}
	}
	
	@Test
	public void writeMiddleSameBottomSameAbove() {
		for (int i = -16; i <= 16; i++) {
			LightIndexColumn index = buildColumn(i + 0, 1, i + 5, 0, i + 6, 1);
			
			index.setOpacity(i + 5, 1);
			
			assertEquals(1, index.getOpacity(i + 4));
			assertEquals(1, index.getOpacity(i + 5));
			assertEquals(1, index.getOpacity(i + 6));
		}
	}
	
	@Test
	public void writeMiddleDiffBottomSameAbove() {
		for (int i = -16; i <= 16; i++) {
			LightIndexColumn index = buildColumn(i + 6, 1);
			
			index.setOpacity(i + 5, 1);
			
			assertEquals(0, index.getOpacity(i + 4));
			assertEquals(1, index.getOpacity(i + 5));
			assertEquals(1, index.getOpacity(i + 6));
		}
	}
	
	@Test
	public void writeMiddleDiffBottomDiffAbove() {
		for (int i = -16; i <= 16; i++) {
			LightIndexColumn index = buildColumn(i + 0, 0);
			
			index.setOpacity(i + 5, 1);
			
			assertEquals(0, index.getOpacity(i + 4));
			assertEquals(1, index.getOpacity(i + 5));
			assertEquals(0, index.getOpacity(i + 6));
		}
	}
	
	@Test
	public void writeMiddleSameBottomDiffAbove() {
		for (int i = -16; i <= 16; i++) {
			LightIndexColumn index = buildColumn(i + 0, 1, i + 6, 0);
			
			index.setOpacity(i + 5, 1);
			
			assertEquals(1, index.getOpacity(i + 4));
			assertEquals(1, index.getOpacity(i + 5));
			assertEquals(0, index.getOpacity(i + 6));
		}
	}
	
	@Test
	public void topNonTransparentBlock() {
		LightIndexColumn index = new LightIndexColumn(SeaLevel);
		
		assertEquals(null, index.getTopNonTransparentBlockY());
		
		index.setOpacity(-16, 1);
		assertEquals(-16, (int)index.getTopNonTransparentBlockY());
		
		index.setOpacity(0, 1);
		assertEquals(0, (int)index.getTopNonTransparentBlockY());
		
		index.setOpacity(1, 1);
		assertEquals(1, (int)index.getTopNonTransparentBlockY());
		
		index.setOpacity(5, 1);
		assertEquals(5, (int)index.getTopNonTransparentBlockY());
		
		index.setOpacity(5, 0);
		assertEquals(1, (int)index.getTopNonTransparentBlockY());
		
		index.setOpacity(1, 0);
		assertEquals(0, (int)index.getTopNonTransparentBlockY());
		
		index.setOpacity(0, 0);
		assertEquals(-16, (int)index.getTopNonTransparentBlockY());
		
		index.setOpacity(-16, 0);
		assertEquals(null, index.getTopNonTransparentBlockY());
	}
	
	private LightIndexColumn buildColumn(int... data) {
		try {
			// write the data
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(buf);
			out.writeShort(data.length / 2);
			for (int i = 0; i < data.length;) {
				out.writeInt(data[i++]);
				out.writeByte(data[i++]);
			}
			out.close();
			
			// read the data
			LightIndexColumn index = new LightIndexColumn(SeaLevel);
			index.readData(new DataInputStream(new ByteArrayInputStream(buf.toByteArray())));
			return index;
		} catch (IOException ex) {
			throw new Error(ex);
		}
	}
}
