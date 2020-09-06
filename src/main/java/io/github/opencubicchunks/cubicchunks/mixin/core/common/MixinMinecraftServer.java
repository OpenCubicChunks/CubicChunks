package io.github.opencubicchunks.cubicchunks.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeStatusListener;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.server.IServerChunkProvider;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.ForcedCubesSaveData;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ForcedChunksSaveData;
import net.minecraft.world.World;
import net.minecraft.world.chunk.listener.IChunkStatusListener;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {

    @Shadow
    protected long serverTime = Util.getMillis();

    @Shadow
    private boolean isRunningScheduledTasks;

    @Shadow @Final private static Logger LOGGER;

    @Shadow public abstract boolean isServerRunning();

    @Shadow protected abstract void runScheduledTasks();

    @Shadow public abstract ServerWorld overworld();

    @Shadow @Final private Map<RegistryKey<World>, ServerWorld> worlds;

    /**
     * @author NotStirred
     * @reason Additional CC functionality and logging.
     */
    @Overwrite
    protected void loadInitialChunks(IChunkStatusListener statusListener) {
        ServerWorld serverworld = this.overworld();
        LOGGER.info("Preparing start region for dimension {}", serverworld.dimension().location());
        BlockPos spawnPos = serverworld.getSharedSpawnPos();
        CubePos spawnPosCube = CubePos.from(spawnPos);

        statusListener.updateSpawnPos(new ChunkPos(spawnPos));
        ((ICubeStatusListener) statusListener).startCubes(spawnPosCube);

        ServerChunkProvider serverchunkprovider = serverworld.getChunkSource();
        serverchunkprovider.getLightEngine().setTaskPerBatch(500);
        this.serverTime = Util.getMillis();
        int radius = (int) Math.ceil(10 * (16 / (float) IBigCube.DIAMETER_IN_BLOCKS)); //vanilla is 10, 32: 5, 64: 3
        int chunkDiameter = Coords.cubeToSection(radius, 0) * 2 + 1;
        int d = radius*2+1;
        ((IServerChunkProvider)serverchunkprovider).registerTicket(TicketType.START, spawnPosCube, radius + 1, Unit.INSTANCE);
        serverchunkprovider.addRegionTicket(TicketType.START, spawnPosCube.asChunkPos(), Coords.cubeToSection(radius + 1, 0), Unit.INSTANCE);

        int i2 = 0;
        while(isServerRunning() && (serverchunkprovider.getTickingGenerated() < chunkDiameter * chunkDiameter || ((IServerChunkProvider) serverchunkprovider).getCubeLoadCounter() < d*d*d)) {
            // from CC
            this.serverTime = Util.getMillis() + 10L;
            this.runScheduledTasks();

            if (i2 == 100) {
                LOGGER.info("Current loaded chunks: " + serverchunkprovider.getTickingGenerated() + " | " + ((IServerChunkProvider)serverchunkprovider).getCubeLoadCounter());
                i2 = 0;
            }

            i2++;
        }
        LOGGER.info("Current loaded chunks: " + serverchunkprovider.getTickingGenerated() + " | " + ((IServerChunkProvider)serverchunkprovider).getCubeLoadCounter());
        this.serverTime = Util.getMillis() + 10L;
        this.runScheduledTasks();

        for(ServerWorld serverworld1 : this.worlds.values()) {
            ForcedChunksSaveData forcedchunkssavedata = serverworld1.getDataStorage().get(ForcedChunksSaveData::new, "chunks");
            ForcedCubesSaveData forcedcubessavedata = serverworld1.getDataStorage().get(ForcedCubesSaveData::new, "cubes");
            if (forcedchunkssavedata != null) {
                LongIterator longiteratorChunks = forcedchunkssavedata.getChunks().iterator();
                LongIterator longiteratorCubes = forcedcubessavedata.getCubes().iterator();

                while(longiteratorChunks.hasNext()) {
                    long i = longiteratorChunks.nextLong();
                    ChunkPos chunkPos = new ChunkPos(i);
                    serverworld1.getChunkSource().updateChunkForced(chunkPos, true);
                }
                while(longiteratorCubes.hasNext()) {
                    long i = longiteratorCubes.nextLong();
                    CubePos cubePos = CubePos.from(i);
                    ((IServerChunkProvider)serverworld1).forceCube(cubePos, true);
                }
            }
        }
        this.serverTime = Util.getMillis() + 10L;
        this.runScheduledTasks();
        statusListener.stop();
        serverchunkprovider.getLightEngine().setTaskPerBatch(5);
    }
}