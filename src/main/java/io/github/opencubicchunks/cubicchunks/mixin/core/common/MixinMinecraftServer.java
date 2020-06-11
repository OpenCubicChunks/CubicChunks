package io.github.opencubicchunks.cubicchunks.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.chunk.ICube;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeStatusListener;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.server.IServerChunkProvider;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.ForcedCubesSaveData;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.ForcedChunksSaveData;
import net.minecraft.world.chunk.listener.IChunkStatusListener;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {

    @Shadow
    protected long serverTime = Util.milliTime();

    @Shadow
    private boolean isRunningScheduledTasks;

    @Shadow @Final private static Logger LOGGER;

    @Shadow protected abstract void setUserMessage(ITextComponent userMessageIn);

    @Shadow public abstract boolean isServerRunning();

    @Shadow public abstract ServerWorld getWorld(DimensionType dimension);

    @Shadow protected abstract void runScheduledTasks();

    /**
     * @author NotStirred
     * @reason Additional CC functionality and logging.
     */
    @Overwrite
    protected void loadInitialChunks(IChunkStatusListener statusListener) {
        this.setUserMessage(new TranslationTextComponent("menu.generatingTerrain"));
        ServerWorld serverworld = this.getWorld(DimensionType.OVERWORLD);
        LOGGER.info("Preparing start region for dimension " + DimensionType.getKey(serverworld.dimension.getType()));
        BlockPos spawnPos = serverworld.getSpawnPoint();
        CubePos spawnPosCube = CubePos.from(spawnPos);

        statusListener.start(new ChunkPos(spawnPos));
        ((ICubeStatusListener) statusListener).startCubes(spawnPosCube);

        ServerChunkProvider serverchunkprovider = serverworld.getChunkProvider();
        serverchunkprovider.getLightManager().func_215598_a(500);
        this.serverTime = Util.milliTime();
        int radius = (int) Math.ceil(10 * (16 / (float)ICube.BLOCK_SIZE)); //vanilla is 10, 32: 5, 64: 3
        int chunkDiameter = Coords.cubeToSection(radius, 0) * 2 + 1;
        int d = radius*2+1;
        ((IServerChunkProvider)serverchunkprovider).registerTicket(TicketType.START, spawnPosCube, radius + 1, Unit.INSTANCE);
        serverchunkprovider.registerTicket(TicketType.START, spawnPosCube.asChunkPos(), Coords.cubeToSection(radius + 1, 0), Unit.INSTANCE);

        int i2 = 0;
        while(isServerRunning() && (serverchunkprovider.getLoadedChunksCount() < chunkDiameter * chunkDiameter || ((IServerChunkProvider) serverchunkprovider).getCubeLoadCounter() < d*d*d)) {
            // from CC
            this.serverTime = Util.milliTime() + 10L;
            this.runScheduledTasks();

            if (i2 == 100) {
                LOGGER.info("Current loaded chunks: " + serverchunkprovider.getLoadedChunksCount() + " | " + ((IServerChunkProvider)serverchunkprovider).getCubeLoadCounter());
                i2 = 0;
            }

            i2++;
        }
        LOGGER.info("Current loaded chunks: " + serverchunkprovider.getLoadedChunksCount() + " | " + ((IServerChunkProvider)serverchunkprovider).getCubeLoadCounter());
        this.serverTime = Util.milliTime() + 10L;
        this.runScheduledTasks();

        for(DimensionType dimensiontype : DimensionType.getAll()) {
            ForcedChunksSaveData forcedchunkssavedata = this.getWorld(dimensiontype).getSavedData().get(ForcedChunksSaveData::new, "chunks");
            ForcedCubesSaveData forcedcubessavedata = this.getWorld(dimensiontype).getSavedData().get(ForcedCubesSaveData::new, "cubes");
            if (forcedchunkssavedata != null) {
                ServerWorld serverworld1 = this.getWorld(dimensiontype);
                LongIterator longiteratorChunks = forcedchunkssavedata.getChunks().iterator();
                LongIterator longiteratorCubes = forcedcubessavedata.getCubes().iterator();

                while(longiteratorChunks.hasNext()) {
                    long i = longiteratorChunks.nextLong();
                    ChunkPos chunkPos = new ChunkPos(i);
                    serverworld1.getChunkProvider().forceChunk(chunkPos, true);
                }
                while(longiteratorCubes.hasNext()) {
                    long i = longiteratorCubes.nextLong();
                    CubePos cubePos = CubePos.from(i);
                    ((IServerChunkProvider)serverworld1).forceCube(cubePos, true);
                }
            }
        }
        this.serverTime = Util.milliTime() + 10L;
        this.runScheduledTasks();
        statusListener.stop();
        serverchunkprovider.getLightManager().func_215598_a(5);
    }
}
