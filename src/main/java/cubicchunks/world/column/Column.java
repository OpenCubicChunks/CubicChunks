/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 Tall Worlds
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

import static cubicchunks.util.Coords.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.chunk.Chunk;
import com.google.common.base.Predicate;
import cubicchunks.TallWorldsMod;

import cubicchunks.util.AddressTools;
import cubicchunks.util.Bits;
import cubicchunks.util.Coords;
import cubicchunks.util.MutableBlockPos;
import cubicchunks.util.RangeInt;
import cubicchunks.world.EntityContainer;
import cubicchunks.world.OpacityIndex;
import cubicchunks.world.WorldContext;
import cubicchunks.world.cube.Cube;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3i;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class Column extends Chunk {

	private TreeMap<Integer, Cube> cubes;
	private OpacityIndex opacityIndex;
	private int roundRobinLightUpdatePointer;
	private List<Cube> roundRobinCubes;
	private EntityContainer entities;

	public Column(World world, int x, int z) {

		// NOTE: this constructor is called by the chunk loader
		super(world, x, z);

		init();
	}

	public Column(World world, int cubeX, int cubeZ, BiomeGenBase[] biomes) {

		// NOTE: this constructor is called by the column generator
		this(world, cubeX, cubeZ);

		init();

		byte[] biomeArray = super.getBiomeArray();
		// save the biome data
		for (int i = 0; i < biomes.length; i++) {
			biomeArray[i] = (byte) biomes[i].biomeID;
		}

		super.setModified(true);
	}

	private void init() {

		this.cubes = new TreeMap<Integer, Cube>();
		this.opacityIndex = new OpacityIndex();
		this.roundRobinLightUpdatePointer = 0;
		this.roundRobinCubes = new ArrayList<Cube>();
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

	public OpacityIndex getOpacityIndex() {
		return this.opacityIndex;
	}

	@Override
	@SideOnly(Side.CLIENT)
	protected void generateHeightMap() {
		// override this so no height map is generated
	}

	public Collection<Cube> getCubes() {
		return Collections.unmodifiableCollection(this.cubes.values());
	}
	
	public Iterable<Cube> getCubes(int minY, int maxY) {
		return this.cubes.subMap(minY, true, maxY, true).values();
	}

	public boolean hasCubes() {
		return !this.cubes.isEmpty();
	}

	public Cube getCube(int y) {
		return this.cubes.get(y);
	}

	private Cube getCube(BlockPos pos) {
		return getCube(Coords.blockToCube(pos.getY()));
	}

	public Cube getOrCreateCube(int cubeY, boolean isModified) {
		Cube cube = getCube(cubeY);
		
		if (cube == null) {
			cube = new Cube(this.getWorld(), this, this.xPosition, cubeY, this.zPosition, isModified);
			this.cubes.put(cubeY, cube);
		}
		return cube;
	}

	public Cube removeCube(int cubeY) {
		return this.cubes.remove(cubeY);
	}

	public List<RangeInt> getCubeYRanges() {
		return getRanges(this.cubes.keySet());
	}

	@Override
	public boolean needsSaving(boolean alwaysTrue) {
		return this.entities.needsSaving(this.getWorld().getTotalWorldTime()) || this.isModified;
	}

	public void markSaved() {
		this.entities.markSaved(this.getWorld().getTotalWorldTime());
		this.setModified(false);
	}

	@Override
	public Block getBlock(final int x, final int y, final int z) {
		return getBlock(new BlockPos(x, y, z));
	}

	@Override
	public Block getBlock(final BlockPos pos) {
		// pass to cube
		Cube cube = getCube(pos);

		if (cube != null) {
			return cube.getBlockAt(pos);
		}

		return Blocks.air;
	}

	@Override
	public IBlockState getBlockState(BlockPos pos) {
		// pass off to the cube
		Cube cube = getCube(pos);

		if (cube != null) {
			return cube.getBlockState(pos);
		}

		return Blocks.air.getDefaultState();
	}

	@Override
	public IBlockState setBlockState(BlockPos pos, IBlockState newBlockState) {

		// is there a chunk for this block?
		int cubeY = Coords.blockToCube(pos.getY());
		Cube cube = this.cubes.get(cubeY);
		if (cube == null) {
			return null;
		}

		// did anything change?
		IBlockState oldBlockState = cube.setBlockState(pos, newBlockState);
		if (oldBlockState == null) {
			// nothing changed
			return null;
		}

		Block oldBlock = oldBlockState.getBlock();
		Block newBlock = newBlockState.getBlock();

		// update rain map
		// NOTE: rainfallMap[xzCoord] is he lowest block that will contain rain
		// so rainfallMap[xzCoord] - 1 is the block that is being rained on
		int x = Coords.blockToLocal(pos.getX());
		int z = Coords.blockToLocal(pos.getZ());
		int xzCoord = z << 4 | x;
		if (pos.getY() >= this.precipitationHeightMap[xzCoord] - 1) {
			// invalidate the rain height map value
			this.precipitationHeightMap[xzCoord] = -999;
		}

		int newOpacity = newBlock.getLightOpacity();
		int oldOpacity = oldBlock.getLightOpacity();

		// did the top non-transparent block change?
		Integer oldSkylightY = getSkylightBlockY(x, z);
		this.opacityIndex.setOpacity(x, pos.getY(), z, newOpacity);
		Integer newSkylightY = getSkylightBlockY(x, z);

		if (oldSkylightY != null && newSkylightY != null && !oldSkylightY.equals(newSkylightY)) {

			// sort the y-values
			int minBlockY = oldSkylightY;
			int maxBlockY = newSkylightY;
			if (minBlockY > maxBlockY) {
				minBlockY = newSkylightY;
				maxBlockY = oldSkylightY;
			}
			assert (minBlockY < maxBlockY) : "Values not sorted! " + minBlockY + ", " + maxBlockY;

			// update light and signal render update
			WorldContext.get(this.getWorld()).getLightingManager().computeSkyLightUpdate(this, x, z, minBlockY, maxBlockY);
			this.getWorld().markBlockRangeForRenderUpdate(pos.getX(), minBlockY, pos.getZ(), pos.getX(), maxBlockY,
					pos.getZ());
		}

		// if opacity changed and ( opacity decreased or block now has any light )
		int skyLight = getLightFor(EnumSkyBlock.SKY, pos);
		int blockLight = getLightFor(EnumSkyBlock.BLOCK, pos);
		if (newOpacity != oldOpacity && (newOpacity < oldOpacity || skyLight > 0 || blockLight > 0)) {
			WorldContext.get(this.getWorld()).getLightingManager().queueSkyLightOcclusionCalculation(pos.getX(), pos.getZ());
		}

		// update opacity index
		this.opacityIndex.setOpacity(x, pos.getY(), z, newBlock.getLightOpacity());

		this.setModified(true);

		// NOTE: after this method, the World calls updateLights on the source block which changes light values again

		return oldBlockState;
	}

	@Override
	public int getBlockMetadata(BlockPos pos) {
		// pos has LocalX, worldY, LocalZ if called from Chunk.getBlockMetadata(blockPos)
		// otherwise it's worldX, worldY, worldZ

		// pass off to the cube
		Cube cube = getCube(pos);

		if (cube != null) {
			IBlockState blockState = cube.getBlockState(Coords.blockToLocal(pos.getX()),
					Coords.blockToLocal(pos.getY()), Coords.blockToLocal(pos.getZ()));
			if (blockState != null) {
				return blockState.getBlock().getMetaFromState(blockState);
			}
		}
		return 0;
	}

	//this is private
	/*@Deprecated
	@Override
	public int getBlockMetadata(int localX, int blockY, int localZ) {
		return getBlockMetadata(new BlockPos(localX, blockY, localZ));
	}*/

	public int getTopCubeY() {
		return this.cubes.lastKey();
	}

	public int getBottomCubeY() {
		return this.cubes.firstKey();
	}

	public Integer getTopFilledCubeY() {
		Integer blockY = null;
		for (int localX=0; localX<Coords.CUBE_SIZE; localX++) {
			for (int localZ=0; localZ<Coords.CUBE_SIZE; localZ++) {
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

	@Override
	@Deprecated
	// don't use this! It's only here because vanilla needs it, but we need to be hacky about it
	public int getTopFilledSegment() {
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
	public boolean canSeeSky(BlockPos pos) {
		int x = Coords.blockToLocal(pos.getX());
		int z = Coords.blockToLocal(pos.getZ());
		Integer skylightBlockY = getSkylightBlockY(x, z);
		if (skylightBlockY == null) {
			return true;
		}
		return pos.getY() >= skylightBlockY;
	}

	public Integer getSkylightBlockY(int localX, int localZ) {
		// NOTE: a "skylight" block is the transparent block that is directly one block above the top non-transparent block
		Integer topBlockY = this.opacityIndex.getTopBlockY(localX, localZ);
		if (topBlockY != null) {
			return topBlockY + 1;
		}
		return null;
	}

	@Override
	@Deprecated
	// don't use this! It's only here because vanilla needs it, but we need to be hacky about it
	public int getHeight(int localX, int localZ) {
		// NOTE: the "height value" here is the height of the transparent block on top of the highest non-transparent
		// block

		Integer skylightBlockY = getSkylightBlockY(localX, localZ);
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
	public int getBlockLightOpacity(BlockPos pos) {
		int x = Coords.blockToLocal(pos.getX());
		int z = Coords.blockToLocal(pos.getZ());
		return this.opacityIndex.getOpacity(x, pos.getY(), z);
	}
	
	//this calls getBlockLightOpacity(BlockPos)
	//private int getBlockLightOpacity(int x, int y, int z)

	public Iterable<Entity> entities() {
		return this.entities.getEntities();
	}

	@Override
	public void addEntity(Entity entity) {
		int cubeY = Coords.getCubeYForEntity(entity);

		// pass off to the cube
		Cube cube = getCube(cubeY);
		if (cube != null) {
			cube.addEntity(entity);
		} else {
			// entities don't have to be in cubes, just add it directly to the column
			entity.addedToChunk = true;
			entity.chunkCoordX = this.xPosition;
			entity.chunkCoordY = cubeY;
			entity.chunkCoordZ = this.zPosition;

			this.entities.add(entity);
			this.setModified(true);
		}
	}

	@Override
	public void removeEntity(Entity entity) {
		removeEntityAtIndex(entity, entity.chunkCoordY);
	}

	@Override
	public void removeEntityAtIndex(Entity entity, int cubeY) {

		if (!entity.addedToChunk) {
			return;
		}

		// pass off to the cube
		Cube cube = getCube(cubeY);

		if (cube != null) {
			cube.removeEntity(entity);
		} else if (this.entities.remove(entity)) {
			entity.addedToChunk = false;
			this.setModified(true);
		} else {
			TallWorldsMod.LOGGER.warn(
					"{} Tried to remove entity {} from column ({},{}), but it was not there. Entity thinks it's in cube ({},{},{})",
					this.getWorld().isRemote ? "CLIENT" : "SERVER", entity.getClass().getName(), this.xPosition, this.zPosition,
					entity.chunkCoordX, entity.chunkCoordY, entity.chunkCoordZ);
		}
	}

	//findEntitiesExcept
	@Override
	public void getEntitiesWithinAABBForEntity(Entity excludedEntity, AxisAlignedBB queryBox, List out, Predicate predicate) {

		// get a y-range that 2 blocks wider than the box for safety
		int minCubeY = Coords.blockToCube(MathHelper.floor_double(queryBox.minY - 2));
		int maxCubeY = Coords.blockToCube(MathHelper.floor_double(queryBox.maxY + 2));

		for (Cube cube : getCubes(minCubeY, maxCubeY)) {
			cube.findEntitiesExcept(excludedEntity, queryBox, out, predicate);
		}

		// check the column too
		this.entities.findEntitiesExcept(excludedEntity, queryBox, out, predicate);
	}

	@Override
	public void getEntitiesOfTypeWithinAAAB(Class entityType, AxisAlignedBB queryBox, List out,
			Predicate predicate) {

		// get a y-range that 2 blocks wider than the box for safety
		int minCubeY = Coords.blockToCube(MathHelper.floor_double(queryBox.minY - 2));
		int maxCubeY = Coords.blockToCube(MathHelper.floor_double(queryBox.maxY + 2));

		for (Cube cube : getCubes(minCubeY, maxCubeY)) {
			cube.findEntities(entityType, queryBox, out, predicate);
		}

		// check the column too
		this.entities.findEntities(entityType, queryBox, out, predicate);
	}

	@Override
	public TileEntity getTileEntity(BlockPos pos, Chunk.EnumCreateEntityType creationType) {
		Cube cube = getCube(pos);

		if (cube != null) {
			return cube.getBlockEntity(pos, creationType);
		}
		return null;
	}

	@Override
	public void addTileEntity(BlockPos pos, TileEntity blockEntity) {

		// pass off to the cube
		int cubeY = Coords.blockToCube(pos.getY());
		Cube cube = getCube(cubeY);

		if (cube != null) {
			cube.addBlockEntity(pos, blockEntity);
		} else {
			TallWorldsMod.LOGGER.warn("No cube at ({},{},{}) to add tile entity (block {},{},{})!", this.xPosition, cubeY, this.zPosition,
					pos.getX(), pos.getY(), pos.getZ());
		}
	}

	@Override
	public void removeTileEntity(BlockPos pos) {
		// pass off to the cube
		Cube cube = getCube(pos);

		if (cube != null) {
			cube.removeBlockEntity(pos);
		}
	}

	@Override
	public void onChunkLoad() {
		this.isChunkLoaded = true;
	}

	@Override
	public void onChunkUnload() {
		this.isChunkLoaded = false;
	}

	/**
	 * This method retrieves the biome at a set of coordinates
	 */
	@Override
	public BiomeGenBase getBiome(BlockPos pos, WorldChunkManager biomeManager) {

		int biomeID = this.getBiomeArray()[Coords.blockToLocal(pos.getZ()) << 4 | Coords.blockToLocal(pos.getX())] & 255;

		if (biomeID == 255) {
			BiomeGenBase biome = biomeManager.getBiomeGenerator(pos);
			biomeID = biome.biomeID;
			this.getBiomeArray()[Coords.blockToLocal(pos.getZ()) << 4 | Coords.blockToLocal(pos.getX())] = (byte) (biomeID & 255);
		}

		return BiomeGenBase.getBiome(biomeID) == null ? BiomeGenBase.plains : BiomeGenBase.getBiome(biomeID);
	}

	@Override
	public boolean isPopulated() {
		boolean isAnyCubeLive = false;
		for (Cube cube : this.cubes.values()) {
			isAnyCubeLive |= cube.getGeneratorStage().isLastStage();
		}
		return this.field_150815_m && this.isTerrainPopulated && isAnyCubeLive;
	}

	/**
	 * TickChunk
	 */
	@Override
	public void func_150804_b(boolean tryToTickFaster) {
		this.field_150815_m = true;//ticked=true;

		// don't need to do anything else here
		// lighting is handled elsewhere now
	}

	@Override
	@Deprecated
	public void generateSkylightMap() {
		// don't call this, use the lighting manager
		throw new UnsupportedOperationException();
	}

	public void resetPrecipitationHeight() {
		// init the rain map to -999, which is a kind of null value
		// this array is actually a cache
		// values will be calculated by the getter
		for (int localX = 0; localX < CUBE_SIZE; localX++) {
			for (int localZ = 0; localZ < CUBE_SIZE; localZ++) {
				int xzCoord = localX | localZ << 4;
				this.precipitationHeightMap[xzCoord] = -999;
			}
		}
	}

	@Override
	public BlockPos getPrecipitationHeight(BlockPos pos) {

		// TODO: update this calculation to use better data structures

		int x = Coords.blockToLocal(pos.getX());
		int z = Coords.blockToLocal(pos.getZ());

		int xzCoord = x | z << 4;
		int height = this.precipitationHeightMap[xzCoord];
		if (height == -999) {

			/*
			 * TODO: compute a new rain height look over the blocks in the top filled cube (if one exists) and do
			 * something like this: int maxBlockY = getTopFilledSegment() + 15; int minBlockY = Coords.cubeToMinBlock(
			 * getBottomCubeY() ); Block block = cube.getBlock( ... ); Material material = block.getMaterial(); if(
			 * material.blocksMovement() || material.isLiquid() ) { height = maxBlockY + 1; }
			 */

			// TEMP: just rain down to the sea
			this.precipitationHeightMap[xzCoord] = (int) this.getWorld().provider.getAverageGroundLevel();
		}

		return new BlockPos(pos.getX(), height, pos.getZ());
	}

	@Override
	//getBroghtestLight. It actually returns light value... 
	public int setLight(BlockPos pos, int skyLightDampeningTerm) {
		// NOTE: this is called by WorldRenderers

		// pass off to cube
		Cube cube = getCube(pos);
		if (cube != null) {
			return cube.getBrightestLight(pos, skyLightDampeningTerm);
		}

		// defaults
		if (!this.getWorld().provider.getHasNoSky() && skyLightDampeningTerm < EnumSkyBlock.SKY.defaultLightValue) {
			return EnumSkyBlock.SKY.defaultLightValue - skyLightDampeningTerm;
		}
		return 0;
	}

	@Override
	public int getLightFor(EnumSkyBlock lightType, BlockPos pos) {
		// NOTE: this is the light function that is called by the rendering code on client

		// pass off to cube
		Cube cube = getCube(pos);
		if (cube != null) {
			return cube.getLightValue(lightType, pos);
		}

		// there's no cube, rely on defaults
		if (lightType == EnumSkyBlock.SKY) {
			if (canSeeSky(pos)) {
				return lightType.defaultLightValue;
			} else {
				return 0;
			}
		}
		return 0;
	}

	@Override
	public void setLightFor(EnumSkyBlock lightType, BlockPos pos, int light) {

		// pass off to cube
		Cube cube = getCube(pos);
		if (cube != null) {
			cube.setLightValue(lightType, pos, light);
			this.setModified(true);
		}
	}

	protected List<RangeInt> getRanges(Iterable<Integer> yValues) {

		// compute a kind of run-length encoding on the cube y-values
		List<RangeInt> ranges = new ArrayList<>();
		Integer start = null;
		Integer stop = null;
		for (int cubeY : yValues) {
			if (start == null || stop == null) {
				// start a new range
				start = cubeY;
				stop = cubeY;
			} else if (cubeY == stop + 1) {
				// extend the range
				stop = cubeY;
			} else {
				// end the range
				ranges.add(new RangeInt(start, stop));

				// start a new range
				start = cubeY;
				stop = cubeY;
			}
		}

		if (start != null && stop != null) {
			// finish the last range
			ranges.add(new RangeInt(start, stop));
		}

		return ranges;
	}

	@Override
	public void resetRelightChecks() {
		this.roundRobinLightUpdatePointer = 0;
		this.roundRobinCubes.clear();
		this.roundRobinCubes.addAll(this.cubes.values());
	}

	@Override
	public void enqueueRelightChecks() {

		if (this.roundRobinCubes.isEmpty()) {
			resetRelightChecks();
		}

		// we get just a few updates this time
		MutableBlockPos pos = new MutableBlockPos();
		MutableBlockPos neighborPos = new MutableBlockPos();
		for (int i = 0; i < 2; i++) {

			// once we've checked all the blocks, stop checking
			int maxPointer = 16 * 16 * this.roundRobinCubes.size();
			if (this.roundRobinLightUpdatePointer >= maxPointer) {
				break;
			}

			// get this update's arguments
			int cubeIndex = Bits.unpackUnsigned(this.roundRobinLightUpdatePointer, 4, 8);
			int localX = Bits.unpackUnsigned(this.roundRobinLightUpdatePointer, 4, 4);
			int localZ = Bits.unpackUnsigned(this.roundRobinLightUpdatePointer, 4, 0);

			// advance to the next block
			// this pointer advances over segment block columns
			// starting from the block columns in the bottom segment and moving upwards
			this.roundRobinLightUpdatePointer++;

			// get the cube that was pointed to
			Cube cube = this.roundRobinCubes.get(cubeIndex);

			int blockX = Coords.localToBlock(this.xPosition, localX);
			int blockZ = Coords.localToBlock(this.zPosition, localZ);

			// for each block in this segment block column...
			for (int localY = 0; localY < 16; ++localY) {

				int blockY = Coords.localToBlock(cube.getY(), localY);
				pos.setBlockPos(blockX, blockY, blockZ);

				if (cube.getBlockState(pos).getBlock().getMaterial() == Material.air) {

					// if there's a light source next to this block, update the light source
					for (EnumFacing facing : EnumFacing.values()) {
						Vec3i facingDir = facing.getDirectionVec();
						neighborPos.setBlockPos(pos.getX() + facingDir.getX(), pos.getY() + facingDir.getY(),
								pos.getZ() + facingDir.getZ());
						if (this.getWorld().getBlockState(neighborPos).getBlock().getLightValue()> 0) {
							this.getWorld().checkLight(neighborPos);
						}
					}

					// then update this block
					this.getWorld().checkLight(pos);
				}
			}
		}
	}

	public void doRandomTicks() {
		if (isEmpty()) {
			return;
		}

		for (Cube cube : this.cubes.values()) {
			cube.doRandomTicks();
		}
	}
}
