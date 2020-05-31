package cubicchunks.cc.mixin.core.common;

import cubicchunks.cc.chunk.ICubeStatusListener;
import cubicchunks.cc.chunk.util.CubePos;
import cubicchunks.cc.server.IServerChunkProvider;
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
import net.minecraft.world.server.*;
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

    /**
     * @author NotStirred
     * @reason Additional CC functionality and logging.
     */
    @Overwrite
    protected void loadInitialChunks(IChunkStatusListener statusListener) {
        this.setUserMessage(new TranslationTextComponent("menu.generatingTerrain"));
        ServerWorld serverworld = ((IMinecraftServer)this).getServerWorld(DimensionType.OVERWORLD);
        LOGGER.info("Preparing start region for dimension " + DimensionType.getKey(serverworld.dimension.getType()));
        BlockPos spawnPos = serverworld.getSpawnPoint();
        CubePos spawnPosCube = CubePos.from(spawnPos);

        statusListener.start(new ChunkPos(spawnPos));
        ((ICubeStatusListener) statusListener).startCubes(spawnPosCube);

        ServerChunkProvider serverchunkprovider = serverworld.getChunkProvider();
        serverchunkprovider.getLightManager().func_215598_a(500);
        this.serverTime = Util.milliTime();
        int radius = 5;
        int d = radius*2+1;
        ((IServerChunkProvider)serverchunkprovider).registerTicket(TicketType.START, spawnPosCube, radius + 1, Unit.INSTANCE);

        int i2 = 0;
        while(isServerRunning() && (serverchunkprovider.getLoadedChunksCount() != d*d || ((IServerChunkProvider) serverchunkprovider).getLoadedCubesCount() != d*d*d)) {
            // from CC
            this.serverTime = Util.milliTime() + 10L;
            ((IMinecraftServer)this).runSchedule();

            if (i2 == 100) {
                LOGGER.info("Current loaded chunks: " + serverchunkprovider.getLoadedChunksCount() + " | " + ((IServerChunkProvider)serverchunkprovider).getLoadedCubesCount());
                i2 = 0;
            }

            i2++;
        }
        LOGGER.info("Current loaded chunks: " + serverchunkprovider.getLoadedChunksCount() + " | " + ((IServerChunkProvider)serverchunkprovider).getLoadedCubesCount());
        this.serverTime = Util.milliTime() + 10L;
        ((IMinecraftServer)this).runSchedule();

        for(DimensionType dimensiontype : DimensionType.getAll()) {
            ForcedChunksSaveData forcedchunkssavedata = ((IMinecraftServer)this).getServerWorld(dimensiontype).getSavedData().get(ForcedChunksSaveData::new, "chunks");
            if (forcedchunkssavedata != null) {
                ServerWorld serverworld1 = ((IMinecraftServer)this).getServerWorld(dimensiontype);
                LongIterator longiterator = forcedchunkssavedata.getChunks().iterator();

                while(longiterator.hasNext()) {
                    long i = longiterator.nextLong();
                    ChunkPos chunkpos = new ChunkPos(i);
                    serverworld1.getChunkProvider().forceChunk(chunkpos, true);
                }
            }
        }

        this.serverTime = Util.milliTime() + 10L;
        ((IMinecraftServer)this).runSchedule();
        statusListener.stop();
        serverchunkprovider.getLightManager().func_215598_a(5);
    }
}
