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
package cubicchunks.debug;

import com.flowpowered.noise.module.source.Perlin;
import cubicchunks.BaseCubicWorldType;
import cubicchunks.CubicChunks;
import cubicchunks.util.CubeCoords;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.cube.Cube;
import cubicchunks.world.provider.IColumnGenerator;
import cubicchunks.world.provider.ICubeGenerator;
import cubicchunks.world.provider.ICubicChunkGenerator;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;

public class DebugWorldType extends BaseCubicWorldType {

	public DebugWorldType() {
		super("DebugCubic");
	}

	@Override
	public boolean getCanBeCreated() {
		return CubicChunks.DEBUG_ENABLED;
	}

	public static void create() {
		new DebugWorldType();
	}

	@Override
	public ICubeGenerator createCubeGenerator(ICubicWorldServer world) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IColumnGenerator createColumnGenerator(ICubicWorldServer world) {
		// TODO Auto-generated method stub
		return null;
	}

	//TODO: Debug Cubic generator
	/*
	@Override public ICubicChunkGenerator createCubeGenerator(ICubicWorldServer world) {
		//TODO: move first light processor directly into cube?
		return new ICubicChunkGenerator() {
			Perlin perlin = new Perlin();
			{
				perlin.setFrequency(0.180);
				perlin.setOctaveCount(1);
				perlin.setSeed((int) world.getSeed());
			}
			
			//TODO: find out what this was/should have been for (it was never used)
			//CustomPopulationProcessor populator = new CustomPopulationProcessor(world);

			@Override public void generateTerrain(Cube cube) {
				if(cube.getY() > 30) {
					cube.initSkyLight();
					return;
				}
				if(cube.getX() == 100 && cube.getZ() == 100) {
					cube.initSkyLight();
					return;//hole in the world
				}
				CubeCoords cubePos = cube.getCoords();
				for(BlockPos pos : BlockPos.getAllInBoxMutable(cubePos.getMinBlockPos(), cubePos.getMaxBlockPos())) {
					double currDensity = perlin.getValue(pos.getX(), pos.getY()*0.5, pos.getZ());
					double aboveDensity = perlin.getValue(pos.getX(), (pos.getY()+1)*0.5, pos.getZ());
					if(cube.getY() >= 16) {
						currDensity -= (pos.getY() - 16*16)/100;
						aboveDensity -= (pos.getY() + 1 - 16*16)/100;
					}
					if(currDensity > 0.5) {
						if(currDensity > 0.5 && aboveDensity <= 0.5) {
							cube.setBlockForGeneration(pos, Blocks.GRASS.getDefaultState());
						} else if(currDensity > aboveDensity && currDensity < 0.7) {
							cube.setBlockForGeneration(pos, Blocks.DIRT.getDefaultState());
						} else {
							cube.setBlockForGeneration(pos, Blocks.STONE.getDefaultState());
						}
					}
				}
				cube.initSkyLight();
			}

			@Override public void populateCube(Cube cube) {
				//populator.calculate(cube);
			}
		};
	}
	*/
}