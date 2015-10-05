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
package cubicchunks.util;

import java.lang.reflect.Field;
import java.util.Set;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class WorldAccess {
	private static final Field w_activeChunkSet = ReflectionHelper.findField(World.class, "activeChunkSet", "field_72993_I");
	private static final Field w_chunkProvider = ReflectionUtil.findFieldNonStatic(World.class, IChunkProvider.class);
	
	static {
		w_activeChunkSet.setAccessible(true);
		w_chunkProvider.setAccessible(true);
	}
	
	public static final Set<ChunkCoordIntPair> getActiveChunkSet(World world) {
		return ReflectionUtil.get(world, w_activeChunkSet, Set.class);
	}

	public static void setChunkProvider(World w, IChunkProvider chunkProvider) {
		ReflectionUtil.set(w, w_chunkProvider, chunkProvider);
	}
}
