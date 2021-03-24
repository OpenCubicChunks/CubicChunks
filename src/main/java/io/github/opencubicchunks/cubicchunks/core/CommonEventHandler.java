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
package io.github.opencubicchunks.cubicchunks.core;

import com.google.common.collect.ImmutableList;
import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorldType;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldInternal;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.ICubicWorldSettings;
import io.github.opencubicchunks.cubicchunks.core.network.PacketCubicWorldData;
import io.github.opencubicchunks.cubicchunks.core.network.PacketDispatcher;
import io.github.opencubicchunks.cubicchunks.core.server.SpawnCubes;
import io.github.opencubicchunks.cubicchunks.core.server.VanillaNetworkHandler;
import io.github.opencubicchunks.cubicchunks.core.server.chunkio.ICubeIO;
import io.github.opencubicchunks.cubicchunks.core.util.ReflectionUtil;
import io.github.opencubicchunks.cubicchunks.core.world.ICubeProviderInternal;
import io.github.opencubicchunks.cubicchunks.core.world.WorldSavedCubicChunksData;
import io.github.opencubicchunks.cubicchunks.core.world.provider.ICubicWorldProvider;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldServerMulti;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CommonEventHandler {
    @SubscribeEvent // this event is fired early enough to replace world with cubic chunks without any issues
    public void onWorldAttachCapabilities(AttachCapabilitiesEvent<World> evt) {
        if (evt.getObject().isRemote || !(evt.getObject() instanceof WorldServer)) {
            return; // we will send packet to the client when it joins, client shouldn't change world types as it wants
        }
        WorldServer world = (WorldServer) evt.getObject();

        WorldSavedCubicChunksData savedData =
                (WorldSavedCubicChunksData) evt.getObject().getPerWorldStorage().getOrLoadData(WorldSavedCubicChunksData.class, "cubicChunksData");
        boolean ccWorldType = evt.getObject().getWorldType() instanceof ICubicWorldType;
        boolean ccGenerator = ccWorldType && ((ICubicWorldType) evt.getObject().getWorldType()).hasCubicGeneratorForWorld(evt.getObject());
        boolean savedCC = savedData != null && savedData.isCubicChunks;
        boolean ccWorldInfo = ((ICubicWorldSettings) world.getWorldInfo()).isCubic() && (savedData == null || savedData.isCubicChunks);
        boolean excludeCC = CubicChunksConfig.isDimensionExcluded(evt.getObject().provider.getDimension());
        boolean forceExclusions = CubicChunksConfig.forceDimensionExcludes;
        // TODO: simplify this mess of booleans and document where each of them comes from
        // these espressions are generated using Quine McCluskey algorithm
        // using the JQM v1.2.0 (Java QuineMcCluskey) program:
        // IS_CC := CC_GEN OR CC_TYPE AND NOT(EXCLUDED) OR SAVED_CC AND NOT(EXCLUDED) OR SAVED_CC AND NOT(F_EX) OR CC_NEW AND NOT(EXCLUDED);
        // ERROR := CC_GEN AND NOT(CC_TYPE);
        boolean impossible = ccGenerator && !ccWorldType;
        if (impossible) {
            throw new Error("Trying to use cubic chunks generator without cubic chunks world type.");
        }
        boolean isCC = ccGenerator
                        || (ccWorldType && !excludeCC)
                        || (savedCC && !excludeCC)
                        || (savedCC && !forceExclusions)
                        || (ccWorldInfo && !excludeCC);
        if ((CubicChunksConfig.forceLoadCubicChunks == CubicChunksConfig.ForceCCMode.LOAD_NOT_EXCLUDED && !excludeCC)
            || CubicChunksConfig.forceLoadCubicChunks == CubicChunksConfig.ForceCCMode.ALWAYS) {
            isCC = true;
        }

        if (savedData == null) {
            int minY = CubicChunksConfig.defaultMinHeight;
            int maxY = CubicChunksConfig.defaultMaxHeight;
            if (world.provider.getDimension() != 0) {
                WorldSavedCubicChunksData overworld = (WorldSavedCubicChunksData) DimensionManager
                        .getWorld(0).getPerWorldStorage().getOrLoadData(WorldSavedCubicChunksData.class, "cubicChunksData");
                if (overworld != null) {
                    minY = overworld.minHeight;
                    maxY = overworld.maxHeight;
                }
            }
            savedData = new WorldSavedCubicChunksData("cubicChunksData", isCC, minY, maxY);
        }
        savedData.markDirty();
        evt.getObject().getPerWorldStorage().setData("cubicChunksData", savedData);
        evt.getObject().getPerWorldStorage().saveAllData();

        if (!isCC) {
            return;
        }

        if (shouldSkipWorld(world)) {
            CubicChunks.LOGGER.info("Skipping world " + evt.getObject() + " with type " + evt.getObject().getWorldType() + " due to potential "
                    + "compatibility issues");
            return;
        }
        CubicChunks.LOGGER.info("Initializing world " + evt.getObject() + " with type " + evt.getObject().getWorldType());

        IntRange generationRange = new IntRange(0, ((ICubicWorldProvider) world.provider).getOriginalActualHeight());
        WorldType type = evt.getObject().getWorldType();
        if (type instanceof ICubicWorldType && ((ICubicWorldType) type).hasCubicGeneratorForWorld(world)) {
            generationRange = ((ICubicWorldType) type).calculateGenerationHeightRange(world);
        }

        int minHeight = savedData.minHeight;
        int maxHeight = savedData.maxHeight;
        ((ICubicWorldInternal.Server) world).initCubicWorldServer(new IntRange(minHeight, maxHeight), generationRange);
    }

    @SubscribeEvent
    public void onWorldServerTick(TickEvent.WorldTickEvent evt) {
        WorldServer world = (WorldServer) evt.world;
        //Forge (at least version 11.14.3.1521) doesn't call this event for client world.
        if (evt.phase == TickEvent.Phase.END && ((ICubicWorld) world).isCubicWorld() && evt.side == Side.SERVER) {
            ((ICubicWorldInternal) world).tickCubicWorld();
        }
    }

    @SubscribeEvent
    public void onPlayerJoinWorld(EntityJoinWorldEvent evt) {
        if (evt.getEntity() instanceof EntityPlayerMP && ((ICubicWorld) evt.getWorld()).isCubicWorld()) {
            PacketDispatcher.sendTo(new PacketCubicWorldData((WorldServer) evt.getWorld()), (EntityPlayerMP) evt.getEntity());
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        VanillaNetworkHandler.removeBedrockPlayer((EntityPlayerMP) event.player);
    }

    @SuppressWarnings("unchecked")
    private final List<Class<?>> allowedServerWorldClasses = ImmutableList.copyOf(new Class[]{
            WorldServer.class,
            WorldServerMulti.class,
            // non-existing classes will be Objects
            ReflectionUtil.getClassOrDefault("WorldServerOF", Object.class), // OptiFine's WorldServer, no package
            ReflectionUtil.getClassOrDefault("WorldServerMultiOF", Object.class), // OptiFine's WorldServerMulti, no package
            ReflectionUtil.getClassOrDefault("net.optifine.override.WorldServerOF", Object.class), // OptiFine's WorldServer
            ReflectionUtil.getClassOrDefault("net.optifine.override.WorldServerMultiOF", Object.class), // OptiFine's WorldServerMulti
            ReflectionUtil.getClassOrDefault("com.forgeessentials.multiworld.WorldServerMultiworld", Object.class) // ForgeEssentials world
    });

    @SuppressWarnings("unchecked")
    private final List<Class<? extends IChunkProvider>> allowedServerChunkProviderClasses = ImmutableList.copyOf(new Class[]{
            ChunkProviderServer.class
    });

    private boolean shouldSkipWorld(World world) {
        return !allowedServerWorldClasses.contains(world.getClass())
                || !allowedServerChunkProviderClasses.contains(world.getChunkProvider().getClass());
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote || !((ICubicWorld) event.getWorld()).isCubicWorld()) {
            return;
        }

        ICubicWorld world = (ICubicWorld) event.getWorld();
        if (!world.isCubicWorld()) {
            return;
        }

        ICubeIO io = ((ICubeProviderInternal.Server) world.getCubeCache()).getCubeIO();
        try {
            io.close();
        } catch (IOException e) {
            CubicChunks.LOGGER.catching(e);
        }
    }
}
