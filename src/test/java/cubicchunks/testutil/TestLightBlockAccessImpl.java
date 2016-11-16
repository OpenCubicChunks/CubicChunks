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
package cubicchunks.testutil;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;

import java.util.HashMap;
import java.util.Map;

import cubicchunks.lighting.ILightBlockAccess;

public class TestLightBlockAccessImpl implements ILightBlockAccess {
	private final int size;
	private final Map<BlockPos, Integer> emittedLightBlock = new HashMap<>();
	private final Map<BlockPos, Integer> lightValuesSky = new HashMap<>();
	private final Map<BlockPos, Integer> lightValuesBlock = new HashMap<>();
	private final Map<BlockPos, Integer> opacities = new HashMap<>();

	public TestLightBlockAccessImpl(int size) {
		this.size = size;
	}

	@Override public int getBlockLightOpacity(BlockPos pos) {
		Integer opacity = opacities.get(pos);
		return opacity == null ? 0 : opacity;
	}

	@Override public int getLightFor(EnumSkyBlock lightType, BlockPos pos) {
		Integer light;
		if (lightType == EnumSkyBlock.BLOCK) {
			light = lightValuesBlock.get(pos);
		} else {
			light = lightValuesSky.get(pos);
		}
		return light == null ? 0 : light;
	}

	@Override public void setLightFor(EnumSkyBlock lightType, BlockPos pos, int val) {
		if (lightType == EnumSkyBlock.BLOCK) {
			lightValuesBlock.put(pos, val);
		} else {
			lightValuesSky.put(pos, val);
		}
	}

	@Override public boolean canSeeSky(BlockPos pos) {
		for (BlockPos p = new BlockPos(pos.getX(), size, pos.getZ()); p.getY() >= -size; p = p.down()) {
			if (getBlockLightOpacity(p) > 0) {
				return pos.getY() > p.getY();
			}
		}
		return true;
	}

	@Override public int getEmittedLight(BlockPos pos, EnumSkyBlock type) {
		Integer ret;
		if (type == EnumSkyBlock.BLOCK) {
			ret = this.emittedLightBlock.get(pos);
		} else {
			ret = canSeeSky(pos) ? 15 : 0;
		}
		return ret == null ? 0 : ret;
	}

	public void setBlockLightSource(BlockPos pos, int value) {
		this.emittedLightBlock.put(pos, value);
	}

	public void setOpacity(BlockPos pos, int opacity) {
		this.opacities.put(pos, opacity);
	}
}
