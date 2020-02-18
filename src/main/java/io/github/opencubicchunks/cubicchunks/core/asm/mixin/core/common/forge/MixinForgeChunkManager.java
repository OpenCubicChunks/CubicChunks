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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.forge;

import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import io.github.opencubicchunks.cubicchunks.core.world.chunkloader.CubicChunkManager;
import io.github.opencubicchunks.cubicchunks.core.world.chunkloader.ICubicTicketInternal;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Loader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(ForgeChunkManager.class)
public abstract class MixinForgeChunkManager {

    @Shadow(remap = false) private static Map<World, Multimap<String, ForgeChunkManager.Ticket>> tickets;

    @Shadow(remap = false) private static Map<World, ImmutableSetMultimap<ChunkPos, ForgeChunkManager.Ticket>> forcedChunks;

    @Shadow(remap = false) private static Map<String, ForgeChunkManager.LoadingCallback> callbacks;

    @Shadow(remap = false) private static BiMap<UUID, ForgeChunkManager.Ticket> pendingEntities;

    @Shadow(remap = false) public static int getMaxTicketLengthFor(String modId) {
        throw new Error("WTF!?");
    }

    @Shadow(remap = false) private static int dormantChunkCacheSize;

    // ChunkEntry is package-private
    @Shadow(remap = false) private static Map<Object, Object>/*<World, Cache<Long, ForgeChunkManager.ChunkEntry>>*/ dormantChunkCache;

    @Shadow(remap = false) private static SetMultimap<String, ForgeChunkManager.Ticket> playerTickets;

    /**
     * @author Barteks2x
     * @reason Add cubic chunks hooks
     */
    @Overwrite(remap = false)
    static void loadWorld(World world) {
        ArrayListMultimap<String, ForgeChunkManager.Ticket> newTickets = ArrayListMultimap.create();
        tickets.put(world, newTickets);

        forcedChunks.put(world, ImmutableSetMultimap.of());

        if (!(world instanceof WorldServer)) {
            return;
        }

        if (dormantChunkCacheSize != 0) { // only put into cache if we're using dormant chunk caching
            dormantChunkCache.put(world, CacheBuilder.newBuilder().maximumSize(dormantChunkCacheSize).build());
        }
        WorldServer worldServer = (WorldServer) world;
        File chunkDir = worldServer.getChunkSaveLocation();
        File chunkLoaderData = new File(chunkDir, "forcedchunks.dat");

        if (chunkLoaderData.exists() && chunkLoaderData.isFile()) {
            ArrayListMultimap<String, ForgeChunkManager.Ticket> loadedTickets = ArrayListMultimap.create();
            Map<String, ListMultimap<String, ForgeChunkManager.Ticket>> playerLoadedTickets = Maps.newHashMap();
            NBTTagCompound forcedChunkData;
            try {
                forcedChunkData = CompressedStreamTools.read(chunkLoaderData);
            } catch (IOException e) {
                FMLLog.log.warn("Unable to read forced chunk data at {} - it will be ignored", chunkLoaderData.getAbsolutePath(), e);
                return;
            }
            NBTTagList ticketList = forcedChunkData.getTagList("TicketList", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < ticketList.tagCount(); i++) {
                NBTTagCompound ticketHolder = ticketList.getCompoundTagAt(i);
                String modId = ticketHolder.getString("Owner");
                boolean isPlayer = ForgeVersion.MOD_ID.equals(modId);

                if (!isPlayer && !Loader.isModLoaded(modId)) {
                    FMLLog.log.warn("Found chunkloading data for mod {} which is currently not available or active - it will be removed from the "
                            + "world save", modId);
                    continue;
                }

                if (!isPlayer && !callbacks.containsKey(modId)) {
                    FMLLog.log.warn("The mod {} has registered persistent chunkloading data but doesn't seem to want to be called back with it - it "
                            + "will be removed from the world save", modId);
                    continue;
                }

                NBTTagList tickets = ticketHolder.getTagList("Tickets", Constants.NBT.TAG_COMPOUND);
                for (int j = 0; j < tickets.tagCount(); j++) {
                    NBTTagCompound ticket = tickets.getCompoundTagAt(j);
                    modId = ticket.hasKey("ModId") ? ticket.getString("ModId") : modId;
                    ForgeChunkManager.Type type = ForgeChunkManager.Type.values()[ticket.getByte("Type")];
                    //byte ticketChunkDepth = ticket.getByte("ChunkListDepth");
                    ForgeChunkManager.Ticket tick = CubicChunkManager.makeTicket(modId, type, world);
                    CubicChunkManager.onDeserializeTicket(ticket, tick); // CubicChunks - read cubic chunks ticket data
                    if (ticket.hasKey("ModData")) {
                        ((ICubicTicketInternal) tick).setModData(ticket.getCompoundTag("ModData"));
                    }
                    if (ticket.hasKey("Player")) {
                        ((ICubicTicketInternal) tick).setPlayer(ticket.getString("Player"));
                        if (!playerLoadedTickets.containsKey(tick.getModId())) {
                            playerLoadedTickets.put(modId, ArrayListMultimap.create());
                        }
                        playerLoadedTickets.get(tick.getModId()).put(tick.getPlayerName(), tick);
                    } else {
                        loadedTickets.put(modId, tick);
                    }
                    if (type == ForgeChunkManager.Type.ENTITY) {
                        ((ICubicTicketInternal) tick).setEntityChunkX(ticket.getInteger("chunkX"));
                        ((ICubicTicketInternal) tick).setEntityChunkZ(ticket.getInteger("chunkZ"));
                        UUID uuid = new UUID(ticket.getLong("PersistentIDMSB"), ticket.getLong("PersistentIDLSB"));
                        // add the ticket to the "pending entity" list
                        pendingEntities.put(uuid, tick);
                    }
                }
            }

            for (ForgeChunkManager.Ticket tick : ImmutableSet.copyOf(pendingEntities.values())) {
                if (tick.getType() == ForgeChunkManager.Type.ENTITY && tick.getEntity() == null) {
                    // force the world to load the entity's chunk
                    // the load will come back through the loadEntity method and attach the entity
                    // to the ticket
                    world.getChunk(((ICubicTicketInternal) tick).getEntityChunkX(), ((ICubicTicketInternal) tick).getEntityChunkZ());
                    CubicChunkManager.onLoadEntityTicketChunk(world, tick); // CubicChunks - load entity cube
                }
            }
            for (ForgeChunkManager.Ticket tick : ImmutableSet.copyOf(pendingEntities.values())) {
                if (tick.getType() == ForgeChunkManager.Type.ENTITY && tick.getEntity() == null) {
                    FMLLog.log.warn("Failed to load persistent chunkloading entity {} from store.", pendingEntities.inverse().get(tick));
                    loadedTickets.remove(tick.getModId(), tick);
                }
            }
            pendingEntities.clear();
            // send callbacks
            for (String modId : loadedTickets.keySet()) {
                ForgeChunkManager.LoadingCallback loadingCallback = callbacks.get(modId);
                if (loadingCallback == null) {
                    continue;
                }
                int maxTicketLength = getMaxTicketLengthFor(modId);
                List<ForgeChunkManager.Ticket> tickets = loadedTickets.get(modId);
                if (loadingCallback instanceof ForgeChunkManager.OrderedLoadingCallback) {
                    ForgeChunkManager.OrderedLoadingCallback orderedLoadingCallback = (ForgeChunkManager.OrderedLoadingCallback) loadingCallback;
                    tickets = orderedLoadingCallback.ticketsLoaded(ImmutableList.copyOf(tickets), world, maxTicketLength);
                }
                if (tickets.size() > maxTicketLength) {
                    FMLLog.log.warn("The mod {} has too many open chunkloading tickets {}. Excess will be dropped", modId, tickets.size());
                    tickets.subList(maxTicketLength, tickets.size()).clear();
                }
                MixinForgeChunkManager.tickets.get(world).putAll(modId, tickets);
                loadingCallback.ticketsLoaded(ImmutableList.copyOf(tickets), world);
            }
            for (String modId : playerLoadedTickets.keySet()) {
                ForgeChunkManager.LoadingCallback loadingCallback = callbacks.get(modId);
                if (loadingCallback == null) {
                    continue;
                }
                ListMultimap<String, ForgeChunkManager.Ticket> tickets = playerLoadedTickets.get(modId);
                if (loadingCallback instanceof ForgeChunkManager.PlayerOrderedLoadingCallback) {
                    ForgeChunkManager.PlayerOrderedLoadingCallback orderedLoadingCallback =
                            (ForgeChunkManager.PlayerOrderedLoadingCallback) loadingCallback;
                    tickets = orderedLoadingCallback.playerTicketsLoaded(ImmutableListMultimap.copyOf(tickets), world);
                    playerTickets.putAll(tickets);
                }
                MixinForgeChunkManager.tickets.get(world).putAll(ForgeVersion.MOD_ID, tickets.values());
                loadingCallback.ticketsLoaded(ImmutableList.copyOf(tickets.values()), world);
            }
        }
    }


    @Inject(
            method = "saveWorld",
            at = @At(value = "CONSTANT", args = "stringValue=ChunkListDepth"),
            locals = LocalCapture.CAPTURE_FAILHARD,
            remap = false
    )
    private static void onSaveTicket(World world, CallbackInfo ci,
            WorldServer worldServer,
            File chunkDir,
            File chunkLoaderData,
            NBTTagCompound forcedChunkData,
            NBTTagList ticketList,
            Multimap<String, ForgeChunkManager.Ticket> ticketSet,
            Iterator<String> var7,
            String modId,
            NBTTagCompound ticketHolder,
            NBTTagList tickets,
            Iterator<ForgeChunkManager.Ticket> var11,
            ForgeChunkManager.Ticket tick,
            NBTTagCompound ticket) {
        CubicChunkManager.onSerializeTicket(ticket, tick);
    }
}
