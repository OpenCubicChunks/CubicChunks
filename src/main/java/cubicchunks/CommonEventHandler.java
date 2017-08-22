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
package cubicchunks;

import cubicchunks.network.PacketDispatcher;
import cubicchunks.network.PacketCubicWorldData;
import cubicchunks.server.SpawnCubes;
import cubicchunks.util.IntRange;
import cubicchunks.util.ReflectionUtil;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.ICubicWorldSettings;
import cubicchunks.world.WorldSavedCubicChunksData;
import cubicchunks.world.provider.ICubicWorldProvider;
import cubicchunks.world.type.ICubicWorldType;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CommonEventHandler {
    @SubscribeEvent // this event is fired early enough to replace world with cubic chunks without any issues
    public void onWorldAttachCapabilities(AttachCapabilitiesEvent<World> evt) {
        if (evt.getObject().isRemote) {
            return; // we will send packet to the client when it joins, client shouldn't change world types as it wants
        }
        ICubicWorldServer world = (ICubicWorldServer) evt.getObject();

        WorldSavedCubicChunksData savedData =
                (WorldSavedCubicChunksData) evt.getObject().getMapStorage().getOrLoadData(WorldSavedCubicChunksData.class, "cubicChunksData");
        boolean forcedCubicChunks = ((ICubicWorldSettings)world.getWorldInfo()).isCubic();
        // if it's our world type - always cubic chunks
        // is stored in WorldSavedData - it's already existing cubic chunks world
        // if forced cubic chunks - cubic chunks has been enabled by user on this world
        if (!(evt.getObject().getWorldType() instanceof ICubicWorldType) && savedData == null && !forcedCubicChunks) {
            return;
        }

        if (shouldSkipWorld((World) world)) {
            CubicChunks.LOGGER.info("Skipping world " + evt.getObject() + " with type " + evt.getObject().getWorldType() + " due to potential "
                    + "compatibility issues");
            return;
        }
        CubicChunks.LOGGER.info("Initializing world " + evt.getObject() + " with type " + evt.getObject().getWorldType());

        IntRange generationRange = new IntRange(0, ((ICubicWorldProvider) world.getProvider()).getOriginalActualHeight());
        WorldType type = evt.getObject().getWorldType();
        if (type instanceof ICubicWorldType) {
            generationRange = ((ICubicWorldType) type).calculateGenerationHeightRange((WorldServer) world);
        }

        if (savedData == null) {
            savedData = new WorldSavedCubicChunksData("cubicChunksData");
        }
        int minHeight = savedData.minHeight;
        int maxHeight = savedData.maxHeight;
        world.initCubicWorldServer(new IntRange(minHeight, maxHeight), generationRange);
        savedData.markDirty();
        evt.getObject().getMapStorage().setData("cubicChunksData", savedData);
        evt.getObject().getMapStorage().saveAllData();
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load evt) {
        if (!((ICubicWorld) evt.getWorld()).isCubicWorld()) {
            return;
        }
        ICubicWorld world = (ICubicWorld) evt.getWorld();

        if (!world.isRemote()) {
            SpawnCubes.update(world);
        }
    }

    @SubscribeEvent
    public void onWorldServerTick(TickEvent.WorldTickEvent evt) {
        ICubicWorldServer world = (ICubicWorldServer) evt.world;
        //Forge (at least version 11.14.3.1521) doesn't call this event for client world.
        if (evt.phase == TickEvent.Phase.END && world.isCubicWorld() && evt.side == Side.SERVER) {
            world.tickCubicWorld();

            if (!world.isRemote()) {
                // There is no event for when the spawn location changes, so check every tick for now
                SpawnCubes.update(world);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerJoinWorld(EntityJoinWorldEvent evt) {
        if (evt.getEntity() instanceof EntityPlayerMP && ((ICubicWorld) evt.getWorld()).isCubicWorld()) {
            PacketDispatcher.sendTo(new PacketCubicWorldData((WorldServer) evt.getWorld()), (EntityPlayerMP) evt.getEntity());
        }
    }

    @SuppressWarnings("unchecked")
    private final List<Class<? extends World>> allowedServerWorldClasses = ImmutableList.copyOf(new Class[]{
            WorldServer.class,
            WorldServerMulti.class,
            // non-existing classes will be Objects
            ReflectionUtil.getClassOrDefault("WorldServerOF", Object.class), // OptiFine's WorldServer, no package
            ReflectionUtil.getClassOrDefault("WorldServerMultiOF", Object.class) // OptiFine's WorldServerMulti, no package
    });

    @SuppressWarnings("unchecked")
    private final List<Class<? extends IChunkProvider>> allowedServerChunkProviderClasses = ImmutableList.copyOf(new Class[]{
            ChunkProviderServer.class
    });

    private boolean shouldSkipWorld(World world) {
        return !allowedServerWorldClasses.contains(world.getClass())
                || !allowedServerChunkProviderClasses.contains(world.getChunkProvider().getClass());
    }
}
