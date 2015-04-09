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

import java.util.Random;

import cubicchunks.world.cube.Cube;
import net.minecraft.block.Block;
import net.minecraft.block.BlockPropertyEnum;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Facing;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class TallGrassGenerator extends SurfaceFeatureGenerator {
	
	private IBlockState block;
	private final int attempts;
	
	public TallGrassGenerator(final World world, final BlockTallGrass.TallGrassTypes tallGrassType, int attempts) {
		super(world);
		
		// HACK! Obfuscation doesn't work correctly in this class when code below is used. 
		// It fails at runtime with NoSuchMethodError:
		// java.lang.NoSuchMethodError: ama.setProperty(Lamp;Ljava/lang/Object;)Lama;
		// use metadata until it's fixed
		// actually there is method to get metadata but it doesn't have deobf mappings
		// And to use new mappings I would need to update m3l
		// and I can't do that. So I used ordinal()
		//this.block = Blocks.TALLGRASS.getDefaultState().setProperty(BlockTallGrass.type, tallGrassType);
		this.block = Blocks.TALLGRASS.getBlockStateForMetadata(tallGrassType.ordinal());
		this.attempts = attempts;
	}

	@Override
	public int getAttempts(Random rand) {
		return this.attempts;
	}
	
	@Override
	public void generateAt(final Random rand, final BlockPos pos, final Biome biome) {
		BlockPos currentPos = pos;

		for(int i = 0; i < 128; ++i) {
			BlockPos randomPos = currentPos.add(rand.nextInt(8) - rand.nextInt(8),
					rand.nextInt(4) - rand.nextInt(4),
					rand.nextInt(8) - rand.nextInt(8));
			
			if(world.hasAirAt(randomPos) && Blocks.TALLGRASS.canBePlacedAt(world, randomPos, block)) {
				this.setBlockOnly(randomPos, block);
			}	
		}
	}
}
