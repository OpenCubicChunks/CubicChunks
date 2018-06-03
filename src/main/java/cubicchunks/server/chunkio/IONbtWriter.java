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

import static cubicchunks.util.WorldServerAccess.getPendingTickListEntriesHashSet;
import static cubicchunks.util.WorldServerAccess.getPendingTickListEntriesThisTick;

import cubicchunks.CubicChunks;
import cubicchunks.util.Coords;
import cubicchunks.world.ServerHeightMap;
import cubicchunks.world.column.IColumn;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
class IONbtWriter {
    
    static byte[] writeNbtBytes(NBTTagCompound nbt) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        CompressedStreamTools.writeCompressed(nbt, buf);
        return buf.toByteArray();
    }

    static NBTTagCompound write(IColumn column) {
        NBTTagCompound columnNbt = new NBTTagCompound();
        NBTTagCompound level = new NBTTagCompound();
        columnNbt.setTag("Level", level);
        columnNbt.setInteger("DataVersion", FMLCommonHandler.instance().getDataFixer().version);
        FMLCommonHandler.instance().getDataFixer().writeVersionData(columnNbt);
        writeBaseColumn(column, level);
        writeBiomes(column, level);
        writeOpacityIndex(column, level);
        MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Save((Chunk) column, columnNbt));
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
        return cubeNbt;
    }

    private static void writeBaseColumn(IColumn column, NBTTagCompound nbt) {// coords
        nbt.setInteger("x", column.getX());
        nbt.setInteger("z", column.getZ());

        // column properties
        nbt.setByte("v", (byte) 1);
        nbt.setLong("InhabitedTime", column.getInhabitedTime());
    }

    private static void writeBiomes(IColumn column, NBTTagCompound nbt) {// biomes
        nbt.setByteArray("Biomes", column.getBiomeArray());
    }

    private static void writeOpacityIndex(IColumn IColumn, NBTTagCompound nbt) {// light index
        nbt.setByteArray("OpacityIndex", ((ServerHeightMap) IColumn.getOpacityIndex()).getData());
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

        cubeNbt.setBoolean("initLightDone", cube.isInitialLightingDone());
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
        NibbleArray add = ebs.getData().getDataForNBT(abyte, data);

        section.setByteArray("Blocks", abyte);
        section.setByteArray("Data", data.getData());

        if (add != null) {
            section.setByteArray("Add", add.getData());
        }

        section.setByteArray("BlockLight", ebs.getBlocklightArray().getData());

        if (cube.getCubicWorld().getProvider().hasSkyLight()) {
            section.setByteArray("SkyLight", ebs.getSkylightArray().getData());
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
        long time = cube.getCubicWorld().getTotalWorldTime();

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
        for (int i = 0; i < cube.edgeNeedSkyLightUpdate.length; i++) {
            if (cube.edgeNeedSkyLightUpdate[i])
                edgeNeedSkyLightUpdate |= 1 << i;
        }
        lightingInfo.setByte("EdgeNeedSkyLightUpdate", edgeNeedSkyLightUpdate);
    }

    private static List<NextTickListEntry> getScheduledTicks(Cube cube) {
        ArrayList<NextTickListEntry> out = new ArrayList<>();

        // make sure this is a server
        if (!(cube.getCubicWorld() instanceof WorldServer)) {
            throw new Error("Column is not on the server!");
        }
        WorldServer worldServer = (WorldServer) cube.getCubicWorld();

        // copy the ticks for this cube
        copyScheduledTicks(out, getPendingTickListEntriesHashSet(worldServer), cube);
        copyScheduledTicks(out, getPendingTickListEntriesThisTick(worldServer), cube);

        return out;
    }

    private static void copyScheduledTicks(ArrayList<NextTickListEntry> out, Collection<NextTickListEntry> scheduledTicks, Cube cube) {
        out.addAll(scheduledTicks.stream().filter(scheduledTick -> cube.containsBlockPos(scheduledTick.position)).collect(Collectors.toList()));
    }
}
