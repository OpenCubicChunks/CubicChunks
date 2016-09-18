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
package cubicchunks.server.chunkio;

import cubicchunks.CubicChunks;
import cubicchunks.lighting.LightingManager;
import cubicchunks.util.Coords;
import cubicchunks.world.ChunkSectionHelper;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.OpacityIndex;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.GeneratorStageRegistry;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;

public class IONbtReader {
	@Nullable
	static Column readColumn(ICubicWorld world, int x, int z, NBTTagCompound nbt) {
		Column column = readBaseColumn(world, x, z, nbt);
		if (column == null) {
			return null;
		}
		readBiomes(nbt, column);
		readOpacityIndex(nbt, column);
		readEntities(world, x, z, nbt, column);
		return column;
	}

	@Nullable
	private static Column readBaseColumn(ICubicWorld world, int x, int z, NBTTagCompound nbt) {// check the version number
		byte version = nbt.getByte("v");
		if (version != 1) {
			throw new IllegalArgumentException(String.format("Column has wrong version: %d", version));
		}

		// check the coords
		int xCheck = nbt.getInteger("x");
		int zCheck = nbt.getInteger("z");
		if (xCheck != x || zCheck != z) {
			CubicChunks.LOGGER.warn(String.format("Column is corrupted! Expected (%d,%d) but got (%d,%d). Column will be regenerated.", x, z, xCheck, zCheck));
			return null;
		}

		// create the column
		Column column = new Column(world, x, z);

		// read the rest of the column properties
		column.setTerrainPopulated(nbt.getBoolean("TerrainPopulated"));
		column.setInhabitedTime(nbt.getLong("InhabitedTime"));
		return column;
	}

	private static void readBiomes(NBTTagCompound nbt, Column column) {// biomes
		column.setBiomeArray(nbt.getByteArray("Biomes"));
	}

	private static void readOpacityIndex(NBTTagCompound nbt, Column column) {// biomes
		((OpacityIndex) column.getOpacityIndex()).readData(nbt.getByteArray("OpacityIndex"));
	}

	private static void readEntities(ICubicWorld world, int x, int z, NBTTagCompound nbt, Column column) {// entities
		column.getEntityContainer().readFromNbt(nbt, "Entities", world, entity -> {
			entity.addedToChunk = true;
			entity.chunkCoordX = x;
			entity.chunkCoordY = Coords.getCubeYForEntity(entity);
			entity.chunkCoordZ = z;
		});
	}

	@Nullable
	static Cube readCube(Column column, final int cubeX, final int cubeY, final int cubeZ, NBTTagCompound nbt) {
		if (column.getX() != cubeX || column.getZ() != cubeZ) {
			throw new IllegalArgumentException(String.format("Invalid column (%d, %d) for cube at (%d, %d, %d)",
					column.getX(), column.getZ(), cubeX, cubeY, cubeZ));
		}
		ICubicWorldServer world = (ICubicWorldServer) column.getWorld();

		Cube cube = readBaseCube(column, cubeX, cubeY, cubeZ, nbt, world);
		readBlocks(nbt, world, cube);
		readEntities(nbt, world, cube);
		readTileEntities(column, nbt, world);
		readScheduledBlockTicks(nbt, world);
		readLightingInfo(cube, nbt, world);

		return cube;
	}

	@Nullable
	private static Cube readBaseCube(Column column, int cubeX, int cubeY, int cubeZ, NBTTagCompound nbt, ICubicWorldServer world) {// check the version number
		byte version = nbt.getByte("v");
		if (version != 1) {
			throw new IllegalArgumentException("Cube has wrong version! " + version);
		}

		// check the coordinates
		int xCheck = nbt.getInteger("x");
		int yCheck = nbt.getInteger("y");
		int zCheck = nbt.getInteger("z");
		if (xCheck != cubeX || yCheck != cubeY || zCheck != cubeZ) {
			CubicChunks.LOGGER.error(String.format("Cube is corrupted! Expected (%d,%d,%d) but got (%d,%d,%d). Cube will be regenerated.", cubeX, cubeY, cubeZ, xCheck, yCheck, zCheck));
			return null;
		}

		// check against column
		assert cubeX == column.xPosition && cubeZ == column.zPosition :
				String.format("Cube is corrupted! Cube (%d,%d,%d) does not match column (%d,%d).", cubeX, cubeY, cubeZ, column.xPosition, column.zPosition);


		// build the cube
		final Cube cube = column.getOrCreateCube(cubeY, false);

		// get the worldgen stage and the target stage
		GeneratorStageRegistry generatorStageRegistry = world.getCubeGenerator().getGeneratorStageRegistry();
		cube.setCurrentStage(generatorStageRegistry.getStage(nbt.getString("currentStage")));
		cube.setTargetStage(generatorStageRegistry.getStage(nbt.getString("targetStage")));
		return cube;
	}

	private static void readBlocks(NBTTagCompound nbt, ICubicWorldServer world, Cube cube) {// is this an empty cube?
		boolean isEmpty = !nbt.hasKey("Blocks");
		if (!isEmpty) {
			ExtendedBlockStorage storage = cube.getStorage();

			// block ids and metadata (ie block states)
			byte[] blockIdLsbs = nbt.getByteArray("Blocks");
			NibbleArray blockIdMsbs = null;
			if (nbt.hasKey("Add")) {
				blockIdMsbs = new NibbleArray(nbt.getByteArray("Add"));
			}
			NibbleArray blockMetadata = new NibbleArray(nbt.getByteArray("Data"));
			ChunkSectionHelper.setBlockStates(storage, blockIdLsbs, blockIdMsbs, blockMetadata);

			// lights
			storage.setBlocklightArray(new NibbleArray(nbt.getByteArray("BlockLight")));
			if (!world.getProvider().getHasNoSky()) {
				storage.setSkylightArray(new NibbleArray(nbt.getByteArray("SkyLight")));
			}
			storage.removeInvalidBlocks();
		}
	}

	private static void readEntities(NBTTagCompound nbt, ICubicWorldServer world, Cube cube) {// entities
		cube.getEntityContainer().readFromNbt(nbt, "Entities", world, entity -> {
			// make sure this entity is really in the chunk
			int entityCubeX = Coords.getCubeXForEntity(entity);
			int entityCubeY = Coords.getCubeYForEntity(entity);
			int entityCubeZ = Coords.getCubeZForEntity(entity);
			if (entityCubeX != cube.getX() || entityCubeY != cube.getY() || entityCubeZ != cube.getZ()) {
				CubicChunks.LOGGER.warn(String.format("Loaded entity %s in cube (%d,%d,%d) to cube (%d,%d,%d)!", entity.getClass()
						.getName(), entityCubeX, entityCubeY, entityCubeZ, cube.getX(), cube.getY(), cube.getZ()));
			}

			entity.addedToChunk = true;
			entity.chunkCoordX = cube.getX();
			entity.chunkCoordY = cube.getY();
			entity.chunkCoordZ = cube.getZ();
		});
	}

	private static void readTileEntities(Column column, NBTTagCompound nbt, ICubicWorldServer world) {// tile entities
		NBTTagList nbtTileEntities = nbt.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);
		if (nbtTileEntities == null) {
			return;
		}
		for (int i = 0; i < nbtTileEntities.tagCount(); i++) {
			NBTTagCompound nbtTileEntity = nbtTileEntities.getCompoundTagAt(i);
			//TileEntity.create
			TileEntity blockEntity = TileEntity.create((World) world, nbtTileEntity);
			if (blockEntity != null) {
				column.addTileEntity(blockEntity);
			}
		}
	}

	private static void readScheduledBlockTicks(NBTTagCompound nbt, ICubicWorldServer world) {
		NBTTagList nbtScheduledTicks = nbt.getTagList("TileTicks", 10);
		if (nbtScheduledTicks == null) {
			return;
		}
		for (int i = 0; i < nbtScheduledTicks.tagCount(); i++) {
			NBTTagCompound nbtScheduledTick = nbtScheduledTicks.getCompoundTagAt(i);
			Block block;
			if (nbtScheduledTick.hasKey("i", Constants.NBT.TAG_STRING)) {
				block = Block.getBlockFromName(nbtScheduledTick.getString("i"));
			} else {
				block = Block.getBlockById(nbtScheduledTick.getInteger("i"));
			}
			world.scheduleBlockUpdate(
					new BlockPos(
							nbtScheduledTick.getInteger("x"),
							nbtScheduledTick.getInteger("y"),
							nbtScheduledTick.getInteger("z")
					),
					block,
					nbtScheduledTick.getInteger("t"),
					nbtScheduledTick.getInteger("p")
			);
		}
	}

	private static void readLightingInfo(Cube cube, NBTTagCompound nbt, ICubicWorldServer world) {
		NBTTagCompound lightingInfo = nbt.getCompoundTag("LightingInfo");
		int[] lastHeightMap = lightingInfo.getIntArray("LastHeightMap");
		int[] currentHeightMap = cube.getColumn().getHeightMap();

		int minBlockY = Coords.cubeToMinBlock(cube.getY());
		int maxBlockYCube = Coords.cubeToMaxBlock(cube.getY());
		//any changes more than 15 blocks above current cube don't have any effect
		int maxBlockY = maxBlockYCube + 15;
		for (int i = 0; i < currentHeightMap.length; i++) {
			int currentY = currentHeightMap[i];
			int lastY = lastHeightMap[i];

			boolean needLightUpdate = currentY != lastY &&
					(currentY >= minBlockY || lastY >= minBlockY) &&
					(currentY <= maxBlockY || lastY <= maxBlockY);
			if (needLightUpdate) {
				int minUpdateY = Math.max(minBlockY, Math.min(currentY, lastY));
				int maxUpdateY = Math.min(minBlockY, Math.max(currentY, lastY));

				int localX = i & 0xF;
				int localZ = i >> 4;
				cube.getWorld().getLightingManager().columnSkylightUpdate(
						LightingManager.UpdateType.QUEUED, cube.getColumn(),
						localX,
						minUpdateY, maxUpdateY,
						localZ
				);
			}
		}
	}

}
