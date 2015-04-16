/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
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
package cubicchunks.world.biome;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.biome.Biome;
import cubicchunks.util.Coords;
import cubicchunks.world.WorldContext;
import cubicchunks.world.cube.Cube;

public class BiomeBlockReplacer {

	private final Biome baseBiome;

	public BiomeBlockReplacer(final Biome biomeToUse) {
		this.baseBiome = biomeToUse;
	}

	public void replaceBlocks(final Random rand, final Cube cube, final Cube above, final int xAbs, final int zAbs,
			final int top, final int bottom, final int alterationTop, final double depthNoiseValue) {
		replaceBlocks_do(rand, cube, above, xAbs, zAbs, top, bottom, alterationTop, depthNoiseValue);
	}

	public final void replaceBlocks_do(final Random rand, final Cube cube, final Cube above, final int xAbs,
			final int zAbs, final int top, final int bottom, final int alterationTop, final double depthNoiseValue) {
		IBlockState surfaceBlock = this.baseBiome.topBlock;
		IBlockState groundBlock = this.baseBiome.fillerBlock;
		final int seaLevel = cube.getWorld().getSeaLevel();

		// How many biome blocks left to set in column? Initially -1
		int numBlocksToChange = -1;

		// Biome blocks depth in current block column. 0 for negative values.
		final int depth = (int) (depthNoiseValue / 3.0D + 3.0D + rand.nextDouble() * 0.25D);

		// TODO:
		/*
		 * Default BuildDepth is 8,388,608. the Earth has a radius of ~6,378,100m. Not too far off. Let's make this
		 * world similar to the earth! Crust - 0 to 35km (varies between 5 and 70km thick due to the sea and mountains)
		 * Upper Mesosphere - 35km to 660km Lower Mesosphere - 660km to 2890km Outer Core - 2890km to 5150km Inner Core
		 * - 5150km to 6360km - apparently, the innermost sections of the core could be a plasma! Crazy!
		 */
		// if( yAbs <= BuildSizeEvent.getBuildDepth() + 16 + rand.nextInt( 16 ) )
		// generate bedrock in the very bottom cube and below plus random bedrock in the cube above that
		// {
		// cube.setBlockForGeneration( xRel, yRel, zRel, Blocks.bedrock );
		// } elseif( yAbs < -32768 + rand.nextInt( 256 ) ) // generate lava sea under y = -32768, plus a rough surface.
		// this is pretty fucking deep though, so nobody will reach this, probably.
		// {
		// cube.setBlockForGeneration( xRel, yRel, zRel, Blocks.lava );
		// } else

		for (int yAbs = top; yAbs >= bottom; --yAbs) {
			BlockPos pos = new BlockPos(xAbs, yAbs, zAbs);
			// Current block
			final Block block = getBlock(cube, above, pos);

			// Set numBlocksToChange to -1 when we reach air, skip everything
			// else
			if (block == null || block.getMaterial() == Material.AIR) {
				numBlocksToChange = -1;
				continue;
			}

			// Do not replace any blocks except already replaced and stone
			if (block != Blocks.STONE && block != surfaceBlock && block != groundBlock && block != Blocks.SANDSTONE) {
				continue;
			}

			boolean canSetBlock = yAbs <= alterationTop;

			// If we are 1 block below air...
			if (numBlocksToChange == -1) {
				// If depth is <= 0 - only stone
				if (depth <= 0) {
					surfaceBlock = Blocks.AIR.getDefaultState();
					groundBlock = Blocks.STONE.getDefaultState();
				}
				// If we are above or at 4 block under water and at or below one
				// block above water
				else if (yAbs >= seaLevel - 4 && yAbs <= seaLevel + 1) {
					surfaceBlock = this.baseBiome.topBlock;
					groundBlock = this.baseBiome.fillerBlock;
				}

				// If top block is air and we are below sea level use water
				// instead
				if (yAbs < seaLevel && (surfaceBlock == null || surfaceBlock.getBlock().getMaterial() == Material.AIR)) {
					if (this.baseBiome.getTemp(pos) < 0.15F) {
						// or ice if it's cold
						surfaceBlock = Blocks.ICE.getDefaultState();
					} else {
						surfaceBlock = Blocks.WATER.getDefaultState();
					}
				}

				// Set num blocks to change to current depth.
				numBlocksToChange = depth;

				if (yAbs >= seaLevel - 1) {
					// If we are above sea level
					if(canSetBlock) {
						setBlock(cube, pos, surfaceBlock);
					}
				} else if (yAbs < seaLevel - 7 - depth) {
					// gravel beaches?
					surfaceBlock = Blocks.AIR.getDefaultState();
					groundBlock = Blocks.STONE.getDefaultState();
					// setBlock(cube, pos, Blocks.GRAVEL.getDefaultState());
				} else {
					// no grass below sea level
					if(canSetBlock) {
						setBlock(cube, pos, groundBlock);
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
			--numBlocksToChange;
			if(canSetBlock) {
				setBlock(cube, pos, groundBlock);
			}

			// random sandstone generation
			if (numBlocksToChange == 0 && groundBlock == Blocks.SAND.getDefaultState()) {
				numBlocksToChange = rand.nextInt(4) + Math.max(0, yAbs - 63);
				groundBlock = Blocks.SANDSTONE.getDefaultState();
			}
		}
	}

	protected final void setBlock(final Cube cube, final BlockPos pos, final IBlockState blockState) {
		assert Coords.blockToCube(pos.getY()) == cube.getY() || Coords.blockToCube(pos.getY()) == cube.getY() - 1;

		cube.setBlockForGeneration(pos, blockState);
	}

	protected final Block getBlock(final Cube cube, final Cube above, final BlockPos pos) {		
		assert WorldContext.get(cube.getWorld()).getCubeCache()
				.cubeExists(Coords.blockToCube(pos.getX()), Coords.blockToCube(pos.getY()), Coords.blockToCube(pos.getZ()));

		if (Coords.blockToCube(pos.getY()) == cube.getY()) {// check if we're in the same cube as Cube
			// If we are in the same cube
			return cube.getBlockAt(pos);
		} else {
			// we are in cube above
			assert Coords.blockToCube(pos.getY()) == cube.getY() + 1;
			assert above != null;
			return above.getBlockAt(pos);
		}
	}
}
