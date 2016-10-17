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
import cubicchunks.lighting.LightingManager;
import cubicchunks.util.Coords;
import cubicchunks.util.MathUtil;
import cubicchunks.world.ClientOpacityIndex;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.IOpacityIndex;
import cubicchunks.world.OpacityIndex;
import cubicchunks.world.cube.Cube;
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

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Column extends Chunk {

	private CubeMap cubeMap;
	private IOpacityIndex opacityIndex;

	private ICubeCache provider;
	private ICubicWorld world;

	public Column(ICubeCache provider, ICubicWorld world, int x, int z) {
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
	 * Returns Y position of the block directly above the top non-transparent block,
	 * or null is there are no non-transparent blocks
	 */
	@Override
	public int getHeight(BlockPos pos) {
		return this.getHeightValue(
				Coords.blockToLocal(pos.getX()),
				Coords.blockToLocal(pos.getZ()));
	}

	/**
	 * Returns Y position of the block directly above the top non-transparent block,
	 * or null is there are no non-transparent blocks
	 */
	@Override
	public int getHeightValue(int localX, int localZ) {
		// NOTE: the "height value" here is the height of the transparent block
		// on top of the highest non-transparent block
		return opacityIndex.getTopBlockY(localX, localZ) + 1;
	}

	@Override
	@Deprecated //TODO: stop this method form being used by vanilla (any algorithms in vanilla that use it are be broken any way)
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
		// return this.getWorld().provider.getAverageGroundLevel();

		int blockY = Coords.NO_HEIGHT;
		for (int localX = 0; localX < Coords.CUBE_SIZE; localX++) {
			for (int localZ = 0; localZ < Coords.CUBE_SIZE; localZ++) {
				int y = this.opacityIndex.getTopBlockY(localX, localZ);
				if (y > blockY) {
					blockY = y;
				}
			}
		}
		return Coords.cubeToMinBlock(Coords.blockToCube(blockY)); // return the lowest block in the Cube (kinda weird I know)
	}

	@Override
	@Deprecated // Vanila can safely use this for block ticking, but just try to avoid it!
	public ExtendedBlockStorage[] getBlockStorageArray() {
		return cubeMap.getStorageArrays();
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

	//forward to cube
	@Override
	public IBlockState getBlockState(BlockPos pos) {
		return this.getCube(pos).getBlockState(pos);
	}

	//forward to cube
	@Override
	public IBlockState getBlockState(final int blockX, final int blockY, final int blockZ) {
		return this.getCube(Coords.blockToLocal(blockY)).getBlockState(blockX, blockY, blockZ);
	}

	@Override
	public IBlockState setBlockState(BlockPos pos, @Nonnull IBlockState newstate) {
		Cube cube = getCube(Coords.blockToCube(pos.getY()));
		IBlockState oldstate = cube.getBlockState(pos);

		// get the old opacity for use when updating the heightmap
		int oldOpacity = oldstate.getLightOpacity(this.getWorld(), pos);

		oldstate = cube.setBlockStateDirect(pos, newstate); // forward to cube
		if(oldstate == null) {
			return oldstate;
		}

		this.doOnBlockSetLightUpdates(pos, newstate, oldOpacity);

		return oldstate;
	}

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
		return getCube(pos).getLightFor(type, pos);
	}

	//forward to cube
	@Override
	public void setLightFor(EnumSkyBlock type, BlockPos pos, int value) {
		getCube(pos).setLightFor(type, pos, value);
	}

	//forward to cube
	@Override
	public int getLightSubtracted(BlockPos pos, int amount) {
		return getCube(pos).getLightSubtracted(pos, amount);
	}

	//forward to cube
	@Override
	public void addEntity(Entity entity) {
		getCube(Coords.getCubeYForEntity(entity)).addEntity(entity);
	}

	@Override
	public void removeEntity(Entity entityIn) {
		this.removeEntityAtIndex(entityIn, entityIn.chunkCoordY);
	}

	//forward to cube
	@Override
	public void removeEntityAtIndex(@Nonnull Entity entity, int cubeY) {
		getCube(cubeY).removeEntity(entity);
	}

	@Override
	public boolean canSeeSky(BlockPos pos) {
		int height = this.getHeightValue(
				Coords.blockToLocal(pos.getX()),
				Coords.blockToLocal(pos.getZ()));

		return pos.getY() >= height;
	}

	//forward to cube
	@Override
	public TileEntity getTileEntity(@Nonnull BlockPos pos, Chunk.EnumCreateEntityType createType) {
		return getCube(pos).getTileEntity(pos, createType);
	}

	@Override
	public void addTileEntity(TileEntity tileEntity) {
		// pass off to the cube
		getCube(tileEntity.getPos()).addTileEntity(tileEntity);
	}

	//forward to cube
	@Override
	public void addTileEntity(@Nonnull BlockPos pos, TileEntity blockEntity) {
		// pass off to the cube
		getCube(pos).addTileEntity(pos, blockEntity);
	}

	//forward to cube
	@Override
	public void removeTileEntity(@Nonnull BlockPos pos) {
		this.getCube(pos).removeTileEntity(pos);
	}

	@Override
	public void onChunkLoad() {
		this.isChunkLoaded = true;
		MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(this));
	}

	@Override
	public void onChunkUnload() {
		this.isChunkLoaded = false;
		MinecraftForge.EVENT_BUS.post(new ChunkEvent.Unload(this));
	}

	//setChunkModified() goes here, it's unchanged

	//forward to cube, then to EntityContainer
	@Override
	public void getEntitiesWithinAABBForEntity(Entity exclude, AxisAlignedBB queryBox, @Nonnull List<Entity> out, Predicate<? super Entity> predicate) {

		// get a y-range that 2 blocks wider than the box for safety
		int minCubeY = Coords.blockToCube(MathHelper.floor_double(queryBox.minY - World.MAX_ENTITY_RADIUS));
		int maxCubeY = Coords.blockToCube(MathHelper.floor_double(queryBox.maxY + World.MAX_ENTITY_RADIUS));

		for (int cubeY = minCubeY;cubeY <= maxCubeY;cubeY++) {
			Cube cube = getCube(cubeY);
			cube.getEntitiesWithinAABBForEntity(exclude, queryBox, out, predicate);
		}
	}

	//forward to cube, then to EntityContainer
	@Override
	public <T extends Entity> void getEntitiesOfTypeWithinAAAB(@Nonnull Class<? extends T> entityType, AxisAlignedBB queryBox, @Nonnull List<T> out, Predicate<? super T> predicate) {

		// get a y-range that 2 blocks wider than the box for safety
		int minCubeY = Coords.blockToCube(MathHelper.floor_double(queryBox.minY - World.MAX_ENTITY_RADIUS));
		int maxCubeY = Coords.blockToCube(MathHelper.floor_double(queryBox.maxY + World.MAX_ENTITY_RADIUS));

		for (int cubeY = minCubeY;cubeY < maxCubeY + 1;cubeY++) {
			Cube cube = getCube(cubeY);
			cube.getEntitiesOfTypeWithinAAAB(entityType, queryBox, out, predicate);
		}
	}

	@Override
	public boolean needsSaving(boolean flag) {
		return this.isModified;
	}

	//getRandomWithSeed(seed) doesn't need changes

	//isEmpty() doesn't need changes

	@Override
	public void populateChunk(IChunkProvider chunkProvider, @Nonnull IChunkGenerator chunkGenerator) {
		throw new UnsupportedOperationException("This method is incompatible with CubicChunks");
	}

	@Override
	//TODO: Actual precipitation heightmap, currently skylight heightmap is used which triggers an old MC alpha bug
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
		return true; //stub... its broken and only used in World.markAndNotifyBlock()
	}

	//isCHunkTicked() doesn't need changes

	//getChunkCoordIntPair doesn't need changes

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

	public int getX() {
		return this.xPosition;
	}

	public int getZ() {
		return this.zPosition;
	}

	public IOpacityIndex getOpacityIndex() {
		return this.opacityIndex;
	}

	public Collection<Cube> getLoadedCubes() {
		return Collections.unmodifiableCollection(this.cubeMap.all());
	}

	/**
	 * Returns ordered Iterable of cubes. If startY < endY - order is bottom to top.
	 * If startY > endY - order is top to bottom.
	 */
	public Iterable<Cube> getLoadedCubes(int startY, int endY) {
		return this.cubeMap.cubes(startY, endY);
	}

	public Cube getCube(int cubeY) {
		return provider.getCube(getX(), cubeY, getZ());
	}

	private Cube getCube(BlockPos pos) {
		return getCube(Coords.blockToCube(pos.getY()));
	}

	public void addCube(Cube cube) {
		this.cubeMap.put(cube.getY(), cube);
	}

	public Cube removeCube(int cubeY) {
		return this.cubeMap.remove(cubeY);
	}

	public boolean hasLoadedCubes() {
		return !this.cubeMap.isEmpty();
	}

	public void markSaved() {
		this.setModified(false);
	}

	@Override
	public int getLowestHeight() {
		return opacityIndex.getLowestTopBlockY();
	}

	public ICubicWorld getCubicWorld() {
		return world;
	}
}
