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
package cubicchunks.world.column;

import com.google.common.base.Predicate;
import cubicchunks.CubicChunks;
import cubicchunks.lighting.LightingManager;
import cubicchunks.util.AddressTools;
import cubicchunks.util.Bits;
import cubicchunks.util.Coords;
import cubicchunks.util.MathUtil;
import cubicchunks.world.ClientOpacityIndex;
import cubicchunks.world.EntityContainer;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.IOpacityIndex;
import cubicchunks.world.OpacityIndex;
import cubicchunks.world.cube.Cube;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Column extends Chunk {

	private CubeMap cubeMap;
	private IOpacityIndex opacityIndex;
	private int roundRobinLightUpdatePointer;
	private Deque<Integer> roundRobinCubeQueue;
	private EntityContainer entities;
	private ICubicWorld world;

	//used by vanillaCubic to mark this column as generated
	private boolean compatBaseTerrainDone = false;
	//used bt vanillaCubic to mark this column as populated
	private boolean compatPopulationDone = false;

	public Column(ICubicWorld world, int x, int z) {
		// NOTE: this constructor is called by the chunk loader
		super((World) world, x, z);
		this.world = world;
		init();
	}

	public Column(ICubicWorld world, int cubeX, int cubeZ, Biome[] biomes) {
		// NOTE: this constructor is called by the column worldgen
		this(world, cubeX, cubeZ);

		byte[] biomeArray = super.getBiomeArray();
		// save the biome data
		for (int i = 0; i < biomes.length; i++) {
			biomeArray[i] = (byte) Biome.getIdForBiome(biomes[i]);
		}

		super.setModified(true);
	}

	//=================================================
	//===============VANILLA METHODS===================
	//=================================================

	@Override
	public boolean isAtLocation(int x, int z) {
		return super.isAtLocation(x, z);
	}

	@Override
	@Deprecated
	// don't use this! It's only here because vanilla needs it
	public int getHeight(BlockPos pos) {
		int localX = Coords.blockToLocal(pos.getX());
		int localZ = Coords.blockToLocal(pos.getZ());
		return this.getHeightValue(localX, localZ);
	}

	@Override
	@Deprecated
	// don't use this! It's only here because vanilla needs it
	public int getHeightValue(int localX, int localZ) {
		// NOTE: the "height value" here is the height of the transparent block
		// on top of the highest non-transparent block

		Integer skylightBlockY = getHeightmapAt(localX, localZ);
		if (skylightBlockY == null) {
			// PANIC!
			// this column doesn't have any blocks in it that aren't air!
			// but we can't return null here because vanilla code expects there to be a surface down there somewhere
			// we don't actually know where the surface is yet, because maybe it hasn't been generated
			// but we do know that the surface has to be at least at sea level,
			// so let's go with that for now and hope for the best
			skylightBlockY = this.getWorld().provider.getAverageGroundLevel() + 1;
		}
		return skylightBlockY;
	}

	@Override
	@Deprecated
	// don't use this! It's only here because vanilla needs it
	public int getTopFilledSegment() {
		//NOTE: this method actually returns block Y coords
		Integer cubeY = getTopFilledCubeY();
		if (cubeY != null) {
			return Coords.cubeToMinBlock(cubeY);
		} else {
			// PANIC!
			// this column doesn't have any blocks in it that aren't air!
			// but we can't return null here because vanilla code expects there to be a surface down there somewhere
			// we don't actually know where the surface is yet, because maybe it hasn't been generated
			// but we do know that the surface has to be at least at sea level,
			// so let's go with that for now and hope for the best
			return this.getWorld().provider.getAverageGroundLevel();
		}
	}

	@Override
	public ExtendedBlockStorage[] getBlockStorageArray() {
		return cubeMap.getStorageArrays();
	}

	@SideOnly(Side.CLIENT)
	protected void generateHeightMap() {
		//this method reduces to no-op with CubicChunks, heightmap is generated in real time
	}

	@Override
	public void generateSkylightMap() {
		throw new UnsupportedOperationException("Functionality of this method is replaced with LightingManager");
	}

	@Override
	public int getBlockLightOpacity(@Nonnull BlockPos pos) {
		return super.getBlockLightOpacity(pos);
	}

	//forward to cube
	@Override
	public IBlockState getBlockState(BlockPos pos) {
		Cube cube = this.getCube(pos);
		IBlockState block = Blocks.AIR.getDefaultState();
		if (cube != null) {
			block = cube.getBlockState(pos);
		}
		return block;
	}

	//forward to cube
	@Override
	public IBlockState getBlockState(final int blockX, final int blockY, final int blockZ) {
		Cube cube = this.getCube(Coords.blockToLocal(blockY));
		IBlockState block = Blocks.AIR.getDefaultState();
		if (cube != null) {
			block = cube.getBlockState(blockX, blockY, blockZ);
		}
		return block;
	}

	@Override
	public IBlockState setBlockState(BlockPos pos, @Nonnull IBlockState newBlockState) {
		// is there a chunk for this block?
		int cubeY = Coords.blockToCube(pos.getY());
		if (!getWorld().isRemote) {
			int i = 0;
		}
		// did anything change?
		IBlockState oldBlockState = this.getBlockState(pos);
		if (oldBlockState == newBlockState) {
			// nothing changed
			return null;
		}

		int oldOpacity = oldBlockState.getLightOpacity(this.getWorld(), pos);

		Block oldBlock = oldBlockState.getBlock();
		Block newBlock = newBlockState.getBlock();

		Cube cube = this.cubeMap.get(cubeY);

		if (cube == null) {
			//nothing we can do. vanilla creates new EBS here
			return null;
		}

		cube.setBlockStateDirect(pos, newBlockState);

		//if(oldBlock != newBlock) {
		{
			if (!this.getWorld().isRemote) {
				if (oldBlock != newBlock) {
					oldBlock.breakBlock(this.getWorld(), pos, oldBlockState);
				}
				TileEntity te = this.getTileEntity(pos, EnumCreateEntityType.CHECK);
				if (te != null && te.shouldRefresh(this.getWorld(), pos, oldBlockState, newBlockState)) {
					this.getWorld().removeTileEntity(pos);
				}
			} else if (oldBlock.hasTileEntity(oldBlockState)) {
				TileEntity te = this.getTileEntity(pos, EnumCreateEntityType.CHECK);
				if (te != null && te.shouldRefresh(this.getWorld(), pos, oldBlockState, newBlockState)) {
					this.getWorld().removeTileEntity(pos);
				}
			}
		}

		if (cube.getBlockState(pos).getBlock() != newBlock) {
			return null;
		}

		this.doOnBlockSetLightUpdates(pos, newBlockState, oldOpacity);

		if (!this.getWorld().isRemote && oldBlock != newBlock) {
			newBlock.onBlockAdded(this.getWorld(), pos, newBlockState);
		}

		if (newBlock.hasTileEntity(newBlockState)) {
			TileEntity te = this.getTileEntity(pos, EnumCreateEntityType.CHECK);

			if (te == null) {
				te = newBlock.createTileEntity(this.getWorld(), newBlockState);
				this.getWorld().setTileEntity(pos, te);
			}

			if (te != null) {
				te.updateContainingBlockInfo();
			}
		}

		this.setModified(true);
		return oldBlockState;
	}

	private void doOnBlockSetLightUpdates(BlockPos pos, IBlockState newBlockState, int oldOpacity) {
		int newOpacity = newBlockState.getLightOpacity(this.getWorld(), pos);
		if (oldOpacity == newOpacity || (oldOpacity >= 15 && newOpacity >= 15)) {
			//nothing to update, this will frequently happen in ore generation
			return;
		}

		int localX = Coords.blockToLocal(pos.getX());
		int localZ = Coords.blockToLocal(pos.getZ());

		// did the top non-transparent block change?
		Integer oldSkylightY = getHeightmapAt(localX, localZ);
		this.opacityIndex.onOpacityChange(localX, pos.getY(), localZ, newOpacity);
		Integer newSkylightY = oldSkylightY;
		if (!getWorld().isRemote) {
			newSkylightY = getHeightmapAt(localX, localZ);
			//if oldSkylightY == null and newOpacity == 0 then we didn't change anything
		} else if (!(oldSkylightY == null && newOpacity == 0)) {
			Integer oldSkylightActual = oldSkylightY == null ? null : oldSkylightY - 1;
			//to avoid unnecessary delay when breaking blocks we need to hack it clientside
			if ((oldSkylightActual == null || pos.getY() > oldSkylightActual - 1) && newOpacity != 0) {
				//we added block, so we can be sure it's correct. Server update will be ignored
				newSkylightY = pos.getY() + 1;
			} else if (newOpacity == 0 && pos.getY() == oldSkylightY - 1) {
				//we changed block to something transparent. Heightmap can change only if we break top block

				//we don't know by how much we changed heightmap, and we could have changed it by any value
				//but for client code any value higher than render distance means the same
				//we need to update it enough not to be unresponsive, and then wait for information from server
				//so only scan 64 blocks down. If we updated more - we would need to wait for renderer updates anyway
				int newTop = oldSkylightActual - 1;
				while (getBlockLightOpacity(new BlockPos(localX, newTop, localZ)) == 0 &&
						newTop > oldSkylightActual - 65) {
					newTop--;
				}
				newSkylightY = newTop;
			} else {
				// no change
				newSkylightY = oldSkylightActual;
			}
			//update the heightmap. If out update it not accurate - it will be corrected when server sends block update
			((ClientOpacityIndex) opacityIndex).setHeight(localX, localZ, newSkylightY);
		}

		int minY = MathUtil.minInteger(oldSkylightY, newSkylightY);
		int maxY = MathUtil.maxInteger(oldSkylightY, newSkylightY);
		if (minY > maxY) {
			int t = minY;
			minY = maxY;
			maxY = t;
		}

		LightingManager lightManager = this.world.getLightingManager();
		lightManager.columnSkylightUpdate(LightingManager.UpdateType.IMMEDIATE, this, localX, minY, maxY, localZ);

	}

	//forward to cube
	@Override
	public int getLightFor(@Nonnull EnumSkyBlock type, BlockPos pos) {
		Cube cube = this.getCube(pos);
		if (cube == null) {
			return this.canSeeSky(pos) ? type.defaultLightValue : 0;
		}
		return cube.getLightFor(type, pos);
	}

	//forward to cube
	@Override
	public void setLightFor(EnumSkyBlock type, BlockPos pos, int value) {
		Cube cube = this.getCube(pos);
		if (cube == null) {
			//What now? There is no such cube; do nothing?
			return;
		}
		cube.setLightFor(type, pos, value);
	}

	//forward to cube
	@Override
	public int getLightSubtracted(BlockPos pos, int amount) {
		Cube cube = this.getCube(pos);
		if (cube == null) {
			//If there is no cube - apply the same logic as vanilla does when EBS doesn't exist
			boolean hasSky = !this.getWorld().provider.getHasNoSky();
			if (hasSky && amount < EnumSkyBlock.SKY.defaultLightValue) {
				return EnumSkyBlock.SKY.defaultLightValue - amount;
			} else {
				return 0;
			}
		}
		return cube.getLightSubtracted(pos, amount);
	}

	//forward to cube when possible
	@Override
	public void addEntity(Entity entity) {
		int cubeY = Coords.getCubeYForEntity(entity);

		Cube cube = getCube(cubeY);
		if (cube != null) {
			cube.addEntity(entity);
		} else {
			// entities don't have to be in cubeMap, just add it directly to the column
			int cubeX = MathHelper.floor_double(entity.posX/16.0D);
			int cubeZ = MathHelper.floor_double(entity.posZ/16.0D);

			if (cubeX != this.xPosition || cubeZ != this.zPosition) {
				CubicChunks.LOGGER.warn(
						"Wrong location! (" + cubeX + ", " + cubeZ + ") should be (" + this.xPosition + ", " +
								this.zPosition + "), " + entity, new Object[]{entity});
				entity.setDead();
			}

			MinecraftForge.EVENT_BUS.post(new EntityEvent.EnteringChunk(entity, this.xPosition, this.zPosition, entity.chunkCoordX, entity.chunkCoordZ));

			entity.addedToChunk = true;
			entity.chunkCoordX = this.xPosition;
			entity.chunkCoordY = cubeY;
			entity.chunkCoordZ = this.zPosition;

			this.entities.addEntity(entity);
			this.setModified(true);
		}
	}

	@Override
	public void removeEntity(Entity entityIn) {
		this.removeEntityAtIndex(entityIn, entityIn.chunkCoordY);
	}

	//forward to cube when possible
	@Override
	public void removeEntityAtIndex(@Nonnull Entity entity, int cubeY) {
		if (!entity.addedToChunk) {
			return;
		}

		// pass off to the cube
		Cube cube = getCube(cubeY);

		if (cube != null) {
			cube.removeEntity(entity);
		} else if (this.entities.remove(entity)) {
			this.setModified(true);
		} else {
			CubicChunks.LOGGER.warn(
					"{} Tried to remove entity {} from cube ({}, {}, {}) from column entity list, but it was not there. Entity thinks it's in cube ({},{},{})",
					this.getWorld().isRemote ? "CLIENT" : "SERVER",
					entity.getClass().getName(),
					this.xPosition, cubeY, this.zPosition,
					entity.chunkCoordX, entity.chunkCoordY, entity.chunkCoordZ);
		}
	}

	@Override
	public boolean canSeeSky(BlockPos pos) {
		int localX = Coords.blockToLocal(pos.getX());
		int localZ = Coords.blockToLocal(pos.getZ());
		Integer height = this.getHeightmapAt(localX, localZ);
		return height == null || pos.getY() >= height;
	}

	//forward to cube
	@Override
	public TileEntity getTileEntity(@Nonnull BlockPos pos, Chunk.EnumCreateEntityType createType) {
		Cube cube = this.getCube(pos);
		if (cube == null) {
			return null;
		}
		return cube.getTileEntity(pos, createType);
	}

	@Override
	public void addTileEntity(TileEntity tileEntity) {
		// pass off to the cube
		int cubeY = Coords.blockToCube(tileEntity.getPos().getY());
		Cube cube = getCube(cubeY);

		if (cube != null) {
			cube.addTileEntity(tileEntity);
		} else {
			CubicChunks.LOGGER.warn("No cube at ({},{},{}) to add tile entity (block {},{},{})!", this.xPosition, cubeY, this.zPosition,
					tileEntity.getPos().getX(), tileEntity.getPos().getY(), tileEntity.getPos().getZ());
		}
	}

	//forward to cube
	@Override
	public void addTileEntity(@Nonnull BlockPos pos, TileEntity blockEntity) {
		// pass off to the cube
		int cubeY = Coords.blockToCube(pos.getY());
		Cube cube = getCube(cubeY);

		if (cube != null) {
			cube.addTileEntity(pos, blockEntity);
		} else {
			CubicChunks.LOGGER.warn("No cube at ({},{},{}) to add tile entity (block {},{},{})!", this.xPosition, cubeY, this.zPosition,
					pos.getX(), pos.getY(), pos.getZ());
		}
	}

	//forward to cube
	@Override
	public void removeTileEntity(@Nonnull BlockPos pos) {
		Cube cube = this.getCube(pos);
		if (cube == null) {
			return;
		}
		cube.removeTileEntity(pos);
	}

	@Override
	public void onChunkLoad() {
		this.isChunkLoaded = true;
		this.getWorld().loadEntities(this.entities.getEntities());
		//the whole logic is actually moved to Cube, but mods still need to get the event
		MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(this));

		//NOTE: cube.onLoad() is called from ServerCubeCache
	}

	@Override
	public void onChunkUnload() {
		this.isChunkLoaded = false;
		this.getWorld().unloadEntities(this.entities.getEntities());
		MinecraftForge.EVENT_BUS.post(new ChunkEvent.Unload(this));

		//NOTE: cube.onUnload() is called from ServerCubeCache
	}

	//setChunkModified() goes here, it's unchanged

	//forward to cube, then to EntityContainer
	@Override
	public void getEntitiesWithinAABBForEntity(Entity excludedEntity, AxisAlignedBB queryBox, @Nonnull List<Entity> out, Predicate<? super Entity> predicate) {

		// get a y-range that 2 blocks wider than the box for safety
		int minCubeY = Coords.blockToCube(MathHelper.floor_double(queryBox.minY - 2));
		int maxCubeY = Coords.blockToCube(MathHelper.floor_double(queryBox.maxY + 2));

		for (Cube cube : getCubes(minCubeY, maxCubeY)) {
			cube.getEntitiesWithinAABBForEntity(excludedEntity, queryBox, out, predicate);
		}

		// check the column too
		this.entities.getEntitiesWithinAABBForEntity(excludedEntity, queryBox, out, predicate);
	}

	//forward to cube, then to EntityContainer
	@Override
	public <T extends Entity> void getEntitiesOfTypeWithinAAAB(@Nonnull Class<? extends T> entityType, AxisAlignedBB queryBox, @Nonnull List<T> out, Predicate<? super T> predicate) {

		// get a y-range that 2 blocks wider than the box for safety
		int minCubeY = Coords.blockToCube(MathHelper.floor_double(queryBox.minY - World.MAX_ENTITY_RADIUS));
		int maxCubeY = Coords.blockToCube(MathHelper.floor_double(queryBox.maxY + World.MAX_ENTITY_RADIUS));

		for (Cube cube : getCubes(minCubeY, maxCubeY)) {
			cube.getEntitiesOfTypeWithinAAAB(entityType, queryBox, out, predicate);
		}

		// check the column too
		this.entities.getEntitiesOfTypeWithinAAAB(entityType, queryBox, out, predicate);
	}

	@Override
	public boolean needsSaving(boolean flag) {
		return this.entities.needsSaving(flag, this.getWorld().getTotalWorldTime(), this.isModified);
	}

	//getRandomWithSeed(seed) doesn't need changes

	//isEmpty() doesn't need changes

	@Override
	public void populateChunk(IChunkProvider chunkProvider, @Nonnull IChunkGenerator chunkGenerator) {
		throw new UnsupportedOperationException("This method is incompatible with CubicChunks");
	}

	//TODO: Actual precipitation heightmap, currently skylight heightmap is used which triggers an old MC alpha bug
	@Override
	public BlockPos getPrecipitationHeight(BlockPos pos) {
		return new BlockPos(pos.getX(), this.getHeight(pos), pos.getZ());
	}

	@Override
	public void onTick(boolean tryToTickFaster) {
		this.chunkTicked = true;
		cubeMap.forEach((c) -> c.tickCube(tryToTickFaster));
	}

	@Override
	@Deprecated
	public boolean isPopulated() {
		return this.chunkTicked && isTerrainPopulated();
	}

	//isCHunkTicked() doesn't need changes

	//getChunkCoordIntPair doesn't need changes

	@Override
	public boolean getAreLevelsEmpty(int minBlockY, int maxBlockY) {
		int minCubeY = Coords.blockToCube(minBlockY);
		int maxCubeY = Coords.blockToCube(maxBlockY);
		for (int cubeY = minCubeY; cubeY <= maxCubeY; cubeY++) {
			Cube cube = getCube(cubeY);
			if (cube != null && cube.hasBlocks()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void setStorageArrays(ExtendedBlockStorage[] newArray) {
		throw new UnsupportedOperationException("This method is incompatible with Cubic Chunks");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void fillChunk(@Nonnull PacketBuffer buf, int p_186033_2_, boolean p_186033_3_) {
		throw new UnsupportedOperationException("This method is incompatible with Cubic Chunks");
	}

	//getBiome doesn't need changes

	//getBiomeArray doesn't need changes

	//setBiomeArray doesn't need changes


	@Override
	public void resetRelightChecks() {
		this.roundRobinLightUpdatePointer = 0;
		this.roundRobinCubeQueue.clear();
		this.roundRobinCubeQueue.addAll(this.cubeMap.all().stream().map(c -> c.getY()).collect(Collectors.toSet()));
	}

	@Override
	public void enqueueRelightChecks() {
		if (this.roundRobinCubeQueue.isEmpty()) {
			return;
		}
		int maxPointer = 16*16 - 1;
		BlockPos blockpos = new BlockPos(Coords.cubeToMinBlock(this.xPosition), 0, Coords.cubeToMinBlock(this.zPosition));
		for (int i = 0; i < 8; ++i) {
			if (this.roundRobinLightUpdatePointer > maxPointer) {
				//next cube
				this.roundRobinLightUpdatePointer = 0;
				this.roundRobinCubeQueue.removeLast();
				if (this.roundRobinCubeQueue.isEmpty()) {
					break;
				}
			}
			int cubeY = this.roundRobinCubeQueue.getLast();
			Cube cube = this.getCube(cubeY);

			int localX = Bits.unpackUnsigned(this.roundRobinLightUpdatePointer, 4, 4);
			int blockYMin = Coords.cubeToMinBlock(cubeY);
			int localZ = Bits.unpackUnsigned(this.roundRobinLightUpdatePointer, 4, 0);
			this.roundRobinLightUpdatePointer++;

			boolean cubeEmpty = cube == null || cube.isEmpty();

			for (int localY = 0; localY < 16; ++localY) {
				BlockPos currentPos = blockpos.add(localX, blockYMin + localY, localZ);
				boolean isEdge = localY == 0 || localY == 15 ||
						localX == 0 || localX == 15 ||
						localZ == 0 || localZ == 15;

				if (!(cubeEmpty && isEdge)) {
					continue;
				}
				IBlockState currentState = cubeEmpty ? null : cube.getStorage().get(localX, localY, localZ);
				//only air blocks need to be updated, but no need to update when this and all surrounding blocks are air
				boolean airInNonEmptyCube =
						!cubeEmpty && currentState.getBlock().isAir(currentState, this.getWorld(), currentPos);
				//surrounding blocks may not be air if the block is at the edge of empty cube
				boolean edgeOfEmptyCube = cubeEmpty && isEdge;
				if (edgeOfEmptyCube || airInNonEmptyCube) {
					for (EnumFacing enumfacing : EnumFacing.values()) {
						BlockPos offsetPos = currentPos.offset(enumfacing);

						//is it light source?
						if (this.getWorld().getBlockState(offsetPos).getLightValue(this.getWorld(), offsetPos) > 0) {
							this.getWorld().checkLight(offsetPos);
						}
					}

					this.getWorld().checkLight(currentPos);
				}
			}
		}
	}

	@Override
	public void checkLight() {
		//no-op on cubic chunks
	}

	//isLoaded doesn't need changes

	//getWorld doesn't need changes

	@Override
	public int[] getHeightMap() {
		return this.opacityIndex.getHeightmap();
	}

	@Override
	public void setHeightMap(int[] newHeightMap) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<BlockPos, TileEntity> getTileEntityMap() {
		//TODO: Important: Fix getTileEntityMap. Need to implement special Map that accesses tile entities from cubeMap
		return super.getTileEntityMap();
	}

	@Override
	public ClassInheritanceMultiMap<Entity>[] getEntityLists() {
		//TODO: need to make it returns something that contains correct data
		//Forge needs it and editing Forge classes with ASM is a bad idea
		return super.getEntityLists();
	}

	@Override
	@Deprecated
	public boolean isTerrainPopulated() {
		//with cubic chunks the whole column is never fully generated,
		//this method is currently used to determine list of chunks to be ticked
		//so let's say a chunk needs to be ticked if any cube needs to be ticked
		//for chunk to be ticked, it needs to be populated (or not ticked before, or close enough t player)
		return this.cubeMap.all().stream().anyMatch(Cube::isPopulated);
	}

	@Override
	@Deprecated
	public boolean isLightPopulated() {
		//with cubic chunks light is never generated in the whole column
		//this method is currently used to determine list of chunks to be ticked
		//so let's say a chunk needs to be ticked if any cube needs to be ticked
		//for chunk to be ticked, light can't be populated. So let's say it's populated
		//only if initial lighting is done in all cubes
		return this.cubeMap.all().stream().allMatch(Cube::isInitialLightingDone);
	}

	@Override
	public void removeInvalidTileEntity(@Nonnull BlockPos pos) {
		throw new UnsupportedOperationException("Not implemented because not used");
	}

	//===========================================
	//===========CubicChunks methods=============
	//===========================================

	private void init() {
		this.cubeMap = new CubeMap();
		//clientside we don't really need that much data. we actually only need top and bottom block Y positions
		if (this.getWorld().isRemote) {
			this.opacityIndex = new ClientOpacityIndex(this);
		} else {
			this.opacityIndex = new OpacityIndex();
		}
		this.roundRobinLightUpdatePointer = 0;
		this.roundRobinCubeQueue = new ArrayDeque<>();
		this.entities = new EntityContainer();

		// make sure no one's using data structures that have been replaced
		// also saves memory
		/*
		 * TODO: setting these vars to null would save memory, but they're final. =( also... make sure we're actually
		 * not using them
		 */
		// this.chunkSections = null;
		// this.heightMap = null;
		// this.skylightUpdateMap = null;

		Arrays.fill(super.getBiomeArray(), (byte) -1);
	}

	public long getAddress() {
		return AddressTools.getAddress(this.xPosition, this.zPosition);
	}

	public int getX() {
		return this.xPosition;
	}

	public int getZ() {
		return this.zPosition;
	}

	public EntityContainer getEntityContainer() {
		return this.entities;
	}

	public IOpacityIndex getOpacityIndex() {
		return this.opacityIndex;
	}

	public Collection<Cube> getAllCubes() {
		return Collections.unmodifiableCollection(this.cubeMap.all());
	}

	public Iterable<Cube> getCubes(int minY, int maxY) {
		return this.cubeMap.cubes(minY, maxY);
	}

	public boolean hasCubes() {
		return !this.cubeMap.isEmpty();
	}

	/**
	 * Warning: This method may give cube that is queued to be unloaded.
	 * You may want to use CubeCache instead
	 */
	public Cube getCube(int cubeY) {
		return this.cubeMap.get(cubeY);
	}

	private Cube getCube(BlockPos pos) {
		return getCube(Coords.blockToCube(pos.getY()));
	}

	public Cube getOrCreateCube(int cubeY, boolean isModified) {
		Cube cube = getCube(cubeY);

		if (cube == null) {
			cube = new Cube(this.world, this, this.xPosition, cubeY, this.zPosition, isModified);
			this.cubeMap.put(cubeY, cube);
			this.roundRobinCubeQueue.addFirst(cubeY);
		}
		return cube;
	}

	public Cube removeCube(int cubeY) {
		return this.cubeMap.remove(cubeY);
	}

	public void markSaved() {
		this.entities.markSaved(this.getWorld().getTotalWorldTime());
		this.setModified(false);
	}

	public Integer getTopFilledCubeY() {
		Integer blockY = null;
		for (int localX = 0; localX < Coords.CUBE_SIZE; localX++) {
			for (int localZ = 0; localZ < Coords.CUBE_SIZE; localZ++) {
				Integer y = this.opacityIndex.getTopBlockY(localX, localZ);
				if (y != null && (blockY == null || y > blockY)) {
					blockY = y;
				}
			}
		}
		if (blockY == null) {
			return null;
		}
		return Coords.blockToCube(blockY);
	}

	/**
	 * Returns Y position of the block directly above the top non-transparent block,
	 * or null is there are no non-transparent blocks
	 */
	public Integer getHeightmapAt(int localX, int localZ) {
		// NOTE: a "skylight" block is the transparent block that is directly one block above the top non-transparent block
		Integer topBlockY = this.opacityIndex.getTopBlockY(localX, localZ);
		if (topBlockY != null) {
			return topBlockY + 1;
		}
		return null;
	}

	@Override
	public int getLowestHeight() {
		return opacityIndex.getLowestTopBlockY();
	}

	public boolean isCompatBaseTerrainDone() {
		return compatBaseTerrainDone;
	}

	public void setCompatBaseTerrainDone(boolean terrain) {
		this.compatBaseTerrainDone = false;
	}

	public boolean isCompatPopulationDone() {
		return compatPopulationDone;
	}

	public void setCompatPopulationDone(boolean populated) {
		this.compatPopulationDone = populated;
	}
}
