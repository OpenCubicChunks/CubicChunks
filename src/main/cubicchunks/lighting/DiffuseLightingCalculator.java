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

import net.minecraft.block.Block;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Facing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3i;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import cubicchunks.TallWorldsMod;
import cubicchunks.generator.GeneratorStage;
import cubicchunks.util.Bits;
import cubicchunks.util.Coords;
import cubicchunks.util.FastIntQueue;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.WorldContext;
import cubicchunks.world.cube.Cube;

public class DiffuseLightingCalculator {
	
	private FastIntQueue queue;
	
	public DiffuseLightingCalculator() {
		this.queue = new FastIntQueue();
	}
	
	public boolean calculate(World world, BlockPos pos, LightType lightType) {
		world.profiler.startSection("diffuseLighting");
		
		WorldContext worldContext = WorldContext.get(world);
		
		// are there enough nearby blocks to do the lighting?
		if (!worldContext.blocksExist(pos, 16, true, GeneratorStage.LIGHTING)) {
			return false;
		}
		
		this.queue.clear();
		
		// should we add or subtract light?
		int oldLight = world.getLightAt(lightType, pos);
		int newLight = computeLightValue(world, pos, lightType);
		if (newLight > oldLight) {
			
			// seed processing with this block
			this.queue.add(packUpdate(0, 0, 0, 0));
			
		} else if (newLight < oldLight) {
			
			// subtract light from the area
			this.queue.add(packUpdate(0, 0, 0, oldLight));
			processLightSubtractions(world, pos, lightType);
			
			// reset the queue so the next processing method re-processes all the entries
			this.queue.reset();
		}
		
		// add light to the area
		processLightAdditions(world, pos, lightType);
		
		// DEBUG
		if (this.queue.size() > 32000) {
			TallWorldsMod.log.warn("{} Warning! Calculated {} light updates at ({},{},{}) for {} light.", 
				world.isClient ? "CLIENT" : "SERVER",
				this.queue.size(),
				pos.getX(), pos.getY(), pos.getZ(),
				lightType.name()
			);
		}
		
		world.profiler.endSection();
		return true;
	}
	
	private void processLightSubtractions(World world, BlockPos pos, LightType lightType) {
		world.profiler.startSection("subtractions");
		
		// TODO: optimize out news?
		BlockPos.MutableBlockPos updatePos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
		
		// for each queued light update...
		while (this.queue.hasNext()) {
			
			// unpack the update
			int update = this.queue.get();
			updatePos.setBlockPos(
				unpackUpdateDx(update) + pos.getX(),
				unpackUpdateDy(update) + pos.getY(),
				unpackUpdateDz(update) + pos.getZ()
			);
			int updateLight = unpackUpdateLight(update);
			
			// if the light changed, skip this update
			int oldLight = world.getLightAt(lightType, updatePos);
			if (oldLight != updateLight) {
				continue;
			}
			
			// set update block light to 0
			world.setLightAt(lightType, updatePos, 0);
			
			// if we ran out of light, don't propagate
			if (updateLight <= 0) {
				continue;
			}
			
			// for each neighbor block...
			for (Facing facing : Facing.values()) {
				Vec3i facingDir = facing.getBlockCoords();
				neighborPos.setBlockPos(
					updatePos.getX() + facingDir.getX(),
					updatePos.getY() + facingDir.getY(),
					updatePos.getZ() + facingDir.getZ()
				);
				
				if (!shouldUpdateLight(world, pos, neighborPos)) {
					continue;
				}
				
				// get the neighbor opacity
				int neighborOpacity = world.getBlockStateAt(neighborPos).getBlock().getOpacity();
				if (neighborOpacity < 1) {
					neighborOpacity = 1;
				}
				
				// if the neighbor block doesn't have the light we expect, bail
				int expectedLight = updateLight - neighborOpacity;
				int actualLight = world.getLightAt(lightType, neighborPos);
				if (actualLight != expectedLight) {
					continue;
				}
				
				if (this.queue.hasRoomFor(1)) {
					// queue an update to subtract light from the neighboring block
					this.queue.add(packUpdate(
						neighborPos.getX() - pos.getX(),
						neighborPos.getY() - pos.getY(),
						neighborPos.getZ() - pos.getZ(),
						expectedLight
					));
				}
			}
		}
		world.profiler.endSection();
	}
	
	private void processLightAdditions(World world, BlockPos pos, LightType lightType) {
		world.profiler.startSection("additions");
		
		// TODO: optimize out news?
		BlockPos.MutableBlockPos updatePos = new BlockPos.MutableBlockPos();
		BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

		// for each queued light update...
		while (this.queue.hasNext()) {
			
			// unpack the update
			int update = this.queue.get();
			updatePos.setBlockPos(
				unpackUpdateDx(update) + pos.getX(),
				unpackUpdateDy(update) + pos.getY(),
				unpackUpdateDz(update) + pos.getZ()
			);
			
			// skip updates that don't change the light
			int oldLight = world.getLightAt(lightType, updatePos);
			int newLight = computeLightValue(world, updatePos, lightType);
			if (newLight == oldLight) {
				continue;
			}
			
			// update the light here
			world.setLightAt(lightType, updatePos, newLight);
			
			// if we didn't get brighter, don't propagate light to the area
			if (newLight <= oldLight) {
				continue;
			}
			
			// for each neighbor block...
			for (Facing facing : Facing.values()) {
				Vec3i facingDir = facing.getBlockCoords();
				neighborPos.setBlockPos(
					updatePos.getX() + facingDir.getX(),
					updatePos.getY() + facingDir.getY(),
					updatePos.getZ() + facingDir.getZ()
				);
				
				if (!shouldUpdateLight(world, pos, neighborPos)) {
					continue;
				}
				
				// if the neighbor already has enough light, bail
				int neighborLight = world.getLightAt(lightType, neighborPos);
				if (neighborLight >= newLight) {
					continue;
				}
				
				if (this.queue.hasRoomFor(1)) {
					// queue an update to add light to the neighboring block
					this.queue.add(packUpdate(
						neighborPos.getX() - pos.getX(),
						neighborPos.getY() - pos.getY(),
						neighborPos.getZ() - pos.getZ(),
						0
					));
				}
			}
		}
		
		world.profiler.endSection();
	}
	
	private boolean shouldUpdateLight(World world, BlockPos pos, BlockPos targetPos) {
		
		// don't update blocks that are too far away
		int manhattanDistance = MathHelper.abs(targetPos.getX() - pos.getX()) 
								+ MathHelper.abs(targetPos.getY() - pos.getY()) 
								+ MathHelper.abs(targetPos.getZ() - pos.getZ());
		if (manhattanDistance > 16) {
			return false;
		}
		
		// don't update blocks we can't write to
		if (!isLightModifiable(world, targetPos.getX(), targetPos.getY(), targetPos.getZ())) {
			return false;
		}
		
		return true;
	}
	
	private boolean isLightModifiable(World world, int blockX, int blockY, int blockZ) {
		
		// get the cube
		int cubeX = Coords.blockToCube(blockX);
		int cubeY = Coords.blockToCube(blockY);
		int cubeZ = Coords.blockToCube(blockZ);
		ICubeCache cubeCache = WorldContext.get(world).getCubeCache();
		if (!cubeCache.cubeExists(cubeX, cubeY, cubeZ)) {
			return false;
		}
		Cube cube = cubeCache.getCube(cubeX, cubeY, cubeZ);
		
		return !cube.isEmpty();
	}
	
	private int computeLightValue(World world, BlockPos pos, LightType lightType) {
		
		// TODO: optimize out news?
		BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();

		if (lightType == LightType.SKY && world.canSeeSky(pos)) {
			// sky light is easy
			return 15;
		} else {
			Block block = world.getBlockStateAt(pos).getBlock();
			
			// init this block's computed light with the light it generates
			int lightAtThisBlock = lightType == LightType.SKY ? 0 : block.getBrightness();
			
			int blockOpacity = block.getOpacity();
			
			// if the block emits light and also blocks it
			if (blockOpacity >= 15 && block.getBrightness() > 0) {
				// reduce blocking
				blockOpacity = 1;
			}
			
			// min clamp on opacity
			if (blockOpacity < 1) {
				blockOpacity = 1;
			}
			
			// if the block still blocks light (meaning, it couldn't have emitted light)
			// also, an opacity of this or higher means it could block all neighbor light
			if (blockOpacity >= 15) {
				return 0;
			}
			// if the block already has the max light
			else if (lightAtThisBlock >= 14) {
				return lightAtThisBlock;
			} else {
				
				// for each block face...
				for (Facing facing : Facing.values()) {
					Vec3i facingDir = facing.getBlockCoords();
					neighborPos.setBlockPos(
						pos.getX() + facingDir.getX(),
						pos.getY() + facingDir.getY(),
						pos.getZ() + facingDir.getZ()
					);
					
					int lightFromNeighbor = world.getLightAt(lightType, neighborPos) - blockOpacity;
					
					// take the max of light from neighbors
					if (lightFromNeighbor > lightAtThisBlock) {
						lightAtThisBlock = lightFromNeighbor;
					}
					
					// short circuit to skip the rest of the neighbors
					if (lightAtThisBlock >= 14) {
						return lightAtThisBlock;
					}
				}
				
				return lightAtThisBlock;
			}
		}
	}
	
	private int packUpdate(int dx, int dy, int dz, int light) {
		return Bits.packSignedToInt(dx, 6, 0) | Bits.packSignedToInt(dy, 6, 6) | Bits.packSignedToInt(dz, 6, 12) | Bits.packUnsignedToInt(light, 6, 18);
	}
	
	private int unpackUpdateDx(int packed) {
		return Bits.unpackSigned(packed, 6, 0);
	}
	
	private int unpackUpdateDy(int packed) {
		return Bits.unpackSigned(packed, 6, 6);
	}
	
	private int unpackUpdateDz(int packed) {
		return Bits.unpackSigned(packed, 6, 12);
	}
	
	private int unpackUpdateLight(int packed) {
		return Bits.unpackUnsigned(packed, 6, 18);
	}
}
