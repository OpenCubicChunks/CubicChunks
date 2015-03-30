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
package cubicchunks.generator.biome.biomegen;

import cubicchunks.generator.populator.WorldGenAbstractTreeCube;
import cubicchunks.server.CubeWorldServer;
import cubicchunks.util.Coords;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.world.World;
import cubicchunks.world.Cube;
import net.minecraft.util.BlockPos;
import net.minecraft.world.gen.feature.TreeGenerator;

public class BiomeSwamplands extends CCBiome {
	@SuppressWarnings("unchecked")
	protected BiomeSwamplands(int par1) {
		super(par1);

		CubeBiomeDecorator.DecoratorConfig cfg = this.decorator().decoratorConfig();

		cfg.treesPerColumn(2);
		cfg.flowersPerColumn(1);
		cfg.deadBushPerColumn(1);
		cfg.mushroomsPerColumn(8);
		cfg.reedsPerColumn(10);
		cfg.clayPerColumn(1);
		cfg.waterlilyPerColumn(4);
		cfg.sandPerColumn(0);
		cfg.gravelPerColumn(0);
		cfg.grassPerColumn(5);
		this.waterColorMultiplier = 14745518;
		this.spawnableMonsterList.add(new CCBiome.SpawnListEntry(EntitySlime.class, 1, 1, 1));
	}

	public WorldGenAbstractTreeCube checkSpawnTree(Random p_150567_1_) {
		return this.worldGeneratorSwamp;
	}

	/**
	 * Provides the basic grass color based on the biome temperature and
	 * rainfall
	 */
	@Override
	public int getBiomeGrassColor(int p_150558_1_, int p_150558_2_, int p_150558_3_) {
		double var4 = field_150606_ad.func_151601_a((double) p_150558_1_ * 0.0225D, (double) p_150558_3_ * 0.0225D);
		return var4 < -0.1D ? 5011004 : 6975545;
	}

	/**
	 * Provides the basic foliage color based on the biome temperature and
	 * rainfall
	 */
	@Override
	public int getBiomeFoliageColor(int p_150571_1_, int p_150571_2_, int p_150571_3_) {
		return 6975545;
	}

	@Override
	public String spawnFlower(Random p_150572_1_, int p_150572_2_, int p_150572_3_, int p_150572_4_) {
		return BlockFlower.field_149859_a[1];
	}

	@Override
	public void decorate(World world, Random rand, int cubeX, int cubeY, int cubeZ) {

		int seaLevel = ((CubeWorldServer) world).getCubeWorldProvider().getSeaLevel();
		int minY = Coords.localToBlock(cubeY, 8);
		int maxY = minY + 16;

		// moved from replaceBlocks
		for (int x = 0; x < 16; x++) {
			int xAbs = Coords.localToBlock(cubeX, x);
			for (int z = 0; z < 16; z++) {
				int zAbs = Coords.localToBlock(cubeZ, z);

				double var9 = field_150606_ad.func_151601_a((double) xAbs * 0.25D, (double) zAbs * 0.25D);
				if (var9 > 0.0D) {
					for (int y = maxY; y >= minY; --y) {
						int yAbs = Coords.localToBlock(cubeY, y);
						Block block = world.getBlock(xAbs, yAbs, zAbs);

						if (block == null || block.getMaterial() != Material.air) {
							if (yAbs == seaLevel - 1 && block != Blocks.water) {
								world.setBlock(xAbs, yAbs, zAbs, Blocks.water);

								if (var9 < 0.12D) {
									world.setBlock(xAbs, yAbs + 1, zAbs, Blocks.waterlily);
								}
							}

							break;
						}
					}
				}
			}
		}
		super.decorate(world, rand, cubeX, cubeY, cubeZ);
	}
}
