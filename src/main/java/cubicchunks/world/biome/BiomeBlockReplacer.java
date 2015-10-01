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
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import cubicchunks.util.AddressTools;
import cubicchunks.util.Coords;
import cubicchunks.util.MutableBlockPos;
import cubicchunks.world.WorldContext;
import cubicchunks.world.cube.Cube;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.BiomeGenBase;

public class BiomeBlockReplacer {

	private final Random rand;
	private final Cube cube;
	private final Cube cubeAbove;
	private final int top;
	private final int bottom;
	private final int alterationTop;
	private final int seaLevel;
	
	private BiomeGenBase baseBiome;
	private IBlockState surfaceBlock;
	private IBlockState groundBlock;

	public BiomeBlockReplacer(final Random rand, final Cube cube, final Cube cubeAbove) {
		this.rand = rand;
		this.cube = cube;
		this.cubeAbove = cubeAbove;
		this.top = Coords.cubeToMaxBlock(cubeAbove.getY());
		this.bottom = Coords.cubeToMinBlock(cube.getY());
		this.alterationTop = Coords.cubeToMaxBlock(cube.getY());
		this.seaLevel = cube.getWorld().provider.getAverageGroundLevel();
	}

	public void replaceBlocks(final BiomeGenBase biomeToUse, final int xAbs, final int zAbs, final double depthNoiseValue) {
		setBiome(biomeToUse);
		process(xAbs, zAbs, depthNoiseValue);
	}

	private void setBiome(final BiomeGenBase biomeToUse) {
		this.baseBiome = biomeToUse;
		this.surfaceBlock = biomeToUse.topBlock;
		this.groundBlock = biomeToUse.fillerBlock;
	}

	public final void process(final int xAbs, final int zAbs, final double depthNoiseValue) {
		// How many biome blocks left to set in column? Initially -1
		int blocksToChange = -1;

		// Biome blocks depth in current block column. 0 for negative values.
		final int depth = (int) (depthNoiseValue / 3.0D + 3.0D + this.rand.nextDouble() * 0.25D);

		MutableBlockPos pos = new MutableBlockPos();

		/*
		 * Default BuildDepth is 8,388,608. the Earth has a radius of ~6,378,100m. Not too far off. Let's make this
		 * world similar to the earth! Crust - 0 to 35km (varies between 5 and 70km thick due to the sea and mountains)
		 * Upper Mesosphere - 35km to 660km Lower Mesosphere - 660km to 2890km Outer Core - 2890km to 5150km Inner Core
		 * - 5150km to 6360km - apparently, the innermost sections of the core could be a plasma! Crazy!
		 */
		for (int yAbs = this.top; yAbs >= this.bottom; --yAbs) {
			pos.setBlockPos(xAbs, yAbs, zAbs);
			
			boolean canSetBlock = yAbs <= this.alterationTop;
			
			if (yAbs <= ((AddressTools.MinY + 64) << 4) + this.rand.nextInt(16)) {
				if (canSetBlock) {
					setBlock(this.cube, pos, Blocks.bedrock.getDefaultState());
				}
			} else if (yAbs < -32768 + this.rand.nextInt(256)) {
				if (canSetBlock) {
					setBlock(this.cube, pos, Blocks.lava.getDefaultState());
				}
			} else {
				// Current block
				final Block block = getBlock(this.cube, this.cubeAbove, pos);
	
				// Set numBlocksToChange to -1 when we reach air, skip everything else
				if (block == null || block.getMaterial() == Material.air) {
					blocksToChange = -1;
					continue;
				}
	
				// Do not replace any blocks except already replaced and stone
				if (block != Blocks.stone && block != this.surfaceBlock && block != this.groundBlock && block != Blocks.sandstone) {
					continue;
				}
	
				// If we are 1 block below air...
				if (blocksToChange == -1) {
					// If depth is <= 0 - only stone
					if (depth <= 0) {
						this.surfaceBlock = Blocks.air.getDefaultState();
						this.groundBlock = Blocks.stone.getDefaultState();
					}
					// If we are above or at 4 block under water and at or below one block above water
					else if (yAbs >= this.seaLevel - 4 && yAbs <= this.seaLevel + 1) {
						// also resets the surface and ground blocks to the biome's defaults
						this.surfaceBlock = this.baseBiome.topBlock;
						this.groundBlock = this.baseBiome.fillerBlock;
					}
	
					// If top block is air and we are below sea level use water instead
					if (yAbs < this.seaLevel && (this.surfaceBlock == null || this.surfaceBlock.getBlock().getMaterial() == Material.air)) {
						if (this.baseBiome.getFloatTemperature(pos) < 0.15F) {
							// or ice if it's cold
							this.surfaceBlock = Blocks.ice.getDefaultState();
						} else {
							this.surfaceBlock = Blocks.water.getDefaultState();
						}
					}
	
					// Set num blocks to change to current depth.
					blocksToChange = depth;
					
					if (yAbs < this.seaLevel - 7 - depth) {
						this.surfaceBlock = Blocks.air.getDefaultState();
						this.groundBlock = Blocks.stone.getDefaultState();
					}
					
					if(canSetBlock) {
						if (yAbs >= this.seaLevel - 1) {
							// If we are above sea level
							setBlock(this.cube, pos, this.surfaceBlock);
						} else if (yAbs < this.seaLevel - 7 - depth) {
							// Covers the ocean floor with gravel.
							setBlock(this.cube, pos, Blocks.gravel.getDefaultState());
						} else {
							// no surface blocks below sea level
							setBlock(this.cube, pos, this.groundBlock);
						}
					}
	
					continue;
				}
	
				// Nothing left to do...
				// so continue
				if (blocksToChange <= 0) {
					continue;
				}
				
				// Decrease blocks to change
				--blocksToChange;
				
				if(canSetBlock) {
					setBlock(this.cube, pos, this.groundBlock);
				}
	
				blocksToChange = placeRandomSandstone(blocksToChange, yAbs);
			}
		}
	}

	private int placeRandomSandstone(final int numBlocksToChange, final int yAbs) {
		int result = 0;
		if (numBlocksToChange == 0 && this.groundBlock == Blocks.sand.getDefaultState()) {
			result = this.rand.nextInt(4) + Math.max(0, yAbs - 63);
			this.groundBlock = Blocks.sandstone.getDefaultState();
		}
		return result;
	}

	protected final void setBlock(final Cube cube, final BlockPos pos, final IBlockState blockState) {
		assert Coords.blockToCube(pos.getY()) == cube.getY() || Coords.blockToCube(pos.getY()) == cube.getY() - 1;

		cube.setBlockForGeneration(pos, blockState);
	}

	protected final Block getBlock(final Cube cube, final Cube cubeAbove, final BlockPos pos) {
		assert WorldContext.get(cube.getWorld()).getCubeCache()
				.cubeExists(Coords.blockToCube(pos.getX()), Coords.blockToCube(pos.getY()),	Coords.blockToCube(pos.getZ()));

		if (Coords.blockToCube(pos.getY()) == cube.getY()) {// check if we're in the same cube as Cube
			// If we are in the same cube
			return cube.getBlockAt(pos);
		} else {
			// we are in cube above
			assert Coords.blockToCube(pos.getY()) == cube.getY() + 1;
			assert cubeAbove != null;
			return cubeAbove.getBlockAt(pos);
		}
	}
}
