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
package cubicchunks.world.cube;

import com.google.common.base.Predicate;
import cubicchunks.CubicChunks;
import cubicchunks.generator.GeneratorStage;
import cubicchunks.util.AddressTools;
import cubicchunks.util.Coords;
import cubicchunks.util.CubeBlockMap;
import cubicchunks.world.EntityContainer;
import cubicchunks.world.column.Column;
import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class Cube {
	
	private static final Logger LOGGER = CubicChunks.LOGGER;
	
	private World world;
	private Column column;
	private int cubeX;
	private int cubeY;
	private int cubeZ;
	private boolean isModified;
	private ExtendedBlockStorage storage;
	private EntityContainer entities;
	private CubeBlockMap<TileEntity> blockEntities;
	private GeneratorStage generatorStage;
	private boolean needsRelightAfterLoad;
	
	public Cube(World world, Column column, int x, int y, int z, boolean isModified) {
		this.world = world;
		this.column = column;
		this.cubeX = x;
		this.cubeY = y;
		this.cubeZ = z;
		this.isModified = isModified;
		
		this.storage = null;
		this.entities = new EntityContainer();
		this.blockEntities = new CubeBlockMap<>();
		this.generatorStage = null;
		this.needsRelightAfterLoad = false;
	}
	
	public boolean isEmpty() {
		return this.storage == null;
	}
	
	public void setEmpty(boolean isEmpty) {
		if (isEmpty) {
			this.storage = null;
		} else {
			this.storage = new ExtendedBlockStorage(Coords.cubeToMinBlock(this.cubeY), !this.world.provider.getHasNoSky());
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
	
	public BlockPos localAddressToBlockPos(int localAddress) {
		int x = Coords.localToBlock(this.cubeX, AddressTools.getLocalX(localAddress));
		int y = Coords.localToBlock(this.cubeY, AddressTools.getLocalY(localAddress));
		int z = Coords.localToBlock(this.cubeZ, AddressTools.getLocalZ(localAddress));
		return new BlockPos(x, y, z);
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
	
	public ExtendedBlockStorage getStorage() {
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
			return Blocks.air;
		}
		//actually: getBlockAt. WTF!?
		return this.storage.getBlockByExtId(localX, localY, localZ);
	}
	
	public IBlockState getBlockState(BlockPos pos) {
		if (isEmpty()) {
			return Blocks.air.getDefaultState();
		}
		int x = Coords.blockToLocal(pos.getX());
		int y = Coords.blockToLocal(pos.getY());
		int z = Coords.blockToLocal(pos.getZ());
		return getBlockState(x, y, z);
	}
	
	public IBlockState getBlockState(int localX, int localY, int localZ) {
		if (isEmpty()) {
			return Blocks.air.getDefaultState();
		}
		return this.storage.get(localX, localY, localZ);
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
		this.storage.set(x, y, z, newBlockState);
		
		Block newBlock = newBlockState.getBlock();
		Block oldBlock = oldBlockState.getBlock();
		
		if (newBlock != oldBlock) {
			if (!this.world.isRemote) {
				// on the server, break the old block
				oldBlock.breakBlock(this.world, pos, oldBlockState);
			} else if (oldBlock instanceof ITileEntityProvider) {
				// on the client, remove the tile entity
				this.world.removeTileEntity(pos);
			}
		}
		
		// did the block change work correctly?
		if (this.storage.getBlockByExtId(x, y, z) != newBlock) {
			return null;
		}
		this.isModified = true;
		
		if (oldBlock instanceof ITileEntityProvider) {
			// update tile entity
			TileEntity blockEntity = getBlockEntity(pos, Chunk.EnumCreateEntityType.CHECK);
			if (blockEntity != null) {
				blockEntity.updateContainingBlockInfo();
			}
		}
		
		if (!this.world.isRemote && newBlock != oldBlock) {
			// on the server, tell the block it was added
			newBlock.onBlockAdded(this.world, pos, newBlockState);
		}
		
		if (newBlock instanceof ITileEntityProvider) {
			// make sure the tile entity is good
			TileEntity blockEntity = getBlockEntity(pos, Chunk.EnumCreateEntityType.CHECK);
			if (blockEntity == null) {
				blockEntity = ((ITileEntityProvider)newBlock).createNewTileEntity(this.world, newBlock.getMetaFromState(newBlockState));
				this.world.setTileEntity(pos, blockEntity);
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
		this.storage.set(x, y, z, newBlockState);
		
		Block newBlock = newBlockState.getBlock();
		
		// did the block change work correctly?
		if (this.storage.getBlockByExtId(x, y, z) != newBlock) {
			return null;
		}
		this.isModified = true;
		
		// update the column light index
		int blockY = Coords.localToBlock(this.cubeY, y);
		this.column.getOpacityIndex().setOpacity(x, blockY, z, newBlock.getLightOpacity());
		
		return oldBlockState;
	}
	
	public boolean hasBlocks() {
		if (isEmpty()) {
			return false;
		}
		
		return !this.storage.isEmpty();
	}
	
	public Iterable<TileEntity> getBlockEntities() {
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
		entity.chunkCoordX = this.cubeX;
		entity.chunkCoordY = this.cubeY;
		entity.chunkCoordZ = this.cubeZ;
		
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
				this.world.isRemote ? "CLIENT" : "SERVER",
				entity.getClass().getName(),
				this.cubeX, this.cubeY, this.cubeZ,
				entity.chunkCoordX, entity.chunkCoordY, entity.chunkCoordZ
			));
		}
		return wasRemoved;
	}
	
	public Iterable<Entity> entities() {
		return this.entities.getEntities();
	}
	
	public void findEntitiesExcept(Entity excludedEntity, AxisAlignedBB queryBox, List<Entity> out, Predicate<? super Entity> predicate) {
		this.entities.findEntitiesExcept(excludedEntity, queryBox, out, predicate);
	}
	
	public <T extends Entity> void findEntities(Class<? extends T> entityType, AxisAlignedBB queryBox, List<T> out, Predicate<? super T> predicate) {
		this.entities.findEntities(entityType, queryBox, out, predicate);
	}
	
	public void getMigratedEntities(List<Entity> out) {
		for (Entity entity : this.entities.getEntities()) {
			int cubeX = Coords.getCubeXForEntity(entity);
			int cubeY = Coords.getCubeYForEntity(entity);
			int cubeZ = Coords.getCubeZForEntity(entity);
			if (cubeX != this.cubeX || cubeY != this.cubeY || cubeZ != this.cubeZ) {
				out.add(entity);
			}
		}
	}
	
	public TileEntity getBlockEntity(BlockPos pos, Chunk.EnumCreateEntityType creationType) {
        
		TileEntity blockEntity = this.blockEntities.get(pos);
		if (blockEntity == null) {
			
			if (creationType == Chunk.EnumCreateEntityType.IMMEDIATE) {
				blockEntity = createBlockEntity(pos);
				this.world.setTileEntity(pos, blockEntity);
			} else if (creationType == Chunk.EnumCreateEntityType.IMMEDIATE) {
				throw new Error("TODO: implement block entity creation queue!");
			}
			
			// is this block not supposed to have a tile entity?
			IBlockState blockState = getBlockState(pos);
			Block block = blockState.getBlock();
			int meta = block.getMetaFromState(blockState);
			
			if (!block.hasTileEntity()) {
				return null;
			}
			
			// make a new tile entity for the block
			blockEntity = ((ITileEntityProvider)block).createNewTileEntity(this.world, meta);
			this.world.setTileEntity(pos, blockEntity);
			
		} else if (blockEntity.isInvalid()) {
			
			// remove the tile entity
			this.blockEntities.remove(pos);
			blockEntity = null;
		}
		
		return blockEntity;
	}
	
	private TileEntity createBlockEntity(BlockPos pos) {
		
		IBlockState blockState = getBlockState(pos);
		Block block = blockState.getBlock();
		int meta = block.getMetaFromState(blockState);
		
		if (block.hasTileEntity()) {
			return ((ITileEntityProvider)block).createNewTileEntity(this.world, meta);
		}
		return null;
	}

	public void addBlockEntity(BlockPos pos, TileEntity blockEntity) {
		
		// update the tile entity
		blockEntity.setWorldObj(this.getWorld());
		blockEntity.setPos(pos);
		
		// is this block supposed to have a tile entity?
		if (getBlockState(pos).getBlock() instanceof ITileEntityProvider) {
			
			// cleanup the old tile entity
			TileEntity oldBlockEntity = this.blockEntities.get(pos);
			if (oldBlockEntity != null) {
				oldBlockEntity.invalidate();
			}
			
			// install the new tile entity
			blockEntity.validate();
			this.blockEntities.put(pos, blockEntity);
			this.isModified = true;
		}
	}
	
	public void removeBlockEntity(BlockPos pos) {
		TileEntity blockEntity = this.blockEntities.remove(pos);
		if (blockEntity != null) {
			blockEntity.invalidate();
			this.isModified = true;
		}
	}
	
	public void onLoad() {
		
		// tell the world about entities
		for (Entity entity : this.entities.getEntities()) {
			entity.onChunkLoad();
		}
		
		this.world.loadEntities(this.entities.getEntities());
		
		// tell the world about tile entities
		this.world.addTileEntities(this.blockEntities.values());
	}
	
	public void onUnload() {
		
		// tell the world to forget about entities
		this.world.unloadEntities(this.entities.getEntities());
		
		// tell the world to forget about tile entities
		for (TileEntity blockEntity : this.blockEntities.values()) {
			this.world.removeTileEntity(blockEntity.getPos());
		}
	}
	
	public boolean needsSaving() {
		return this.entities.needsSaving(this.world.getTotalWorldTime()) || this.isModified;
	}
	
	public void markSaved() {
		this.entities.markSaved(this.world.getTotalWorldTime());
		this.isModified = false;
	}
	
	public boolean isUnderground(BlockPos pos) {
		int x = Coords.blockToLocal(pos.getX());
		int z = Coords.blockToLocal(pos.getZ());
		Integer topNonTransparentBlockY = this.column.getOpacityIndex().getTopBlockY(x, z);
		if (topNonTransparentBlockY == null) {
			return false;
		}
		return pos.getY() < topNonTransparentBlockY;
	}
	
	public int getBrightestLight(BlockPos pos, int skyLightDampeningTerm) {
		
		// get sky light
		int skyLight = getLightValue(EnumSkyBlock.SKY, pos);
		skyLight -= skyLightDampeningTerm;
		
		// get block light
		int blockLight = getLightValue(EnumSkyBlock.BLOCK, pos);
		
		// FIGHT!!!
		if (blockLight > skyLight) {
			return blockLight;
		}
		return skyLight;
	}
	
	public int getLightValue(EnumSkyBlock lightType, BlockPos pos) {
		
		int x = Coords.blockToLocal(pos.getX());
		int y = Coords.blockToLocal(pos.getY());
		int z = Coords.blockToLocal(pos.getZ());
		
		switch (lightType) {
			case SKY:
				if (!this.world.provider.getHasNoSky()) {
					if (isEmpty()) {
						if (isUnderground(pos)) {
							return 0;
						} else {
							return 15;
						}
					}
					
					return this.storage.getExtSkylightValue(x, y, z);
				} else {
					return 0;
				}
				
			case BLOCK:
				if (isEmpty()) {
					return 0;
				}
				
				return this.storage.getExtBlocklightValue(x, y, z);
				
			default:
				return lightType.defaultLightValue;
		}
	}
	
	public void setLightValue(EnumSkyBlock lightType, BlockPos pos, int light) {
		
		// make sure we're not empty
		if (isEmpty()) {
			setEmpty(false);
		}
		
		int x = Coords.blockToLocal(pos.getX());
		int y = Coords.blockToLocal(pos.getY());
		int z = Coords.blockToLocal(pos.getZ());
		
		switch (lightType) {
			case SKY:
				if (!this.world.provider.getHasNoSky()) {
					this.storage.setExtSkylightValue(x, y, z, light);
					this.isModified = true;
				}
			break;
			
			case BLOCK:
				this.storage.setExtBlocklightValue(x, y, z, light);
				this.isModified = true;
			break;
		}
	}
	
	public void doRandomTicks() {
		
		if (isEmpty() || this.storage.isEmpty()) {
			return;
		}
		
		// do three random ticks
		for (int i = 0; i < 3; i++) {
			
			// get a random block
			int index = this.world.rand.nextInt();
			int x = index & 0xF;
			int y = (index >> 8) & 0xF;
			int z = (index >> 16) & 0xF;
			
			IBlockState blockState = this.storage.get(x, y, z);
			Block block = blockState.getBlock();
			
			if (block.getTickRandomly()) {
				// tick it
				BlockPos pos = new BlockPos(
					Coords.localToBlock(this.cubeX, x),
					Coords.localToBlock(this.cubeY, y),
					Coords.localToBlock(this.cubeZ, z)
				);
				block.randomTick(this.world, pos, blockState, this.world.rand);
			}
		}
	}
	
	public void markForRenderUpdate() {
		this.world.markBlockRangeForRenderUpdate(
			Coords.cubeToMinBlock(this.cubeX), Coords.cubeToMinBlock(this.cubeY), Coords.cubeToMinBlock(this.cubeZ),
			Coords.cubeToMaxBlock(this.cubeX), Coords.cubeToMaxBlock(this.cubeY), Coords.cubeToMaxBlock(this.cubeZ)
		);
	}
	
	public long cubeRandomSeed() {
		long hash = 3;
		hash = 41 * hash + this.world.getSeed();
		hash = 41 * hash + getX();
		hash = 41 * hash + getY();
		return 41 * hash + getZ();
	}

	public boolean needsRelightAfterLoad() {
		return this.needsRelightAfterLoad;
	}
	public void setNeedsRelightAfterLoad(boolean val) {
		this.needsRelightAfterLoad = val;
	}
}
