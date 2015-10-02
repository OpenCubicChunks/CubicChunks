/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 Tall Worlds
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

import java.util.Random;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import cubicchunks.generator.terrain.GlobalGeneratorConfig;
import cubicchunks.util.Coords;
import cubicchunks.world.cube.Cube;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.feature.WorldGenMinable;

public class MineralGenerator extends FeatureGenerator {
	private final double minY;
	private final double maxY;

	private final WorldGenMinable vanillaGen;
	private final double probability;

	/**
	 * Creates new OreGenerator with given min/max height, vein size and number of generation attempts.
	 * <p>
	 * minY and maxY:
	 * <ul>
	 * <li>-1 - seaLevel-maxTerrainHeight
	 * <li>0 - sea level.
	 * <li>1 - seaLevel+maxTerrainHeight
	 * </ul>
	 *
	 * @param minY
	 *            Minimum generation height
	 * @param maxY
	 *            Maximum generation height
	 * @param size
	 *            Maximum vein size
	 */
	public MineralGenerator(final World world, final IBlockState state, final double minY, final double maxY,
			final int size, final double probability) {
		super(world);
		// use vanilla generator. This class odesn't have height limits
		this.vanillaGen = new WorldGenMinable(state, size);
		this.minY = minY;
		this.maxY = maxY;
		this.probability = probability;
	}

	@Override
	public void generate(final Random rand, final Cube cube, final BiomeGenBase biome) {
		BlockPos cubeCenter = Coords.getCubeCenter(cube);

		double maxBlockY = this.maxY * GlobalGeneratorConfig.MAX_ELEV + GlobalGeneratorConfig.SEA_LEVEL;
		double minBlockY = this.minY * GlobalGeneratorConfig.MAX_ELEV + GlobalGeneratorConfig.SEA_LEVEL;

		if (rand.nextDouble() > this.probability) {
			return;
		}
		BlockPos currentPos = cubeCenter.add(rand.nextInt(16), rand.nextInt(16), rand.nextInt(16));
		if (currentPos.getY() <= maxBlockY && currentPos.getY() >= minBlockY) {
			this.vanillaGen.generate(this.world, rand, currentPos);
		}
	}
}
