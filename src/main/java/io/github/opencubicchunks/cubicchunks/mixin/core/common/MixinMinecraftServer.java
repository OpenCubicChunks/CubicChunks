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

    @Shadow @Final private static Logger LOGGER;
    @Shadow protected long nextTickTime;
    @Shadow @Final private Map<RegistryKey<World>, ServerWorld> levels;
    @Shadow protected abstract void waitUntilNextTick();
    @Shadow public abstract ServerWorld overworld();
    @Shadow public abstract boolean isRunning();

    /**
     * @author NotStirred
     * @reason Additional CC functionality and logging.
     */
    @Overwrite
    private void prepareLevels(IChunkStatusListener statusListener) {
        ServerWorld serverworld = this.overworld();
        LOGGER.info("Preparing start region for dimension {}", serverworld.dimension().location());
        BlockPos spawnPos = serverworld.getSharedSpawnPos();
        CubePos spawnPosCube = CubePos.from(spawnPos);

        statusListener.updateSpawnPos(new ChunkPos(spawnPos));
        ((ICubeStatusListener) statusListener).startCubes(spawnPosCube);

        ServerChunkProvider serverchunkprovider = serverworld.getChunkSource();
        serverchunkprovider.getLightEngine().setTaskPerBatch(500);
        this.nextTickTime = Util.getMillis();
        int radius = (int) Math.ceil(10 * (16 / (float) IBigCube.DIAMETER_IN_BLOCKS)); //vanilla is 10, 32: 5, 64: 3
        int chunkDiameter = Coords.cubeToSection(radius, 0) * 2 + 1;
        int d = radius*2+1;
        ((IServerChunkProvider)serverchunkprovider).addCubeRegionTicket(TicketType.START, spawnPosCube, radius + 1, Unit.INSTANCE);
        serverchunkprovider.addRegionTicket(TicketType.START, spawnPosCube.asChunkPos(), Coords.cubeToSection(radius + 1, 0), Unit.INSTANCE);

        while(this.isRunning() && (serverchunkprovider.getTickingGenerated() < chunkDiameter * chunkDiameter || ((IServerChunkProvider) serverchunkprovider).getTickingGeneratedCubes() < d*d*d)) {
            // from CC
            this.nextTickTime = Util.getMillis() + 10L;
            this.waitUntilNextTick();
        }
        LOGGER.info("Current loaded chunks: " + serverchunkprovider.getTickingGenerated() + " | " + ((IServerChunkProvider)serverchunkprovider).getTickingGeneratedCubes());
        this.nextTickTime = Util.getMillis() + 10L;
        this.waitUntilNextTick();

        for(ServerWorld serverworld1 : this.levels.values()) {
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
        this.nextTickTime = Util.getMillis() + 10L;
        this.waitUntilNextTick();
        statusListener.stop();
        serverchunkprovider.getLightEngine().setTaskPerBatch(5);
    }
}