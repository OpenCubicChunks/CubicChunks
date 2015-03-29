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
package cubicchunks.lighting;

import net.minecraft.util.BlockPos;
import net.minecraft.world.LightType;
import cubicchunks.CubeCache;
import cubicchunks.CubeProviderTools;
import cubicchunks.CubeWorld;
import cubicchunks.util.Coords;
import cubicchunks.util.CubeProcessor;
import cubicchunks.world.Cube;
import cubicchunks.world.LightIndex;

public class FirstLightProcessor extends CubeProcessor {
	
	public FirstLightProcessor(String name, CubeCache cache, int batchSize) {
		super(name, cache, batchSize);
	}
	
	@Override
	public boolean calculate(Cube cube) {
		// only light if the neighboring cubes exist
		CubeCache cache = ((CubeWorld)cube.getWorld()).getCubeCache();
		if (!CubeProviderTools.cubeAndNeighborsExist(cache, cube.getX(), cube.getY(), cube.getZ())) {
			return false;
		}
		
		// update the sky light
		for (int localX = 0; localX < 16; localX++) {
			for (int localZ = 0; localZ < 16; localZ++) {
				updateSkylight(cube, localX, localZ);
			}
		}
		
		// light blocks in this cube
		for (int localX = 0; localX < 16; localX++) {
			for (int localY = 0; localY < 16; localY++) {
				for (int localZ = 0; localZ < 16; localZ++) {
					boolean wasLit = lightBlock(cube, localX, localY, localZ);
					
					// if the lighting failed, then try again later
					if (!wasLit) {
						return false;
					}
				}
			}
		}
		
		// populate the nearby faces of adjacent cubes
		// this is for cases when a sheer wall is up against an empty cube
		// unless this is called, the wall will not get directly lit
		lightXSlab(cache.getCube(cube.getX() - 1, cube.getY(), cube.getZ()), 15);
		lightXSlab(cache.getCube(cube.getX() + 1, cube.getY(), cube.getZ()), 0);
		lightYSlab(cache.getCube(cube.getX(), cube.getY() - 1, cube.getZ()), 15);
		lightYSlab(cache.getCube(cube.getX(), cube.getY() + 1, cube.getZ()), 0);
		lightZSlab(cache.getCube(cube.getX(), cube.getY(), cube.getZ() - 1), 15);
		lightZSlab(cache.getCube(cube.getX(), cube.getY(), cube.getZ() + 1), 0);
		
		return true;
	}
	
	private void updateSkylight(Cube cube, int localX, int localZ) {
		// compute bounds on the sky light gradient
		Integer gradientMaxBlockY = cube.getColumn().getSkylightBlockY(localX, localZ);
		Integer gradientMinBlockY = null;
		if (gradientMaxBlockY != null) {
			gradientMinBlockY = gradientMaxBlockY - 15;
		} else {
			// there are no solid blocks in this column. Everything should be skylit
			gradientMaxBlockY = Integer.MIN_VALUE;
		}
		
		// get the cube bounds
		int cubeMinBlockY = Coords.cubeToMinBlock(cube.getY());
		int cubeMaxBlockY = Coords.cubeToMaxBlock(cube.getY());
		
		// could this sky light possibly reach this cube?
		if (cubeMinBlockY > gradientMaxBlockY) {
			// set everything to sky light
			for (int localY = 0; localY < 16; localY++) {
				cube.setLightValue(LightType.SKY, new BlockPos(localX, localY, localZ), 15);
			}
		} else if (cubeMaxBlockY < gradientMinBlockY) {
			// set everything to dark
			for (int localY = 0; localY < 16; localY++) {
				cube.setLightValue(LightType.SKY, new BlockPos(localX, localY, localZ), 0);
			}
		} else {
			LightIndex index = cube.getColumn().getLightIndex();
			
			// need to calculate the light
			int light = 15;
			int startBlockY = Math.max(gradientMaxBlockY, cubeMaxBlockY);
			for (int blockY = startBlockY; blockY >= cubeMinBlockY; blockY--) {
				int opacity = index.getOpacity(localX, blockY, localZ);
				if (opacity == 0 && light < 15) {
					// after something blocks light, apply a linear falloff
					opacity = 1;
				}
				
				// decrease the light
				light = Math.max(0, light - opacity);
				
				if (blockY <= cubeMaxBlockY) {
					// apply the light
					int localY = Coords.blockToLocal(blockY);
					cube.setLightValue(LightType.SKY, new BlockPos(localX, localY, localZ), light);
				}
			}
		}
	}
	
	private void lightXSlab(Cube cube, int localX) {
		for (int localY = 0; localY < 16; localY++) {
			for (int localZ = 0; localZ < 16; localZ++) {
				lightBlock(cube, localX, localY, localZ);
			}
		}
	}
	
	private void lightYSlab(Cube cube, int localY) {
		for (int localX = 0; localX < 16; localX++) {
			for (int localZ = 0; localZ < 16; localZ++) {
				lightBlock(cube, localX, localY, localZ);
			}
		}
	}
	
	private void lightZSlab(Cube cube, int localZ) {
		for (int localX = 0; localX < 16; localX++) {
			for (int localY = 0; localY < 16; localY++) {
				lightBlock(cube, localX, localY, localZ);
			}
		}
	}
	
	private boolean lightBlock(Cube cube, int localX, int localY, int localZ) {
		// conditions for lighting a block in phase 1:
		// must be below a non-transparent block
		// must be above an opaque block that's below sea level
		// must be a clear block
		// must have a sky
		
		// conditions for lighting a block in phase 2:
		// must be at or below an opaque block below sea level
		// must be a block light source
		
		// get the opaque block below sea level (if one exists)
		int blockY = Coords.localToBlock(cube.getY(), localY);
		LightIndex index = cube.getColumn().getLightIndex();
		Integer opaqueBelowSeaLevelBlockY = index.getTopOpaqueBlockBelowSeaLevel(localX, localZ);
		
		boolean lightBlock = false;
		if (opaqueBelowSeaLevelBlockY == null || blockY > opaqueBelowSeaLevelBlockY) {
			// get the top nontransparent block (if one exists)
			Integer topNonTransparentBlockY = index.getTopNonTransparentBlockY(localX, localZ);
			
			boolean hasSky = !cube.getColumn().getWorld().dimension.hasNoSky();
			if (hasSky && topNonTransparentBlockY != null && blockY < topNonTransparentBlockY && index.getOpacity(localX, blockY, localZ) == 0) {
				lightBlock = true;
			}
		} else if (cube.getBlockState(localX, localY, localZ).getBlock().getBrightness() > 0) {
			lightBlock = true;
		}
		
		if (lightBlock) {
			int blockX = Coords.localToBlock(cube.getX(), localX);
			int blockZ = Coords.localToBlock(cube.getZ(), localZ);
			
			return cube.getWorld().updateLightingAt(new BlockPos(blockX, blockY, blockZ));
		}
		
		return true;
	}
}
