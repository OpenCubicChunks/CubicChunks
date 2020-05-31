package io.github.opencubicchunks.cubicchunks.core.world.server;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.util.concurrent.ThreadTaskExecutor;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.village.PointOfInterestManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ChunkTaskPriorityQueue;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.chunk.IChunkLightProvider;
import net.minecraft.world.chunk.listener.IChunkStatusListener;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.storage.DimensionSavedDataManager;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class CubeManager extends ChunkManager {
    public CubeManager(ServerWorld world, File worldDir, DataFixer dataFix, TemplateManager templateManager,
                       Executor chunkExecutor, ThreadTaskExecutor<Runnable> mainThread, IChunkLightProvider lightProvider,
                       ChunkGenerator<?> worldGen, IChunkStatusListener statusListener,
                       Supplier<DimensionSavedDataManager> getDimensionSavedData, int viewDistance) {
        super(world, worldDir, dataFix, templateManager, chunkExecutor, mainThread, lightProvider,
                worldGen, statusListener, getDimensionSavedData, viewDistance);
    }

    // getLoadedChunk
    @Nullable
    protected ChunkHolder func_219220_a(long chunkPosIn) {
        return super.func_219220_a(chunkPosIn);
    }

    // getLoadedChunkImmutable?
    @Nullable
    protected ChunkHolder func_219219_b(long chunkPosIn) {
        return super.func_219219_b(chunkPosIn);
    }

    // getChunkLevelSupplier?
    protected IntSupplier func_219191_c(long chunkPosIn) {
        return () -> {
            ChunkHolder chunkholder = this.func_219219_b(chunkPosIn);
            return chunkholder == null ? ChunkTaskPriorityQueue.field_219419_a - 1 : Math.min(chunkholder.func_219281_j(), ChunkTaskPriorityQueue.field_219419_a - 1);
        };
    }

    // getChunkDebugString
    public String func_219170_a(ChunkPos pos) {
        return super.func_219170_a(pos);
    }

    // scheduleFullChunkLoad
    public CompletableFuture<Either<Chunk, ChunkHolder.IChunkLoadingError>> func_219188_b(ChunkPos chunkPos) {
        return super.func_219188_b(chunkPos);
    }

    public void close() throws IOException {
        super.close();
    }

    protected void save(boolean flush) {
        super.save(flush);
    }

    protected void tick(BooleanSupplier hasMoreTime) {
        super.tick(hasMoreTime);
    }

    protected boolean refreshOffThreadCache() {
        return super.refreshOffThreadCache();
    }

    // what is this? scheduleLoadChunk?
    public CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> func_219244_a(ChunkHolder chunkHolder, ChunkStatus status) {
        return super.func_219244_a(chunkHolder, status);
    }

    // releaseLightTicket
    protected void func_219209_c(ChunkPos chunkPos) {
        super.func_219209_c(chunkPos);
    }

    // scheduleSendChunkToClients?
    public CompletableFuture<Either<Chunk, ChunkHolder.IChunkLoadingError>> func_219179_a(ChunkHolder chunkHolder) {
        return super.func_219179_a(chunkHolder);
    }

    // scheduleRescheduleTicks???
    public CompletableFuture<Either<Chunk, ChunkHolder.IChunkLoadingError>> func_222961_b(ChunkHolder chunkHolder) {
        return super.func_222961_b(chunkHolder);
    }

    // getSomeCounter..????
    public int func_219174_c() {
        return super.func_219174_c();
    }

    protected void setViewDistance(int viewDistance) {
        super.setViewDistance(viewDistance);
    }

    // sends chunk data to client
    protected void setChunkLoadedAtClient(ServerPlayerEntity player, ChunkPos chunkPosIn, IPacket<?>[] packetCache, boolean wasLoaded, boolean load) {
        super.setChunkLoadedAtClient(player, chunkPosIn, packetCache, wasLoaded, load);
    }

    public int getLoadedChunkCount() {
        return super.getLoadedChunkCount();
    }

    /*
    protected ChunkManager.ProxyTicketManager getTicketManager() {
        return super.getTicketManager();
    }
     */

    protected Iterable<ChunkHolder> getLoadedChunksIterable() {
        return super.getLoadedChunksIterable();
    }

    /*
    // writeDebugCsv?
    void func_225406_a(Writer p_225406_1_) throws IOException {
        super.func_225406_a(p_225406_1_);
    }

    boolean isOutsideSpawningRadius(ChunkPos chunkPosIn) {
        return super.isOutsideSpawningRadius(chunkPosIn);
    }

    void setPlayerTracking(ServerPlayerEntity player, boolean track) {
        super.setPlayerTracking(player, track);
    }
    */

    // PlayerChunkMap.updateMovingPlayer
    public void updatePlayerPosition(ServerPlayerEntity player) {
        super.updatePlayerPosition(player);
    }

    public Stream<ServerPlayerEntity> getTrackingPlayers(ChunkPos pos, boolean boundaryOnly) {
        return super.getTrackingPlayers(pos, boundaryOnly);
    }

    protected void track(Entity entityIn) {
        super.track(entityIn);
    }

    protected void untrack(Entity entity) {
        super.untrack(entity);
    }

    protected void tickEntityTracker() {
        super.tickEntityTracker();
    }

    protected void sendToAllTracking(Entity entity, IPacket<?> packet) {
        super.sendToAllTracking(entity, packet);
    }

    protected void sendToTrackingAndSelf(Entity entity, IPacket<?> packet) {
        super.sendToTrackingAndSelf(entity, packet);
    }

    protected PointOfInterestManager getPointOfInterestManager() {
        return super.getPointOfInterestManager();
    }


    public class ProxyTicketManager extends TicketManager {
        protected ProxyTicketManager(Executor p_i50469_2_, Executor p_i50469_3_) {
            super(p_i50469_2_, p_i50469_3_);
        }

        protected boolean contains(long chunkPos) {
            return CubeManager.this.field_219261_o.contains(chunkPos);
        }

        @Nullable
        protected ChunkHolder func_219335_b(long chunkPosIn) {
            return CubeManager.this.func_219220_a(chunkPosIn);
        }

        @Nullable
        protected ChunkHolder func_219372_a(long p_219372_1_, int p_219372_3_, @Nullable ChunkHolder p_219372_4_, int p_219372_5_) {
            return CubeManager.this.func_219213_a(p_219372_1_, p_219372_3_, p_219372_4_, p_219372_5_);
        }
    }
}