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
import cubicchunks.util.AddressTools;
import cubicchunks.util.Coords;
import cubicchunks.util.CubeCoords;
import cubicchunks.util.XYZAddressable;
import cubicchunks.world.EntityContainer;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.IOpacityIndex;
import cubicchunks.world.column.Column;
import cubicchunks.worldgen.generator.ICubePrimer;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ICrashReportDetail;
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
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nullable;

public class Cube implements XYZAddressable {

	public static final int SIZE = 16;

	private static final Logger LOGGER = CubicChunks.LOGGER;

	//used to track if the cube should be unloaded or not, done instead of removing cube from
	//unloadQueue each time something loads it
	public  boolean unloaded;

	private boolean isModified = false;
	private boolean isPopulated = false;
	private boolean isFullyPopulated = false;
	private boolean isInitialLightingDone = false;

	private ICubicWorld world;
	private Column column;

	private CubeCoords coords;
	private ExtendedBlockStorage storage;

	private EntityContainer entities;
	private Map<BlockPos, TileEntity> tileEntityMap;

	/**
	 * "queue containing the BlockPos of tile entities queued for creation"
	 */
	private ConcurrentLinkedQueue<BlockPos> tileEntityPosQueue;

	private final LightUpdateData lightUpdateData = new LightUpdateData(this);

	private boolean isCubeLoaded;

	public Cube(Column column, int cubeY) {
		this.world = column.getCubicWorld();
		this.column = column;
		this.coords = new CubeCoords(column.getX(), cubeY, column.getZ());

		this.entities = new EntityContainer();
		this.tileEntityMap = new HashMap<>();
		this.tileEntityPosQueue = new ConcurrentLinkedQueue<>();
	}

	@SuppressWarnings("deprecation") // when a block is generated, does it really have any extra
	                                 // information it could give us about its opacity by knowing its location?
	public Cube(Column column, int cubeY, ICubePrimer primer) {
		this(column, cubeY);

		int miny = Coords.cubeToMinBlock(cubeY);
		IOpacityIndex opindex = column.getOpacityIndex();

		for (int x = 0; x < 16;x++) {
			for (int z = 0; z < 16;z++) {

				for (int y = 15; y >= 0;y--) {
					IBlockState newstate = primer.getBlockState(x, y, z);

					if (newstate.getMaterial() != Material.AIR) {
						if(storage == null) {
							newStorage();
						}
						storage.set(x, y, z, newstate);

						if(newstate.getLightOpacity() != 0) {
							column.setModified(true); //TODO: this is a bit of am abstraction leak... maybe OpacityIndex needs its own isModified
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

	public IBlockState getBlockState(BlockPos pos) {
		return this.getBlockState(pos.getX(), pos.getY(), pos.getZ());
	}

	// forward to Column, as we don't know how to do skylight and stuff
	public IBlockState setBlockState(BlockPos pos, IBlockState newstate) {
		return column.setBlockState(pos, newstate);
	}

	public IBlockState getBlockState(int blockX, int blockY, int blockZ) {
		try {
			if(storage == null) {
				return Blocks.AIR.getDefaultState();
			}
			return storage.get(Coords.blockToLocal(blockX),
			                   Coords.blockToLocal(blockY),
			                   Coords.blockToLocal(blockZ));

		} catch (Throwable t) {
			CrashReport report = CrashReport.makeCrashReport(t, "Getting block state");
			CrashReportCategory category = report.makeCategory("Block being got");
			category.setDetail("Location", new ICrashReportDetail<String>() {
				@Override
				public String call() throws Exception {
					return CrashReportCategory.getCoordinateInfo(blockX, blockY, blockZ);
				}
			});
			throw new ReportedException(report);
		}
	}

	/**
	 * Sets a block state in this cube, lighting not included
	 * 
	 * @param pos the location of the block
	 * @param newstate the new block state
	 * @return The old block state, or null if there was no change
	 */
	@Nullable
	public IBlockState setBlockStateDirect(BlockPos pos, IBlockState newstate) {
		int localX = Coords.blockToLocal(pos.getX());
		int localY = Coords.blockToLocal(pos.getY());
		int localZ = Coords.blockToLocal(pos.getZ());

		IBlockState oldstate = getBlockState(pos);

		if(oldstate == newstate) {
			return null; // nothing changed
		}

		Block oldblock = oldstate.getBlock();
		Block newblock = newstate.getBlock();

		if(storage == null) {
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

		if(storage.get(localX, localY, localZ).getBlock() != newblock) { // A TileEntity changed the bock on us!!!
			return null; // something changed... but its out of our control
			             // (aka another Cube.setBlockState() call handled it)
			             // so return as if 'nothing changed'
		}

		// If capturing blocks, only run block physics for TE's. Non-TE's are handled in ForgeHooks.onPlaceItemIntoWorld
		if (!this.world.isRemote()
				&& oldblock != newblock
				&& (!((World)this.world).captureBlockSnapshots || newblock.hasTileEntity(newstate))) {

			newblock.onBlockAdded((World)this.world, pos, newstate);
		}

		if (newblock.hasTileEntity(newstate)) {
			TileEntity te = this.getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK);

			if (te == null)
			{
				te = newblock.createTileEntity((World)this.world, newstate);
				this.world.setTileEntity(pos, te);
			}

			if (te != null)
			{
				te.updateContainingBlockInfo();
			}
		}

		this.isModified = true; // a block state changes, so we will need saving
		return oldstate;
	}

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
				if (this.world.getProvider().getHasNoSky()) {
					return 0;
				}
				if(storage == null) {
					return lightType.defaultLightValue;
				}
				return this.storage.getExtSkylightValue(localX, localY, localZ);
			case BLOCK:
				if(storage == null) {
					return lightType.defaultLightValue;
				}
				return this.storage.getExtBlocklightValue(localX, localY, localZ);
			default:
				return lightType.defaultLightValue;
		}
	}

	public void setLightFor(EnumSkyBlock lightType, BlockPos pos, int light) {
		this.isModified = true;

		int x = Coords.blockToLocal(pos.getX());
		int y = Coords.blockToLocal(pos.getY());
		int z = Coords.blockToLocal(pos.getZ());

		switch (lightType) {
			case SKY:
				if (!this.world.getProvider().getHasNoSky()) {
					if(storage == null) {
						newStorage();
					}
					this.storage.setExtSkylightValue(x, y, z, light);
				}
				break;

			case BLOCK:
				if(storage == null) {
					newStorage();
				}
				this.storage.setExtBlocklightValue(x, y, z, light);
				break;
		}
	}

	public void setSkylight(int localX, int localY, int localZ, int value) {
		if (!this.world.getProvider().getHasNoSky()) {
			if(storage == null) {
				newStorage();
			}
			this.isModified = true;
			this.storage.setExtSkylightValue(localX, localY, localZ, value);
		}
	}

	public int getSkylight(int localX, int localY, int localZ) {
		if (this.world.getProvider().getHasNoSky()) {
			return 0;
		}
		if(storage == null) {
			return EnumSkyBlock.SKY.defaultLightValue;
		}
		return this.storage.getExtSkylightValue(localX, localY, localZ);
	}


	public int getLightSubtracted(BlockPos pos, int skyLightDampeningTerm) {
		// get sky light
		int skyLight = getLightFor(EnumSkyBlock.SKY, pos);
		skyLight -= skyLightDampeningTerm;

		// get block light
		int blockLight = getLightFor(EnumSkyBlock.BLOCK, pos);

		// FIGHT!!!
		return Math.max(blockLight, skyLight);
	}

	private TileEntity createTileEntity(BlockPos pos) {
		IBlockState blockState = getBlockState(pos);
		Block block = blockState.getBlock();

		if (block.hasTileEntity(blockState)) {
			return block.createTileEntity((World) this.world, blockState);
		}
		return null;
	}

	public void addEntity(Entity entity) {
		// make sure the entity is in this cube
		int cubeX = Coords.getCubeXForEntity(entity);
		int cubeY = Coords.getCubeYForEntity(entity);
		int cubeZ = Coords.getCubeZForEntity(entity);
		if (cubeX != this.coords.getCubeX() || cubeY != this.coords.getCubeY() || cubeZ != this.coords.getCubeZ()) {
			LOGGER.warn(String.format("Wrong entity (%s) location. Entity thinks it's in (%d,%d,%d) but actua location is (%d,%d,%d)!",
					entity.getClass().getName(), cubeX, cubeY, cubeZ, this.coords.getCubeX(), this.coords.getCubeY(), this.coords.getCubeZ()));
			entity.setDead();
		}

		//post the event, we can't send cube position here :(
		MinecraftForge.EVENT_BUS.post(new EntityEvent.EnteringChunk(
				entity, this.getX(), this.getZ(), entity.chunkCoordX, entity.chunkCoordZ));

		// tell the entity it's in this cube
		entity.addedToChunk = true;
		entity.chunkCoordX = this.coords.getCubeX();
		entity.chunkCoordY = this.coords.getCubeY();
		entity.chunkCoordZ = this.coords.getCubeZ();

		this.entities.addEntity(entity);
		this.isModified = true;
	}

	public boolean removeEntity(Entity entity) {
		boolean wasRemoved = this.entities.remove(entity);
		if (wasRemoved) {
			this.isModified = true;
		}
		return wasRemoved;
	}

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

	public void addTileEntity(TileEntity tileEntity) {
		this.addTileEntity(tileEntity.getPos(), tileEntity);
		if (this.isCubeLoaded) { //TODO: test to see if this is needed
			this.getWorld().addTileEntity(tileEntity);
		}
	}

	public void addTileEntity(BlockPos pos, TileEntity tileEntity) {
		// update the tile entity
		tileEntity.setWorldObj((World) this.world);
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

	public void getEntitiesWithinAABBForEntity(Entity excluded, AxisAlignedBB queryBox, List<Entity> out, Predicate<? super Entity> predicate) {
		this.entities.getEntitiesWithinAABBForEntity(excluded, queryBox, out, predicate);
	}

	public <T extends Entity> void getEntitiesOfTypeWithinAAAB(Class<? extends T> entityType, AxisAlignedBB queryBox, List<T> out, Predicate<? super T> predicate) {
		this.entities.getEntitiesOfTypeWithinAAAB(entityType, queryBox, out, predicate);
	}

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

	//TODO: Vary icky light population code! REMOVE IT!
	private void tryDoFirstLight() {
		BlockPos pos = this.getCoords().getMinBlockPos();
		final int radius = 17;
		if(!world.isAreaLoaded(pos.add(-radius, -radius, -radius), pos.add(15+radius, 15+radius, 15+radius))) {
			return;
		}
		//client cubes (setClientCube) are always fully generated, isInitialLightingDone is never false
		((ICubicWorldServer)this.world).getFirstLightProcessor().diffuseSkylight(this);
		this.isInitialLightingDone = true;
	}

	//=================================
	//=========Other methods===========
	//=================================

	public boolean isEmpty() {
		return storage == null || this.storage.isEmpty();
	}

	public long getAddress() {
		return AddressTools.getAddress(this.coords.getCubeX(), this.coords.getCubeY(), this.coords.getCubeZ());
	}

	public BlockPos localAddressToBlockPos(int localAddress) {
		int x = Coords.localToBlock(this.coords.getCubeX(), AddressTools.getLocalX(localAddress));
		int y = Coords.localToBlock(this.coords.getCubeY(), AddressTools.getLocalY(localAddress));
		int z = Coords.localToBlock(this.coords.getCubeZ(), AddressTools.getLocalZ(localAddress));
		return new BlockPos(x, y, z);
	}

	public ICubicWorld getWorld() {
		return this.world;
	}

	public Column getColumn() {
		return this.column;
	}

	public int getX() {
		return this.coords.getCubeX();
	}

	public int getY() {
		return this.coords.getCubeY();
	}

	public int getZ() {
		return this.coords.getCubeZ();
	}

	public CubeCoords getCoords() {
		return this.coords;
	}

	public boolean containsBlockPos(BlockPos blockPos) {
		return this.coords.getCubeX() == Coords.blockToCube(blockPos.getX())
				&& this.coords.getCubeY() == Coords.blockToCube(blockPos.getY())
				&& this.coords.getCubeZ() == Coords.blockToCube(blockPos.getZ());
	}

	public ExtendedBlockStorage getStorage() {
		return this.storage;
	}

	public ExtendedBlockStorage setStorage(ExtendedBlockStorage ebs) {
		return this.storage = ebs;
	}

	private void newStorage() {
		storage = new ExtendedBlockStorage(Coords.cubeToMinBlock(getY()), !world.getProvider().getHasNoSky());
	}

	public Map<BlockPos, TileEntity> getTileEntityMap() {
		return this.tileEntityMap;
	}

	public EntityContainer getEntityContainer() {
		return this.entities;
	}

	public void onLoad() {
		// tell the world about tile entities
		this.world.addTileEntities(this.tileEntityMap.values());
		this.world.loadEntities(this.entities.getEntities());
		this.isCubeLoaded = true;
	}

	public void onUnload() {
		//first mark as unloaded so that entity list and tile entity map isn't modified while iterating
		//and it also preserves all entities/time entities so they can be saved
		this.isCubeLoaded = false;

		// tell the world to forget about entities
		this.world.unloadEntities(this.entities.getEntities());

		// tell the world to forget about tile entities
		for (TileEntity blockEntity : this.tileEntityMap.values()) {
			this.world.removeTileEntity(blockEntity.getPos());
		}
	}

	public boolean needsSaving() {
		return this.entities.needsSaving(true, this.world.getTotalWorldTime(), this.isModified);
	}

	public void markSaved() {
		this.entities.markSaved(this.world.getTotalWorldTime());
		this.isModified = false;
	}

	public void markForRenderUpdate() {
		this.world.markBlockRangeForRenderUpdate(
				Coords.cubeToMinBlock(this.coords.getCubeX()), Coords.cubeToMinBlock(this.coords.getCubeY()), Coords.cubeToMinBlock(this.coords.getCubeZ()),
				Coords.cubeToMaxBlock(this.coords.getCubeX()), Coords.cubeToMaxBlock(this.coords.getCubeY()), Coords.cubeToMaxBlock(this.coords.getCubeZ())
		);
	}

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

	public void setClientCube() {
		this.isPopulated = true;
		this.isFullyPopulated = true;
		this.isInitialLightingDone = true;
	}

	public void setPopulated(boolean populated) {
		this.isPopulated = populated;
		this.isModified = true;
	}

	public void setFullyPopulated(boolean populated) {
		this.isFullyPopulated = populated;
		this.isModified = true;
	}

	/**
	 * @return weather or not the populator has been run for this Cube or not
	 */
	public boolean isPopulated() {
		return isPopulated;
	}

	/**
	 * @return weather this Cube is fully populated or not...
	 *         aka when even Cubes whos population effects this Cube are populated!
	 */
	public boolean isFullyPopulated() {
		return this.isFullyPopulated;
	}

	public void setInitialLightingDone(boolean initialLightingDone) {
		this.isInitialLightingDone = initialLightingDone;
		this.isModified = true;
	}

	public boolean isInitialLightingDone() {
		return isInitialLightingDone;
	}

	/**
	 * Lights up all blocks that will definitely be lit. It's faster than using world.checkLightFor on everything.
	 */
	public void initSkyLight() {
		((ICubicWorldServer)this.getWorld()).getFirstLightProcessor().initializeSkylight(this);
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

			minY = MathHelper.clamp_int(minY, 0, 15);
			maxY = MathHelper.clamp_int(maxY, 0, 15);

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
