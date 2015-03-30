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

import cubicchunks.generator.populator.WorldGeneratorCube;
import cubicchunks.generator.populator.generators.WorldGenSpikesCube;
import net.minecraft.block.Blocks;
import net.minecraft.entity.boss.EntityDragon;

public class BiomeEndDecorator extends CubeBiomeDecorator {
	protected WorldGeneratorCube spikeGen;

	public BiomeEndDecorator() {
		this.spikeGen = new WorldGenSpikesCube(Blocks.end_stone);
	}

	@Override
	protected void decorate_do(CCBiome biome) {
		this.generateOres();

		gen.generateAtSurface(this.spikeGen, 1, 0.2);

		if (gen.chunk_X == 0 && gen.chunk_Z == 0) {
			EntityDragon dragon = new EntityDragon(this.currentWorld);
			dragon.setLocationAndAngles(0.0D, 128.0D, 0.0D, this.randomGenerator.nextFloat() * 360.0F, 0.0F);
			gen.world.spawnEntityInWorld(dragon);
		}
	}
}