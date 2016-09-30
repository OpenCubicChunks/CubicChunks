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
package cubicchunks.worldgen.generator.vanilla;

import java.util.List;

import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import cubicchunks.world.cube.CubePrimer;
import cubicchunks.world.provider.ICubeGenerator;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome.SpawnListEntry;
import net.minecraft.world.chunk.IChunkGenerator;

public class VanillaCubeGenerator implements ICubeGenerator{

	private ICubicWorld world;
	private IChunkGenerator vanilaGen;
	
	public VanillaCubeGenerator(ICubicWorld world){
		this.world     = world;
		this.vanilaGen = ((WorldServer)world).getChunkProvider().chunkGenerator; // get the chunk generator that would be used
	}
	
	@Override
	public CubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
		return null;
	}

	@Override
	public void populate(Cube cube) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Vec3i[] getPopRequirment(Cube cube) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void recreateStructures(Cube cube) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
		// TODO Auto-generated method stub
		return null;
	}

}
