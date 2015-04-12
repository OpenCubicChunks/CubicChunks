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
package cubicchunks.world.cube;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.IBlockEntityProvider;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk.ChunkEntityCreationType;
import net.minecraft.world.chunk.storage.ChunkSection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;

import cubicchunks.generator.GeneratorStage;
import cubicchunks.util.AddressTools;
import cubicchunks.util.Coords;
import cubicchunks.util.CubeBlockMap;
import cubicchunks.world.EntityContainer;
import cubicchunks.world.column.Column;

public class Cube {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Cube.class);
	
	private World world;
	private Column column;
	private int cubeX;
	private int cubeY;
	private int cubeZ;
	private boolean isModified;
	private ChunkSection storage;
	private EntityContainer entities;
	private CubeBlockMap<BlockEntity> blockEntities;
	private GeneratorStage generatorStage;
	
	public Cube(World world, Column column, int x, int y, int z, boolean isModified) {
		this.world = world;
		this.column = column;
		this.cubeX = x;
		this.cubeY = y;
		this.cubeZ = z;
		this.isModified = isModified;
		
		this.storage = null;
		this.entities = new EntityContainer();
		this.blockEntities = new CubeBlockMap<BlockEntity>();
		this.generatorStage = null;
	}
	
	public boolean isEmpty() {
		return this.storage == null;
	}
	
	public void setEmpty(boolean isEmpty) {
		if (isEmpty) {
			this.storage = null;
		} else {
			this.storage = new ChunkSection(Coords.cubeToMinBlock(this.cubeY), !this.world.dimension.hasNoSky());
		}
	}
	
	public GeneratorStage getGeneratorStage() {
		return this.generatorStage;
	}
	
	public void setGeneratorStage(GeneratorStage val) {
		this.generatorStage = val;
	}
	
	public long getAddress() {
		return AddressTools.getAddress(this.cubeX, this.cubeY, this.cubeZ);
	}
	
	public World getWorld() {
		return this.world;
	}
	
	public Column getColumn() {
		return this.column;
	}
	
	public int getX() {
		return this.cubeX;
	}
	
	public int getY() {
		return this.cubeY;
	}
	
	public int getZ() {
		return this.cubeZ;
	}
	
	public boolean containsBlockPos(BlockPos blockPos) {
		return this.cubeX == Coords.blockToCube(blockPos.getX())
			&& this.cubeY == Coords.blockToCube(blockPos.getY())
			&& this.cubeZ == Coords.blockToCube(blockPos.getZ());
	}
	
	public ChunkSection getStorage() {
		return this.storage;
	}
	
	public Block getBlockAt(final BlockPos pos) {
		int x = Coords.blockToLocal(pos.getX());
		int y = Coords.blockToLocal(pos.getY());
		int z = Coords.blockToLocal(pos.getZ());
		return getBlockAt(x, y, z);
	}
	
	public Block getBlockAt(final int localX, final int localY, final int localZ) {
		if (isEmpty()) {
			return Blocks.AIR;
		}
		return this.storage.getBlockAt(localX, localY, localZ);
	}
	
	public IBlockState getBlockState(BlockPos pos) {
		if (isEmpty()) {
			return Blocks.AIR.getDefaultState();
		}
		int x = Coords.blockToLocal(pos.getX());
		int y = Coords.blockToLocal(pos.getY());
		int z = Coords.blockToLocal(pos.getZ());
		return getBlockState(x, y, z);
	}
	
	public IBlockState getBlockState(int localX, int localY, int localZ) {
		if (isEmpty()) {
			return Blocks.AIR.getDefaultState();
		}
		return this.storage.getBlockStateAt(localX, localY, localZ);
	}
	
	public IBlockState setBlockState(BlockPos pos, IBlockState newBlockState) {
		
		// did anything actually change?
		IBlockState oldBlockState = getBlockState(pos);
		if (newBlockState == oldBlockState) {
			return null;
		}
		
		// make sure we're not empty
		if (isEmpty()) {
			setEmpty(false);
		}
		
		int x = Coords.blockToLocal(pos.getX());
		int y = Coords.blockToLocal(pos.getY());
		int z = Coords.blockToLocal(pos.getZ());

		// set the block
		this.storage.setBlockStateAt(x, y, z, newBlockState);
		
		Block newBlock = newBlockState.getBlock();
		Block oldBlock = oldBlockState.getBlock();
		
		if (newBlock != oldBlock) {
			if (!this.world.isClient) {
				// on the server, break the old block
				oldBlock.onRemoved(this.world, pos, oldBlockState);
			} else if (oldBlock instanceof IBlockEntityProvider) {
				// on the client, remove the tile entity
				this.world.removeBlockEntity(pos);
			}
		}
		
		// did the block change work correctly?
		if (this.storage.getBlockAt(x, y, z) != newBlock) {
			return null;
		}
		this.isModified = true;
		
		if (oldBlock instanceof IBlockEntityProvider) {
			// update tile entity
			BlockEntity blockEntity = getBlockEntity(pos, ChunkEntityCreationType.CHECK);
			if (blockEntity != null) {
				blockEntity.updateContainingBlockInfo();
			}
		}
		
		if (!this.world.isClient && newBlock != oldBlock) {
			// on the server, tell the block it was added
			newBlock.onSet(this.world, pos, newBlockState);
		}
		
		if (newBlock instanceof IBlockEntityProvider) {
			// make sure the tile entity is good
			BlockEntity blockEntity = getBlockEntity(pos, ChunkEntityCreationType.CHECK);
			if (blockEntity == null) {
				blockEntity = ((IBlockEntityProvider)newBlock).getBlockEntity(this.world, newBlock.getMetadataForBlockState(newBlockState));
				this.world.setBlockEntity(pos, blockEntity);
			}
			if (blockEntity != null) {
				blockEntity.updateContainingBlockInfo();
			}
		}
		
		return oldBlockState;
	}
	
	public IBlockState setBlockForGeneration(BlockPos pos, IBlockState newBlockState) {
		
		IBlockState oldBlockState = getBlockState(pos);
		
		// did anything actually change?
		if (newBlockState == oldBlockState) {
			return null;
		}
		
		// make sure we're not empty
		if (isEmpty()) {
			setEmpty(false);
		}
		
		int x = Coords.blockToLocal(pos.getX());
		int y = Coords.blockToLocal(pos.getY());
		int z = Coords.blockToLocal(pos.getZ());

		// set the block
		this.storage.setBlockStateAt(x, y, z, newBlockState);
		
		Block newBlock = newBlockState.getBlock();
		
		// did the block change work correctly?
		if (this.storage.getBlockAt(x, y, z) != newBlock) {
			return null;
		}
		this.isModified = true;
		
		// update the column light index
		int blockY = Coords.localToBlock(this.cubeY, y);
		this.column.getLightIndex().setOpacity(x, blockY, z, newBlock.getOpacity());
		
		return oldBlockState;
	}
	
	public boolean hasBlocks() {
		if (isEmpty()) {
			return false;
		}
		
		return !this.storage.isSectionEmpty();
	}
	
	public Iterable<BlockEntity> getBlockEntities() {
		return this.blockEntities.values();
	}
	
	public EntityContainer getEntityContainer() {
		return this.entities;
	}
	
	public void addEntity(Entity entity) {
		
		// make sure the entity is in this cube
		int cubeX = Coords.getCubeXForEntity(entity);
		int cubeY = Coords.getCubeYForEntity(entity);
		int cubeZ = Coords.getCubeZForEntity(entity);
		if (cubeX != this.cubeX || cubeY != this.cubeY || cubeZ != this.cubeZ) {
			LOGGER.warn(String.format("Entity %s in cube (%d,%d,%d) added to cube (%d,%d,%d)!", entity.getClass().getName(), cubeX, cubeY, cubeZ, this.cubeX, this.cubeY, this.cubeZ));
		}
		
		// tell the entity it's in this cube
		entity.addedToChunk = true;
		entity.chunkX = this.cubeX;
		entity.chunkY = this.cubeY;
		entity.chunkZ = this.cubeZ;
		
		this.entities.add(entity);
		this.isModified = true;
	}
	
	public boolean removeEntity(Entity entity) {
		boolean wasRemoved = this.entities.remove(entity);
		if (wasRemoved) {
			entity.addedToChunk = false;
			this.isModified = true;
		} else {
			LOGGER.warn(String.format("%s Tried to remove entity %s from cube (%d,%d,%d), but it was not there. Entity thinks it's in cube (%d,%d,%d)",
				this.world.isClient ? "CLIENT" : "SERVER",
				entity.getClass().getName(),
				this.cubeX, this.cubeY, this.cubeZ,
				entity.chunkX, entity.chunkY, entity.chunkZ
			));
		}
		return wasRemoved;
	}
	
	public Iterable<Entity> entities() {
		return this.entities.entities();
	}
	
	public void findEntitiesExcept(Entity excludedEntity, AxisAlignedBB queryBox, List<Entity> out, Predicate<? super Entity> predicate) {
		this.entities.findEntitiesExcept(excludedEntity, queryBox, out, predicate);
	}
	
	public <T extends Entity> void findEntities(Class<? extends T> entityType, AxisAlignedBB queryBox, List<T> out, Predicate<? super T> predicate) {
		this.entities.findEntities(entityType, queryBox, out, predicate);
	}
	
	public void getMigratedEntities(List<Entity> out) {
		for (Entity entity : this.entities.entities()) {
			int cubeX = Coords.getCubeXForEntity(entity);
			int cubeY = Coords.getCubeYForEntity(entity);
			int cubeZ = Coords.getCubeZForEntity(entity);
			if (cubeX != this.cubeX || cubeY != this.cubeY || cubeZ != this.cubeZ) {
				out.add(entity);
			}
		}
	}
	
	public BlockEntity getBlockEntity(BlockPos pos, ChunkEntityCreationType creationType) {
        
		BlockEntity blockEntity = this.blockEntities.get(pos);
		if (blockEntity == null) {
			
			if (creationType == ChunkEntityCreationType.IMMEDIATE) {
				blockEntity = createBlockEntity(pos);
				this.world.setBlockEntity(pos, blockEntity);
			} else if (creationType == ChunkEntityCreationType.IMMEDIATE) {
				throw new Error("TODO: implement block entity creation queue!");
			}
			
			// is this block not supposed to have a tile entity?
			IBlockState blockState = getBlockState(pos);
			Block block = blockState.getBlock();
			int meta = block.getMetadataForBlockState(blockState);
			
			if (!block.hasBlockEntity()) {
				return null;
			}
			
			// make a new tile entity for the block
			blockEntity = ((IBlockEntityProvider)block).getBlockEntity(this.world, meta);
			this.world.setBlockEntity(pos, blockEntity);
			
		} else if (blockEntity.isInvalid()) {
			
			// remove the tile entity
			this.blockEntities.remove(pos);
			blockEntity = null;
		}
		
		return blockEntity;
	}
	
	private BlockEntity createBlockEntity(BlockPos pos) {
		
		IBlockState blockState = getBlockState(pos);
		Block block = blockState.getBlock();
		int meta = block.getMetadataForBlockState(blockState);
		
		if (block.hasBlockEntity()) {
			return ((IBlockEntityProvider)block).getBlockEntity(this.world, meta);
		}
		return null;
	}

	public void addBlockEntity(BlockPos pos, BlockEntity blockEntity) {
		
		// update the tile entity
		blockEntity.setLevel(this.world);
		blockEntity.setPosition(pos);
		
		// is this block supposed to have a tile entity?
		if (getBlockState(pos).getBlock() instanceof IBlockEntityProvider) {
			
			// cleanup the old tile entity
			BlockEntity oldBlockEntity = this.blockEntities.get(pos);
			if (oldBlockEntity != null) {
				oldBlockEntity.setInvalid();
			}
			
			// install the new tile entity
			blockEntity.setValid();
			this.blockEntities.put(pos, blockEntity);
			this.isModified = true;
		}
	}
	
	public void removeBlockEntity(BlockPos pos) {
		BlockEntity blockEntity = this.blockEntities.remove(pos);
		if (blockEntity != null) {
			blockEntity.setInvalid();
			this.isModified = true;
		}
	}
	
	public void onLoad() {
		
		// tell the world about entities
		for (Entity entity : this.entities.entities()) {
			entity.onChunkLoad();
		}
		
		this.world.loadEntitiesInBulk(this.entities.entities());
		
		// tell the world about tile entities
		this.world.addBlockEntities(this.blockEntities.values());
	}
	
	public void onUnload() {
		
		// tell the world to forget about entities
		this.world.unloadEntitiesInBulk(this.entities.entities());
		
		// tell the world to forget about tile entities
		for (BlockEntity blockEntity : this.blockEntities.values()) {
			this.world.removeBlockEntity(blockEntity.getBlockCoords());
		}
	}
	
	public boolean needsSaving() {
		return this.entities.needsSaving(this.world.getGameTime()) || this.isModified;
	}
	
	public void markSaved() {
		this.entities.markSaved(this.world.getGameTime());
		this.isModified = false;
	}
	
	public boolean isUnderground(BlockPos pos) {
		int x = Coords.blockToLocal(pos.getX());
		int z = Coords.blockToLocal(pos.getZ());
		Integer topNonTransparentBlockY = this.column.getLightIndex().getTopNonTransparentBlockY(x, z);
		if (topNonTransparentBlockY == null) {
			return false;
		}
		return pos.getY() < topNonTransparentBlockY;
	}
	
	public int getBrightestLight(BlockPos pos, int skyLightDampeningTerm) {
		
		// get sky light
		int skyLight = getLightValue(LightType.SKY, pos);
		skyLight -= skyLightDampeningTerm;
		
		// get block light
		int blockLight = getLightValue(LightType.BLOCK, pos);
		
		// FIGHT!!!
		if (blockLight > skyLight) {
			return blockLight;
		}
		return skyLight;
	}
	
	public int getLightValue(LightType lightType, BlockPos pos) {
		
		int x = Coords.blockToLocal(pos.getX());
		int y = Coords.blockToLocal(pos.getY());
		int z = Coords.blockToLocal(pos.getZ());
		
		switch (lightType) {
			case SKY:
				if (!this.world.dimension.hasNoSky()) {
					if (isEmpty()) {
						if (isUnderground(pos)) {
							return 0;
						} else {
							return 15;
						}
					}
					
					return this.storage.getSkyLightAtCoords(x, y, z);
				} else {
					return 0;
				}
				
			case BLOCK:
				if (isEmpty()) {
					return 0;
				}
				
				return this.storage.getBlockLightAtCoords(x, y, z);
				
			default:
				return lightType.defaultValue;
		}
	}
	
	public void setLightValue(LightType lightType, BlockPos pos, int light) {
		
		// make sure we're not empty
		if (isEmpty()) {
			setEmpty(false);
		}
		
		int x = Coords.blockToLocal(pos.getX());
		int y = Coords.blockToLocal(pos.getY());
		int z = Coords.blockToLocal(pos.getZ());
		
		switch (lightType) {
			case SKY:
				if (!this.world.dimension.hasNoSky()) {
					this.storage.setSkyLightAtCoords(x, y, z, light);
					this.isModified = true;
				}
			break;
			
			case BLOCK:
				this.storage.setBlockLightAtCoords(x, y, z, light);
				this.isModified = true;
			break;
		}
	}
	
	public void doRandomTicks() {
		
		if (isEmpty() || this.storage.isSectionEmpty()) {
			return;
		}
		
		// do three random ticks
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int i = 0; i < 3; i++) {
			
			// get a random block
			int index = this.world.rand.nextInt();
			int x = index & 15;
			int y = (index >> 8) & 15;
			int z = (index >> 16) & 15;
			
			IBlockState blockState = this.storage.getBlockStateAt(x, y, z);
			Block block = blockState.getBlock();
			
			if (block.hasRandomTick()) {
				// tick it
				pos.setBlockPos(
					Coords.localToBlock(this.cubeX, x),
					Coords.localToBlock(this.cubeY, y),
					Coords.localToBlock(this.cubeZ, z)
				);
				block.onTick(this.world, pos, blockState, this.world.rand);
			}
		}
	}
	
	public void markForRenderUpdate() {
		this.world.markBlockRangeForRenderUpdate(Coords.cubeToMinBlock(this.cubeX), Coords.cubeToMinBlock(this.cubeY), Coords.cubeToMinBlock(this.cubeZ), Coords.cubeToMaxBlock(this.cubeX), Coords.cubeToMaxBlock(this.cubeY), Coords.cubeToMaxBlock(this.cubeZ));
	}
	
	public long cubeRandomSeed() {
		long hash = 3;
		hash = 41 * hash + getX();
		hash = 41 * hash + getY();
		return 41 * hash + getZ();
	}
}
