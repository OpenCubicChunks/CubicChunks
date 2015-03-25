/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 * 
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package cubicchunks.world;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Predicate;

import cubicchunks.generator.GeneratorStage;
import cubicchunks.util.AddressTools;
import cubicchunks.util.Coords;
import cubicchunks.util.CubeBlockMap;

public class Cube {
	
	private static final Logger log = LogManager.getLogger();
	
	private World m_world;
	private Column m_column;
	private int m_x;
	private int m_y;
	private int m_z;
	private boolean m_isModified;
	private ChunkSection m_storage;
	private EntityContainer m_entities;
	private CubeBlockMap<BlockEntity> m_blockEntities;
	private GeneratorStage m_generatorStage;
	
	public Cube(World world, Column column, int x, int y, int z, boolean isModified) {
		m_world = world;
		m_column = column;
		m_x = x;
		m_y = y;
		m_z = z;
		m_isModified = isModified;
		
		m_storage = null;
		m_entities = new EntityContainer();
		m_blockEntities = new CubeBlockMap<BlockEntity>();
		m_generatorStage = null;
	}
	
	public boolean isEmpty() {
		return m_storage == null;
	}
	
	public void setEmpty(boolean isEmpty) {
		if (isEmpty) {
			m_storage = null;
		} else {
			m_storage = new ChunkSection(m_y << 4, !m_world.dimension.hasNoSky());
		}
	}
	
	public GeneratorStage getGeneratorStage() {
		return m_generatorStage;
	}
	
	public void setGeneratorStage(GeneratorStage val) {
		m_generatorStage = val;
	}
	
	public long getAddress() {
		return AddressTools.getAddress(m_x, m_y, m_z);
	}
	
	public World getWorld() {
		return m_world;
	}
	
	public Column getColumn() {
		return m_column;
	}
	
	public int getX() {
		return m_x;
	}
	
	public int getY() {
		return m_y;
	}
	
	public int getZ() {
		return m_z;
	}
	
	public ChunkSection getStorage() {
		return m_storage;
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
		return m_storage.getBlockStateAt(localX, localY, localZ);
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
		m_storage.setBlockStateAt(x, y, z, newBlockState);
		
		Block newBlock = newBlockState.getBlock();
		Block oldBlock = oldBlockState.getBlock();
		
		if (newBlock != oldBlock) {
			if (!m_world.isClient) {
				// on the server, break the old block
				oldBlock.onRemoved(m_world, pos, oldBlockState);
			} else if (oldBlock instanceof IBlockEntityProvider) {
				// on the client, remove the tile entity
				m_world.removeBlockEntity(pos);
			}
		}
		
		// did the block change work correctly?
		if (m_storage.getBlockAt(x, y, z) != newBlock) {
			return null;
		}
		m_isModified = true;
		
		if (oldBlock instanceof IBlockEntityProvider) {
			// update tile entity
			BlockEntity blockEntity = getBlockEntity(pos, ChunkEntityCreationType.CHECK);
			if (blockEntity != null) {
				blockEntity.updateContainingBlockInfo();
			}
		}
		
		if (!m_world.isClient && newBlock != oldBlock) {
			// on the server, tell the block it was added
			newBlock.onSet(m_world, pos, newBlockState);
		}
		
		if (newBlock instanceof IBlockEntityProvider) {
			// make sure the tile entity is good
			BlockEntity blockEntity = getBlockEntity(pos, ChunkEntityCreationType.CHECK);
			if (blockEntity == null) {
				blockEntity = ((IBlockEntityProvider)newBlock).getBlockEntity(m_world, newBlock.getMetadataForBlockState(newBlockState));
				m_world.setBlockEntity(pos, blockEntity);
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
		m_storage.setBlockStateAt(x, y, z, newBlockState);
		
		Block newBlock = newBlockState.getBlock();
		
		// did the block change work correctly?
		if (m_storage.getBlockAt(x, y, z) != newBlock) {
			return null;
		}
		m_isModified = true;
		
		// update the column light index
		int blockY = Coords.localToBlock(m_y, y);
		m_column.getLightIndex().setOpacity(x, blockY, z, newBlock.getOpacity());
		
		return oldBlockState;
	}
	
	public boolean hasBlocks() {
		if (isEmpty()) {
			return false;
		}
		
		return !m_storage.isSectionEmpty();
	}
	
	public Iterable<BlockEntity> blockEntities() {
		return m_blockEntities.values();
	}
	
	public void addEntity(Entity entity) {
		
		// make sure the entity is in this cube
		int cubeX = Coords.getCubeXForEntity(entity);
		int cubeY = Coords.getCubeYForEntity(entity);
		int cubeZ = Coords.getCubeZForEntity(entity);
		if (cubeX != m_x || cubeY != m_y || cubeZ != m_z) {
			log.warn(String.format("Entity %s in cube (%d,%d,%d) added to cube (%d,%d,%d)!", entity.getClass().getName(), cubeX, cubeY, cubeZ, m_x, m_y, m_z));
		}
		
		// tell the entity it's in this cube
		entity.addedToChunk = true;
		entity.chunkX = m_x;
		entity.chunkY = m_y;
		entity.chunkZ = m_z;
		
		m_entities.add(entity);
		m_isModified = true;
	}
	
	public boolean removeEntity(Entity entity) {
		boolean wasRemoved = m_entities.remove(entity);
		if (wasRemoved) {
			entity.addedToChunk = false;
			m_isModified = true;
		} else {
			log.warn(String.format("%s Tried to remove entity %s from cube (%d,%d,%d), but it was not there. Entity thinks it's in cube (%d,%d,%d)",
				m_world.isClient ? "CLIENT" : "SERVER",
				entity.getClass().getName(),
				m_x, m_y, m_z,
				entity.chunkX, entity.chunkY, entity.chunkZ
			));
		}
		return wasRemoved;
	}
	
	public Iterable<Entity> entities() {
		return m_entities.entities();
	}
	
	public void findEntitiesExcept(Entity excludedEntity, AxisAlignedBB queryBox, List<Entity> out, Predicate<? super Entity> predicate) {
		m_entities.findEntitiesExcept(excludedEntity, queryBox, out, predicate);
	}
	
	public <T extends Entity> void findEntities(Class<? extends T> entityType, AxisAlignedBB queryBox, List<T> out, Predicate<? super T> predicate) {
		m_entities.findEntities(entityType, queryBox, out, predicate);
	}
	
	public void getMigratedEntities(List<Entity> out) {
		for (Entity entity : m_entities.entities()) {
			int cubeX = Coords.getCubeXForEntity(entity);
			int cubeY = Coords.getCubeYForEntity(entity);
			int cubeZ = Coords.getCubeZForEntity(entity);
			if (cubeX != m_x || cubeY != m_y || cubeZ != m_z) {
				out.add(entity);
			}
		}
	}
	
	public BlockEntity getBlockEntity(BlockPos pos, ChunkEntityCreationType creationType) {
        
		BlockEntity blockEntity = m_blockEntities.get(pos);
		if (blockEntity == null) {
			
			if (creationType == ChunkEntityCreationType.IMMEDIATE) {
				blockEntity = createBlockEntity(pos);
				m_world.setBlockEntity(pos, blockEntity);
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
			blockEntity = ((IBlockEntityProvider)block).getBlockEntity(m_world, meta);
			m_world.setBlockEntity(pos, blockEntity);
			
		} else if (blockEntity.isInvalid()) {
			
			// remove the tile entity
			m_blockEntities.remove(pos);
			blockEntity = null;
		}
		
		return blockEntity;
	}
	
	private BlockEntity createBlockEntity(BlockPos pos) {
		
		IBlockState blockState = getBlockState(pos);
		Block block = blockState.getBlock();
		int meta = block.getMetadataForBlockState(blockState);
		
		if (block.hasBlockEntity()) {
			return ((IBlockEntityProvider)block).getBlockEntity(m_world, meta);
		}
		return null;
	}

	public void addBlockEntity(BlockPos pos, BlockEntity blockEntity) {
		
		// update the tile entity
		blockEntity.setLevel(m_world);
		blockEntity.setPosition(pos);
		
		// is this block supposed to have a tile entity?
		if (getBlockState(pos).getBlock() instanceof IBlockEntityProvider) {
			
			// cleanup the old tile entity
			BlockEntity oldBlockEntity = m_blockEntities.get(pos);
			if (oldBlockEntity != null) {
				oldBlockEntity.setInvalid();
			}
			
			// install the new tile entity
			blockEntity.setValid();
			m_blockEntities.put(pos, blockEntity);
			m_isModified = true;
		}
	}
	
	public void removeBlockEntity(BlockPos pos) {
		BlockEntity blockEntity = m_blockEntities.remove(pos);
		if (blockEntity != null) {
			blockEntity.setInvalid();
			m_isModified = true;
		}
	}
	
	public void onLoad() {
		
		// tell the world about entities
		for (Entity entity : m_entities.entities()) {
			entity.onChunkLoad();
		}
		
		m_world.loadEntitiesInBulk(m_entities.entities());
		
		// tell the world about tile entities
		m_world.addBlockEntities(m_blockEntities.values());
	}
	
	public void onUnload() {
		
		// tell the world to forget about entities
		m_world.unloadEntitiesInBulk(m_entities.entities());
		
		// tell the world to forget about tile entities
		for (BlockEntity blockEntity : m_blockEntities.values()) {
			m_world.removeBlockEntity(blockEntity.getBlockCoords());
		}
	}
	
	public boolean needsSaving() {
		return m_entities.needsSaving(m_world.getGameTime()) || m_isModified;
	}
	
	public void markSaved() {
		m_entities.markSaved(m_world.getGameTime());
		m_isModified = false;
	}
	
	public boolean isUnderground(BlockPos pos) {
		int x = Coords.blockToLocal(pos.getX());
		int z = Coords.blockToLocal(pos.getZ());
		Integer topNonTransparentBlockY = m_column.getLightIndex().getTopNonTransparentBlockY(x, z);
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
				if (!m_world.dimension.hasNoSky()) {
					if (isEmpty()) {
						if (isUnderground(pos)) {
							return 0;
						} else {
							return 15;
						}
					}
					
					return m_storage.getSkyLightAtCoords(x, y, z);
				} else {
					return 0;
				}
				
			case BLOCK:
				if (isEmpty()) {
					return 0;
				}
				
				return m_storage.getBlockLightAtCoords(x, y, z);
				
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
				if (!m_world.dimension.hasNoSky()) {
					m_storage.setSkyLightAtCoords(x, y, z, light);
					m_isModified = true;
				}
			break;
			
			case BLOCK:
				m_storage.setBlockLightAtCoords(x, y, z, light);
				m_isModified = true;
			break;
		}
	}
	
	public void doRandomTicks() {
		
		if (isEmpty() || m_storage.isSectionEmpty()) {
			return;
		}
		
		// do three random ticks
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int i = 0; i < 3; i++) {
			
			// get a random block
			int index = m_world.rand.nextInt();
			int x = index & 15;
			int y = (index >> 8) & 15;
			int z = (index >> 16) & 15;
			
			IBlockState blockState = m_storage.getBlockStateAt(x, y, z);
			Block block = blockState.getBlock();
			
			if (block.hasRandomTick()) {
				// tick it
				pos.setBlockPos(
					Coords.localToBlock(m_x, x),
					Coords.localToBlock(m_y, y),
					Coords.localToBlock(m_z, z)
				);
				block.onTick(m_world, pos, blockState, m_world.rand);
			}
		}
	}
	
	public void markForRenderUpdate() {
		m_world.markBlockRangeForRenderUpdate(Coords.cubeToMinBlock(m_x), Coords.cubeToMinBlock(m_y), Coords.cubeToMinBlock(m_z), Coords.cubeToMaxBlock(m_x), Coords.cubeToMaxBlock(m_y), Coords.cubeToMaxBlock(m_z));
	}
}
