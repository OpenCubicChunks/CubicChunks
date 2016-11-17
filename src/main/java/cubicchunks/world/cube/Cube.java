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

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ReportedException;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nullable;

import cubicchunks.util.AddressTools;
import cubicchunks.util.Coords;
import cubicchunks.util.CubePos;
import cubicchunks.util.XYZAddressable;
import cubicchunks.util.ticket.TicketList;
import cubicchunks.world.EntityContainer;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.IHeightMap;
import cubicchunks.world.column.Column;
import cubicchunks.worldgen.generator.ICubePrimer;

import static cubicchunks.CubicChunks.LOGGER;

/**
 * A cube is our extension of minecraft's chunk system to three dimensions. Each cube encloses a cubic area in the world
 * with a side length of {@link Cube#SIZE}, aligned to multiples of that length and stored within columns.
 */
public class Cube implements XYZAddressable {

	/**
	 * Side length of a cube
	 */
	public static final int SIZE = 16;

	private final LightUpdateData lightUpdateData = new LightUpdateData(this);
	/**
	 * Tickets keep this chunk loaded and ticking. See the docs of {@link TicketList} and {@link
	 * cubicchunks.util.ticket.ITicket} for additional information.
	 */
	private TicketList tickets; // tickets prevent this Cube from being unloaded
	/**
	 * Has anything within the cube changed since it was loaded from disk?
	 */
	private boolean isModified = false;
	/**
	 * Has the cube generator's populate() method been called for this cube?
	 */
	private boolean isPopulated = false;
	/**
	 * Has the cube generator's populate() method been called for every cube potentially writing to this cube during
	 * population?
	 */
	private boolean isFullyPopulated = false;
	/**
	 * Has the initial light map been calculated?
	 */
	private boolean isInitialLightingDone = false;
	/**
	 * The world of this cube
	 */
	private ICubicWorld world;
	/**
	 * The column of this cube
	 */
	private Column column;
	/**
	 * The position of this cube, in cube space
	 */
	private CubePos coords;
	/**
	 * Blocks in this cube
	 */
	private ExtendedBlockStorage storage;
	/**
	 * Entities in this cube
	 */
	private EntityContainer entities;
	/**
	 * The position of tile entities in this cube, and their corresponding tile entity
	 */
	private Map<BlockPos, TileEntity> tileEntityMap;
	/**
	 * The positions of tile entities queued for creation
	 */
	private ConcurrentLinkedQueue<BlockPos> tileEntityPosQueue;
	/**
	 * Is this cube loaded and not queued for unload
	 */
	private boolean isCubeLoaded;

	/**
	 * Create a new cube in the specified column at the specified location. The newly created cube will only contain air
	 * blocks.
	 *
	 * @param column column of this cube
	 * @param cubeY cube y position
	 */
	public Cube(Column column, int cubeY) {
		this.world = column.getCubicWorld();
		this.column = column;
		this.coords = new CubePos(column.getX(), cubeY, column.getZ());

		this.tickets = new TicketList();

		this.entities = new EntityContainer();
		this.tileEntityMap = new HashMap<>();
		this.tileEntityPosQueue = new ConcurrentLinkedQueue<>();
	}

	/**
	 * Create a new cube at the specified location by copying blocks from a cube primer.
	 *
	 * @param column column of this cube
	 * @param cubeY cube y position
	 * @param primer primer containing the blocks for this cube
	 */
	@SuppressWarnings("deprecation") // when a block is generated, does it really have any extra
	// information it could give us about its opacity by knowing its location?
	public Cube(Column column, int cubeY, ICubePrimer primer) {
		this(column, cubeY);

		int miny = Coords.cubeToMinBlock(cubeY);
		IHeightMap opindex = column.getOpacityIndex();

		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {

				for (int y = 15; y >= 0; y--) {
					IBlockState newstate = primer.getBlockState(x, y, z);

					if (newstate.getMaterial() != Material.AIR) {
						if (storage == null) {
							newStorage();
						}
						storage.set(x, y, z, newstate);

						if (newstate.getLightOpacity() != 0) {
							column.setModified(true); //TODO: this is a bit of am abstraction leak... maybe ServerHeightMap needs its own isModified
							opindex.onOpacityChange(x, miny + y, z, newstate.getLightOpacity());
						}
					}
				}
			}
		}
		isModified = true;
	}

	//======================================
	//========Chunk vanilla methods=========
	//======================================

	/**
	 * Retrieve the block state at the specified location
	 *
	 * @param pos target location
	 *
	 * @return The block state
	 *
	 * @see Cube#getBlockState(int, int, int)
	 */
	public IBlockState getBlockState(BlockPos pos) {
		return this.getBlockState(pos.getX(), pos.getY(), pos.getZ());
	}

	/**
	 * Set the block state at the specified location
	 *
	 * @param pos target location
	 * @param newstate target state of the block at that position
	 *
	 * @return The the old state of the block at the position, or null if there was no change
	 *
	 * @see Column#setBlockState(BlockPos, IBlockState)
	 */
	// forward to Column, as we don't know how to do skylight and stuff
	public IBlockState setBlockState(BlockPos pos, IBlockState newstate) {
		return column.setBlockState(pos, newstate);
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
	 * @see Cube#getBlockState(BlockPos)
	 */
	public IBlockState getBlockState(int blockX, int blockY, int blockZ) {
		try {
			if (storage == null) {
				return Blocks.AIR.getDefaultState();
			}
			return storage.get(Coords.blockToLocal(blockX),
				Coords.blockToLocal(blockY),
				Coords.blockToLocal(blockZ));

		} catch (Throwable t) {
			CrashReport report = CrashReport.makeCrashReport(t, "Getting block state");
			CrashReportCategory category = report.makeCategory("Block being got");
			category.setDetail("Location", () ->
				CrashReportCategory.getCoordinateInfo(blockX, blockY, blockZ));
			throw new ReportedException(report);
		}
	}

	/**
	 * Sets a block state in this cube, lighting not included
	 *
	 * @param pos the location of the block
	 * @param newstate the new block state
	 *
	 * @return The old block state, or null if there was no change
	 */
	@Nullable
	public IBlockState setBlockStateDirect(BlockPos pos, IBlockState newstate) {
		// TODO this method could probably be split up
		int localX = Coords.blockToLocal(pos.getX());
		int localY = Coords.blockToLocal(pos.getY());
		int localZ = Coords.blockToLocal(pos.getZ());

		IBlockState oldstate = getBlockState(pos);

		if (oldstate == newstate) {
			return null; // nothing changed
		}

		Block oldblock = oldstate.getBlock();
		Block newblock = newstate.getBlock();

		if (storage == null) {
			newStorage();
		}

		storage.set(localX, localY, localZ, newstate); // set the block state!

		// deal with Block.breakBlock() and TileEntity's
		if (!this.world.isRemote()) {
			if (newblock != oldblock) { //Only fire block breaks when the block changes.
				oldblock.breakBlock((World) this.world, pos, oldstate);
			}

			TileEntity te = this.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);

			if (te != null && te.shouldRefresh((World) this.world, pos, oldstate, newstate)) {
				this.world.removeTileEntity(pos);
			}
		} else if (oldblock.hasTileEntity(oldstate)) {
			TileEntity te = this.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);

			if (te != null && te.shouldRefresh((World) this.world, pos, oldstate, newstate)) {
				this.world.removeTileEntity(pos);
			}
		}

		if (storage.get(localX, localY, localZ).getBlock() != newblock) { // A TileEntity changed the bock on us!!!
			return null; // something changed... but its out of our control
			// (aka another Cube.setBlockState() call handled it)
			// so return as if 'nothing changed'
		}

		// If capturing blocks, only run block physics for TE's. Non-TE's are handled in ForgeHooks.onPlaceItemIntoWorld
		if (!this.world.isRemote()
			&& oldblock != newblock
			&& (!((World) this.world).captureBlockSnapshots || newblock.hasTileEntity(newstate))) {

			newblock.onBlockAdded((World) this.world, pos, newstate);
		}

		if (newblock.hasTileEntity(newstate)) {
			TileEntity te = this.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);

			if (te == null) {
				te = newblock.createTileEntity((World) this.world, newstate);
				this.world.setTileEntity(pos, te);
			}

			if (te != null) {
				te.updateContainingBlockInfo();
			}
		}

		this.isModified = true; // a block state changes, so we will need saving
		return oldstate;
	}

	/**
	 * Retrieve the raw light level at the specified location
	 *
	 * @param lightType The type of light (sky or block light)
	 * @param pos The position at which light should be checked
	 *
	 * @return the light level
	 */
	public int getLightFor(EnumSkyBlock lightType, BlockPos pos) {
		//it may not look like this but it's actually the same logic as in vanilla
		if (this.isEmpty()) {
			if (this.column.canSeeSky(pos)) {
				return lightType.defaultLightValue;
			}
			return 0;
		}

		int localX = Coords.blockToLocal(pos.getX());
		int localY = Coords.blockToLocal(pos.getY());
		int localZ = Coords.blockToLocal(pos.getZ());

		switch (lightType) {
			case SKY:
				return getSkylight(localX, localY, localZ);
			case BLOCK:
				if (storage == null) {
					return lightType.defaultLightValue;
				}
				return this.storage.getExtBlocklightValue(localX, localY, localZ);
			default:
				return lightType.defaultLightValue;
		}
	}

	/**
	 * Set the raw light level at the specified location
	 *
	 * @param lightType The type of light (sky or block light)
	 * @param pos The position at which light should be updated
	 * @param light the light level
	 */
	public void setLightFor(EnumSkyBlock lightType, BlockPos pos, int light) {
		this.isModified = true;

		int x = Coords.blockToLocal(pos.getX());
		int y = Coords.blockToLocal(pos.getY());
		int z = Coords.blockToLocal(pos.getZ());

		switch (lightType) {
			case SKY:
				setSkylight(x, y, z, light);
				break;

			case BLOCK:
				if (storage == null) {
					newStorage();
				}
				this.storage.setExtBlocklightValue(x, y, z, light);
				break;
		}
	}

	/**
	 * Set sky light level at this location. Has no effect if the world has no sky.
	 *
	 * @param localX block x position in local block coordinates
	 * @param localY block y position in local block coordinates
	 * @param localZ block z position in local block coordinates
	 * @param value the new light level
	 */
	public void setSkylight(int localX, int localY, int localZ, int value) {
		if (!this.world.getProvider().hasNoSky()) {
			if (storage == null) {
				newStorage();
			}
			this.isModified = true;
			this.storage.setExtSkylightValue(localX, localY, localZ, value);
		}
	}

	/**
	 * Retrieve sky light levels at this location. Always returns 0 for worlds with no sky.
	 *
	 * @param localX block x position in local block coordinates
	 * @param localY block y position in local block coordinates
	 * @param localZ block z position in local block coordinates
	 *
	 * @return sky light levels at this location.
	 */
	public int getSkylight(int localX, int localY, int localZ) {
		if (this.world.getProvider().hasNoSky()) {
			return 0;
		}
		if (storage == null) {
			return EnumSkyBlock.SKY.defaultLightValue;
		}
		return this.storage.getExtSkylightValue(localX, localY, localZ);
	}

	/**
	 * Retrieve actual light level at the specified location. This is the brightest of all types of light affecting this
	 * block
	 *
	 * @param pos the target position
	 * @param skyLightDampeningTerm skylight falloff factor
	 *
	 * @return actual light level at this location
	 */
	public int getLightSubtracted(BlockPos pos, int skyLightDampeningTerm) {
		// get sky light
		int skyLight = getLightFor(EnumSkyBlock.SKY, pos);
		skyLight -= skyLightDampeningTerm;

		// get block light
		int blockLight = getLightFor(EnumSkyBlock.BLOCK, pos);

		// FIGHT!!!
		return Math.max(blockLight, skyLight);
	}

	/**
	 * Create a tile entity at the given position if the block is able to hold one
	 *
	 * @param pos position where the tile entity should be placed
	 *
	 * @return the created tile entity, or <code>null</code> if the block at that position does not provide tile
	 * entities
	 */
	@Nullable
	private TileEntity createTileEntity(BlockPos pos) {
		IBlockState blockState = getBlockState(pos);
		Block block = blockState.getBlock();

		if (block.hasTileEntity(blockState)) {
			return block.createTileEntity((World) this.world, blockState);
		}
		return null;
	}

	/**
	 * Add an entity to this cube
	 *
	 * @param entity entity to add
	 */
	public void addEntity(Entity entity) {
		// make sure the entity is in this cube
		int cubeX = Coords.getCubeXForEntity(entity);
		int cubeY = Coords.getCubeYForEntity(entity);
		int cubeZ = Coords.getCubeZForEntity(entity);
		if (cubeX != this.coords.getX() || cubeY != this.coords.getY() || cubeZ != this.coords.getZ()) {
			LOGGER.warn(String.format("Wrong entity (%s) location. Entity thinks it's in (%d,%d,%d) but actua location is (%d,%d,%d)!",
				entity.getClass().getName(), cubeX, cubeY, cubeZ, this.coords.getX(), this.coords.getY(), this.coords.getZ()));
			entity.setDead();
		}

		//post the event, we can't send cube position here :(
		MinecraftForge.EVENT_BUS.post(new EntityEvent.EnteringChunk(
			entity, this.getX(), this.getZ(), entity.chunkCoordX, entity.chunkCoordZ));

		// tell the entity it's in this cube
		entity.addedToChunk = true;
		entity.chunkCoordX = this.coords.getX();
		entity.chunkCoordY = this.coords.getY();
		entity.chunkCoordZ = this.coords.getZ();

		this.entities.addEntity(entity);
		this.isModified = true;
	}

	/**
	 * Remove an entity from this cube
	 *
	 * @param entity The entity to remove
	 */
	public boolean removeEntity(Entity entity) {
		boolean wasRemoved = this.entities.remove(entity);
		if (wasRemoved) {
			this.isModified = true;
		}
		return wasRemoved;
	}

	/**
	 * Retrieve the tile entity at the specified location
	 *
	 * @param pos target location
	 * @param createType how fast the tile entity is needed
	 *
	 * @return the tile entity at the specified location, or <code>null</code> if there is no entity and
	 * <code>createType</code> was not {@link net.minecraft.world.chunk.Chunk.EnumCreateEntityType#IMMEDIATE}
	 */
	public TileEntity getTileEntity(BlockPos pos, Chunk.EnumCreateEntityType createType) {
		TileEntity blockEntity = this.tileEntityMap.get(pos);
		if (blockEntity != null && blockEntity.isInvalid()) {
			this.tileEntityMap.remove(pos);
			blockEntity = null;
		}

		if (blockEntity == null) {
			if (createType == Chunk.EnumCreateEntityType.IMMEDIATE) {
				blockEntity = createTileEntity(pos);
				this.world.setTileEntity(pos, blockEntity);
			} else if (createType == Chunk.EnumCreateEntityType.QUEUED) {
				this.tileEntityPosQueue.add(pos);
			}
		}

		return blockEntity;
	}

	/**
	 * Add a tile entity to this cube
	 *
	 * @param tileEntity The tile entity to add
	 */
	public void addTileEntity(TileEntity tileEntity) {
		this.addTileEntity(tileEntity.getPos(), tileEntity);
		if (this.isCubeLoaded) { //TODO: test to see if this is needed
			this.getCubicWorld().addTileEntity(tileEntity);
		}
	}

	/**
	 * Add a tile entity to this cube at the specified location
	 *
	 * @param pos The target location
	 * @param tileEntity The tile entity to add
	 */
	public void addTileEntity(BlockPos pos, TileEntity tileEntity) {
		// update the tile entity
		tileEntity.setWorld((World) this.world);
		tileEntity.setPos(pos);

		IBlockState blockState = this.getBlockState(pos);
		// is this block supposed to have a tile entity?
		if (blockState.getBlock().hasTileEntity(blockState)) {

			// cleanup the old tile entity
			TileEntity oldBlockEntity = this.tileEntityMap.get(pos);
			if (oldBlockEntity != null) {
				oldBlockEntity.invalidate();
			}

			// install the new tile entity
			tileEntity.validate();
			this.tileEntityMap.put(pos, tileEntity);
			this.isModified = true;
			tileEntity.onLoad();
		}
	}

	/**
	 * Remove the tile entity at the specified location
	 *
	 * @param pos target location
	 */
	public void removeTileEntity(BlockPos pos) {
		//it doesn't make sense to me to check if cube is loaded, but vanilla does it
		if (this.isCubeLoaded) { //TODO: test and see if this is needed
			TileEntity tileEntity = this.tileEntityMap.remove(pos);
			if (tileEntity != null) {
				tileEntity.invalidate();
				this.isModified = true;
			}
		}
	}

	/**
	 * Retrieve all matching entities within a specific area of the world that are also in this cube
	 *
	 * @param excluded don't include this entity in the results
	 * @param queryBox section of the world being checked
	 * @param out list to which found entities should be added
	 * @param predicate filter to match entities against
	 */
	public void getEntitiesWithinAABBForEntity(Entity excluded, AxisAlignedBB queryBox, List<Entity> out, Predicate<? super Entity> predicate) {
		this.entities.getEntitiesWithinAABBForEntity(excluded, queryBox, out, predicate);
	}

	/**
	 * Retrieve all matching entities of the specified type within a specific area of the world that are also in this
	 * world
	 *
	 * @param entityType the type of entity to retrieve
	 * @param queryBox section of the world being checked
	 * @param out list to which found entities should be added
	 * @param predicate filter to match entities against
	 * @param <T> type parameter for the class of entities being searched for
	 */
	public <T extends Entity> void getEntitiesOfTypeWithinAAAB(Class<? extends T> entityType, AxisAlignedBB queryBox, List<T> out, Predicate<? super T> predicate) {
		this.entities.getEntitiesOfTypeWithinAAAB(entityType, queryBox, out, predicate);
	}


	/**
	 * Tick this cube
	 *
	 * @param tryToTickFaster Whether costly calculations should be skipped in order to catch up with ticks
	 */
	public void tickCube(boolean tryToTickFaster) {
		if (!this.isInitialLightingDone && this.isPopulated) {
			this.tryDoFirstLight(); //TODO: Very icky light population code! REMOVE IT!
		}

		while (!this.tileEntityPosQueue.isEmpty()) {
			BlockPos blockpos = this.tileEntityPosQueue.poll();

			IBlockState state = this.getBlockState(blockpos);
			Block block = state.getBlock();

			if (this.getTileEntity(blockpos, Chunk.EnumCreateEntityType.CHECK) == null &&
				block.hasTileEntity(state)) {
				TileEntity tileentity = this.createTileEntity(blockpos);
				this.world.setTileEntity(blockpos, tileentity);
				this.world.markBlockRangeForRenderUpdate(blockpos, blockpos);
			}
		}
	}

	/**
	 * Calculate diffuse skylight in this cube if surrounding cubes are loaded
	 */
	//TODO: Redo light population code
	private void tryDoFirstLight() {
		BlockPos pos = this.getCoords().getMinBlockPos();
		final int radius = 17;
		// TODO replace hardcoded constant with reference to Cube#SIZE
		if (!world.isAreaLoaded(pos.add(-radius, -radius, -radius), pos.add(15 + radius, 15 + radius, 15 + radius))) {
			return;
		}
		//client cubes (setClientCube) are always fully generated, isInitialLightingDone is never false
		((ICubicWorldServer) this.world).getFirstLightProcessor().diffuseSkylight(this);
		this.isInitialLightingDone = true;
	}

	//=================================
	//=========Other methods===========
	//=================================

	/**
	 * Check if there are any non-air blocks in this cube
	 *
	 * @return <code>true</code> if this cube contains only air blocks, <code>false</code> otherwise
	 */
	public boolean isEmpty() {
		return storage == null || this.storage.isEmpty();
	}

	/**
	 * Return the long-encoded address of this cube's coordinates
	 *
	 * @return the cube's address
	 *
	 * @see AddressTools#getAddress(int, int, int)
	 */
	public long getAddress() {
		return AddressTools.getAddress(this.coords.getX(), this.coords.getY(), this.coords.getZ());
	}

	/**
	 * Convert an integer-encoded address to a local block to a global block position
	 *
	 * @param localAddress the address of the block
	 *
	 * @return the block position
	 */
	public BlockPos localAddressToBlockPos(int localAddress) {
		int x = Coords.localToBlock(this.coords.getX(), AddressTools.getLocalX(localAddress));
		int y = Coords.localToBlock(this.coords.getY(), AddressTools.getLocalY(localAddress));
		int z = Coords.localToBlock(this.coords.getZ(), AddressTools.getLocalZ(localAddress));
		return new BlockPos(x, y, z);
	}

	/**
	 * @return this cube's world
	 */
	public ICubicWorld getCubicWorld() {
		return this.world;
	}

	/**
	 * @return this cube's column
	 */
	public Column getColumn() {
		return this.column;
	}

	/**
	 * Retrieve this cube's x coordinate in cube space
	 *
	 * @return cube x position
	 */
	public int getX() {
		return this.coords.getX();
	}

	/**
	 * Retrieve this cube's y coordinate in cube space
	 *
	 * @return cube y position
	 */
	public int getY() {
		return this.coords.getY();
	}

	/**
	 * Retrieve this cube's z coordinate in cube space
	 *
	 * @return cube z position
	 */
	public int getZ() {
		return this.coords.getZ();
	}

	/**
	 * @return this cube's position
	 */
	public CubePos getCoords() {
		return this.coords;
	}

	/**
	 * Check whether a given global block position is contained in this cube
	 *
	 * @param blockPos the position of the block
	 *
	 * @return <code>true</code> if the position is within this cube, <code>false</code> otherwise
	 */
	public boolean containsBlockPos(BlockPos blockPos) {
		return this.coords.getX() == Coords.blockToCube(blockPos.getX())
			&& this.coords.getY() == Coords.blockToCube(blockPos.getY())
			&& this.coords.getZ() == Coords.blockToCube(blockPos.getZ());
	}

	public ExtendedBlockStorage getStorage() {
		return this.storage;
	}

	public ExtendedBlockStorage setStorage(ExtendedBlockStorage ebs) {
		return this.storage = ebs;
	}

	private void newStorage() {
		storage = new ExtendedBlockStorage(Coords.cubeToMinBlock(getY()), !world.getProvider().hasNoSky());
	}

	/**
	 * Retrieve a map of positions to their respective tile entities
	 *
	 * @return a map containing all tile entities in this cube
	 */
	public Map<BlockPos, TileEntity> getTileEntityMap() {
		return this.tileEntityMap;
	}

	/**
	 * Retrieve a list of entities in this cube
	 *
	 * @return the entities' container
	 */
	public EntityContainer getEntityContainer() {
		return this.entities;
	}

	/**
	 * Finish the cube loading process
	 */
	public void onLoad() {
		// tell the world about tile entities
		this.world.addTileEntities(this.tileEntityMap.values());
		this.world.loadEntities(this.entities.getEntities());
		this.isCubeLoaded = true;
	}

	/**
	 * Mark this cube as no longer part of this world
	 */
	public void onUnload() {
		//first mark as unloaded so that entity list and tile entity map isn't modified while iterating
		//and it also preserves all entities/time entities so they can be saved
		this.isCubeLoaded = false;

		// tell the world to forget about entities
		this.world.unloadEntities(this.entities.getEntities());

		for (Entity entity : this.entities.getEntities()) {
			//CHECKED: 1.10.2-12.18.1.2092
			entity.addedToChunk = false; // World tries to remove entities from Cubes
			// if (addedToCube || Column is loaded)
			// so we need to set addedToChunk to false as a hack!
			// else World would reload this Cube!
		}

		// tell the world to forget about tile entities
		for (TileEntity blockEntity : this.tileEntityMap.values()) {
			this.world.removeTileEntity(blockEntity.getPos());
		}
	}

	/**
	 * Check if any modifications happened to this cube since it was loaded from disk
	 *
	 * @return <code>true</code> if this cube should be written back to disk
	 */
	public boolean needsSaving() {
		return this.entities.needsSaving(true, this.world.getTotalWorldTime(), this.isModified);
	}

	/**
	 * Mark this cube as saved to disk
	 */
	public void markSaved() {
		this.entities.markSaved(this.world.getTotalWorldTime());
		this.isModified = false;
	}

	/**
	 * Retrieve a list of tickets currently holding this cube loaded
	 *
	 * @return the list of tickets
	 */
	public TicketList getTickets() {
		return tickets;
	}

	public void markForRenderUpdate() {
		this.world.markBlockRangeForRenderUpdate(
			Coords.cubeToMinBlock(this.coords.getX()), Coords.cubeToMinBlock(this.coords.getY()), Coords.cubeToMinBlock(this.coords.getZ()),
			Coords.cubeToMaxBlock(this.coords.getX()), Coords.cubeToMaxBlock(this.coords.getY()), Coords.cubeToMaxBlock(this.coords.getZ())
		);
	}

	/**
	 * Return a seed for random number generation. This seed is persistent across server restarts and returns the same
	 * value on every call for any given cube in any given world.
	 *
	 * @return this cube's random seed
	 */
	public long cubeRandomSeed() {
		long hash = 3;
		hash = 41*hash + this.world.getSeed();
		hash = 41*hash + getX();
		hash = 41*hash + getY();
		return 41*hash + getZ();
	}

	public LightUpdateData getLightUpdateData() {
		return this.lightUpdateData;
	}

	/**
	 * Mark this cube as a client side cube. Less work is done in this case, as we expect to receive updates from the
	 * server
	 */
	public void setClientCube() {
		this.isPopulated = true;
		this.isFullyPopulated = true;
		this.isInitialLightingDone = true;
	}

	/**
	 * Check whether this cube was populated, i.e. if this cube was passed as argument to {@link
	 * cubicchunks.worldgen.generator.ICubeGenerator#populate(Cube)}. Check there for more information regarding
	 * population.
	 *
	 * @return <code>true</code> if this cube has been populated, <code>false</code> otherwise
	 */
	public boolean isPopulated() {
		return isPopulated;
	}

	/**
	 * Mark this cube as populated. This means that this cube was passed as argument to {@link
	 * cubicchunks.worldgen.generator.ICubeGenerator#populate(Cube)}. Check there for more information regarding
	 * population.
	 *
	 * @param populated whether this cube was populated
	 */
	public void setPopulated(boolean populated) {
		this.isPopulated = populated;
		this.isModified = true;
	}

	/**
	 * Check whether this cube was fully populated, i.e. if any cube potentially writing to this cube was passed as an
	 * argument to {@link cubicchunks.worldgen.generator.ICubeGenerator#populate(Cube)}. Check there for more
	 * information regarding population
	 *
	 * @return <code>true</code> if this cube has been populated, <code>false</code> otherwise
	 */
	public boolean isFullyPopulated() {
		return this.isFullyPopulated;
	}

	/**
	 * Mark this cube as fully populated. This means that any cube potentially writing to this cube was passed as an
	 * argument to {@link cubicchunks.worldgen.generator.ICubeGenerator#populate(Cube)}. Check there for more
	 * information regarding population
	 *
	 * @param populated whether this cube was fully populated
	 */
	public void setFullyPopulated(boolean populated) {
		this.isFullyPopulated = populated;
		this.isModified = true;
	}

	/**
	 * Check whether this cube's initial diffuse skylight has been calculated
	 *
	 * @return <code>true</code> if it has been calculated, <code>false</code> otherwise
	 */
	public boolean isInitialLightingDone() {
		return isInitialLightingDone;
	}

	/**
	 * Notify this cube that it's initial diffuse skylight has been calculated
	 */
	public void setInitialLightingDone(boolean initialLightingDone) {
		this.isInitialLightingDone = initialLightingDone;
		this.isModified = true;
	}

	public static class LightUpdateData {
		private final Cube cube;
		private final short[] minMaxHeights = new short[256];
		//TODO: nullify minMaxHeights if toUpdateCounter is 0
		private int toUpdateCounter = 0;

		public LightUpdateData(Cube cube) {
			this.cube = cube;
			Arrays.fill(minMaxHeights, (short) 0xFFFF);
		}

		public void queueLightUpdate(int localX, int localZ, int minY, int maxY) {
			if (localX < 0 || localX > 15) {
				throw new IndexOutOfBoundsException("LocalX must be between 0 and 15, but was " + localX);
			}
			if (localZ < 0 || localZ > 15) {
				throw new IndexOutOfBoundsException("LocalZ must be between 0 and 15, but was " + localZ);
			}
			if (minY > maxY) {
				throw new IllegalArgumentException("minY > maxY (" + minY + " > " + maxY + ")");
			}

			minY -= Coords.cubeToMinBlock(cube.getY());
			maxY -= Coords.cubeToMinBlock(cube.getY());

			minY = MathHelper.clamp(minY, 0, 15);
			maxY = MathHelper.clamp(maxY, 0, 15);

			int index = index(localX, localZ);
			short v = minMaxHeights[localX << 4 | localZ];
			if (v == -1) {
				toUpdateCounter++;
				assert toUpdateCounter >= 0 && toUpdateCounter <= 256;
			}
			int min = unpackMin(v);
			int max = unpackMax(v);

			if (minY < min) {
				min = minY;
			}
			if (maxY > max) {
				max = maxY;
			}

			v = pack(min, max);
			assert v >= 0 && v < 256;
			this.minMaxHeights[index] = v;
		}

		public int getMin(int localX, int localZ) {
			return unpackMin(minMaxHeights[index(localX, localZ)]);
		}

		public int getMax(int localX, int localZ) {
			return unpackMax(minMaxHeights[index(localX, localZ)]);
		}

		public void remove(int localX, int localZ) {
			int index = index(localX, localZ);
			if (minMaxHeights[index] != -1) {
				toUpdateCounter--;
			}
			minMaxHeights[index] = -1;
		}

		private short pack(int min, int max) {
			return (short) (min << 4 | max);
		}

		private int unpackMin(short val) {
			if (val == -1) {
				return 16;
			}
			return val >> 4;
		}

		private int unpackMax(short val) {
			if (val == -1) {
				return -1;
			}
			return val & 0xf;
		}

		private int index(int x, int z) {
			return x << 4 | z;
		}
	}

}
