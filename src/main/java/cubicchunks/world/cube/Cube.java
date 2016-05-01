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
import cubicchunks.worldgen.GeneratorStage;
import cubicchunks.util.AddressTools;
import cubicchunks.util.Coords;
import cubicchunks.util.CubeBlockMap;
import cubicchunks.world.EntityContainer;
import cubicchunks.world.IOpacityIndex;
import cubicchunks.world.column.Column;
import net.minecraft.block.Block;
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
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

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
	private CubeBlockMap<TileEntity> tileEntityMap;
	private GeneratorStage generatorStage;
	private boolean needsRelightAfterLoad;
	/**
	 * "queue containing the BlockPos of tile entities queued for creation"
	 */
	private ConcurrentLinkedQueue<BlockPos> tileEntityPosQueue;

	private final LightUpdateData lightUpdateData = new LightUpdateData(this);

	private boolean isCubeLoaded;

	public Cube(World world, Column column, int x, int y, int z, boolean isModified) {
		this.world = world;
		this.column = column;
		this.cubeX = x;
		this.cubeY = y;
		this.cubeZ = z;
		this.isModified = isModified;

		this.storage = null;
		this.entities = new EntityContainer();
		this.tileEntityMap = new CubeBlockMap<>();
		this.generatorStage = null;
		this.needsRelightAfterLoad = false;
		this.tileEntityPosQueue = new ConcurrentLinkedQueue<>();
	}

	//======================================
	//========Chunk vanilla methods=========
	//======================================

	public IBlockState getBlockState(BlockPos pos) {
		return this.getBlockState(pos.getX(), pos.getY(), pos.getZ());
	}

	public IBlockState getBlockState(int blockX, int blockY, int blockZ) {
		try {
			if(this.isEmpty()) {
				return Blocks.AIR.getDefaultState();
			}
			int localX = Coords.blockToLocal(blockX);
			int localY = Coords.blockToLocal(blockY);
			int localZ = Coords.blockToLocal(blockZ);
			return this.getStorage().get(localX, localY, localZ);
		} catch (Throwable t) {
			CrashReport report = CrashReport.makeCrashReport(t, "Getting block state");
			CrashReportCategory category = report.makeCategory("Block being got");
			category.addCrashSectionCallable("Location", () -> CrashReportCategory.getCoordinateInfo(blockX, blockY, blockZ));
			throw new ReportedException(report);
		}
	}

	public void setBlockStateDirect(BlockPos pos, IBlockState newBlockState) {
		if (this.isEmpty()) {
			if (newBlockState.getBlock() == Blocks.AIR) {
				return;
			}
			this.setEmpty(false);
		}
		this.isModified = true;

		int localX = Coords.blockToLocal(pos.getX());
		int localY = Coords.blockToLocal(pos.getY());
		int localZ = Coords.blockToLocal(pos.getZ());
		this.getStorage().set(localX, localY, localZ, newBlockState);
	}

	public int getLightFor(EnumSkyBlock lightType, BlockPos pos) {
		//it may not look like this but it's actually the same logic as in vanilla
		if(this.isEmpty()) {
			if(this.column.canSeeSky(pos)) {
				return lightType.defaultLightValue;
			}
			return 0;
		}

		int localX = Coords.blockToLocal(pos.getX());
		int localY = Coords.blockToLocal(pos.getY());
		int localZ = Coords.blockToLocal(pos.getZ());

		switch (lightType) {
			case SKY:
				if(this.world.provider.getHasNoSky()) {
					return 0;
				}
				return this.storage.getExtSkylightValue(localX, localY, localZ);
			case BLOCK:
				return this.storage.getExtBlocklightValue(localX, localY, localZ);
			default:
				return lightType.defaultLightValue;
		}
	}

	public void setLightFor(EnumSkyBlock lightType, BlockPos pos, int light) {
		// make sure we're not empty
		if (isEmpty()) {
			setEmpty(false);
		}

		this.isModified = true;

		int x = Coords.blockToLocal(pos.getX());
		int y = Coords.blockToLocal(pos.getY());
		int z = Coords.blockToLocal(pos.getZ());

		switch (lightType) {
			case SKY:
				if (!this.world.provider.getHasNoSky()) {
					this.storage.setExtSkylightValue(x, y, z, light);
				}
				break;

			case BLOCK:
				this.storage.setExtBlocklightValue(x, y, z, light);
				break;
		}
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
			return block.createTileEntity(this.world, blockState);
		}
		return null;
	}

	public void addEntity(Entity entity) {
		// make sure the entity is in this cube
		int cubeX = Coords.getCubeXForEntity(entity);
		int cubeY = Coords.getCubeYForEntity(entity);
		int cubeZ = Coords.getCubeZForEntity(entity);
		if (cubeX != this.cubeX || cubeY != this.cubeY || cubeZ != this.cubeZ) {
			LOGGER.warn(String.format("Wrong entity location. Entity thinks it's in (%d,%d,%d) but actua location is (%d,%d,%d)!",
					entity.getClass().getName(), cubeX, cubeY, cubeZ, this.cubeX, this.cubeY, this.cubeZ));
			entity.setDead();
		}

		//post the event, we can't send cube position here :(
		MinecraftForge.EVENT_BUS.post(new EntityEvent.EnteringChunk(
				entity, this.getX(), this.getZ(), entity.chunkCoordX, entity.chunkCoordZ));

		// tell the entity it's in this cube
		entity.addedToChunk = true;
		entity.chunkCoordX = this.cubeX;
		entity.chunkCoordY = this.cubeY;
		entity.chunkCoordZ = this.cubeZ;

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
		if (this.isCubeLoaded) {
			this.getWorld().addTileEntity(tileEntity);
		}
	}

	public void addTileEntity(BlockPos pos, TileEntity tileEntity) {
		// update the tile entity
		tileEntity.setWorldObj(this.getWorld());
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
		if(this.isCubeLoaded) {
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

	public void tickCube() {
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

	//=================================
	//=========Other methods===========
	//=================================

	public boolean isEmpty() {
		return this.storage == null;
	}

	public void setEmpty(boolean isEmpty) {
		if (isEmpty) {
			this.storage = null;
		} else if (storage == null) {
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

	@Deprecated
	public Block getBlockAt(final BlockPos pos) {
		int x = Coords.blockToLocal(pos.getX());
		int y = Coords.blockToLocal(pos.getY());
		int z = Coords.blockToLocal(pos.getZ());
		return getBlockAt(x, y, z);
	}

	@Deprecated
	public Block getBlockAt(final int localX, final int localY, final int localZ) {
		if (isEmpty()) {
			return Blocks.AIR;
		}
		//actually: getBlockAt. WTF!?
		return this.storage.get(localX, localY, localZ).getBlock();
	}

	public IBlockState setBlockForGeneration(BlockPos blockOrLocalPos, IBlockState newBlockState) {
		IBlockState oldBlockState = getBlockState(blockOrLocalPos);

		// did anything actually change?
		if (newBlockState == oldBlockState) {
			return null;
		}

		// make sure we're not empty
		if (isEmpty()) {
			setEmpty(false);
		}

		int x = Coords.blockToLocal(blockOrLocalPos.getX());
		int y = Coords.blockToLocal(blockOrLocalPos.getY());
		int z = Coords.blockToLocal(blockOrLocalPos.getZ());

		// set the block
		this.storage.set(x, y, z, newBlockState);

		Block newBlock = newBlockState.getBlock();

		// did the block change work correctly?
		if (this.storage.get(x, y, z) != newBlockState) {
			return null;
		}
		this.isModified = true;

		//update the column light index
		int blockY = Coords.localToBlock(this.cubeY, y);

		IOpacityIndex index = this.column.getOpacityIndex();
		int opacity = newBlock.getLightOpacity(newBlockState);
		index.onOpacityChange(x, blockY, z, opacity);
		return oldBlockState;
	}

	public boolean hasBlocks() {
		if (isEmpty()) {
			return false;
		}

		return !this.storage.isEmpty();
	}

	public Iterable<TileEntity> getTileEntityMap() {
		return this.tileEntityMap.values();
	}

	public EntityContainer getEntityContainer() {
		return this.entities;
	}

	public void onLoad() {

		// tell the world about entities
		for (Entity entity : this.entities.getEntities()) {
			entity.onChunkLoad();
		}

		this.world.loadEntities(this.entities.getEntities());

		// tell the world about tile entities
		this.world.addTileEntities(this.tileEntityMap.values());
		this.isCubeLoaded = true;
	}

	public void onUnload() {

		// tell the world to forget about entities
		this.world.unloadEntities(this.entities.getEntities());

		// tell the world to forget about tile entities
		for (TileEntity blockEntity : this.tileEntityMap.values()) {
			this.world.removeTileEntity(blockEntity.getPos());
		}

		this.isCubeLoaded = false;
	}

	public boolean needsSaving() {
		return this.entities.needsSaving(true, this.world.getTotalWorldTime(), this.isModified);
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

	public LightUpdateData getLightUpdateData() {
		return this.lightUpdateData;
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
			if(minMaxHeights[index] != -1) {
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
