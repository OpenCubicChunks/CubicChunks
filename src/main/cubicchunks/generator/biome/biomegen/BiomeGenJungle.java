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

import cubicchunks.generator.populator.DecoratorHelper;
import cubicchunks.generator.populator.WorldGenAbstractTreeCube;
import cubicchunks.generator.populator.WorldGeneratorCube;
import cubicchunks.generator.populator.generators.WorldGenMegaJungleCube;
import cubicchunks.generator.populator.generators.WorldGenShrubCube;
import cubicchunks.generator.populator.generators.WorldGenTallGrassCube;
import cubicchunks.generator.populator.generators.WorldGenTreesCube;
import java.util.Random;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.world.World;

public class BiomeGenJungle extends CCBiome {
	private final boolean type;

	@SuppressWarnings("unchecked")
	public BiomeGenJungle(int id, boolean type) {
		super(id);
		this.type = type;

		CubeBiomeDecorator.DecoratorConfig cfg = this.decorator().decoratorConfig();

		if (type) {
			cfg.treesPerColumn(2);
		} else {
			cfg.treesPerColumn(50);
		}

		cfg.grassPerColumn(25);
		cfg.flowersPerColumn(4);

		if (!type) {
			this.spawnableMonsterList.add(new CCBiome.SpawnListEntry(EntityOcelot.class, 2, 1, 1));
		}

		this.spawnableCreatureList.add(new CCBiome.SpawnListEntry(EntityChicken.class, 10, 4, 4));
	}

	@Override
	public WorldGenAbstractTreeCube checkSpawnTree(Random rand) {
		return (rand.nextInt(10) == 0 ? this.worldGeneratorBigTree
				: (rand.nextInt(2) == 0 ? new WorldGenShrubCube(3, 0)
						: (!this.type && rand.nextInt(3) == 0 ? new WorldGenMegaJungleCube(false, 10, 20, 3, 3)
								: new WorldGenTreesCube(false, 4 + rand.nextInt(7), 3, 3, true))));
	}

	/**
	 * Gets a WorldGen appropriate for this biome.
	 */
	@Override
	public WorldGeneratorCube getRandomWorldGenForGrass(Random par1Random) {
		return par1Random.nextInt(4) == 0 ? new WorldGenTallGrassCube(Blocks.tallgrass, 2) : new WorldGenTallGrassCube(
				Blocks.tallgrass, 1);
	}

	@Override
	public void decorate(World world, Random rand, int x, int y, int z) {
		super.decorate(world, rand, x, y, z);

		DecoratorHelper gen = new DecoratorHelper(world, rand, x, y, z);
		gen.generateAtRandSurfacePlus32(new WorldGenMelon(), 1, 1);
	}
}
