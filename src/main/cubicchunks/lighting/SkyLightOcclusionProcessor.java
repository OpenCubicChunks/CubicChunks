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
package cubicchunks.lighting;

import net.minecraft.util.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import cubicchunks.util.Coords;
import cubicchunks.util.processor.BlockColumnProcessor;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.WorldContext;
import cubicchunks.world.column.Column;

public class SkyLightOcclusionProcessor extends BlockColumnProcessor {
	
	public SkyLightOcclusionProcessor(String name, ICubeCache provider, int batchSize) {
		super(name, provider, batchSize);
	}
	
	@Override
	public boolean calculate(Column column, int localX, int localZ, int blockX, int blockZ) {
		World world = column.getWorld();
		
		// get the height
		Integer height = column.getSkylightBlockY(localX, localZ);
		if (height == null) {
			// nothing to do
			return true;
		}
		
		// get the min column height among neighbors
		int minHeight1 = world.getChunkMinimumHeight(blockX - 1, blockZ);
		int minHeight2 = world.getChunkMinimumHeight(blockX + 1, blockZ);
		int minHeight3 = world.getChunkMinimumHeight(blockX, blockZ - 1);
		int minHeight4 = world.getChunkMinimumHeight(blockX, blockZ + 1);
		int minNeighborHeight = Math.min(minHeight1, Math.min(minHeight2, Math.min(minHeight3, minHeight4)));
		
		boolean actuallyUpdated = false;
		actuallyUpdated |= updateSkylight(world, blockX, blockZ, minNeighborHeight);
		actuallyUpdated |= updateSkylight(world, blockX - 1, blockZ, height);
		actuallyUpdated |= updateSkylight(world, blockX + 1, blockZ, height);
		actuallyUpdated |= updateSkylight(world, blockX, blockZ - 1, height);
		actuallyUpdated |= updateSkylight(world, blockX, blockZ + 1, height);
		
		if (actuallyUpdated) {
			column.setModified(true);
		}
		
		return true;
	}
	
	private boolean updateSkylight(World world, int blockX, int blockZ, int maxBlockY) {
		
		// get the skylight block for this block column
		Column column = WorldContext.get(world).getCubeCache().getColumn(
			Coords.blockToCube(blockX),
			Coords.blockToCube(blockZ)
		);
		int localX = Coords.blockToLocal(blockX);
		int localZ = Coords.blockToLocal(blockZ);
		Integer height = column.getSkylightBlockY(localX, localZ);
		if (height == null) {
			// nothing to do
			return false;
		}
		
		if (height > maxBlockY) {
			return updateSkylight(world, blockX, blockZ, maxBlockY, height);
		} else if (height < maxBlockY) {
			return updateSkylight(world, blockX, blockZ, height, maxBlockY);
		}
		
		return false;
	}
	
	private boolean updateSkylight(World world, int blockX, int blockZ, int minBlockY, int maxBlockY) {
		if (maxBlockY <= minBlockY) {
			return false;
		}
		
		// TODO: optimize out news?
		BlockPos bottom = new BlockPos(blockX, minBlockY, blockZ);
		BlockPos top = new BlockPos(blockX, maxBlockY, blockZ);
		if (!world.checkBlockRangeIsInWorld(bottom, top)) {
			return false;
		}
		
		for (int blockY = minBlockY; blockY <= maxBlockY; blockY++) {
			WorldContext.get(world).getLightingManager().computeDiffuseLighting(new BlockPos(blockX, blockY, blockZ), LightType.SKY);
		}
		
		return true;
	}
}
