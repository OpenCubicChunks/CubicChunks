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
package cubicchunks.worldgen.generator.custom.features;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cubicchunks.world.ICubicWorld;

public class SurfaceBlockReplacer extends SurfaceFeatureGenerator {
	private final IBlockState block;
	private final List<Block> replacable;
	private final List<Block> allowedAboveSurface;
	private final int radius;
	private final int height;

	private SurfaceBlockReplacer(Builder builder) {
		super(builder.world);
		this.block = builder.block;
		this.replacable = new ArrayList<Block>(builder.replacable);
		this.allowedAboveSurface = new ArrayList<Block>(builder.allowedAboveSurface);
		this.radius = builder.radius;
		this.height = builder.height;
	}

	@Override
	public void generateAt(Random rand, BlockPos pos, Biome biome) {
		double radiusSq = this.radius*this.radius;
		for (int x = -this.radius; x <= this.radius; x++) {
			for (int y = -this.height; y <= this.height; y++) {
				for (int z = -this.radius; z <= this.radius; z++) {
					if (x*x + z*z > radiusSq) {
						continue;
					}
					BlockPos currentPos = pos.add(x, y, z);
					Block currrentBlock = getBlockState(currentPos).getBlock();
					if (this.canReplace(currrentBlock)) {
						this.setBlockOnly(currentPos, this.block);
					}
				}
			}
		}
	}

	@Override
	protected boolean isSurfaceAt(BlockPos pos) {
		IBlockState stateBelow = getBlockState(pos.down());
		Block below = stateBelow.getBlock();
		if (!below.isOpaqueCube(stateBelow)) {
			return false;
		}
		Block blockAboveSurface = getBlockState(pos).getBlock();
		for (Block b : this.allowedAboveSurface) {
			if (blockAboveSurface == b) {
				return true;
			}
		}
		return false;
	}

	private boolean canReplace(Block block) {
		for (Block b : this.replacable) {
			if (block == b) {
				return true;
			}
		}
		return false;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private final List<Block> replacable;
		private final List<Block> allowedAboveSurface;
		private IBlockState block;
		private int radius;
		private int height;
		private ICubicWorld world;

		private Builder() {
			this.replacable = new ArrayList<Block>(2);
			this.allowedAboveSurface = new ArrayList<Block>(2);
		}

		public Builder setBlock(IBlockState block) {
			this.block = block;
			return this;
		}

		public Builder block(Block block) {
			this.block = block.getDefaultState();
			return this;
		}

		public Builder radius(int radius) {
			this.radius = radius;
			return this;
		}

		public Builder height(int height) {
			this.height = height;
			return this;
		}

		public Builder addReplacable(Block block) {
			this.replacable.add(block);
			return this;
		}

		public Builder addAllowedAboveSurface(Block block) {
			this.allowedAboveSurface.add(block);
			return this;
		}

		public Builder world(ICubicWorld world) {
			this.world = world;
			return this;
		}

		public SurfaceBlockReplacer build() {
			return new SurfaceBlockReplacer(this);
		}
	}
}
