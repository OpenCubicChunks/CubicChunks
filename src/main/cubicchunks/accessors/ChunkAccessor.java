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
import java.lang.reflect.Method;

import net.minecraft.world.chunk.Chunk;

public class ChunkAccessor {
	
	private static Method m_methodPropagateSkylightOcclusion;
	private static Method m_methodRecheckGaps;
	private static Field m_fieldIsGapLightingUpdated;
	
	static {
		try {
			m_methodPropagateSkylightOcclusion = Chunk.class.getDeclaredMethod("propagateSkylightOcclusion", int.class, int.class);
			m_methodPropagateSkylightOcclusion.setAccessible(true);
			
			m_methodRecheckGaps = Chunk.class.getDeclaredMethod("recheckGaps", boolean.class);
			m_methodRecheckGaps.setAccessible(true);
			
			m_fieldIsGapLightingUpdated = Chunk.class.getDeclaredField("isGapLightingUpdated");
			m_fieldIsGapLightingUpdated.setAccessible(true);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static void propagateSkylightOcclusion(Chunk chunk, int localX, int localZ) {
		try {
			m_methodPropagateSkylightOcclusion.invoke(chunk, localX, localZ);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static void recheckGaps(Chunk chunk, boolean isClient) {
		try {
			m_methodRecheckGaps.invoke(chunk, isClient);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
	
	public static boolean isGapLightingUpdated(Chunk chunk) {
		try {
			return m_fieldIsGapLightingUpdated.getBoolean(chunk);
		} catch (Exception ex) {
			throw new Error(ex);
		}
	}
}
