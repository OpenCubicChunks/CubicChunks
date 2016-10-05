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
package cubicchunks.worldgen.generator.flat;

import java.util.List;

import cubicchunks.util.Coords;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.CubePrimer;
import cubicchunks.worldgen.generator.IColumnGenerator;
import cubicchunks.worldgen.generator.ICubeGenerator;
import cubicchunks.worldgen.generator.ICubePrimer;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.SpawnListEntry;

public class FlatTerrainProcessor implements ICubeGenerator, IColumnGenerator {

	private ICubicWorld world;
	
	public FlatTerrainProcessor(ICubicWorld world){
		this.world = world;
	}
	private Biome[] biomes;
	@Override
	public void generateColumn(Column column) {
		
		this.biomes = this.world.getBiomeProvider()
				.getBiomes(this.biomes, 
						Coords.cubeToMinBlock(column.getX()),
						Coords.cubeToMinBlock(column.getZ()),
						Coords.CUBE_MAX_X, Coords.CUBE_MAX_Z);
		
		byte[] abyte = column.getBiomeArray();
        for (int i = 0; i < abyte.length; ++i)
        {
            abyte[i] = (byte)Biome.getIdForBiome(this.biomes[i]);
        }
	}

	@Override
	public void recreateStructures(Column column) {}

	@Override
	public ICubePrimer generateCube(int cubeX, int cubeY, int cubeZ) {
		ICubePrimer primer = new CubePrimer();
		
		if (cubeY >= 0) {
			return primer;
		}
		if (cubeY == -1) {
			for (int x = 0; x < 16; x++) {
				for (int z = 0; z < 16; z++) {
					primer.setBlockState(x, 15, z, Blocks.GRASS.getDefaultState());
					for (int y = 14; y >= 10; y--) {
						primer.setBlockState(x, y, z, Blocks.DIRT.getDefaultState());
					}
					for (int y = 9; y >= 0; y--) {
						primer.setBlockState(x, y, z, Blocks.STONE.getDefaultState());
					}
				}
			}
			return primer;
		}
		
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = 0; y < 16; y++) {
					primer.setBlockState(x, y, z, Blocks.STONE.getDefaultState());
				}
			}
		}
		
		return primer;
	}

	@Override
	public void populate(Cube cube) {
		if(cube.containsBlockPos(new BlockPos(700, 100, 200))){
			for(int i = 0;i < 100;i++){
				EntityWither wither = new EntityWither((World)world);
				wither.setPositionAndRotation(700.0, 100.0, 200.0, 0.0f, 0.0f);
				world.spawnEntityInWorld(wither);
			}
		}
	}

	@Override
	public Vec3i[] getPopRequirment(Cube cube) {
		return NO_POPULTOR_REQUIRMENT;
	}

	@Override
	public void recreateStructures(Cube cube) {}

	@Override
	public List<SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
		Biome biome = this.world.getBiome(pos);
        return biome.getSpawnableList(creatureType);
	}

	@Override
	public BlockPos getClosestStructure(String name, BlockPos pos) {
		return name.equals("Stronghold") ? new BlockPos(700, 100, 200) : null;
	}
}
