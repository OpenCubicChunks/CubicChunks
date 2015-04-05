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
import cubicchunks.generator.GeneratorStage;
import cubicchunks.util.Coords;
import cubicchunks.util.processor.CubeProcessor;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.LightIndex;
import cubicchunks.world.WorldContext;
import cubicchunks.world.cube.Cube;

public class FirstLightProcessor extends CubeProcessor {
	
	public FirstLightProcessor(String name, ICubeCache cache, int batchSize) {
		super(name, cache, batchSize);
	}
	
	@Override
	public boolean calculate(Cube cube) {
		
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		
		// only light if the neighboring cubes exist
		WorldContext context = WorldContext.get(cube.getWorld());
		if (!context.cubeAndNeighborsExist(cube.getX(), cube.getY(), cube.getZ(), true, GeneratorStage.STRUCTURES)) {
			return false;
		}
		
		int minBlockX = Coords.cubeToMinBlock(cube.getX());
		int maxBlockX = Coords.cubeToMaxBlock(cube.getX());
		int minBlockY = Coords.cubeToMinBlock(cube.getY());
		int maxBlockY = Coords.cubeToMaxBlock(cube.getY());
		int minBlockZ = Coords.cubeToMinBlock(cube.getZ());
		int maxBlockZ = Coords.cubeToMaxBlock(cube.getZ());
		
		// update the sky light
		for (pos.x = minBlockX; pos.x <= maxBlockX; pos.x++) {
			for (pos.z = minBlockZ; pos.z <= maxBlockZ; pos.z++) {
				updateSkylight(cube, pos);
			}
		}
		
		// light blocks in this cube
		for (pos.x = minBlockX; pos.x <= maxBlockX; pos.x++) {
			for (pos.y = minBlockY; pos.y <= maxBlockY; pos.y++) {
				for (pos.z = minBlockZ; pos.z <= maxBlockZ; pos.z++) {
					boolean wasLit = lightBlock(cube, pos);
					
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
		lightXSlab(cache.getCube(cube.getX() - 1, cube.getY(), cube.getZ()), 15, pos);
		lightXSlab(cache.getCube(cube.getX() + 1, cube.getY(), cube.getZ()), 0, pos);
		lightYSlab(cache.getCube(cube.getX(), cube.getY() - 1, cube.getZ()), 15, pos);
		lightYSlab(cache.getCube(cube.getX(), cube.getY() + 1, cube.getZ()), 0, pos);
		lightZSlab(cache.getCube(cube.getX(), cube.getY(), cube.getZ() - 1), 15, pos);
		lightZSlab(cache.getCube(cube.getX(), cube.getY(), cube.getZ() + 1), 0, pos);
		
		return true;
	}
	
	private void updateSkylight(Cube cube, BlockPos.MutableBlockPos pos) {
		
		int localX = Coords.blockToLocal(pos.getX());
		int localZ = Coords.blockToLocal(pos.getZ());
		
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
			for (pos.y=cubeMinBlockY; pos.y<=cubeMaxBlockY; pos.y++) {
				cube.setLightValue(LightType.SKY, pos, 15);
			}
			
		} else if (cubeMaxBlockY < gradientMinBlockY) {
			
			// set everything to dark
			for (pos.y=cubeMinBlockY; pos.y<=cubeMaxBlockY; pos.y++) {
				cube.setLightValue(LightType.SKY, pos, 0);
			}
			
		} else {
			LightIndex index = cube.getColumn().getLightIndex();
			
			// need to calculate the light
			int light = 15;
			int startBlockY = Math.max(gradientMaxBlockY, cubeMaxBlockY);
			for (pos.y = startBlockY; pos.y >= cubeMinBlockY; pos.y--) {
				int opacity = index.getOpacity(localX, pos.y, localZ);
				if (opacity == 0 && light < 15) {
					// after something blocks light, apply a linear falloff
					opacity = 1;
				}
				
				// decrease the light
				light = Math.max(0, light - opacity);
				
				if (pos.y <= cubeMaxBlockY) {
					// apply the light
					cube.setLightValue(LightType.SKY, pos, light);
				}
			}
		}
	}
	
	private void lightXSlab(Cube cube, int localX, BlockPos.MutableBlockPos pos) {
		pos.x = Coords.localToBlock(cube.getX(), localX);
		int minBlockY = Coords.cubeToMinBlock(cube.getY());
		int maxBlockY = Coords.cubeToMaxBlock(cube.getY());
		int minBlockZ = Coords.cubeToMinBlock(cube.getZ());
		int maxBlockZ = Coords.cubeToMaxBlock(cube.getZ());
		for (pos.y = minBlockY; pos.y <= maxBlockY; pos.y++) {
			for (pos.z = minBlockZ; pos.z <= maxBlockZ; pos.z++) {
				lightBlock(cube, pos);
			}
		}
	}
	
	private void lightYSlab(Cube cube, int localY, BlockPos.MutableBlockPos pos) {
		int minBlockX = Coords.cubeToMinBlock(cube.getX());
		int maxBlockX = Coords.cubeToMaxBlock(cube.getX());
		pos.y = Coords.localToBlock(cube.getY(), localY);
		int minBlockZ = Coords.cubeToMinBlock(cube.getZ());
		int maxBlockZ = Coords.cubeToMaxBlock(cube.getZ());
		for (pos.x = minBlockX; pos.x <= maxBlockX; pos.x++) {
			for (pos.z = minBlockZ; pos.z <= maxBlockZ; pos.z++) {
				lightBlock(cube, pos);
			}
		}
	}
	
	private void lightZSlab(Cube cube, int localZ, BlockPos.MutableBlockPos pos) {
		int minBlockX = Coords.cubeToMinBlock(cube.getX());
		int maxBlockX = Coords.cubeToMaxBlock(cube.getX());
		int minBlockY = Coords.cubeToMinBlock(cube.getY());
		int maxBlockY = Coords.cubeToMaxBlock(cube.getY());
		pos.z = Coords.localToBlock(cube.getZ(), localZ);
		for (pos.x = minBlockX; pos.x <= maxBlockX; pos.x++) {
			for (pos.y = minBlockY; pos.y <= maxBlockY; pos.y++) {
				lightBlock(cube, pos);
			}
		}
	}
	
	private boolean lightBlock(Cube cube, BlockPos.MutableBlockPos pos) {
		
		int localX = Coords.blockToLocal(pos.getX());
		int localY = Coords.blockToLocal(pos.getY());
		int localZ = Coords.blockToLocal(pos.getZ());
		
		// conditions for lighting a block in phase 1:
		// must be below a non-transparent block
		// must be above an opaque block that's below sea level
		// must be a clear block
		// must have a sky
		
		// conditions for lighting a block in phase 2:
		// must be at or below an opaque block below sea level
		// must be a block light source
		
		// get the opaque block below sea level (if one exists)
		LightIndex index = cube.getColumn().getLightIndex();
		Integer opaqueBelowSeaLevelBlockY = index.getTopOpaqueBlockBelowSeaLevel(localX, localZ);
		
		boolean lightBlock = false;
		if (opaqueBelowSeaLevelBlockY == null || pos.y > opaqueBelowSeaLevelBlockY) {
			
			// get the top nontransparent block (if one exists)
			Integer topNonTransparentBlockY = index.getTopNonTransparentBlockY(localX, localZ);
			
			boolean hasSky = !cube.getColumn().getWorld().dimension.hasNoSky();
			if (hasSky && topNonTransparentBlockY != null && pos.y < topNonTransparentBlockY && index.getOpacity(localX, pos.y, localZ) == 0) {
				lightBlock = true;
			}
			
		} else if (cube.getBlockState(localX, localY, localZ).getBlock().getBrightness() > 0) {
			lightBlock = true;
		}
		
		if (lightBlock) {
			return cube.getWorld().updateLightingAt(pos);
		}
		
		return true;
	}
}
