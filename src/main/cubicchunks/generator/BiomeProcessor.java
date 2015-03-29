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
package cubicchunks.generator;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.BlockPos;
import cubicchunks.CubeCache;
import cubicchunks.CubeProviderTools;
import cubicchunks.CubeWorld;
import cubicchunks.generator.biome.biomegen.CubeBiomeGenBase;
import cubicchunks.generator.noise.NoiseGeneratorPerlin;
import cubicchunks.server.CubeWorldServer;
import cubicchunks.util.Coords;
import cubicchunks.util.CubeProcessor;
import cubicchunks.world.Cube;

public class BiomeProcessor extends CubeProcessor {
	
	private CubeWorldServer worldServer;
	
	private Random rand;
	private NoiseGeneratorPerlin m_noiseGen;
	private double[] noise;
	private CubeBiomeGenBase[] biomes;
	
	private int seaLevel;
	
	public BiomeProcessor(String name, CubeWorldServer worldServer, int batchSize) {
		super(name, worldServer.getCubeCache(worldServer), batchSize);
		
		this.worldServer = worldServer;
		
		this.rand = new Random(worldServer.getSeed());
		this.m_noiseGen = new NoiseGeneratorPerlin(this.rand, 4);
		this.noise = new double[256];
		this.biomes = null;
		
		this.seaLevel = worldServer.getCubeWorldProvider().getSeaLevel();
	}
	
	@Override
	public boolean calculate(Cube cube) {
		// only continue if the neighboring cubes exist
		CubeCache cache = CubeWorld.getCubeCache(this.worldServer);
		if (!CubeProviderTools.cubeAndNeighborsExist(cache, cube.getX(), cube.getY(), cube.getZ())) {
			return false;
		}
		
		// generate biome info. This is a hackjob.
		this.biomes = (CubeBiomeGenBase[])this.worldServer.getCubeWorldProvider().getWorldColumnMananger().loadBlockGeneratorData(this.biomes, Coords.cubeToMinBlock(cube.getX()), Coords.cubeToMinBlock(cube.getZ()), 16, 16);
		
		this.noise = this.m_noiseGen.arrayNoise2D_pre(this.noise, Coords.cubeToMinBlock(cube.getX()), Coords.cubeToMinBlock(cube.getZ()), 16, 16, 16, 16, 1);
		
		Cube above = cache.getCube(cube.getX(), cube.getY() + 1, cube.getZ());
		Cube below = cache.getCube(cube.getX(), cube.getY() - 1, cube.getZ());
		
		int topOfCube = Coords.cubeToMaxBlock(cube.getY());
		int bottomOfCube = Coords.cubeToMinBlock(cube.getY());
		
		// already checked that cubes above and below exist
		int alterationTop = topOfCube;
		int top = topOfCube + 8;
		int bottom = bottomOfCube - 8;
		
		for (int xRel = 0; xRel < 16; xRel++) {
			int xAbs = cube.getX() << 4 | xRel;
			
			for (int zRel = 0; zRel < 16; zRel++) {
				int zAbs = cube.getZ() << 4 | zRel;
				
				int xzCoord = zRel << 4 | xRel;
				
				CubeBiomeGenBase biome = this.biomes[xzCoord];
				
				// Biome blocks depth in current block column. 0 for negative values.
				
				int depth = (int) (this.noise[zRel + xRel * 16] / 3D + 3D + this.rand.nextDouble() * 0.25D);
				
				// How many biome blocks left to set in column? Initially -1
				int numBlocksToChange = -1;
				
				Block topBlock = biome.topBlock;
				Block fillerBlock = biome.fillerBlock;
				
				for (int blockY = top; blockY >= bottom; --blockY) {
					// Current block
					Block block = Coords.blockToCube(blockY) == cube.getY() ? // if in current cube, get block from current cube, else get block from lower cube
					cube.getBlockState(new BlockPos(xRel, Coords.blockToLocal(blockY), zRel)).getBlock()
							: below.getBlockState(new BlockPos(xRel, Coords.blockToLocal(blockY), zRel)).getBlock();
					
					// Set numBlocksToChange to -1 when we reach air, skip everything else
					if (block == Blocks.AIR) {
						numBlocksToChange = -1;
						continue;
					}
					
					// Why? If the block has already been replaced, skip it and go to the next block
					if ( /* worldY <= alterationTop && */block != Blocks.STONE) {
						continue;
					}
					
					// If we are 1 block below air...
					if (numBlocksToChange == -1) {
						// If depth is <= 0 - only stone
						if (depth <= 0) {
							topBlock = Blocks.AIR;
							fillerBlock = Blocks.STONE;
						}
						// If we are above or at 4 block under water and at or below one block above water
						else if (blockY >= this.seaLevel - 4 && blockY <= this.seaLevel + 1) {
							topBlock = biome.topBlock;
							fillerBlock = biome.fillerBlock;
						}
						// If top block is air and we are below sea level use water instead
						if (blockY < this.seaLevel && topBlock == Blocks.AIR) {
							topBlock = Blocks.WATER;
						}
						// Set num blocks to change to current depth.
						numBlocksToChange = depth;
						
						// Modify blocks only if we are at or below alteration top
						if (blockY <= alterationTop) {
							if (blockY >= this.seaLevel - 1) {
								// If we are above sea level
								replaceBlocksForBiome_setBlock(topBlock, cube, below, xAbs, blockY, zAbs);
							} else {
								// Don't set grass underwater
								replaceBlocksForBiome_setBlock(fillerBlock, cube, below, xAbs, blockY, zAbs);
							}
						}
						
						continue;
					}
					// Nothing left to do...
					// so continue
					if (numBlocksToChange <= 0) {
						continue;
					}
					// Decrease blocks to change
					numBlocksToChange--;
					
					// Set blocks only if we are below or at alteration top
					if (blockY <= alterationTop) {
						replaceBlocksForBiome_setBlock(fillerBlock, cube, below, xAbs, blockY, zAbs);
					}
					
					// random sandstone generation
					if (numBlocksToChange == 0 && fillerBlock == Blocks.SAND) {
						numBlocksToChange = this.rand.nextInt(4);
						fillerBlock = Blocks.SANDSTONE;
					}
				}
			}
		}
		
		return true;
	}
	
	private void replaceBlocksForBiome_setBlock(Block block, Cube cube, Cube below, int xAbs, int yAbs, int zAbs) {
		int xRel = xAbs & 15;
		int yRel = yAbs & 15;
		int zRel = zAbs & 15;
		
		BlockPos newPos = new BlockPos(xRel, yRel, zRel);
		
		if (Coords.blockToCube(yAbs) == cube.getY()) // check if we're in the same cube as Cube
		{
			// If we are in the same cube
			cube.setBlockForGeneration(newPos, block.getDefaultState());
		} else // we're actually in the cube below
		{
			assert this.worldServer.getCubeCache(this.worldServer).cubeExists(Coords.blockToCube(xAbs), Coords.blockToCube(yAbs), Coords.blockToCube(zAbs));
			
			below.setBlockForGeneration(newPos, block.getDefaultState());
		}
	}
}
