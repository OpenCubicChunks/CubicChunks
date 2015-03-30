/*
 *  This file is part of Cubic Chunks, licensed under the MIT License (MIT).
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
package cubicchunks.accessors;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.util.LongHashMap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ClientChunkCache;

public class ChunkProviderClientAccessor {
	
	private static Field m_fieldChunkMapping;
	private static Field m_fieldChunkListing;
	private static Field m_fieldBlankChunk;
	
	static {
		try {
			m_fieldChunkMapping = ClientChunkCache.class.getDeclaredField("chunkMapping");
			m_fieldChunkMapping.setAccessible(true);
			
			m_fieldChunkListing = ClientChunkCache.class.getDeclaredField("chunkListing");
			m_fieldChunkListing.setAccessible(true);
			
			m_fieldBlankChunk = ClientChunkCache.class.getDeclaredField("blankChunk");
			m_fieldBlankChunk.setAccessible(true);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static LongHashMap getChunkMapping(ClientChunkCache provider) {
		try {
			return (LongHashMap)m_fieldChunkMapping.get(provider);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static List<Chunk> getChunkListing(ClientChunkCache provider) {
		try {
			return (List<Chunk>)m_fieldChunkListing.get(provider);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static Chunk getBlankChunk(ClientChunkCache provider) {
		try {
			return (Chunk)m_fieldBlankChunk.get(provider);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static void setBlankChunk(ClientChunkCache provider, Chunk val) {
		try {
			m_fieldBlankChunk.set(provider, val);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
}