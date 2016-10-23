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

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import cubicchunks.lighting.LightingManager;
import cubicchunks.util.Coords;
import cubicchunks.util.MathUtil;
import cubicchunks.world.ClientHeightMap;
import cubicchunks.world.ICubeProvider;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.IHeightMap;
import cubicchunks.world.ServerHeightMap;
import cubicchunks.world.cube.Cube;

/**
 * A quasi-chunk, representing an infinitely tall section of the world.
 */
public class Column extends Chunk {

	private CubeMap cubeMap;
	private IHeightMap opacityIndex;

	private ICubeProvider provider;
	private ICubicWorld world;

	public Column(ICubeProvider provider, ICubicWorld world, int x, int z) {
		// NOTE: this constructor is called by the chunk loader
		super((World) world, x, z);

		this.provider = provider;
		this.world = world;
		init();
	}

	//=================================================
	//===============VANILLA METHODS===================
	//=================================================

	/**
	 * Return Y position of the block directly above the top non-transparent block, or {@link Coords#NO_HEIGHT} + 1 if
	 * there are no non-transparent blocks
	 */
	@Override
	public int getHeight(BlockPos pos) {
		return this.getHeightValue(
			Coords.blockToLocal(pos.getX()),
			Coords.blockToLocal(pos.getZ()));
	}

	/**
	 * Return Y position of the block directly above the top non-transparent block, or {@link Coords#NO_HEIGHT} + 1 if
	 * there are no non-transparent blocks
	 */
	@Override
	public int getHeightValue(int localX, int localZ) {
		// NOTE: the "height value" here is the height of the transparent block
		// on top of the highest non-transparent block
		return opacityIndex.getTopBlockY(localX, localZ) + 1;
	}

	@Override
	@Deprecated
	//TODO: stop this method form being used by vanilla (any algorithms in vanilla that use it are be broken any way)
	// don't use this! It's only here because vanilla needs it
	public int getTopFilledSegment() {
		//NOTE: this method actually returns block Y coords

		// PANIC!
		// this column doesn't have any blocks in it that aren't air!
		// but we can't return null here because vanilla code expects there to be a surface down there somewhere
		// we don't actually know where the surface is yet, because maybe it hasn't been generated
		// but we do know that the surface has to be at least at sea level,
		// so let's go with that for now and hope for the best

		// old solution
		// return this.getCubicWorld().provider.getAverageGroundLevel();

		int blockY = Coords.NO_HEIGHT;
		for (int localX = 0; localX < Cube.SIZE; localX++) {
			for (int localZ = 0; localZ < Cube.SIZE; localZ++) {
				int y = this.opacityIndex.getTopBlockY(localX, localZ);
				if (y > blockY) {
					blockY = y;
				}
			}
		}
		return Coords.cubeToMinBlock(Coords.blockToCube(blockY)); // return the lowest block in the Cube (kinda weird I know)
	}

	@Override
	@Deprecated // Vanilla can safely use this for block ticking, but just try to avoid it!
	public ExtendedBlockStorage[] getBlockStorageArray() {
		return cubeMap.getStoragesToTick();
	}

	@SideOnly(Side.CLIENT)
	protected void generateHeightMap() {
		//this method reduces to no-op with CubicChunks, heightmap is generated in real time
	}

	@Override
	@Deprecated
	public void generateSkylightMap() {
		throw new UnsupportedOperationException("Functionality of this method is replaced with LightingManager");
	}


	/**
	 * Retrieve the block state at the specified location
	 *
	 * @param pos target location
	 *
	 * @return The block state
	 *
	 * @see Column#getBlockState(int, int, int)
	 * @see Cube#getBlockState(BlockPos)
	 */
	@Override
	public IBlockState getBlockState(BlockPos pos) {
		//forward to cube
		return this.getCube(pos).getBlockState(pos);
	}

	/**
	 * Retrieve the block state at the specified location
	 *
	 * @param blockX block x position
	 * @param blockY block y position
	 * @param blockZ block z position
	 *
	 * @return The block state
	 *
	 * @see Column#getBlockState(BlockPos)
	 * @see Cube#getBlockState(int, int, int)
	 */
	@Override
	public IBlockState getBlockState(final int blockX, final int blockY, final int blockZ) {
		//forward to cube
		return this.getCube(Coords.blockToLocal(blockY)).getBlockState(blockX, blockY, blockZ);
	}

	/**
	 * Set the block state at the specified location
	 *
	 * @param pos target location
	 * @param newstate target state of the block at that position
	 *
	 * @return The the old state of the block at the position, or null if there was no change
	 */
	@Override
	public IBlockState setBlockState(BlockPos pos, @Nonnull IBlockState newstate) {
		Cube cube = getCube(Coords.blockToCube(pos.getY()));
		IBlockState oldstate = cube.getBlockState(pos);

		// get the old opacity for use when updating the heightmap
		int oldOpacity = oldstate.getLightOpacity(this.getWorld(), pos);

		oldstate = cube.setBlockStateDirect(pos, newstate); // forward to cube
		if (oldstate == null) {
			// Nothing changed
			return null;
		}

		this.doOnBlockSetLightUpdates(pos, newstate, oldOpacity);

		return oldstate;
	}

	/**
	 * Update lighting at the specified location after a block was changed
	 *
	 * @param pos target location
	 * @param newBlockState the new state of that block
	 * @param oldOpacity the original opacity of that block
	 */
	//TODO: This looks ugly idk
	private void doOnBlockSetLightUpdates(BlockPos pos, IBlockState newBlockState, int oldOpacity) {
		int newOpacity = newBlockState.getLightOpacity(this.getWorld(), pos);
		if (oldOpacity == newOpacity || (oldOpacity >= 15 && newOpacity >= 15)) {
			//nothing to update, this will frequently happen in ore generation
			return;
		}

		int localX = Coords.blockToLocal(pos.getX());
		int localZ = Coords.blockToLocal(pos.getZ());

		// did the top non-transparent block change?
		int oldSkylightY = getHeightValue(localX, localZ);
		this.opacityIndex.onOpacityChange(localX, pos.getY(), localZ, newOpacity);
		setModified(true);

		int newSkylightY = oldSkylightY;
		if (!getWorld().isRemote) {
			newSkylightY = getHeightValue(localX, localZ);
			//if oldSkylightY == null and newOpacity == 0 then we didn't change anything
		} else if (!(oldSkylightY < world.getMinHeight() && newOpacity == 0)) {
			int oldSkylightActual = oldSkylightY - 1;
			//to avoid unnecessary delay when breaking blocks we need to hack it clientside
			if ((pos.getY() > oldSkylightActual - 1) && newOpacity != 0) {
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
			((ClientHeightMap) opacityIndex).setHeight(localX, localZ, newSkylightY);
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

	/**
	 * Retrieve the raw light level at the specified location
	 *
	 * @param type The type of light (sky or block light)
	 * @param pos The position at which light should be checked
	 *
	 * @return the light level
	 *
	 * @see Cube#getLightFor(EnumSkyBlock, BlockPos)
	 */
	@Override
	public int getLightFor(@Nonnull EnumSkyBlock type, BlockPos pos) {
		//forward to cube
		return getCube(pos).getLightFor(type, pos);
	}

	/**
	 * Set the raw light level at the specified location
	 *
	 * @param type The type of light (sky or block light)
	 * @param pos The position at which light should be updated
	 * @param value the light level
	 *
	 * @see Cube#setLightFor(EnumSkyBlock, BlockPos, int)
	 */
	@Override
	public void setLightFor(EnumSkyBlock type, BlockPos pos, int value) {
		//forward to cube
		getCube(pos).setLightFor(type, pos, value);
	}

	/**
	 * Retrieve actual light level at the specified location. This is the brightest of all types of light affecting this
	 * block
	 *
	 * @param pos the target position
	 * @param amount skylight falloff factor
	 *
	 * @return actual light level at this location
	 *
	 * @see Cube#getLightSubtracted(BlockPos, int)
	 */
	@Override
	public int getLightSubtracted(BlockPos pos, int amount) {
		//forward to cube
		return getCube(pos).getLightSubtracted(pos, amount);
	}

	/**
	 * Add an entity to this column
	 *
	 * @param entity entity to add
	 *
	 * @see Cube#addEntity(Entity)
	 */
	@Override
	public void addEntity(Entity entity) {
		//forward to cube
		getCube(Coords.getCubeYForEntity(entity)).addEntity(entity);
	}

	/**
	 * Remove an entity from this column
	 *
	 * @param entityIn The entity to remove
	 *
	 * @see Column#removeEntityAtIndex(Entity, int)
	 * @see Cube#removeEntity(Entity)
	 */
	@Override
	public void removeEntity(Entity entityIn) {
		this.removeEntityAtIndex(entityIn, entityIn.chunkCoordY);
	}

	/**
	 * Remove an entity from the cube at the specified location
	 *
	 * @param entity The entity to remove
	 * @param cubeY cube y location
	 *
	 * @see Cube#removeEntity(Entity)
	 */
	@Override
	public void removeEntityAtIndex(@Nonnull Entity entity, int cubeY) {
		//forward to cube
		getCube(cubeY).removeEntity(entity);
	}

	/**
	 * Check whether the block at the specified location has a clear line of view towards the sky
	 *
	 * @param pos target location
	 *
	 * @return <code>true</code> if there is no block between this block and the sky, <code>false</code> otherwise
	 */
	@Override
	public boolean canSeeSky(BlockPos pos) {
		int height = this.getHeightValue(
			Coords.blockToLocal(pos.getX()),
			Coords.blockToLocal(pos.getZ()));

		return pos.getY() >= height;
	}

	/**
	 * Retrieve the tile entity at the specified location
	 *
	 * @param pos target location
	 * @param createType how fast the tile entity is needed
	 *
	 * @return the tile entity at the specified location, or <code>null</code> if there is no entity and
	 * <code>createType</code> was not {@link net.minecraft.world.chunk.Chunk.EnumCreateEntityType#IMMEDIATE}
	 *
	 * @see Cube#getTileEntity(BlockPos, EnumCreateEntityType)
	 */
	@Override
	public TileEntity getTileEntity(@Nonnull BlockPos pos, Chunk.EnumCreateEntityType createType) {
		//forward to cube
		return getCube(pos).getTileEntity(pos, createType);
	}

	/**
	 * Add a tile entity to this column
	 *
	 * @param tileEntity The tile entity to add
	 *
	 * @see Cube#addTileEntity(TileEntity)
	 */
	@Override
	public void addTileEntity(TileEntity tileEntity) {
		// pass off to the cube
		getCube(tileEntity.getPos()).addTileEntity(tileEntity);
	}

	/**
	 * Add a tile entity to this column at the specified location
	 *
	 * @param pos The target location
	 * @param blockEntity The tile entity to add
	 *
	 * @see Cube#addTileEntity(BlockPos, TileEntity)
	 */
	@Override
	public void addTileEntity(@Nonnull BlockPos pos, TileEntity blockEntity) {
		// pass off to the cube
		getCube(pos).addTileEntity(pos, blockEntity);
	}

	/**
	 * Remove the tile entity at the specified location
	 *
	 * @param pos target location
	 */
	@Override
	public void removeTileEntity(@Nonnull BlockPos pos) {
		//forward to cube
		this.getCube(pos).removeTileEntity(pos);
	}

	/**
	 * Called when this column is finished loading
	 */
	@Override
	public void onChunkLoad() {
		this.isChunkLoaded = true;
		MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(this));
	}

	/**
	 * Called when this column is being unloaded
	 */
	@Override
	public void onChunkUnload() {
		this.isChunkLoaded = false;
		MinecraftForge.EVENT_BUS.post(new ChunkEvent.Unload(this));
	}

	//setChunkModified() goes here, it's unchanged

	/**
	 * Retrieve all matching entities within a specific area of the world
	 *
	 * @param exclude don't include this entity in the results
	 * @param queryBox section of the world being checked
	 * @param out list to which found entities should be added
	 * @param predicate filter to match entities against
	 */
	@Override
	public void getEntitiesWithinAABBForEntity(Entity exclude, AxisAlignedBB queryBox, @Nonnull List<Entity> out, Predicate<? super Entity> predicate) {

		// get a y-range that 2 blocks wider than the box for safety
		int minCubeY = Coords.blockToCube(MathHelper.floor_double(queryBox.minY - World.MAX_ENTITY_RADIUS));
		int maxCubeY = Coords.blockToCube(MathHelper.floor_double(queryBox.maxY + World.MAX_ENTITY_RADIUS));

		for (int cubeY = minCubeY; cubeY <= maxCubeY; cubeY++) {
			Cube cube = getCube(cubeY);
			cube.getEntitiesWithinAABBForEntity(exclude, queryBox, out, predicate);
		}
	}

	/**
	 * Retrieve all matching entities of the specified type within a specific area of the world
	 *
	 * @param entityType the type of entity to retrieve
	 * @param queryBox section of the world being checked
	 * @param out list to which found entities should be added
	 * @param predicate filter to match entities against
	 * @param <T> type parameter for the class of entities being searched for
	 */
	@Override
	public <T extends Entity> void getEntitiesOfTypeWithinAAAB(@Nonnull Class<? extends T> entityType, AxisAlignedBB queryBox, @Nonnull List<T> out, Predicate<? super T> predicate) {

		// get a y-range that 2 blocks wider than the box for safety
		int minCubeY = Coords.blockToCube(MathHelper.floor_double(queryBox.minY - World.MAX_ENTITY_RADIUS));
		int maxCubeY = Coords.blockToCube(MathHelper.floor_double(queryBox.maxY + World.MAX_ENTITY_RADIUS));

		for (int cubeY = minCubeY; cubeY < maxCubeY + 1; cubeY++) {
			Cube cube = getCube(cubeY);
			cube.getEntitiesOfTypeWithinAAAB(entityType, queryBox, out, predicate);
		}
	}

	/**
	 * Check whether this column needs to be written back to disk for persistence
	 *
	 * @param flag unused
	 *
	 * @return <code>true</code> if there were modifications since the time this column was loaded from disk,
	 * <code>false</code> otherwise
	 */
	@Override
	public boolean needsSaving(boolean flag) {
		return this.isModified;
	}

	//getRandomWithSeed(seed) doesn't need changes

	//isEmpty() doesn't need changes

	@Override
	@Deprecated
	public void populateChunk(IChunkProvider chunkProvider, @Nonnull IChunkGenerator chunkGenerator) {
		throw new UnsupportedOperationException("This method is incompatible with CubicChunks");
	}

	/**
	 * Retrieve lowest block still affected by rain and snow
	 *
	 * @param pos The target block column to check
	 *
	 * @return The lowest block at the same x and z coordinates that is still hit by rain and snow
	 */
	@Override
	//TODO: Actual precipitation heightmap, currently skylight heightmap is used which triggers an old MC alpha bug
	public BlockPos getPrecipitationHeight(BlockPos pos) {
		return new BlockPos(pos.getX(), this.getHeight(pos), pos.getZ());
	}

	/**
	 * Tick this column
	 *
	 * @param tryToTickFaster Whether costly calculations should be skipped in order to catch up with ticks
	 */
	@Override
	public void onTick(boolean tryToTickFaster) {
		this.chunkTicked = true;
		cubeMap.forEach((c) -> c.tickCube(tryToTickFaster));
	}

	@Override
	@Deprecated
	public boolean isPopulated() {
		return true; //stub... its broken and only used in World.markAndNotifyBlock()
	}

	//isCHunkTicked() doesn't need changes

	//getChunkCoordIntPair doesn't need changes

	/**
	 * See if there is any blocks in the specified section of the world. Note that while parameters are block
	 * coordinates, the check is actually aligned to cubes.
	 *
	 * @param minBlockY Lower end of the section being checked
	 * @param maxBlockY Upper end of the section being checked
	 *
	 * @return <code>true</code> if there is only air blocks in the checked cubes, <code>false</code> otherwise
	 */
	@Override
	// used for by ChunkCache, and that is used for rendering to see
	// if there are any blocks, or is there just air
	public boolean getAreLevelsEmpty(int minBlockY, int maxBlockY) {
		int minCubeY = Coords.blockToCube(minBlockY);
		int maxCubeY = Coords.blockToCube(maxBlockY);
		for (int cubeY = minCubeY; cubeY <= maxCubeY; cubeY++) {
			Cube cube = getCube(cubeY); // yes, load/generate a chunk if there is none... 
			// even if its not loaded there is still technical something there
			if (!cube.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	@Deprecated
	public void setStorageArrays(ExtendedBlockStorage[] newArray) {
		throw new UnsupportedOperationException("This method is incompatible with Cubic Chunks");
	}

	@Override
	@Deprecated
	@SideOnly(Side.CLIENT)
	public void fillChunk(@Nonnull PacketBuffer buf, int p_186033_2_, boolean p_186033_3_) {
		throw new UnsupportedOperationException("This method is incompatible with Cubic Chunks");
	}

	//getBiome doesn't need changes

	//getBiomeArray doesn't need changes

	//setBiomeArray doesn't need changes


	// TODO: lighting should not be done in Column
	@Override
	@Deprecated
	public void resetRelightChecks() {
		throw new UnsupportedOperationException("This method is incompatible with Cubic Chunks");
	}

	//TODO: enqueueRelightChecks() must die! (it should be in its own lighting system or at least only in Cube)
	@Override
	@Deprecated
	public void enqueueRelightChecks() {
		// stub
	}

	@Override
	@Deprecated
	public void checkLight() {
		//no-op on cubic chunks
	}

	//isLoaded doesn't need changes

	//getWorld doesn't need changes

	@Override
	@Deprecated // TODO: only used by IONbtReader and IONbtWriter ... and those are super broken
	//       add throw new UnsupportedOperationException();
	public int[] getHeightMap() {
		return this.opacityIndex.getHeightmap();
	}

	@Override
	@Deprecated
	public void setHeightMap(int[] newHeightMap) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Retrieve a map of all tile entities in this column and their locations
	 *
	 * @return A map, mapping positions to tile entities at that position
	 */
	@Override
	public Map<BlockPos, TileEntity> getTileEntityMap() {
		//TODO: Important: Fix getTileEntityMap. Need to implement special Map that accesses tile entities from cubeMap
		return super.getTileEntityMap();
	}

	/**
	 * Retrieve a list of all entities in this column
	 *
	 * @return the list of entities
	 */
	@Override
	public ClassInheritanceMultiMap<Entity>[] getEntityLists() {
		//TODO: need to make it returns something that contains correct data
		//Forge needs it and editing Forge classes with ASM is a bad idea
		return super.getEntityLists();
	}

	@Override
	@Deprecated //TODO: only used in PlayerCubeMap.getChunkIterator() (see todo's in that)
	//      add throw new UnsupportedOperationException();
	public boolean isTerrainPopulated() {
		//with cubic chunks the whole column is never fully generated,
		//this method is currently used to determine list of chunks to be ticked
		//and PlayerCubeMap :(
		return true; //TODO: stub, replace with new UnsupportedOperationException();
	}

	@Override
	@Deprecated
	public boolean isLightPopulated() {
		//with cubic chunks light is never generated in the whole column
		//this method is currently used to determine list of chunks to be ticked
		//and PlayerCubeMap :(
		return true; //TODO: stub, replace with new UnsupportedOperationException();
	}

	/**
	 * Check if this column needs to be ticked
	 *
	 * @return <code>true</code> if any cube in this column needs to be ticked, <code>false</code> otherwise
	 */
	public boolean shouldTick() {
		for (Cube cube : cubeMap) {
			if (cube.getTickets().shouldTick()) {
				return true;
			}
		}
		return false;
	}

	@Override
	@Deprecated
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
			this.opacityIndex = new ClientHeightMap(this);
		} else {
			this.opacityIndex = new ServerHeightMap();
		}

		// make sure no one's using data structures that have been replaced
		// also saves memory
		/*
		 * TODO: setting these vars to null would save memory, also... make sure we're actually
		 * not using them
		 */
		// this.chunkSections = null;
		// this.heightMap = null;
		// this.skylightUpdateMap = null;

		Arrays.fill(super.getBiomeArray(), (byte) -1);
	}

	/**
	 * @return x position of this column
	 */
	public int getX() {
		return this.xPosition;
	}

	/**
	 * @return z position of this column
	 */
	public int getZ() {
		return this.zPosition;
	}

	/**
	 * @return the height map of this column
	 */
	public IHeightMap getOpacityIndex() {
		return this.opacityIndex;
	}

	/**
	 * Retrieve all cubes in this column that are currently loaded
	 *
	 * @return the cubes
	 */
	public Collection<Cube> getLoadedCubes() {
		return this.cubeMap.all();
	}


	// =========================================
	// =======Mini CubeCache like methods=======
	// =========================================

	/**
	 * Iterate over all loaded cubes in this column in order. If <code>startY < endY</code>, order is bottom to top,
	 * otherwise order is top to bottom.
	 *
	 * @param startY initial cube y position
	 * @param endY last cube y position
	 *
	 * @return an iterator over all loaded cubes between <code>startY</code> and <code>endY</code>
	 */
	public Iterable<Cube> getLoadedCubes(int startY, int endY) {
		return this.cubeMap.cubes(startY, endY);
	}

	/**
	 * Retrieve the cube at the specified location if it is loaded.
	 *
	 * @param cubeY cube y position
	 *
	 * @return the cube at that position, or <code>null</code> if it is not loaded
	 */
	@Nullable
	public Cube getLoadedCube(int cubeY) {
		return provider.getLoadedCube(getX(), cubeY, getZ());
	}

	/**
	 * Retrieve the cube at the specified location
	 *
	 * @param cubeY cube y position
	 *
	 * @return the cube at that position
	 */
	public Cube getCube(int cubeY) {
		return provider.getCube(getX(), cubeY, getZ());
	}

	/**
	 * Retrieve the cube containing the specified block
	 *
	 * @param pos the target block position
	 *
	 * @return the cube containing that block
	 */
	private Cube getCube(BlockPos pos) {
		return getCube(Coords.blockToCube(pos.getY()));
	}

	/**
	 * Add a cube to this column
	 *
	 * @param cube the cube being added
	 */
	public void addCube(Cube cube) {
		this.cubeMap.put(cube);
	}

	/**
	 * Remove the cube at the specified height
	 *
	 * @param cubeY cube y position
	 *
	 * @return the removed cube if it existed, otherwise <code>null</code>
	 */
	public Cube removeCube(int cubeY) {
		return this.cubeMap.remove(cubeY);
	}

	/**
	 * Check if there are any loaded cube in this column
	 *
	 * @return <code>true</code> if there is at least on loaded cube in this column, <code>false</code> otherwise
	 */
	public boolean hasLoadedCubes() {
		return !this.cubeMap.isEmpty();
	}

	// ======= end cube cache like methods =======
	// ===========================================

	/**
	 * Notify this column that it has been saved
	 */
	public void markSaved() {
		this.setModified(false);
	}

	/**
	 * Among all top blocks in this column, return the height of the lowest one
	 *
	 * @return the height of the lowest top block
	 */
	@Override
	public int getLowestHeight() {
		return opacityIndex.getLowestTopBlockY();
	}

	/**
	 * Retrieve the world to which this column belongs
	 *
	 * @return the world
	 */
	public ICubicWorld getCubicWorld() {
		return world;
	}
}
