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
package io.github.opencubicchunks.cubicchunks.core.world.chunkloader;

import static io.github.opencubicchunks.cubicchunks.core.util.ReflectionUtil.cast;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.IForgeChunkManager;
import io.github.opencubicchunks.cubicchunks.core.server.PlayerCubeMap;
import io.github.opencubicchunks.cubicchunks.core.util.ReflectionUtil;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.ITicket;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.management.PlayerChunkMapEntry;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * CubicChunks extension of {@link ForgeChunkManager}
 */
// TODO: figure out a way to put the right parts of it into API
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mod.EventBusSubscriber(modid = CubicChunks.MODID)
public class CubicChunkManager {

    /**
     * ForgeChunkManager doesn't give any way to work with Y coordinates for forced chunks.
     * So cubic chunks needs to get creative for it to work.
     *
     * When a mod requests a forced chunk for the first time for that chunk, we store a snapshot
     * of which cubes have been loaded for the given column at that time and attempt to keep that loaded.
     * This solution can be exploited by players, so it may be changed in the future.
     *
     * When serializing forced chunks, we also store this map in a file, and load it when a world is loaded.
     */

    private static final MethodHandle ticketConstructor = ReflectionUtil.constructHandle(
            ForgeChunkManager.Ticket.class, String.class,
            ForgeChunkManager.Type.class, World.class);

    /**
     * Force the supplied chunk coordinate to be loaded by the supplied ticket. If the ticket's {@link ForgeChunkManager.Ticket#maxDepth} is
     * exceeded, the least
     * recently registered chunk is unforced and may be unloaded.
     * It is safe to force the chunk several times for a ticket, it will not generate duplication or change the ordering.
     *
     * @param ticket The ticket registering the chunk
     * @param chunk The chunk to force
     */
    public static void forceChunk(ForgeChunkManager.Ticket ticket, CubePos chunk) {
        if (ticket == null || chunk == null) {
            return;
        }
        if (ticket.getType() == ForgeChunkManager.Type.ENTITY && ticket.getEntity() == null) {
            throw new RuntimeException("Attempted to use an entity ticket to force a chunk, without an entity");
        }
        if (ticket.isPlayerTicket() ? !IForgeChunkManager.getPlayerTickets().containsValue(ticket) :
                !IForgeChunkManager.getTickets().get(ticket.world).containsEntry(ticket.getModId(), ticket)) {
            FMLLog.log.fatal("The mod {} attempted to force load a chunk with an invalid ticket. This is not permitted.", ticket.getModId());
            return;
        }
        ((ICubicTicketInternal) ticket).addRequestedCube(chunk);
        Cube cube = (Cube) ((ICubicWorld) ticket.world).getCubeFromCubeCoords(chunk);
        cube.getTickets().add((ICubicTicketInternal) ticket);
        MinecraftForge.EVENT_BUS.post(new ForceCubeEvent(ticket, chunk));

        if (((ICubicTicketInternal) ticket).getMaxCubeDepth() > 0
                && ((ICubicTicketInternal) ticket).requestedCubes().size() > ((ICubicTicketInternal) ticket).getMaxCubeDepth()) {
            CubePos removed = ((ICubicTicketInternal) ticket).requestedCubes().iterator().next();
            unforceChunk(ticket, removed);
        }
    }

    /**
     * Reorganize the internal chunk list so that the chunk supplied is at the *end* of the list
     * This helps if you wish to guarantee a certain "automatic unload ordering" for the chunks
     * in the ticket list
     *
     * @param ticket The ticket holding the chunk list
     * @param chunk The chunk you wish to push to the end (so that it would be unloaded last)
     */
    public static void reorderChunk(ForgeChunkManager.Ticket ticket, CubePos chunk) {
        if (ticket == null || chunk == null || !((ICubicTicketInternal) ticket).requestedCubes().contains(chunk)) {
            return;
        }
        ((ICubicTicketInternal) ticket).removeRequestedCube(chunk);
        ((ICubicTicketInternal) ticket).addRequestedCube(chunk);
    }

    /**
     * Unforce the supplied chunk, allowing it to be unloaded and stop ticking.
     *
     * @param ticket The ticket holding the chunk
     * @param chunk The chunk to unforce
     */
    public static void unforceChunk(ForgeChunkManager.Ticket ticket, CubePos chunk) {
        if (ticket == null || chunk == null) {
            return;
        }
        ((ICubicTicketInternal) ticket).removeRequestedCube(chunk);
        MinecraftForge.EVENT_BUS.post(new UnforceCubeEvent(ticket, chunk));
        Cube cube = (Cube) ((ICubicWorld) ticket.world).getCubeFromCubeCoords(chunk);
        cube.getTickets().remove((ICubicTicketInternal) ticket);
    }

    // internals

    private static ModContainer getContainer(Object mod) {
        ModContainer container = Loader.instance().getModObjectList().inverse().get(mod);
        return container;
    }

    public static ForgeChunkManager.Ticket makeTicket(String str, ForgeChunkManager.Type type, World world) {
        try {
            return cast(ticketConstructor.invoke(str, type, world));
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public static void onDeserializeTicket(NBTTagCompound ticketNBT, ForgeChunkManager.Ticket ticket) {
        NBTTagCompound cubicNBT = ticketNBT.getCompoundTag("cubicchunks");
        if (cubicNBT == null) {
            return;
        }
        int entityCubeY = cubicNBT.getInteger("entityCubeY");
        Map<ChunkPos, IntSet> coordsMap = new HashMap<>();

        NBTTagList chunkMap = cubicNBT.getTagList("chunkMap", Constants.NBT.TAG_COMPOUND);
        for (NBTBase entryTagBase : chunkMap) {
            NBTTagCompound entry = (NBTTagCompound) entryTagBase;
            int x = entry.getInteger("x");
            int z = entry.getInteger("z");
            IntSet cubes = new IntArraySet(entry.getIntArray("cubes"));
            coordsMap.put(new ChunkPos(x, z), cubes);
        }
        // only store the cube Y coords, don't load
        // mods will request a column to be loaded on loading callback and then we will force the cubes
        ((ICubicTicketInternal) ticket).setAllForcedChunkCubes(coordsMap);
    }

    public static void onSerializeTicket(NBTTagCompound ticket, ForgeChunkManager.Ticket tick) {
        if (((ICubicTicketInternal) tick).getAllForcedChunkCubes().isEmpty()) {
            return;
        }
        NBTTagCompound cubicNBT = new NBTTagCompound();
        cubicNBT.setInteger("entityCubeY", ((ICubicTicketInternal) tick).getEntityChunkY());

        NBTTagList chunkMap = new NBTTagList();
        ((ICubicTicketInternal) tick).getAllForcedChunkCubes().forEach((pos, cubes) -> {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setInteger("x", pos.x);
            entry.setInteger("z", pos.z);
            entry.setIntArray("cubes", cubes.toIntArray());
            chunkMap.appendTag(entry);
        });
        cubicNBT.setTag("chunkMap", chunkMap);

        ticket.setTag("cubicchunks", cubicNBT);
    }

    public static void onLoadEntityTicketChunk(World world, ForgeChunkManager.Ticket tick) {
        if (((ICubicWorld) world).isCubicWorld()) {
            ICubicTicketInternal ticket = (ICubicTicketInternal) tick;
            ((ICubicWorld) world).getCubeFromCubeCoords(ticket.getEntityChunkX(), ticket.getEntityChunkY(), ticket.getEntityChunkZ());
        }
    }

    @SubscribeEvent public static void onForgeChunkManagerForceChunk(ForgeChunkManager.ForceChunkEvent event) {
        ForgeChunkManager.Ticket ticket = event.getTicket();
        World worldInstance = ticket.world;
        if (!((ICubicWorld) worldInstance).isCubicWorld() || !(worldInstance instanceof WorldServer)) {
            return;
        }
        addForcedCubesHeuristic(event, ticket, (WorldServer) worldInstance);
    }

    private static void addForcedCubesHeuristic(ForgeChunkManager.ForceChunkEvent event, ForgeChunkManager.Ticket ticket, WorldServer worldInstance) {
        IntSet yCoords = ((ICubicTicketInternal) ticket).getAllForcedChunkCubes().get(event.getLocation());
        if (yCoords != null && !yCoords.isEmpty()) {
            yCoords.forEach(cubeY ->
                    ((ICubicWorldInternal) ticket.world)
                            .getCubeFromCubeCoords(event.getLocation().x, cubeY, event.getLocation().z)
                            .getTickets().add((ITicket) ticket)
            );
            return;
        }
        WorldServer world = worldInstance;
        PlayerCubeMap cubeMap = (PlayerCubeMap) world.getPlayerChunkMap();
        PlayerChunkMapEntry columnWatcher = cubeMap.getEntry(event.getLocation().x, event.getLocation().z);

        if (columnWatcher == null) {
            ((ICubicTicketInternal) ticket).setForcedChunkCubes(event.getLocation(), new IntArraySet());
            return; // TODO: some different heuristic?
        }
        List<EntityPlayerMP> players = columnWatcher.getWatchingPlayers();
        int verticalViewDistance = CubicChunksConfig.verticalCubeLoadDistance;
        if (yCoords == null) {
            yCoords = new IntArraySet(players.size() * verticalViewDistance * 3);
        }
        for (EntityPlayerMP player : players) {
            for (int dy = -verticalViewDistance; dy <= verticalViewDistance; dy++) {
                int cubeY = Coords.getCubeYForEntity(player) + dy;
                Cube cube = (Cube) ((ICubicWorld) world).getCubeFromCubeCoords(event.getLocation().x, cubeY, event.getLocation().z);
                cube.getTickets().add((ITicket) ticket);
                yCoords.add(cubeY);
            }
        }
        ((ICubicTicketInternal) ticket).setForcedChunkCubes(event.getLocation(), yCoords);
    }

    @SubscribeEvent public static void onForgeChunkManagerUnforceChunk(ForgeChunkManager.UnforceChunkEvent event) {
        ForgeChunkManager.Ticket ticket = event.getTicket();
        World world = ticket.world;
        if (!((ICubicWorld) world).isCubicWorld()) {
            return;
        }
        for (int cubeY : ((ICubicTicketInternal) ticket).getAllForcedChunkCubes().get(event.getLocation())) {
            Cube cube = (Cube) ((ICubicWorld) world).getCubeFromCubeCoords(event.getLocation().x, cubeY, event.getLocation().z);
            cube.getTickets().remove((ITicket) ticket);
        }
        ((ICubicTicketInternal) ticket).clearForcedChunkCubes(event.getLocation());
    }

    public static int getCubeDepthFor(String modId) {
        return CubicChunksConfig.modMaxCubesPerChunkloadingTicket.getOrDefault(modId, CubicChunksConfig.defaultMaxCubesPerChunkloadingTicket);
    }
}
