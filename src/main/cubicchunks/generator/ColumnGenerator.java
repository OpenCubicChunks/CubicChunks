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
package cubicchunks.generator;

import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import cubicchunks.util.Coords;
import cubicchunks.world.Column;

public class ColumnGenerator {
	
	private WorldServer m_worldServer;
	private Biome[] m_biomes;
	
	public ColumnGenerator(WorldServer worldServer) {
		this.m_worldServer = worldServer;
	}
	
	public Column generateColumn(int cubeX, int cubeZ) {
		
		// generate biome info. This is a hackjob.
		this.m_biomes = (Biome[])this.m_worldServer.dimension.getBiomeManager().getBiomeMap(
			this.m_biomes,
			Coords.cubeToMinBlock(cubeX), 
			Coords.cubeToMinBlock(cubeZ),
			16,
			16
		);
		
		// UNDONE: generate temperature map
		// UNDONE: generate rainfall map
		
		return new Column(this.m_worldServer, cubeX, cubeZ, this.m_biomes);
	}
}