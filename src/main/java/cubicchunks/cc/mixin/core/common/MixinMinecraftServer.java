package cubicchunks.cc.mixin.core.common;

import cubicchunks.cc.server.IServerChunkProvider;
import it.unimi.dsi.fastutil.longs.LongIterator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {



    @Shadow
    protected long serverTime = Util.milliTime();

    @Shadow
    private boolean isRunningScheduledTasks;


    @Shadow @Final private static Logger LOGGER;

    /**
     * @author NotStirred
     */
    @Overwrite
    protected void loadInitialChunks(IChunkStatusListener p_213186_1_) {
        //this.setUserMessage(new TranslationTextComponent("menu.generatingTerrain"));
        ServerWorld serverworld = ((IMinecraftServer)this).getServerWorld(DimensionType.OVERWORLD);
        LOGGER.info("Preparing start region for dimension " + DimensionType.getKey(serverworld.dimension.getType()));
        BlockPos blockpos = serverworld.getSpawnPoint();
        p_213186_1_.start(new ChunkPos(blockpos));
        ServerChunkProvider serverchunkprovider = serverworld.getChunkProvider();
        serverchunkprovider.getLightManager().func_215598_a(500);
        this.serverTime = Util.milliTime();
        ((IServerChunkProvider)serverchunkprovider).registerTicket(TicketType.START, SectionPos.from(blockpos), 11, Unit.INSTANCE);

        while(serverchunkprovider.func_217229_b() != 441) {
            this.serverTime = Util.milliTime() + 10L;
            ((IMinecraftServer)this).runSchedule();
            LOGGER.info("Current loaded chunks: " + serverchunkprovider.func_217229_b());
        }

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
        p_213186_1_.stop();
        serverchunkprovider.getLightManager().func_215598_a(5);
    }
}
