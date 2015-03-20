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
package cubicchunks.generator.biome.biomegen;

import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.init.Blocks;
import net.minecraft.world.gen.feature.WorldGenSpikes;
import net.minecraft.world.gen.feature.WorldGenerator;

public class BiomeEndDecorator extends BiomeDecorator {
	
	protected WorldGenerator spikeGen;
	
	public BiomeEndDecorator() {
		this.spikeGen = new WorldGenSpikes(Blocks.end_stone);
	}
	
	protected void func_150513_a(CubeBiomeGenBase p_150513_1_) {
		this.generateOres();
		
		if (this.randomGenerator.nextInt(5) == 0) {
			int var2 = this.cubeX + this.randomGenerator.nextInt(16) + 8;
			int var3 = this.cubeZ + this.randomGenerator.nextInt(16) + 8;
			int var4 = this.currentWorld.getTopSolidOrLiquidBlock(var2, var3);
			this.spikeGen.generate(this.currentWorld, this.randomGenerator, var2, var4, var3);
		}
		
		if (this.cubeX == 0 && this.cubeZ == 0) {
			EntityDragon var5 = new EntityDragon(this.currentWorld);
			var5.setLocationAndAngles(0.0D, 128.0D, 0.0D, this.randomGenerator.nextFloat() * 360.0F, 0.0F);
			this.currentWorld.spawnEntityInWorld(var5);
		}
	}
}
