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
package cubicchunks.generator.features;

import cubicchunks.generator.terrain.GlobalGeneratorConfig;
import cubicchunks.util.Coords;
import cubicchunks.world.cube.Cube;
import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.MineralDepositGenerator;

public class MineralGenerator extends FeatureGenerator {
	private final double minY;
	private final double maxY;
	private final int attempts;
	
	private final MineralDepositGenerator vanillaGen;
	private final double probability;

	/**
	 * Creates new OreGenerator with given min/max height, vein size and number of generation attempts.
	 * minY and maxY:
	 * 0 - sea level. 
	 * -1 - seaLevel-maxTerrainHeight
	 * 1 - seaLevel+maxTerrainHeight
	 * @param minY Minimum generation height
	 * @param maxY Maximum generation height
	 * @param size Maximum vein size
	 * @param attempts Number of generation attempts. 
	 */
	public MineralGenerator(World world, IBlockState state, double minY, double maxY, int size, int attempts, double probability) {
		super(world);
		// use vanilla generator. This class odesn't have height limits
		this.vanillaGen = new MineralDepositGenerator(state, size);
		this.minY = minY;
		this.maxY = maxY;
		this.attempts = attempts;
		this.probability = probability;
	}

	@Override
	public void generate(Random rand, Cube cube, Biome biome) {
		BlockPos cubeCenter = Coords.getCubeCenter(cube);
		
		double maxBlockY = maxY * GlobalGeneratorConfig.maxElev + GlobalGeneratorConfig.seaLevel;
		double minBlockY = minY * GlobalGeneratorConfig.maxElev + GlobalGeneratorConfig.seaLevel;
		
		int y1 = cubeCenter.getY();
		int y2 = y1 + 15;
		
		//are we in correct height range?
		if(y1 > maxBlockY || y2 < minBlockY){
			return;
		}
		
		for(int i = 0; i < this.attempts; i++){
			if(rand.nextDouble() > this.probability){
				continue;
			}
			BlockPos currentPos = cubeCenter.add(rand.nextInt(16), rand.nextInt(16), rand.nextInt(16));
			if(currentPos.getY() <= maxBlockY && currentPos.getY() >= minBlockY){
				this.vanillaGen.generate(world, rand, currentPos);
			}
		}
	}
}
