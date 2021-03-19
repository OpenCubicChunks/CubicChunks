/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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
package io.github.opencubicchunks.cubicchunks.core.server.chunkio;

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.localToBlock;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.world.ClientHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.ServerHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class IONbtReader {

    @Nullable
    static Chunk readColumn(World world, int x, int z, NBTTagCompound nbt) {
        NBTTagCompound level = nbt.getCompoundTag("Level");
        Chunk column = readBaseColumn(world, x, z, level);
        if (column == null) {
            return null;
        }
        readBiomes(level, column);
        readOpacityIndex(level, column);

        column.setModified(false); // its exactly the same as on disk so its not modified
        return column; // TODO: use Chunk, not IColumn, whenever possible
    }

    @Nullable
    private static Chunk readBaseColumn(World world, int x, int z, NBTTagCompound nbt) {// check the version number
        byte version = nbt.getByte("v");
        if (version != 1) {
            throw new IllegalArgumentException(String.format("Column has wrong version: %d", version));
        }

        // check the coords
        int xCheck = nbt.getInteger("x");
        int zCheck = nbt.getInteger("z");
        if (xCheck != x || zCheck != z) {
            CubicChunks.LOGGER
                    .warn(String.format("Column is corrupted! Expected (%d,%d) but got (%d,%d). Column will be regenerated.", x, z, xCheck, zCheck));
            return null;
        }

        // create the column
        Chunk column = new Chunk(world, x, z);

        // read the rest of the column properties
        column.setInhabitedTime(nbt.getLong("InhabitedTime"));

        if (column.getCapabilities() != null && nbt.hasKey("ForgeCaps")) {
            column.getCapabilities().deserializeNBT(nbt.getCompoundTag("ForgeCaps"));
        }
        return column;
    }

    private static void readBiomes(NBTTagCompound nbt, Chunk column) {// biomes
        System.arraycopy(nbt.getByteArray("Biomes"), 0, column.getBiomeArray(), 0, Cube.SIZE * Cube.SIZE);
    }

    private static void readOpacityIndex(NBTTagCompound nbt, Chunk chunk) {// biomes
        IHeightMap hmap = ((IColumn) chunk).getOpacityIndex();
        if (hmap instanceof ServerHeightMap) {
            ((ServerHeightMap) hmap).readData(nbt.getByteArray("OpacityIndex"));
        } else {
            ((ClientHeightMap) hmap).setData(nbt.getByteArray("OpacityIndexClient"));
        }
    }

    @Nullable
    static Cube readCubeAsyncPart(Chunk column, final int cubeX, final int cubeY, final int cubeZ, NBTTagCompound nbt) {
        if (column.x != cubeX || column.z != cubeZ) {
            throw new IllegalArgumentException(String.format("Invalid column (%d, %d) for cube at (%d, %d, %d)",
                    column.x, column.z, cubeX, cubeY, cubeZ));
        }
        World world = column.getWorld();
        NBTTagCompound level = nbt.getCompoundTag("Level");
        Cube cube = readBaseCube(column, cubeX, cubeY, cubeZ, level, world);
        if (cube == null) {
            return null;
        }
        readBiomes(cube, level);
        readLightingInfo(cube, level, world);
        readBlocks(level, world, cube);

        return cube;
    }

    static void readCubeSyncPart(Cube cube, World world, NBTTagCompound nbt) {
        // a hack so that the Column won't try to get cube from CubeCache/CubeProvider.
        cube.getColumn().preCacheCube(cube);
        NBTTagCompound level = nbt.getCompoundTag("Level");
        readEntities(level, world, cube);
        readTileEntities(level, world, cube);
        readScheduledBlockTicks(level, world);
        cube.markSaved(); // its exactly the same as on disk so its not modified
    }

    @Nullable
    private static Cube readBaseCube(Chunk column, int cubeX, int cubeY, int cubeZ, NBTTagCompound nbt, World world) {// check the version number
        byte version = nbt.getByte("v");
        if (version != 1) {
            throw new IllegalArgumentException(String.format("Cube at CubePos:(%d, %d, %d), has wrong version! %d", cubeX, cubeY, cubeZ, version));
        }

        // check the coordinates
        int xCheck = nbt.getInteger("x");
        int yCheck = nbt.getInteger("y");
        int zCheck = nbt.getInteger("z");
        if (xCheck != cubeX || yCheck != cubeY || zCheck != cubeZ) {
            CubicChunks.LOGGER.error(String
                    .format("Cube is corrupted! Expected (%d,%d,%d) but got (%d,%d,%d). Cube will be regenerated.", cubeX, cubeY, cubeZ, xCheck,
                            yCheck, zCheck));
            return null;
        }

        // check against column
        assert cubeX == column.x && cubeZ == column.z :
                String.format("Cube is corrupted! Cube (%d,%d,%d) does not match column (%d,%d).", cubeX, cubeY, cubeZ, column.z,
                        column.z);


        // build the cube
        final Cube cube = new Cube(column, cubeY);

        // set the worldgen stage
        cube.setPopulated(nbt.getBoolean("populated"));
        cube.setSurfaceTracked(nbt.getBoolean("isSurfaceTracked")); // previous versions will get their surface tracking redone. This is intended
        cube.setFullyPopulated(nbt.getBoolean("fullyPopulated"));

        int lightVersion = nbt.getInteger("initLightVersion");
        cube.setInitialLightingDone(
                (!CubicChunksConfig.updateKnownBrokenLightingOnLoad || lightVersion >= 1)
                && nbt.getBoolean("initLightDone"));

        if (cube.getCapabilities() != null && nbt.hasKey("ForgeCaps")) {
            cube.getCapabilities().deserializeNBT(nbt.getCompoundTag("ForgeCaps"));
        }
        return cube;
    }

    @SuppressWarnings("deprecation") private static void readBlocks(NBTTagCompound nbt, World world, Cube cube) {
        boolean isEmpty = !nbt.hasKey("Sections");// is this an empty cube?
        if (!isEmpty) {
            NBTTagList sectionList = nbt.getTagList("Sections", 10);
            nbt = sectionList.getCompoundTagAt(0);

            ExtendedBlockStorage ebs = new ExtendedBlockStorage(Coords.cubeToMinBlock(cube.getY()), cube.getWorld().provider.hasSkyLight());

            byte[] abyte = nbt.getByteArray("Blocks");
            NibbleArray data = new NibbleArray(nbt.getByteArray("Data"));
            NibbleArray add = nbt.hasKey("Add", Constants.NBT.TAG_BYTE_ARRAY) ? new NibbleArray(nbt.getByteArray("Add")) : null;
            NibbleArray add2neid = nbt.hasKey("Add2", Constants.NBT.TAG_BYTE_ARRAY) ? new NibbleArray(nbt.getByteArray("Add2")) : null;

            for (int i = 0; i < 4096; i++) {
                int x = i & 15;
                int y = i >> 8 & 15;
                int z = i >> 4 & 15;

                int toAdd = add == null ? 0 : add.getFromIndex(i);
                toAdd = (toAdd & 0xF) | (add2neid == null ? 0 : add2neid.getFromIndex(i) << 4);
                int id = (toAdd << 12) | ((abyte[i] & 0xFF) << 4) | data.getFromIndex(i);
                ebs.getData().set(x, y, z, Block.BLOCK_STATE_IDS.getByValue(id));
            }

            ebs.setBlockLight(new NibbleArray(nbt.getByteArray("BlockLight")));

            if (world.provider.hasSkyLight()) {
                ebs.setSkyLight(new NibbleArray(nbt.getByteArray("SkyLight")));
            }

            ebs.recalculateRefCounts();
            cube.setStorage(ebs);
        }
    }

    private static void readEntities(NBTTagCompound nbt, World world, Cube cube) {// entities
        cube.getEntityContainer().readFromNbt(nbt, "Entities", world, entity -> {
            // make sure this entity is really in the chunk
            int entityCubeX = Coords.getCubeXForEntity(entity);
            int entityCubeY = Coords.getCubeYForEntity(entity);
            int entityCubeZ = Coords.getCubeZForEntity(entity);
            if (entityCubeX != cube.getX() || entityCubeY != cube.getY() || entityCubeZ != cube.getZ()) {
                CubicChunks.LOGGER.warn(String.format("Loaded entity %s in cube (%d,%d,%d) to cube (%d,%d,%d)!", entity.getClass()
                        .getName(), entityCubeX, entityCubeY, entityCubeZ, cube.getX(), cube.getY(), cube.getZ()));
            }

            // The entity needs to know what Cube it is in, this is normally done in Cube.addEntity()
            // but Cube.addEntity() is not used when loading entities
            // (unlike vanilla which uses Chunk.addEntity() even when loading entities)
            entity.addedToChunk = true;
            entity.chunkCoordX = cube.getX();
            entity.chunkCoordY = cube.getY();
            entity.chunkCoordZ = cube.getZ();
        });
    }

    private static void readTileEntities(NBTTagCompound nbt, World world, Cube cube) {// tile entities
        NBTTagList nbtTileEntities = nbt.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < nbtTileEntities.tagCount(); i++) {
            NBTTagCompound nbtTileEntity = nbtTileEntities.getCompoundTagAt(i);
            //TileEntity.create
            TileEntity blockEntity = TileEntity.create(world, nbtTileEntity);
            if (blockEntity != null) {
                if (!cube.getCoords().containsBlock(blockEntity.getPos())) {
                    CubicChunks.LOGGER.warn("TileEntity " + blockEntity + " is not in cube at " + cube.getCoords() + ", tile entity will be skipped");
                    continue;
                }
                cube.addTileEntity(blockEntity);
            }
        }
    }

    private static void readScheduledBlockTicks(NBTTagCompound nbt, World world) {
        if (!(world instanceof WorldServer)) {
            // if not server, reading from client cache which doesn't have scheduled ticks
            return;
        }
        NBTTagList nbtScheduledTicks = nbt.getTagList("TileTicks", 10);
        for (int i = 0; i < nbtScheduledTicks.tagCount(); i++) {
            NBTTagCompound nbtScheduledTick = nbtScheduledTicks.getCompoundTagAt(i);
            Block block;
            if (nbtScheduledTick.hasKey("i", Constants.NBT.TAG_STRING)) {
                block = Block.getBlockFromName(nbtScheduledTick.getString("i"));
            } else {
                block = Block.getBlockById(nbtScheduledTick.getInteger("i"));
            }
            if (block == null) {
                continue;
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

    private static void readLightingInfo(Cube cube, NBTTagCompound nbt, World world) {
        NBTTagCompound lightingInfo = nbt.getCompoundTag("LightingInfo");
        int[] lastHeightMap = lightingInfo.getIntArray("LastHeightMap"); // NO NO NO! TODO: Why is hightmap being stored in Cube's data?! kill it!
        int[] currentHeightMap = cube.getColumn().getHeightMap();
        byte edgeNeedSkyLightUpdate = 0x3F;
        if (lightingInfo.hasKey("EdgeNeedSkyLightUpdate"))
            edgeNeedSkyLightUpdate = lightingInfo.getByte("EdgeNeedSkyLightUpdate");
        LightingManager.CubeLightUpdateInfo cubeLightUpdateInfo = cube.getCubeLightUpdateInfo();
        if (cubeLightUpdateInfo != null) {
            for (int i = 0; i < EnumFacing.VALUES.length; i++) {
                if ((edgeNeedSkyLightUpdate >>> i & 1) != 0) {
                    cubeLightUpdateInfo.markEdgeNeedSkyLightUpdate(EnumFacing.VALUES[i]);
                }
            }
        }

        // assume changes outside of this cube have no effect on this cube.
        // In practice changes up to 15 blocks above can affect it,
        // but it will be fixed by lighting update in other cube anyway
        int minBlockY = Coords.cubeToMinBlock(cube.getY());
        int maxBlockY = Coords.cubeToMaxBlock(cube.getY());
        LightingManager lightManager = ((ICubicWorldInternal) cube.getWorld()).getLightingManager();
        for (int i = 0; i < currentHeightMap.length; i++) {
            int currentY = currentHeightMap[i];
            int lastY = lastHeightMap[i];

            //sort currentY and lastY
            int minUpdateY = Math.min(currentY, lastY);
            int maxUpdateY = Math.max(currentY, lastY);

            boolean needLightUpdate = minUpdateY != maxUpdateY &&
                    //if max update Y is below minY - nothing to update
                    !(maxUpdateY < minBlockY) &&
                    //if min update Y is above maxY - nothing to update
                    !(minUpdateY > maxBlockY);
            if (needLightUpdate) {

                //clamp min/max update Y to be within current cube bounds
                if (minUpdateY < minBlockY) {
                    minUpdateY = minBlockY;
                }
                if (maxUpdateY > maxBlockY) {
                    maxUpdateY = maxBlockY;
                }
                assert minUpdateY <= maxUpdateY : "minUpdateY > maxUpdateY: " + minUpdateY + ">" + maxUpdateY;

                int localX = i & 0xF;
                int localZ = i >> 4;
                lightManager.markCubeBlockColumnForUpdate(cube,
                        localToBlock(cube.getX(), localX), localToBlock(cube.getZ(), localZ));
            }
        }
    }

    private static void readBiomes(Cube cube, NBTTagCompound nbt) {// biomes
        if (nbt.hasKey("Biomes"))
            cube.setBiomeArray(nbt.getByteArray("Biomes"));
    }
}
