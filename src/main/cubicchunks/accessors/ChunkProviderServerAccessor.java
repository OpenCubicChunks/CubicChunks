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
package cubicchunks.accessors;

import java.lang.reflect.Field;
import java.util.List;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ServerChunkCache;

public class ChunkProviderServerAccessor {
	
	private static Field m_fieldLoadedChunks;
	private static Field m_fieldBlankChunk;
	
	static {
		try {
			m_fieldLoadedChunks = ServerChunkCache.class.getDeclaredField("loadedChunks");
			m_fieldLoadedChunks.setAccessible(true);
			
			m_fieldBlankChunk = ServerChunkCache.class.getDeclaredField("defaultEmptyChunk");
			m_fieldBlankChunk.setAccessible(true);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static List<Chunk> getLoadedChunks(ServerChunkCache cache) {
		try {
			return (List<Chunk>)m_fieldLoadedChunks.get(cache);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static Chunk getBlankChunk(ServerChunkCache cache) {
		try {
			return (Chunk)m_fieldBlankChunk.get(cache);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static void setBlankChunk(ServerChunkCache cache, Chunk val) {
		try {
			m_fieldBlankChunk.set(cache, val);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
}
