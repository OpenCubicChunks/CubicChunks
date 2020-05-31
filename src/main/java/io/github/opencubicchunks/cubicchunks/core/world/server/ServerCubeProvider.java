package io.github.opencubicchunks.cubicchunks.core.world.server;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.village.PointOfInterestManager;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.chunk.listener.IChunkStatusListener;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.ServerWorldLightManager;
import net.minecraft.world.server.TicketType;
import net.minecraft.world.storage.DimensionSavedDataManager;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class ServerCubeProvider extends ServerChunkProvider {
    public ServerCubeProvider(ServerWorld worldIn, File worldDirectory, DataFixer dataFixer, TemplateManager templateManagerIn,
                              Executor executorIn, ChunkGenerator<?> chunkGeneratorIn, int viewDistance,
                              IChunkStatusListener chunkStatusListener, Supplier<DimensionSavedDataManager> dimensionDataSupplier) {
        super(worldIn, worldDirectory, dataFixer, templateManagerIn, executorIn, chunkGeneratorIn, viewDistance,
                chunkStatusListener, dimensionDataSupplier);
    }

    public ServerWorldLightManager getLightManager() {
        return super.getLightManager();
    }

    // getFullyLoadedChunkCount???
    public int func_217229_b() {
        return super.func_217229_b();
    }

    public IChunk getChunk(int chunkX, int chunkZ, ChunkStatus requiredStatus, boolean load) {
        return super.getChunk(chunkX, chunkZ, requiredStatus, load);
    }

    @Nullable // getChunkNow
    public Chunk func_225313_a(int chunkX, int chunkZ) {
        return super.func_225313_a(chunkX, chunkZ);
    }

    // loadChunk?
    public CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> func_217232_b(
            int chunkX, int chunkZ, ChunkStatus requiredStatus, boolean load) {
        return super.func_217232_b(chunkX, chunkZ, requiredStatus, load);
    }

    public boolean chunkExists(int x, int z) {
        return super.chunkExists(x, z);
    }

    public IBlockReader getChunkForLight(int chunkX, int chunkZ) {
        return super.getChunkForLight(chunkX, chunkZ);
    }

    public boolean isChunkLoaded(Entity entityIn) {
        return super.isChunkLoaded(entityIn);
    }

    public boolean canTick(BlockPos pos) {
        return super.canTick(pos);
    }

    // canTick?
    public boolean func_223435_b(Entity entity) {
        return super.func_223435_b(entity);
    }

    public void save(boolean flush) {
        super.save(flush);
    }

    public void close() throws IOException {
        super.close();
    }

    public void tick(BooleanSupplier hasTimeLeft) {
        super.tick(hasTimeLeft);
    }

    public String makeString() {
        return super.makeString();
    }

    // getQueueSize
    public int func_225314_f() {
        return super.func_225314_f();
    }

    public ChunkGenerator<?> getChunkGenerator() {
        return super.getChunkGenerator();
    }

    public int getLoadedChunkCount() {
        return super.getLoadedChunkCount();
    }

    public void markBlockChanged(BlockPos pos) {
        super.markBlockChanged(pos);
    }

    public void markLightChanged(LightType type, SectionPos pos) {
        super.markLightChanged(type, pos);
    }

    public <T> void registerTicket(TicketType<T> type, ChunkPos pos, int distance, T value) {
        super.registerTicket(type, pos, distance, value);
    }

    public <T> void releaseTicket(TicketType<T> type, ChunkPos pos, int distance, T value) {
        super.releaseTicket(type, pos, distance, value);
    }

    public void forceChunk(ChunkPos pos, boolean add) {
        super.forceChunk(pos, add);
    }

    public void updatePlayerPosition(ServerPlayerEntity player) {
        super.updatePlayerPosition(player);
    }

    public void untrack(Entity entityIn) {
        super.untrack(entityIn);
    }

    public void track(Entity entityIn) {
        super.track(entityIn);
    }

    public void sendToTrackingAndSelf(Entity entityIn, IPacket<?> packet) {
        super.sendToTrackingAndSelf(entityIn, packet);
    }

    public void sendToAllTracking(Entity entityIn, IPacket<?> packet) {
        super.sendToAllTracking(entityIn, packet);
    }

    public void setViewDistance(int viewDistance) {
        super.setViewDistance(viewDistance);
    }

    public void setAllowedSpawnTypes(boolean hostile, boolean peaceful) {
        super.setAllowedSpawnTypes(hostile, peaceful);
    }

    // getChunkDebugString?
    public String func_217208_a(ChunkPos chunkPosIn) {
        return super.func_217208_a(chunkPosIn);
    }

    public PointOfInterestManager getPointOfInterestManager() {
        return super.getPointOfInterestManager();
    }

}
