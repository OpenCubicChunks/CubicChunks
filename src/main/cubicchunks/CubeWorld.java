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
package cubicchunks;

import net.minecraft.profiler.Profiler;
import net.minecraft.world.Dimension;
import net.minecraft.world.World;
import net.minecraft.world.WorldInfo;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ISaveHandler;
import cubicchunks.lighting.LightingManager;
import cubicchunks.server.ServerCubeCache;

public abstract class CubeWorld extends World {
	
	protected CubeWorld(ISaveHandler a1, WorldInfo a2, Dimension a3,
			Profiler a4, boolean a5) {
		super(a1, a2, a3, a4, a5);
		// TODO Auto-generated constructor stub
	}

	public static ServerCubeCache getCubeCache(WorldServer worldServer) {
		return null;
	}
	
	public static LightingManager getLightingManager(World world) {
		return null;
	}
}
