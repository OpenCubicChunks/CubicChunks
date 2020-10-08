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

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.CubeDataEvent;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.world.ClientHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.ServerHeightMap;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static net.minecraftforge.common.MinecraftForge.EVENT_BUS;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
class IONbtWriter {
    
    static byte[] writeNbtBytes(NBTTagCompound nbt) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(nbt, buf);
        return buf.toByteArray();
    }

    static NBTTagCompound write(Chunk column) {
        NBTTagCompound columnNbt = new NBTTagCompound();
        NBTTagCompound level = new NBTTagCompound();
        columnNbt.setTag("Level", level);
        columnNbt.setInteger("DataVersion", FMLCommonHandler.instance().getDataFixer().version);
        FMLCommonHandler.instance().getDataFixer().writeVersionData(columnNbt);
        writeBaseColumn(column, level);
        writeBiomes(column, level);
        writeOpacityIndex(column, level);
        EVENT_BUS.post(new ChunkDataEvent.Save(column, columnNbt));
        return columnNbt;
    }

    static NBTTagCompound write(final Cube cube) {
        NBTTagCompound cubeNbt = new NBTTagCompound();
        //Added to preserve compatibility with vanilla NBT chunk format.
        NBTTagCompound level = new NBTTagCompound();
        cubeNbt.setTag("Level", level);
        cubeNbt.setInteger("DataVersion", FMLCommonHandler.instance().getDataFixer().version);
        FMLCommonHandler.instance().getDataFixer().writeVersionData(cubeNbt);
        writeBaseCube(cube, level);
        writeBlocks(cube, level);
        writeEntities(cube, level);
        writeTileEntities(cube, level);
        writeScheduledTicks(cube, level);
        writeLightingInfo(cube, level);
        writeBiomes(cube, level);
        writeModData(cube, cubeNbt);
        return cubeNbt;
    }

    private static void writeBaseColumn(Chunk column, NBTTagCompound nbt) {// coords
        nbt.setInteger("x", column.x);
        nbt.setInteger("z", column.z);

        // column properties
        nbt.setByte("v", (byte) 1);
        nbt.setLong("InhabitedTime", column.getInhabitedTime());

        if (column.getCapabilities() != null) {
            try {
                nbt.setTag("ForgeCaps", column.getCapabilities().serializeNBT());
            } catch (Exception exception) {
                CubicChunks.LOGGER.error("A capability provider has thrown an exception trying to write state. It will not persist. "
                                + "Report this to the mod author", exception);
            }
        }
    }

    private static void writeBiomes(Chunk column, NBTTagCompound nbt) {// biomes
        nbt.setByteArray("Biomes", column.getBiomeArray());
    }

    private static void writeOpacityIndex(Chunk column, NBTTagCompound nbt) {// light index
        IHeightMap hmap = ((IColumn) column).getOpacityIndex();
        if (hmap instanceof ServerHeightMap) {
            nbt.setByteArray("OpacityIndex", ((ServerHeightMap) hmap).getData());
        } else {
            nbt.setByteArray("OpacityIndexClient", ((ClientHeightMap) hmap).getData());
        }
    }

    private static void writeBaseCube(Cube cube, NBTTagCompound cubeNbt) {
        cubeNbt.setByte("v", (byte) 1);

        // coords
        cubeNbt.setInteger("x", cube.getX());
        cubeNbt.setInteger("y", cube.getY());
        cubeNbt.setInteger("z", cube.getZ());

        // save the worldgen stage and the target stage
        cubeNbt.setBoolean("populated", cube.isPopulated());
        cubeNbt.setBoolean("isSurfaceTracked", cube.isSurfaceTracked());
        cubeNbt.setBoolean("fullyPopulated", cube.isFullyPopulated());

        // this will allow to detect worlds with older versions of light propagation in CC
        cubeNbt.setInteger("initLightVersion", 1);
        cubeNbt.setBoolean("initLightDone", cube.isInitialLightingDone());

        if (cube.getCapabilities() != null) {
            try {
                cubeNbt.setTag("ForgeCaps", cube.getCapabilities().serializeNBT());
            } catch (Exception exception) {
                CubicChunks.LOGGER.error("A capability provider has thrown an exception trying to write state. It will not persist. "
                        + "Report this to the mod author", exception);
            }
        }
    }

    private static void writeBlocks(Cube cube, NBTTagCompound cubeNbt) {
        ExtendedBlockStorage ebs = cube.getStorage();
        if (ebs == null) {
            return; // no data to save anyway
        }
        NBTTagList sectionList = new NBTTagList();
        NBTTagCompound section = new NBTTagCompound();
        sectionList.appendTag(section);
        cubeNbt.setTag("Sections", sectionList);
        byte[] abyte = new byte[Cube.SIZE * Cube.SIZE * Cube.SIZE];
        NibbleArray data = new NibbleArray();
        NibbleArray add = null;
        NibbleArray add2neid = null;

        for (int i = 0; i < 4096; ++i) {
            int x = i & 15;
            int y = i >> 8 & 15;
            int z = i >> 4 & 15;

            @SuppressWarnings("deprecation")
            int id = Block.BLOCK_STATE_IDS.get(ebs.getData().get(x, y, z));

            int in1 = (id >> 12) & 0xF;
            int in2 = (id >> 16) & 0xF;

            if (in1 != 0) {
                if (add == null) {
                    add = new NibbleArray();
                }
                add.setIndex(i, in1);
            }
            if (in2 != 0) {
                if (add2neid == null) {
                    add2neid = new NibbleArray();
                }
                add2neid.setIndex(i, in2);
            }

            abyte[i] = (byte) (id >> 4 & 255);
            data.setIndex(i, id & 15);
        }

        section.setByteArray("Blocks", abyte);
        section.setByteArray("Data", data.getData());

        if (add != null) {
            section.setByteArray("Add", add.getData());
        }
        if (add2neid != null) {
            section.setByteArray("Add2", add2neid.getData());
        }

        section.setByteArray("BlockLight", ebs.getBlockLight().getData());

        if (cube.getWorld().provider.hasSkyLight()) {
            section.setByteArray("SkyLight", ebs.getSkyLight().getData());
        }
    }

    private static void writeEntities(Cube cube, NBTTagCompound cubeNbt) {// entities
        cube.getEntityContainer().writeToNbt(cubeNbt, "Entities", entity -> {
            // make sure this entity is really in the chunk
            int cubeX = Coords.getCubeXForEntity(entity);
            int cubeY = Coords.getCubeYForEntity(entity);
            int cubeZ = Coords.getCubeZForEntity(entity);
            if (cubeX != cube.getX() || cubeY != cube.getY() || cubeZ != cube.getZ()) {
                CubicChunks.LOGGER.warn(String.format("Saved entity %s in cube (%d,%d,%d) to cube (%d,%d,%d)! Entity thinks its in (%d,%d,%d)",
                        entity.getClass().getName(),
                        cubeX, cubeY, cubeZ,
                        cube.getX(), cube.getY(), cube.getZ(),
                        entity.chunkCoordX, entity.chunkCoordY, entity.chunkCoordZ
                ));
            }
        });
    }

    private static void writeTileEntities(Cube cube, NBTTagCompound cubeNbt) {// tile entities
        NBTTagList nbtTileEntities = new NBTTagList();
        cubeNbt.setTag("TileEntities", nbtTileEntities);
        for (TileEntity blockEntity : cube.getTileEntityMap().values()) {
            NBTTagCompound nbtTileEntity = new NBTTagCompound();
            blockEntity.writeToNBT(nbtTileEntity);
            nbtTileEntities.appendTag(nbtTileEntity);
        }
    }

    private static void writeScheduledTicks(Cube cube, NBTTagCompound cubeNbt) {// scheduled block ticks
        Iterable<NextTickListEntry> scheduledTicks = getScheduledTicks(cube);
        long time = cube.getWorld().getTotalWorldTime();

        NBTTagList nbtTicks = new NBTTagList();
        cubeNbt.setTag("TileTicks", nbtTicks);
        for (NextTickListEntry scheduledTick : scheduledTicks) {
            NBTTagCompound nbtScheduledTick = new NBTTagCompound();
            ResourceLocation resourcelocation = Block.REGISTRY.getNameForObject(scheduledTick.getBlock());
            nbtScheduledTick.setString("i", resourcelocation.toString());
            nbtScheduledTick.setInteger("x", scheduledTick.position.getX());
            nbtScheduledTick.setInteger("y", scheduledTick.position.getY());
            nbtScheduledTick.setInteger("z", scheduledTick.position.getZ());
            nbtScheduledTick.setInteger("t", (int) (scheduledTick.scheduledTime - time));
            nbtScheduledTick.setInteger("p", scheduledTick.priority);
            nbtTicks.appendTag(nbtScheduledTick);
        }
    }

    private static void writeLightingInfo(Cube cube, NBTTagCompound cubeNbt) {
        NBTTagCompound lightingInfo = new NBTTagCompound();
        cubeNbt.setTag("LightingInfo", lightingInfo);

        int[] lastHeightmap = cube.getColumn().getHeightMap();
        lightingInfo.setIntArray("LastHeightMap", lastHeightmap); //TODO: why are we storing the height map on a Cube???
        byte edgeNeedSkyLightUpdate = 0;
        LightingManager.CubeLightUpdateInfo cubeLightUpdateInfo = cube.getCubeLightUpdateInfo();
        if (cubeLightUpdateInfo != null) {
            for (EnumFacing enumFacing : cubeLightUpdateInfo.edgeNeedSkyLightUpdate) {
                edgeNeedSkyLightUpdate |= 1 << enumFacing.ordinal();
            }
        }
        lightingInfo.setByte("EdgeNeedSkyLightUpdate", edgeNeedSkyLightUpdate);
    }

    private static void writeModData(Cube cube, NBTTagCompound level) {
        EVENT_BUS.post(new CubeDataEvent.Save(cube, level));
    }

    private static void writeBiomes(Cube cube, NBTTagCompound nbt) {// biomes
        byte[] biomes = cube.getBiomeArray();
        if (biomes != null)
            nbt.setByteArray("Biomes", biomes);
    }

    private static List<NextTickListEntry> getScheduledTicks(Cube cube) {
        ArrayList<NextTickListEntry> out = new ArrayList<>();

        // make sure this is a server, otherwise don't save these, writing to client cache
        if (!(cube.getWorld() instanceof WorldServer)) {
            return out;
        }
        WorldServer worldServer = cube.getWorld();

        out.addAll(((ICubicWorldInternal.Server) worldServer).getScheduledTicks().getForCube(cube.getCoords()));
        out.addAll(((ICubicWorldInternal.Server) worldServer).getThisTickScheduledTicks().getForCube(cube.getCoords()));

        return out;
    }
}
