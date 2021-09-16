package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import static io.github.opencubicchunks.cubicchunks.CubicChunks.LOGGER;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.VerticalViewDistanceListener;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.ChunkMapTrackedEntityAccess;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.DistanceManagerAccess;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.IOWorkerAccess;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.TicketAccess;
import io.github.opencubicchunks.cubicchunks.network.PacketCubes;
import io.github.opencubicchunks.cubicchunks.network.PacketDispatcher;
import io.github.opencubicchunks.cubicchunks.network.PacketHeightmap;
import io.github.opencubicchunks.cubicchunks.network.PacketUnloadCube;
import io.github.opencubicchunks.cubicchunks.network.PacketUpdateCubePosition;
import io.github.opencubicchunks.cubicchunks.network.PacketUpdateLight;
import io.github.opencubicchunks.cubicchunks.server.level.CubeHolder;
import io.github.opencubicchunks.cubicchunks.server.level.CubeHolderPlayerProvider;
import io.github.opencubicchunks.cubicchunks.server.level.CubeMap;
import io.github.opencubicchunks.cubicchunks.server.level.CubeMapInternal;
import io.github.opencubicchunks.cubicchunks.server.level.CubeTaskPriorityQueue;
import io.github.opencubicchunks.cubicchunks.server.level.CubeTaskPriorityQueueSorter;
import io.github.opencubicchunks.cubicchunks.server.level.CubicDistanceManager;
import io.github.opencubicchunks.cubicchunks.server.level.CubicTicketType;
import io.github.opencubicchunks.cubicchunks.server.level.ServerCubeCache;
import io.github.opencubicchunks.cubicchunks.server.level.progress.CubeProgressListener;
import io.github.opencubicchunks.cubicchunks.utils.ChunkIoMainThreadTaskUtils;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.utils.CubeCollectorFuture;
import io.github.opencubicchunks.cubicchunks.utils.ExecutorUtils;
import io.github.opencubicchunks.cubicchunks.utils.Utils;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeStatus;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.ImposterProtoCube;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.LevelCube;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.ProtoCube;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.storage.AsyncSaveData;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.storage.CubicSectionStorage;
import io.github.opencubicchunks.cubicchunks.world.server.CubicServerLevel;
import io.github.opencubicchunks.cubicchunks.world.server.CubicThreadedLevelLightEngine;
import io.github.opencubicchunks.cubicchunks.world.storage.CubeSerializer;
import io.github.opencubicchunks.cubicchunks.world.storage.RegionCubeIO;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.PlayerMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.Mth;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ChunkMap.class)
public abstract class MixinChunkMap implements CubeMap, CubeMapInternal, VerticalViewDistanceListener, CubeHolderPlayerProvider {

    private static final double TICK_UPDATE_DISTANCE = 128.0;
    private static final boolean USE_ASYNC_SERIALIZATION = true;

    private static final Executor COLUMN_LOADING_EXECUTOR = Executors.newSingleThreadExecutor();

    @Shadow @Final ServerLevel level;
    @Shadow int viewDistance;

    private CubeTaskPriorityQueueSorter cubeQueueSorter;

    private final Long2ObjectLinkedOpenHashMap<ChunkHolder> updatingCubeMap = new Long2ObjectLinkedOpenHashMap<>();
    private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleCubeMap = this.updatingCubeMap.clone();

    private final LongSet cubesToDrop = new LongOpenHashSet();
    private final LongSet cubeEntitiesInLevel = new LongOpenHashSet();
    private final Long2ObjectLinkedOpenHashMap<ChunkHolder> pendingCubeUnloads = new Long2ObjectLinkedOpenHashMap<>();

    // field_219264_r, worldgenMailbox
    private ProcessorHandle<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> cubeWorldgenMailbox;
    // field_219265_s, mainThreadMailbox
    private ProcessorHandle<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> cubeMainThreadMailbox;

    private final AtomicInteger tickingGeneratedCubes = new AtomicInteger();

    private final Long2ByteMap cubeTypeCache = new Long2ByteOpenHashMap();
    private final Queue<Runnable> cubeUnloadQueue = Queues.newConcurrentLinkedQueue();

    private ServerChunkCache serverChunkCache;
    private RegionCubeIO regionCubeIO;

    private final Map<CubePos, CompletableFuture<Boolean>> cubeSavingFutures = new ConcurrentHashMap<>();

    private int verticalViewDistance;
    private int incomingVerticalViewDistance;

    @Shadow @Final private ThreadedLevelLightEngine lightEngine;

    @Shadow private boolean modified;

    @Shadow @Final private ChunkMap.DistanceManager distanceManager;

    @Shadow @Final private StructureManager structureManager;

    @Shadow @Final private BlockableEventLoop<Runnable> mainThreadExecutor;

    @Shadow @Final private ChunkProgressListener progressListener;

    @Shadow @Final private ChunkGenerator generator;

    @Shadow @Final private String storageName;

    @Shadow @Final private Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;

    @Shadow @Final private PlayerMap playerMap;

    @Shadow @Final private PoiManager poiManager;

    @Shadow private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap;

    @Shadow protected abstract boolean skipPlayer(ServerPlayer player);

    @Shadow protected abstract void updateChunkTracking(ServerPlayer player, ChunkPos chunkPosIn, Packet<?>[] packetCache,
                                                        boolean wasLoaded, boolean load);

    @Shadow private static int checkerboardDistance(ChunkPos chunkPosIn, int x, int y) {
        throw new Error("Mixin didn't apply");
    }

    @Shadow public abstract Stream<ServerPlayer> getPlayers(ChunkPos chunkPos, boolean bl);

    @Shadow @Nullable protected abstract CompoundTag readChunk(ChunkPos pos) throws IOException;

    @Shadow private static void postLoadProtoChunk(ServerLevel serverLevel, List<CompoundTag> list) {
        throw new Error("Mixin didn't apply");
    }

    @SuppressWarnings("UnresolvedMixinReference") @Redirect(method = "<init>", at = @At(value = "NEW", target = "net/minecraft/server/level/ChunkMap$DistanceManager"))
    private ChunkMap.DistanceManager setIsCubic(ChunkMap chunkMap, Executor executor, Executor executor2, ServerLevel level) {
        ChunkMap.DistanceManager distanceManager = chunkMap.new DistanceManager(executor, executor2);
        //noinspection ConstantConditions
        ((CubicDistanceManager) distanceManager).initCubic(((CubicLevelHeightAccessor) this.level).isCubic());
        return distanceManager;
    }

    @Inject(method = "<init>", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onConstruct(ServerLevel serverLevel, LevelStorageSource.LevelStorageAccess levelStorageAccess, DataFixer dataFixer, StructureManager structureManager_, Executor executor,
                             BlockableEventLoop<Runnable> blockableEventLoop, LightChunkGetter lightChunkGetter, ChunkGenerator chunkGenerator, ChunkProgressListener chunkProgressListener,
                             ChunkStatusUpdateListener chunkStatusUpdateListener, Supplier<DimensionDataStorage> supplier, int i, boolean bl,
                             CallbackInfo ci, File file, ProcessorMailbox<Runnable> worldgenMailbox, ProcessorHandle<Runnable> mainThreadProcessorHandle,
                             ProcessorMailbox<Runnable> lightMailbox) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }

        this.cubeQueueSorter = new CubeTaskPriorityQueueSorter(ImmutableList.of(worldgenMailbox, mainThreadProcessorHandle, lightMailbox), executor, Integer.MAX_VALUE);
        this.cubeWorldgenMailbox = this.cubeQueueSorter.createExecutor(worldgenMailbox, false);
        this.cubeMainThreadMailbox = this.cubeQueueSorter.createExecutor(mainThreadProcessorHandle, false);

        ((CubicThreadedLevelLightEngine) this.lightEngine).postConstructorSetup(this.cubeQueueSorter,
            this.cubeQueueSorter.createExecutor(lightMailbox, false));

        try {
            regionCubeIO = new RegionCubeIO(file, "chunk", "cube");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;processUnloads(Ljava/util/function/BooleanSupplier;)V"))
    protected void onTickScheduleUnloads(BooleanSupplier hasMoreTime, CallbackInfo ci) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }
        this.processCubeUnloads(hasMoreTime);
    }

    // Forge dimension stuff gone in 1.16, TODO when forge readds dimension code
    // @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap;isEmpty()Z"))
    // private boolean canUnload(Long2ObjectLinkedOpenHashMap<ChunkHolder> loadedChunks)
    // {
    //     return loadedChunks.isEmpty() && loadedCubes.isEmpty();
    // }

    @Inject(method = "saveAllChunks", at = @At("HEAD"))
    protected void save(boolean flush, CallbackInfo ci) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }
        if (flush) {
            List<ChunkHolder> list = this.visibleCubeMap.values().stream()
                .filter(ChunkHolder::wasAccessibleSinceLastSave)
                .peek(ChunkHolder::refreshAccessibility)
                .collect(Collectors.toList());
            MutableBoolean savedAny = new MutableBoolean();

            do {
                savedAny.setFalse();
                @SuppressWarnings("unchecked") final CompletableFuture<Boolean>[] saveFutures = list.stream().map((cubeHolder) -> {
                        CompletableFuture<CubeAccess> cubeFuture;
                        do {
                            cubeFuture = ((CubeHolder) cubeHolder).getCubeToSave();
                            this.mainThreadExecutor.managedBlock(cubeFuture::isDone);
                        } while (cubeFuture != ((CubeHolder) cubeHolder).getCubeToSave());

                        return cubeFuture.join();
                    }).filter((cube) -> cube instanceof ImposterProtoCube || cube instanceof LevelCube)
                    .map(cube1 -> USE_ASYNC_SERIALIZATION ? cubeSaveAsync(cube1) : CompletableFuture.completedFuture(cubeSave(cube1)))
                    .distinct().toArray(CompletableFuture[]::new);
                for (CompletableFuture<Boolean> future : saveFutures) {
                    if (future.join()) {
                        savedAny.setTrue();
                    }
                }

            } while (savedAny.isTrue());

            this.processCubeUnloads(() -> true);
            regionCubeIO.flush();
            LOGGER.info("Cube Storage ({}): All cubes are saved", this.storageName);
        } else {
            this.visibleCubeMap.values().stream().filter(ChunkHolder::wasAccessibleSinceLastSave).forEach((cubeHolder) -> {
                CubeAccess cube = ((CubeHolder) cubeHolder).getCubeToSave().getNow(null);
                if (cube instanceof ImposterProtoCube || cube instanceof LevelCube) {
                    this.cubeSave(cube);
                    cubeHolder.refreshAccessibility();
                }
            });
        }

    }

    @Override public void setServerChunkCache(ServerChunkCache cache) {
        serverChunkCache = cache;
    }

    // save()
    private boolean cubeSave(CubeAccess cube) {
        ((CubicSectionStorage) this.poiManager).flush(cube.getCubePos());
        if (!cube.isDirty()) {
            return false;
        } else {
            cube.setDirty(false);
            CubePos cubePos = cube.getCubePos();

            try {
                ChunkStatus status = cube.getCubeStatus();
                if (status.getChunkType() != ChunkStatus.ChunkType.LEVELCHUNK) {
                    if (isExistingCubeFull(cubePos)) {
                        return false;
                    }
                    if (status == ChunkStatus.EMPTY && cube.getAllStarts().values().stream().noneMatch(StructureStart::isValid)) {
                        return false;
                    }
                }

                if (status.getChunkType() != ChunkStatus.ChunkType.LEVELCHUNK) {
                    CompoundTag compoundnbt = regionCubeIO.loadCubeNBT(cubePos);
                    if (compoundnbt != null && CubeSerializer.getChunkStatus(compoundnbt) == ChunkStatus.ChunkType.LEVELCHUNK) {
                        return false;
                    }

                    if (status == ChunkStatus.EMPTY && cube.getAllStarts().values().stream().noneMatch(StructureStart::isValid)) {
                        return false;
                    }
                }

                CompoundTag cubeNbt = CubeSerializer.write(this.level, cube, null);
                //TODO: FORGE EVENT : reimplement ChunkDataEvent#Save
//                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkDataEvent.Save(p_219229_1_, p_219229_1_.getWorldForge() != null ?
//                p_219229_1_.getWorldForge() : this.level, compoundnbt));
                regionCubeIO.saveCubeNBT(cubePos, cubeNbt);
                this.markCubePosition(cubePos, status.getChunkType());
                return true;
            } catch (Exception exception) {
                LOGGER.error("Failed to save chunk {},{},{}", cubePos.getX(), cubePos.getY(), cubePos.getZ(), exception);
                return false;
            }
        }
    }

    private CompletableFuture<Boolean> cubeSaveAsync(CubeAccess cube) {
        ((CubicSectionStorage) this.poiManager).flush(cube.getCubePos());
        if (!cube.isDirty()) {
            return CompletableFuture.completedFuture(false);
        } else {
            cube.setDirty(false);
            CubePos cubePos = cube.getCubePos();

            try {
                ChunkStatus status = cube.getCubeStatus();
                if (status.getChunkType() != ChunkStatus.ChunkType.LEVELCHUNK) {
                    if (isExistingCubeFull(cubePos)) {
                        return CompletableFuture.completedFuture(false);
                    }
                    if (status == ChunkStatus.EMPTY && cube.getAllStarts().values().stream().noneMatch(StructureStart::isValid)) {
                        return CompletableFuture.completedFuture(false);
                    }
                }

                if (status.getChunkType() != ChunkStatus.ChunkType.LEVELCHUNK) {
                    CompoundTag cubeNbt = regionCubeIO.loadCubeNBT(cubePos);
                    if (cubeNbt != null && CubeSerializer.getChunkStatus(cubeNbt) == ChunkStatus.ChunkType.LEVELCHUNK) {
                        return CompletableFuture.completedFuture(false);
                    }

                    if (status == ChunkStatus.EMPTY && cube.getAllStarts().values().stream().noneMatch(StructureStart::isValid)) {
                        return CompletableFuture.completedFuture(false);
                    }
                }

                final AsyncSaveData asyncSaveData = new AsyncSaveData(this.level, cube);
                this.markCubePosition(cubePos, status.getChunkType());
                final CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> CubeSerializer.write(this.level, cube, asyncSaveData), ExecutorUtils.SERIALIZER)
                    .thenAccept(tag -> regionCubeIO.saveCubeNBT(cubePos, tag)).thenApply(unused -> true).exceptionally(throwable -> {
                        LOGGER.error("Failed to save chunk {},{},{}", cubePos.getX(), cubePos.getY(), cubePos.getZ(), throwable);
                        return false;
                    });
                this.cubeSavingFutures.put(cubePos, future);
                return future;
            } catch (Exception exception) {
                LOGGER.error("Failed to save chunk {},{},{}", cubePos.getX(), cubePos.getY(), cubePos.getZ(), exception);
                return CompletableFuture.completedFuture(false);
            }
        }
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At("HEAD"))
    private void onTick(CallbackInfo info) {
        cubeSavingFutures.entrySet().removeIf(entry -> entry.getValue().isDone());
    }

    // TODO: is this used from ASM?
    private CompoundTag readCubeNBT(CubePos cubePos) throws IOException {
        return regionCubeIO.loadCubeNBT(cubePos);
    }

    // processUnloads
    private void processCubeUnloads(BooleanSupplier hasMoreTime) {
        LongIterator longiterator = this.cubesToDrop.iterator();

        for (int i = 0; longiterator.hasNext() && (hasMoreTime.getAsBoolean() || i < 200 || this.cubesToDrop.size() > 2000); longiterator.remove()) {
            long j = longiterator.nextLong();
            ChunkHolder chunkholder = this.updatingCubeMap.remove(j);
            if (chunkholder != null) {
                this.pendingCubeUnloads.put(j, chunkholder);
                this.modified = true;
                ++i;
                this.scheduleCubeUnload(j, chunkholder);
            }
        }

        Runnable runnable;
        while ((hasMoreTime.getAsBoolean() || this.cubeUnloadQueue.size() > 2000) && (runnable = this.cubeUnloadQueue.poll()) != null) {
            runnable.run();
        }
    }

    // scheduleUnload
    private void scheduleCubeUnload(long cubePos, ChunkHolder chunkHolderIn) {
        CompletableFuture<CubeAccess> toSaveFuture = ((CubeHolder) chunkHolderIn).getCubeToSave();
        toSaveFuture.thenAcceptAsync(cube -> {
            CompletableFuture<CubeAccess> newToSaveFuture = ((CubeHolder) chunkHolderIn).getCubeToSave();
            if (newToSaveFuture != toSaveFuture) {
                this.scheduleCubeUnload(cubePos, chunkHolderIn);
            } else {
                if (this.pendingCubeUnloads.remove(cubePos, chunkHolderIn) && cube != null) {
                    if (cube instanceof LevelCube levelCube) {
                        levelCube.setLoaded(false);
                        //TODO: reimplement forge event ChunkEvent#Unload.
                        //net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Unload((Chunk)cube));
                    }

                    if (USE_ASYNC_SERIALIZATION) {
                        this.cubeSaveAsync(cube);
                    } else {
                        this.cubeSave(cube);
                    }
                    if (this.cubeEntitiesInLevel.remove(cubePos) && cube instanceof LevelCube levelCube) {
                        ((CubicServerLevel) this.level).onCubeUnloading(levelCube);
                    }

                    ((CubicThreadedLevelLightEngine) this.lightEngine).setCubeStatusEmpty(cube.getCubePos());
                    this.lightEngine.tryScheduleUpdate();
                    CubePos pos = CubePos.from(cubePos);

                    for (int localX = 0; localX < CubeAccess.DIAMETER_IN_SECTIONS; localX++) {
                        for (int localZ = 0; localZ < CubeAccess.DIAMETER_IN_SECTIONS; localZ++) {
                            long chunkPos = pos.asChunkPos(localX, localZ).toLong();
                            Ticket<?>[] tickets = ((DistanceManagerAccess) distanceManager).invokeGetTickets(chunkPos).stream().filter((ticket ->
                                ticket.getType() == CubicTicketType.COLUMN && ((TicketAccess) ticket).getKey().equals(pos))).toArray(Ticket[]::new);
                            for (Ticket<?> ticket : tickets) {
                                ((DistanceManagerAccess) this.distanceManager).invokeRemoveTicket(chunkPos, ticket);
                            }
                        }
                    }
                }

            }
        }, this.cubeUnloadQueue::add).whenComplete((v, throwable) -> {
            if (throwable != null) {
                LOGGER.error("Failed to save cube " + ((CubeHolder) chunkHolderIn).getCubePos(), throwable);
            }
        });
    }

    // TODO: add to interface
    // markPositionReplaceable
    private void markCubePositionReplaceable(CubePos cubePos) {
        this.cubeTypeCache.put(cubePos.asLong(), (byte) -1);
    }

    // markPosition
    private byte markCubePosition(CubePos cubePos, ChunkStatus.ChunkType status) {
        return this.cubeTypeCache.put(cubePos.asLong(), (byte) (status == ChunkStatus.ChunkType.PROTOCHUNK ? -1 : 1));
    }

    @Override
    public LongSet getCubesToDrop() {
        return this.cubesToDrop;
    }

    @Override
    @Nullable
    public ChunkHolder getUpdatingCubeIfPresent(long cubePosIn) {
        return updatingCubeMap.get(cubePosIn);
    }

    @Override
    public ChunkHolder getVisibleCubeIfPresent(long cubePosIn) {
        return this.visibleCubeMap.get(cubePosIn);
    }

    // TODO: remove when cubic chunks versions are done
    @SuppressWarnings("UnresolvedMixinReference")
    // lambda$func_219244_a$13 or lambda$schedule$13 or lambda$null$13
    @Inject(method = "schedule", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/level/progress/ChunkProgressListener;onStatusChange(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/ChunkStatus;)V")
    )
    @Group(name = "MixinChunkManager.on_func_219244_a_StatusChange", min = 1, max = 1)
    private void on_func_219244_a_StatusChange(ChunkHolder chunkHolderIn, ChunkStatus chunkStatusIn,
                                               CallbackInfoReturnable<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> cir) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }

        if (((CubeHolder) chunkHolderIn).getCubePos() != null) {
            ((CubeProgressListener) progressListener).onCubeStatusChange(
                ((CubeHolder) chunkHolderIn).getCubePos(),
                chunkStatusIn);
        }
    }

    @SuppressWarnings("UnresolvedMixinReference")
    // lambda$scheduleUnload$10 or lambda$scheduleSave$10 or ???
    @Inject(method = "*", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/level/progress/ChunkProgressListener;onStatusChange(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/ChunkStatus;)V")
    )
    @Group(name = "MixinChunkManager.onScheduleSaveStatusChange", min = 1, max = 1)
    private void onScheduleSaveStatusChange(ChunkHolder chunkHolderIn, CompletableFuture<?> completablefuture,
                                            long chunkPosIn, ChunkAccess p_219185_5_, CallbackInfo ci) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }

        if (((CubeHolder) chunkHolderIn).getCubePos() != null) {
            ((CubeProgressListener) progressListener).onCubeStatusChange(
                ((CubeHolder) chunkHolderIn).getCubePos(), null);
        }
    }

    //A lambda inside a lambda in the method scheduleChunkGeneration.
    @SuppressWarnings({ "UnresolvedMixinReference", "target" })
    @Inject(
        method = "lambda$scheduleChunkGeneration$18(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/server/level/ChunkHolder;Lnet/minecraft/world/level/chunk/ChunkStatus;"
            + "Ljava/util/concurrent/Executor;Ljava/util/List;)Ljava/util/concurrent/CompletableFuture;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/progress/ChunkProgressListener;onStatusChange(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/ChunkStatus;)V"
        )
    )
    private void onGenerateStatusChange(ChunkPos chunkpos, ChunkHolder chunkHolderIn, ChunkStatus chunkStatusIn, Executor executor, List<?> list,
                                        CallbackInfoReturnable<CompletableFuture<?>> cir) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }
        if (((CubeHolder) chunkHolderIn).getCubePos() != null) {
            ((CubeProgressListener) progressListener).onCubeStatusChange(
                ((CubeHolder) chunkHolderIn).getCubePos(), null);
        }
    }

    @Inject(method = "promoteChunkMap",
        at = @At(value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap;clone()Lit/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap;",
            remap = false
        )
    )
    private void promoteCubeMap(CallbackInfoReturnable<Boolean> cir) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }
        this.visibleCubeMap = updatingCubeMap.clone();
    }

    @Override
    public Iterable<ChunkHolder> getCubes() {
        return Iterables.unmodifiableIterable(this.visibleCubeMap.values());
    }

    @Override
    public CompletableFuture<Either<CubeAccess, ChunkHolder.ChunkLoadingFailure>> scheduleCube(ChunkHolder cubeHolder, ChunkStatus chunkStatusIn) {
        CubePos cubePos = ((CubeHolder) cubeHolder).getCubePos();
        if (chunkStatusIn == ChunkStatus.EMPTY) {
            CompletableFuture<Either<CubeAccess, ChunkHolder.ChunkLoadingFailure>> cubeFuture = this.scheduleCubeLoad(cubePos);
            return cubeFuture.thenCompose(maybeCube -> {
                Optional<CubeAccess> left = maybeCube.left();
                ChunkStatus target = left.map(ChunkAccess::getStatus).orElse(chunkStatusIn);
                return scheduleAndWaitForColumns(target, cubePos).thenApplyAsync(columns -> {
                    maybeCube.left().ifPresent(cube -> {
                        if (!(cube instanceof ImposterProtoCube) && cube instanceof ProtoCube primer && primer.getCubeStatus().isOrAfter(ChunkStatus.FEATURES)) {
                            primer.onEnteringFeaturesStatus();
                        }
                    });
                    return maybeCube;
                }, mainThreadExecutor);
            });
        } else {
            CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> columnFutures = scheduleAndWaitForColumns(chunkStatusIn, cubePos);

            CompletableFuture<Either<CubeAccess, ChunkHolder.ChunkLoadingFailure>> parentCubeFuture = Utils.unsafeCast(
                ((CubeHolder) cubeHolder).getOrScheduleCubeFuture(chunkStatusIn.getParent(), (ChunkMap) (Object) this)
            );
            return columnFutures.thenComposeAsync(columns -> parentCubeFuture).thenComposeAsync(
                (Either<CubeAccess, ChunkHolder.ChunkLoadingFailure> inputSection) -> {
                    Optional<CubeAccess> optional = inputSection.left();
                    if (!optional.isPresent()) {
                        return CompletableFuture.completedFuture(inputSection);
                    } else {
                        if (chunkStatusIn == ChunkStatus.LIGHT) {
                            ((CubicDistanceManager) this.distanceManager).addCubeTicket(CubicTicketType.LIGHT, cubePos,
                                33 + CubeStatus.getDistance(ChunkStatus.FEATURES), cubePos);
                        }

                        CubeAccess cube = optional.get();
                        if (cube.getCubeStatus().isOrAfter(chunkStatusIn)) {
                            CompletableFuture<Either<CubeAccess, ChunkHolder.ChunkLoadingFailure>> completablefuture1 = Utils
                                .unsafeCast(chunkStatusIn.load(this.level, this.structureManager, this.lightEngine, (chunk) -> Utils.unsafeCast(this.protoCubeToFullCube(cubeHolder)), cube));
                            ((CubeProgressListener) this.progressListener).onCubeStatusChange(cubePos, chunkStatusIn);
                            return completablefuture1;
                        } else {
                            return this.scheduleCubeGeneration(cubeHolder, chunkStatusIn);
                        }
                    }
                }, this.mainThreadExecutor);
        }
    }

    // getDependencyStatus
    private ChunkStatus getCubeDependencyStatus(ChunkStatus status, int distance) {
        ChunkStatus parent;
        if (distance == 0) {
            parent = status.getParent();
        } else {
            parent = CubeStatus.getStatus(CubeStatus.getDistance(status) + distance);
        }
        return parent;
    }

    // scheduleChunkGeneration
    private CompletableFuture<Either<CubeAccess, ChunkHolder.ChunkLoadingFailure>> scheduleCubeGeneration(ChunkHolder cubeHolder, ChunkStatus chunkStatusIn) {
        CubePos cubePos = ((CubeHolder) cubeHolder).getCubePos();
        CompletableFuture<Either<List<CubeAccess>, ChunkHolder.ChunkLoadingFailure>> future =
            this.getCubeRangeFuture(cubePos, CubeStatus.getCubeTaskRange(chunkStatusIn), (count) -> this.getCubeDependencyStatus(chunkStatusIn, count));
        this.level.getProfiler().incrementCounter(() -> "cubeGenerate " + chunkStatusIn.getName());

        Executor executor = (runnable) -> this.cubeWorldgenMailbox.tell(CubeTaskPriorityQueueSorter.createMsg(cubeHolder, runnable));

        return future.thenComposeAsync((sectionOrError) -> {
            return sectionOrError.map((neighborSections) -> {
                try {
                    CompletableFuture<Either<CubeAccess, ChunkHolder.ChunkLoadingFailure>> finalFuture = Utils.unsafeCast(
                        chunkStatusIn.generate(executor, this.level, this.generator, this.structureManager, this.lightEngine,
                            (chunk) -> Utils.unsafeCast(this.protoCubeToFullCube(cubeHolder)), Utils.unsafeCast(neighborSections)));
                    ((CubeProgressListener) this.progressListener).onCubeStatusChange(cubePos, chunkStatusIn);
                    return finalFuture;
                } catch (Exception exception) {
                    CrashReport crashreport = CrashReport.forThrowable(exception, "Exception generating new chunk");
                    CrashReportCategory crashreportcategory = crashreport.addCategory("Chunk to be generated");
                    crashreportcategory.setDetail("Location", String.format("%d,%d,%d", cubePos.getX(), cubePos.getY(), cubePos.getZ()));
                    crashreportcategory.setDetail("Position hash", CubePos.asLong(cubePos.getX(), cubePos.getY(), cubePos.getZ()));
                    crashreportcategory.setDetail("Generator", this.generator);
                    throw new ReportedException(crashreport);
                }
            }, (loadingFailure) -> {
                this.releaseCubeLightTicket(cubePos);
                return CompletableFuture.completedFuture(Either.right(loadingFailure));
            });
        }, executor);
    }

    private CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> scheduleAndWaitForColumns(ChunkStatus targetStatus, CubePos cubePos) {

        CompletableFuture<CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> nestedChainedFuture = CompletableFuture.supplyAsync(() -> {
            CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> chainedFutures = null;
            for (int localX = 0; localX < CubeAccess.DIAMETER_IN_SECTIONS; localX++) {
                for (int localZ = 0; localZ < CubeAccess.DIAMETER_IN_SECTIONS; localZ++) {
                    ChunkPos chunkPos = cubePos.asChunkPos(localX, localZ);
                    CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> columnFutureForCube =
                        ((ServerCubeCache) serverChunkCache).getColumnFutureForCube(cubePos, chunkPos.x, chunkPos.z, targetStatus, true);
                    if (chainedFutures == null) {
                        chainedFutures = columnFutureForCube;
                    } else {
                        chainedFutures = chainedFutures.thenCompose((existingFuture) -> columnFutureForCube);
                    }
                }
            }
            return chainedFutures;
        }, mainThreadExecutor);
        return nestedChainedFuture.thenComposeAsync((future) -> future, COLUMN_LOADING_EXECUTOR);
    }

    @Override
    public CompletableFuture<Either<List<CubeAccess>, ChunkHolder.ChunkLoadingFailure>> getCubeRangeFuture(CubePos pos, int radius, IntFunction<ChunkStatus> getParentStatus) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        int requiredAreaLength = (2 * radius + 1);
        int requiredCubeCount = requiredAreaLength * requiredAreaLength * requiredAreaLength;
        CubeCollectorFuture collectorFuture = new CubeCollectorFuture(requiredCubeCount);

        // to index: x*d*d + y*d + z
        // extract x: index/(d*d)
        // extract y: (index/d) % d
        // extract z: index % d
        int cubeIdx = 0;
        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dy = -radius; dy <= radius; ++dy) {
                for (int dz = -radius; dz <= radius; ++dz) {

                    // determine the required cube's position
                    int distance = Math.max(Math.max(Math.abs(dz), Math.abs(dx)), Math.abs(dy));
                    final CubePos cubePos = CubePos.of(x + dx, y + dy, z + dz);
                    long posLong = cubePos.asLong();

                    // get the required cube's chunk holder
                    CubeHolder chunkholder = (CubeHolder) this.getUpdatingCubeIfPresent(posLong);
                    if (chunkholder == null) {
                        //noinspection MixinInnerClass
                        return CompletableFuture.completedFuture(Either.right(new ChunkHolder.ChunkLoadingFailure() {
                            public String toString() {
                                return "Unloaded " + cubePos.toString();
                            }
                        }));
                    }

                    final int idx2 = cubeIdx;
                    ChunkStatus parentStatus = getParentStatus.apply(distance);


                    if (CubicChunks.OPTIMIZED_CUBELOAD) {
                        chunkholder.addCubeStageListener(parentStatus, (either, error) -> {
                            collectorFuture.add(idx2, either, error);
                        }, Utils.unsafeCast(this));
                    } else {
                        CompletableFuture<Either<CubeAccess, ChunkHolder.ChunkLoadingFailure>> future =
                            chunkholder.getOrScheduleCubeFuture(parentStatus, Utils.unsafeCast(this));
                        future.whenComplete((either, error) -> collectorFuture.add(idx2, either, error));
                    }
                    ++cubeIdx;
                }
            }
        }

        return collectorFuture.thenApply((cubeEithers) -> {
            List<CubeAccess> returnFutures = Lists.newArrayList();
            int i = 0;

            for (final Either<CubeAccess, ChunkHolder.ChunkLoadingFailure> either : cubeEithers) {
                Optional<CubeAccess> optional = either.left();
                if (!optional.isPresent()) {
                    final int d = radius * 2 + 1;
                    final int idx = i;
                    //noinspection MixinInnerClass
                    return Either.right(new ChunkHolder.ChunkLoadingFailure() {
                        public String toString() {
                            return "Unloaded " + CubePos.of(
                                x + idx / (d * d),
                                y + (idx / d) % d,
                                z + idx % d) + " " + either.right().get()
                                .toString();
                        }
                    });
                }

                returnFutures.add(optional.get());
                ++i;
            }

            return Either.left(returnFutures);
        });
    }

    @Override
    public void releaseCubeLightTicket(CubePos cubePos) {
        this.mainThreadExecutor.tell(Util.name(() -> {
            ((CubicDistanceManager) this.distanceManager).removeCubeTicket(CubicTicketType.LIGHT,
                cubePos, 33 + CubeStatus.getDistance(ChunkStatus.FEATURES), cubePos);
        }, () -> {
            return "release light ticket " + cubePos;
        }));
    }

    // protoChunkToFullChunk
    private CompletableFuture<Either<CubeAccess, ChunkHolder.ChunkLoadingFailure>> protoCubeToFullCube(ChunkHolder holder) {
        CompletableFuture<Either<CubeAccess, ChunkHolder.ChunkLoadingFailure>> fullFuture =
            ((CubeHolder) holder).getCubeFutureIfPresentUnchecked(ChunkStatus.FULL.getParent());
        return fullFuture.thenApplyAsync((sectionOrError) -> {
            ChunkStatus chunkstatus = CubeHolder.getCubeStatusFromLevel(holder.getTicketLevel());
            return !chunkstatus.isOrAfter(ChunkStatus.FULL) ? CubeHolder.MISSING_CUBE : sectionOrError.mapLeft((prevCube) -> {
                CubePos cubePos = ((CubeHolder) holder).getCubePos();
                LevelCube cube;
                if (prevCube instanceof ImposterProtoCube) {
                    cube = ((ImposterProtoCube) prevCube).getCube();
                } else {
                    cube = new LevelCube(this.level, (ProtoCube) prevCube, (bigCube) -> {
                        //TODO: Verify this is ok
                        postLoadProtoChunk(this.level, ((ProtoCube) prevCube).getCubeEntities());
                    });
                    ((CubeHolder) holder).replaceProtoCube(new ImposterProtoCube(cube, level));
                }

                cube.setFullStatus(() -> ChunkHolder.getFullChunkStatus(holder.getTicketLevel()));
                cube.postLoad();
                if (this.cubeEntitiesInLevel.add(cubePos.asLong())) {
                    cube.setLoaded(true);
                    cube.registerAllBlockEntitiesAfterLevelLoad();
                }
                return cube;
            });
        }, (runnable) -> {
            this.cubeMainThreadMailbox.tell(CubeTaskPriorityQueueSorter.createMsg(
                runnable, ((CubeHolder) holder).getCubePos().asLong(), holder::getTicketLevel));
        });
    }

    @Override
    public CompletableFuture<Either<LevelCube, ChunkHolder.ChunkLoadingFailure>> prepareAccessibleCube(ChunkHolder chunkHolder) {
        return ((CubeHolder) chunkHolder).getOrScheduleCubeFuture(ChunkStatus.FULL, (ChunkMap) (Object) this).thenApplyAsync((o) -> {
            return o.mapLeft((icube) -> {
                LevelCube cube = (LevelCube) icube;
                cube.unpackTicks();
                return cube;
            });
        }, (runnable) -> {
            this.cubeMainThreadMailbox.tell(CubeTaskPriorityQueueSorter.createMsg(chunkHolder, runnable));
        });
    }

    @Override
    public CompletableFuture<Either<LevelCube, ChunkHolder.ChunkLoadingFailure>> prepareTickingCube(ChunkHolder chunkHolder) {
        CubePos cubePos = ((CubeHolder) chunkHolder).getCubePos();
        CompletableFuture<Either<List<CubeAccess>, ChunkHolder.ChunkLoadingFailure>> tickingFuture = this.getCubeRangeFuture(cubePos, 1,
            (i) -> {
                return ChunkStatus.FULL;
            });
        CompletableFuture<Either<LevelCube, ChunkHolder.ChunkLoadingFailure>> postProcessedFuture =
            tickingFuture.thenApplyAsync((o) -> {
                return o.flatMap((p_219208_0_) -> {
                    LevelCube cube = (LevelCube) p_219208_0_.get(p_219208_0_.size() / 2);
                    cube.postProcessGeneration();
                    return Either.left(cube);
                });
            }, (runnable) -> {
                this.cubeMainThreadMailbox.tell(CubeTaskPriorityQueueSorter.createMsg(chunkHolder, runnable));
            });
        postProcessedFuture.thenAcceptAsync((cubeLoadingErrorEither) -> {
            cubeLoadingErrorEither.mapLeft((cube) -> {
                this.tickingGeneratedCubes.getAndIncrement();
                Object[] objects = new Object[2];
                this.getPlayers(cubePos, false).forEach((serverPlayerEntity) -> {
                    this.playerLoadedCube(serverPlayerEntity, objects, cube);
                });
                for (int dx = 0; dx < CubeAccess.DIAMETER_IN_SECTIONS; dx++) {
                    for (int dz = 0; dz < CubeAccess.DIAMETER_IN_SECTIONS; dz++) {
                        ChunkPos pos = cubePos.asChunkPos(dx, dz);
                        this.getPlayers(pos, false).forEach(player -> {
                            this.updatePlayerHeightmap(player, pos);
                        });
                    }
                }
                return Either.left(cube);
            });
        }, (p_219202_2_) -> {
            this.cubeMainThreadMailbox.tell(CubeTaskPriorityQueueSorter.createMsg(chunkHolder, p_219202_2_));
        });
        return postProcessedFuture;
    }

    // TODO: change the actual ChunkSerializer to handle columns?
    @Redirect(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;write(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/nbt/CompoundTag;)V"))
    private void writeColumn(ChunkMap chunkManager, ChunkPos chunkPos, CompoundTag chunkNBT) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            chunkManager.write(chunkPos, chunkNBT);
            return;
        }
        regionCubeIO.saveChunkNBT(chunkPos, chunkNBT);
    }

    @SuppressWarnings({ "UnresolvedMixinReference", "ConstantConditions" })
    @Redirect(method = "lambda$scheduleChunkLoad$13(Lnet/minecraft/world/level/ChunkPos;)Lcom/mojang/datafixers/util/Either;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;readChunk(Lnet/minecraft/world/level/ChunkPos;)Lnet/minecraft/nbt/CompoundTag;"))
    private CompoundTag readColumn(ChunkMap chunkManager, ChunkPos chunkPos) throws IOException {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return this.readChunk(chunkPos);
        }
        return regionCubeIO.loadChunkNBT(chunkPos);
    }

    // readChunk
    @Nullable
    private CompoundTag readCube(CubePos cubePos) throws IOException {
        return this.regionCubeIO.loadCubeNBT(cubePos);
//        return partialCubeData == null ? null : partialCubeData.getNbt(); // == null ? null : this.upgradeChunkTag(this.level.dimension(), this.overworldDataStorage, compoundnbt);
    }

    // scheduleChunkLoad
    private CompletableFuture<Either<CubeAccess, ChunkHolder.ChunkLoadingFailure>> scheduleCubeLoad(CubePos cubePos) {
        this.level.getProfiler().incrementCounter("cubeLoad");

        final CompletableFuture<Boolean> savingFuture = cubeSavingFutures.containsKey(cubePos) ? cubeSavingFutures.get(cubePos) : CompletableFuture.completedFuture(true);
        return savingFuture.thenComposeAsync(unused -> {
            final CompletableFuture<CompoundTag> cubeNBTFuture = regionCubeIO.getCubeNBTFuture(cubePos);
            final CompletableFuture<CompoundTag> poiFuture = ((IOWorkerAccess) ((CubicSectionStorage) this.poiManager).getIOWorker()).invokeLoadAsync(cubePos.asChunkPos());
            return cubeNBTFuture.thenCombineAsync(poiFuture, (cubeNBT, poiNBT) -> {
                if (cubeNBT != null) {
                    boolean flag = cubeNBT.contains("Level", 10) && cubeNBT.getCompound("Level").contains("Status", 8);
                    if (flag) {
                        ChunkIoMainThreadTaskUtils.executeMain(() -> {
                            if (poiNBT != null) {
                                ((CubicSectionStorage) this.poiManager).updateCube(cubePos, poiNBT);
                            }
                        });
                        return CubeSerializer.read(this.level, this.structureManager, poiManager, cubePos, cubeNBT);
                    }
                    LOGGER.error("Cube file at {} is missing level data, skipping", cubePos);
                }
                return null;
            }, ExecutorUtils.SERIALIZER).handleAsync((iBigCube, throwable) -> {
                if (throwable != null) {
                    LOGGER.error("Couldn't load cube {}", cubePos, throwable);
                }
                ChunkIoMainThreadTaskUtils.drainQueue();
                if (iBigCube != null) {
                    this.markCubePosition(cubePos, iBigCube.getCubeStatus().getChunkType());
                    return Either.left(iBigCube);
                }
                this.markCubePositionReplaceable(cubePos);
                return Either.left(new ProtoCube(cubePos, UpgradeData.EMPTY, level));
            }, this.mainThreadExecutor);
        }, this.mainThreadExecutor);
    }

    @Override
    public Stream<ServerPlayer> getPlayers(CubePos pos, boolean boundaryOnly) {
        int hViewDistanceCubes = Coords.sectionToCubeRenderDistance(this.viewDistance);
        int vViewDistanceCubes = Coords.sectionToCubeRenderDistance(this.verticalViewDistance);
        return this.playerMap.getPlayers(pos.asLong()).filter((serverPlayerEntity) -> {
            int hDist = CubeMap.getCubeCheckerboardDistanceXZ(pos, serverPlayerEntity, true);
            int vDist = CubeMap.getCubeCheckerboardDistanceY(pos, serverPlayerEntity, true);
            if (hDist > hViewDistanceCubes || vDist > vViewDistanceCubes) {
                return false;
            } else {
                return !boundaryOnly || (hDist == hViewDistanceCubes && vDist == vViewDistanceCubes);
            }
        });
    }

    /**
     * @author NotStirred
     * @reason Due to vanilla calling ChunkManager#updatePlayerPos, which updates player#managedSectionPos, this is required.
     */
    @Inject(method = "updatePlayerStatus", at = @At("HEAD"), cancellable = true)
    void updatePlayerStatus(ServerPlayer player, boolean track, CallbackInfo ci) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }
        ci.cancel();

        boolean cannotGenerateChunks = this.skipPlayer(player);
        boolean cannotGenerateChunksTracker = this.playerMap.ignoredOrUnknown(player);
        int xFloor = Coords.getCubeXForEntity(player);
        int yFloor = Coords.getCubeYForEntity(player);
        int zFloor = Coords.getCubeZForEntity(player);
        if (track) {
            this.playerMap.addPlayer(CubePos.of(xFloor, yFloor, zFloor).asChunkPos().toLong(), player, cannotGenerateChunks);
            this.updatePlayerCubePos(player); //This also sends the vanilla packet, as player#ManagedSectionPos is changed in this method.
            if (!cannotGenerateChunks) {
                this.distanceManager.addPlayer(SectionPos.of(player), player); //Vanilla
                ((CubicDistanceManager) this.distanceManager).addCubePlayer(CubePos.from(SectionPos.of(player)), player);
            }
        } else {
            SectionPos managedSectionPos = player.getLastSectionPos(); //Vanilla
            CubePos cubePos = CubePos.from(managedSectionPos);
            this.playerMap.removePlayer(cubePos.asChunkPos().toLong(), player);
            if (!cannotGenerateChunksTracker) {
                this.distanceManager.removePlayer(managedSectionPos, player); //Vanilla
                ((CubicDistanceManager) this.distanceManager).removeCubePlayer(cubePos, player);
            }
        }

        //Vanilla
        int i = Mth.floor(player.getX()) >> 4;
        int j = Mth.floor(player.getZ()) >> 4;

        for (int l = i - this.viewDistance; l <= i + this.viewDistance; ++l) {
            for (int k = j - this.viewDistance; k <= j + this.viewDistance; ++k) {
                ChunkPos chunkpos = new ChunkPos(l, k);
                this.updateChunkTracking(player, chunkpos, new Packet[2], !track, track);
            }
        }
        //CC
        int hViewDistanceCubes = Coords.sectionToCubeRenderDistance(this.viewDistance);
        int vViewDistanceCubes = Coords.sectionToCubeRenderDistance(this.verticalViewDistance);

        for (int ix = xFloor - hViewDistanceCubes; ix <= xFloor + hViewDistanceCubes; ++ix) {
            for (int iy = yFloor - vViewDistanceCubes; iy <= yFloor + vViewDistanceCubes; ++iy) {
                for (int iz = zFloor - hViewDistanceCubes; iz <= zFloor + hViewDistanceCubes; ++iz) {
                    CubePos cubePos = CubePos.of(ix, iy, iz);
                    this.updateCubeTracking(player, cubePos, new Object[2], !track, track);
                }
            }
        }
    }

    /**
     * @author NotStirred
     * @reason To fix crash when vanilla updated player#managedSectionPos
     */
    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    public void move(ServerPlayer player, CallbackInfo ci) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }
        ci.cancel();
        for (ChunkMap.TrackedEntity trackedEntity : this.entityMap.values()) {
            if (((ChunkMapTrackedEntityAccess) trackedEntity).getEntity() == player) {
                trackedEntity.updatePlayers(this.level.players());
            } else {
                trackedEntity.updatePlayer(player);
            }
        }

        SectionPos managedSectionPos = player.getLastSectionPos();
        SectionPos newSectionPos = SectionPos.of(player);

        CubePos cubePosManaged = CubePos.from(managedSectionPos);
        CubePos newCubePos = CubePos.from(player);

        long managedPosAsLong = cubePosManaged.asLong();
        long posAsLong = newCubePos.asLong();

        long managedSectionPosLong = managedSectionPos.chunk().toLong();
        long newSectionPosLong = newSectionPos.chunk().toLong();

        boolean prevNoGenerate = this.playerMap.ignored(player);
        boolean nowNoGenerate = this.skipPlayer(player);

        boolean sectionPosChanged = managedSectionPos.asLong() != newSectionPos.asLong();

        if (sectionPosChanged || prevNoGenerate != nowNoGenerate) {
            this.updatePlayerCubePos(player);
            // remove player is generation was allowed on last update
            if (!prevNoGenerate) {
                this.distanceManager.removePlayer(managedSectionPos, player);
                ((CubicDistanceManager) this.distanceManager).removeCubePlayer(cubePosManaged, player);

            }

            // update the position if generation is allowed now
            if (!nowNoGenerate) {
                // we are mixin into this method, so it should work as this:
                this.distanceManager.addPlayer(newSectionPos, player); //Vanilla
                ((CubicDistanceManager) this.distanceManager).addCubePlayer(newCubePos, player);
            }

            if (!prevNoGenerate && nowNoGenerate) {
                this.playerMap.ignorePlayer(player);
            }

            if (prevNoGenerate && !nowNoGenerate) {
                this.playerMap.unIgnorePlayer(player);
            }

            if (managedPosAsLong != posAsLong) {
                // THIS IS FINE
                // this method is actually empty, positions don't actually matter
                this.playerMap.updatePlayer(managedSectionPosLong, newSectionPosLong, player);
            }
        }
        int hViewDistanceCubes = Coords.sectionToCubeRenderDistance(this.viewDistance);
        int vViewDistanceCubes = Coords.sectionToCubeRenderDistance(this.verticalViewDistance);

        int newCubeX = Coords.getCubeXForEntity(player);
        int newCubeY = Coords.getCubeYForEntity(player);
        int newCubeZ = Coords.getCubeZForEntity(player);

        int managedX = cubePosManaged.getX();
        int managedY = cubePosManaged.getY();
        int managedZ = cubePosManaged.getZ();

        if (Math.abs(managedX - newCubeX) <= hViewDistanceCubes * 2 &&
            Math.abs(managedY - newCubeY) <= vViewDistanceCubes * 2 &&
            Math.abs(managedZ - newCubeZ) <= hViewDistanceCubes * 2) {
            int minX = Math.min(newCubeX, managedX) - hViewDistanceCubes;
            int minY = Math.min(newCubeY, managedY) - vViewDistanceCubes;
            int minZ = Math.min(newCubeZ, managedZ) - hViewDistanceCubes;
            int maxX = Math.max(newCubeX, managedX) + hViewDistanceCubes;
            int maxY = Math.max(newCubeY, managedY) + vViewDistanceCubes;
            int maxZ = Math.max(newCubeZ, managedZ) + hViewDistanceCubes;

            for (int ix = minX; ix <= maxX; ++ix) {
                for (int iz = minZ; iz <= maxZ; ++iz) {
                    for (int iy = minY; iy <= maxY; ++iy) {
                        CubePos cubePos1 = CubePos.of(ix, iy, iz);
                        boolean loadedBefore = CubeMap.isInCubeDistance(cubePos1, managedX, managedY, managedZ, hViewDistanceCubes, vViewDistanceCubes);
                        boolean loadedNow = CubeMap.isInCubeDistance(cubePos1, newCubeX, newCubeY, newCubeZ, hViewDistanceCubes, vViewDistanceCubes);
                        this.updateCubeTracking(player, cubePos1, new Object[2], loadedBefore, loadedNow);
                    }
                }
            }
        } else {
            for (int ix = managedX - hViewDistanceCubes; ix <= managedX + hViewDistanceCubes; ++ix) {
                for (int iz = managedZ - hViewDistanceCubes; iz <= managedZ + hViewDistanceCubes; ++iz) {
                    for (int iy = managedY - vViewDistanceCubes; iy <= managedY + vViewDistanceCubes; ++iy) {
                        CubePos cubePos2 = CubePos.of(ix, iy, iz);
                        this.updateCubeTracking(player, cubePos2, new Object[2], true, false);
                    }
                }
            }

            for (int ix = newCubeX - hViewDistanceCubes; ix <= newCubeX + hViewDistanceCubes; ++ix) {
                for (int iz = newCubeZ - hViewDistanceCubes; iz <= newCubeZ + hViewDistanceCubes; ++iz) {
                    for (int iy = newCubeY - vViewDistanceCubes; iy <= newCubeY + vViewDistanceCubes; ++iy) {
                        CubePos cubePos3 = CubePos.of(ix, iy, iz);
                        this.updateCubeTracking(player, cubePos3, new Object[2], false, true);
                    }
                }
            }
        }

        //Start Chunk Tracking
        int newSectionX = Mth.floor(player.getX()) >> 4;
        int newSectionZ = Mth.floor(player.getZ()) >> 4;

        int oldSectionX = managedSectionPos.x();
        int oldSectionZ = managedSectionPos.z();
        if (Math.abs(oldSectionX - newSectionX) <= this.viewDistance * 2 && Math.abs(oldSectionZ - newSectionZ) <= this.viewDistance * 2) {
            int k2 = Math.min(newSectionX, oldSectionX) - this.viewDistance;
            int i3 = Math.min(newSectionZ, oldSectionZ) - this.viewDistance;
            int j3 = Math.max(newSectionX, oldSectionX) + this.viewDistance;
            int k3 = Math.max(newSectionZ, oldSectionZ) + this.viewDistance;

            for (int l3 = k2; l3 <= j3; ++l3) {
                for (int k1 = i3; k1 <= k3; ++k1) {
                    ChunkPos chunkpos1 = new ChunkPos(l3, k1);
                    boolean flag5 = checkerboardDistance(chunkpos1, oldSectionX, oldSectionZ) <= this.viewDistance;
                    boolean flag6 = checkerboardDistance(chunkpos1, newSectionX, newSectionZ) <= this.viewDistance;
                    this.updateChunkTracking(player, chunkpos1, new Packet[2], flag5, flag6);
                }
            }
        } else {
            for (int i1 = oldSectionX - this.viewDistance; i1 <= oldSectionX + this.viewDistance; ++i1) {
                for (int j1 = oldSectionZ - this.viewDistance; j1 <= oldSectionZ + this.viewDistance; ++j1) {
                    ChunkPos chunkpos = new ChunkPos(i1, j1);
                    this.updateChunkTracking(player, chunkpos, new Packet[2], true, false);
                }
            }
            for (int j2 = newSectionX - this.viewDistance; j2 <= newSectionX + this.viewDistance; ++j2) {
                for (int l2 = newSectionZ - this.viewDistance; l2 <= newSectionZ + this.viewDistance; ++l2) {
                    ChunkPos chunkpos2 = new ChunkPos(j2, l2);
                    this.updateChunkTracking(player, chunkpos2, new Packet[2], false, true);
                }
            }
        }
    }

    // this needs to be at HEAD, otherwise we are not going to see the view distance being different. Should not set view distance. Should NOT BE CANCELLED
    @Inject(method = "setViewDistance", at = @At("HEAD"))
    protected void setVerticalViewDistance(int horizontalDistance, CallbackInfo ci) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }

        int hViewDistanceSections = Mth.clamp(horizontalDistance + 1, 3, 33);
        int vViewDistanceSections = Mth.clamp(incomingVerticalViewDistance + 1, 3, 33);

        int hNewViewDistanceCubes = Coords.sectionToCubeRenderDistance(hViewDistanceSections);
        int vNewViewDistanceCubes = Coords.sectionToCubeRenderDistance(vViewDistanceSections);
        int hViewDistanceCubes = Coords.sectionToCubeRenderDistance(this.viewDistance);
        int vViewDistanceCubes = Coords.sectionToCubeRenderDistance(this.verticalViewDistance);

        if (hNewViewDistanceCubes != hViewDistanceCubes || vNewViewDistanceCubes != vViewDistanceCubes) {
            int oldViewDistance = this.viewDistance;
            this.viewDistance = hViewDistanceSections;
            this.verticalViewDistance = vViewDistanceSections;

            ((CubicDistanceManager) this.distanceManager).updatePlayerCubeTickets(hViewDistanceSections, vViewDistanceSections);

            for (ChunkHolder chunkholder : this.updatingCubeMap.values()) {
                CubePos cubePos = ((CubeHolder) chunkholder).getCubePos();
                Object[] objects = new Object[2];
                this.getPlayers(cubePos, false).forEach((player) -> {
                    boolean wasLoaded = CubeMap.isInViewDistance(cubePos, player, true, hViewDistanceCubes, vViewDistanceCubes);
                    boolean isLoaded = CubeMap.isInViewDistance(cubePos, player, true, hNewViewDistanceCubes, vNewViewDistanceCubes);
                    this.updateCubeTracking(player, cubePos, objects, wasLoaded, isLoaded);
                });
            }
            // reset it so that vanilla code can still see the old value
            this.viewDistance = oldViewDistance;
        }
    }

    @Override
    public void setIncomingVerticalViewDistance(int verticalDistance) {
        this.incomingVerticalViewDistance = verticalDistance;
    }

    // func_223489_c, updatePlayerPos
    private SectionPos updatePlayerCubePos(ServerPlayer serverPlayerEntityIn) {
        SectionPos sectionpos = SectionPos.of(serverPlayerEntityIn);
        serverPlayerEntityIn.setLastSectionPos(sectionpos);
        PacketDispatcher.sendTo(new PacketUpdateCubePosition(sectionpos), serverPlayerEntityIn);
        serverPlayerEntityIn.connection.send(new ClientboundSetChunkCacheCenterPacket(sectionpos.x(), sectionpos.z()));
        return sectionpos;
    }

    // updateChunkTracking
    protected void updateCubeTracking(ServerPlayer player, CubePos cubePosIn, Object[] packetCache, boolean wasLoaded, boolean load) {
        if (player.level == this.level) {
            //TODO: reimplement forge event
            //net.minecraftforge.event.ForgeEventFactory.fireChunkWatch(wasLoaded, load, player, cubePosIn, this.world);
            if (load && !wasLoaded) {
                ChunkHolder chunkholder = ((CubeMap) this).getVisibleCubeIfPresent(cubePosIn.asLong());
                if (chunkholder != null) {
                    LevelCube cube = ((CubeHolder) chunkholder).getCubeIfComplete();
                    if (cube != null) {
                        this.playerLoadedCube(player, packetCache, cube);
                        for (int dx = 0; dx < CubeAccess.DIAMETER_IN_SECTIONS; dx++) {
                            for (int dz = 0; dz < CubeAccess.DIAMETER_IN_SECTIONS; dz++) {
                                ChunkPos pos = cubePosIn.asChunkPos(dx, dz);
                                this.getPlayers(pos, false).forEach(p -> {
                                    this.updatePlayerHeightmap(p, pos);
                                });
                            }
                        }
                    }
                    //TODO: reimplement debugpacket
                    //DebugPacketSender.sendChuckPos(this.world, cubePosIn);
                }
            }
            if (!load && wasLoaded) {
                //Vanilla: //player.sendChunkUnload(chunkPosIn)
                //I moved to MixinChunkManager to be in the same place as sendCubeLoad
                this.untrackPlayerChunk(player, cubePosIn);
            }
        }
    }

    // func_219215_b/checkerboardDistance is in ICubeManager now
    //getChunkDistance is in ICubeManager now

    @Override
    public CompletableFuture<Void> packCubeTicks(LevelCube cubeIn) {
        return this.mainThreadExecutor.submit(() -> {
            cubeIn.packTicks(this.level);
        });
    }

    @Override
    public CompletableFuture<Either<LevelCube, ChunkHolder.ChunkLoadingFailure>> prepareEntityTickingCube(CubePos pos) {
        return this.getCubeRangeFuture(pos, 2, (index) -> {
            return ChunkStatus.FULL;
        }).thenApplyAsync((eitherSectionError) -> {
            return eitherSectionError.mapLeft((cubes) -> {
                return (LevelCube) cubes.get(cubes.size() / 2);
            });
        }, this.mainThreadExecutor);
    }

    private void untrackPlayerChunk(ServerPlayer player, CubePos cubePosIn) {
        if (player.isAlive()) {
            PacketDispatcher.sendTo(new PacketUnloadCube(cubePosIn), player);
        }
    }

    @Redirect(method = "playerLoadedChunk", at = @At(value = "NEW", target = "net/minecraft/network/protocol/game/ClientboundLightUpdatePacket"))
    private ClientboundLightUpdatePacket onVanillaLightPacketConstruct(ChunkPos pos, LevelLightEngine lightManager, BitSet bits1, BitSet bits2, boolean bool) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return new ClientboundLightUpdatePacket(pos, lightManager, bits1, bits2, bool);
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(0);
        buf.writeVarInt(0);
        buf.writeBoolean(false);
        buf.writeBitSet(new BitSet());
        buf.writeBitSet(new BitSet());
        buf.writeBitSet(new BitSet());
        buf.writeBitSet(new BitSet());
        buf.writeByteArray(new byte[2048]);
        buf.writeByteArray(new byte[2048]);
        return new ClientboundLightUpdatePacket(buf);
    }

    // playerLoadedChunk
    private void playerLoadedCube(ServerPlayer player, Object[] packetCache, LevelCube cubeIn) {
        if (packetCache[0] == null) {
            packetCache[0] = new PacketCubes(Collections.singletonList(cubeIn));
            packetCache[1] = new PacketUpdateLight(cubeIn.getCubePos(), this.lightEngine, true);
        }

        CubePos pos = cubeIn.getCubePos();

        PacketDispatcher.sendTo(packetCache[1], player);
        PacketDispatcher.sendTo(packetCache[0], player);
        List<Entity> leashedEntities = Lists.newArrayList();
        List<Entity> passengerEntities = Lists.newArrayList();

        for (ChunkMap.TrackedEntity entityTracker : this.entityMap.values()) {
            Entity entity = ((ChunkMapTrackedEntityAccess) entityTracker).getEntity();
            if (entity != player && CubePos.from(entity).equals(pos)) {
                entityTracker.updatePlayer(player);
                if (entity instanceof Mob && ((Mob) entity).getLeashHolder() != null) {
                    leashedEntities.add(entity);
                }

                if (!entity.getPassengers().isEmpty()) {
                    passengerEntities.add(entity);
                }
            }
        }

        if (!leashedEntities.isEmpty()) {
            for (Entity entity1 : leashedEntities) {
                player.connection.send(new ClientboundSetEntityLinkPacket(entity1, ((Mob) entity1).getLeashHolder()));
            }
        }

        if (!passengerEntities.isEmpty()) {
            for (Entity entity2 : passengerEntities) {
                player.connection.send(new ClientboundSetPassengersPacket(entity2));
            }
        }
    }

    @Override
    public boolean noPlayersCloseForSpawning(CubePos cubePos) {
        long cubePosAsLong = cubePos.asLong();
        return !((CubicDistanceManager) this.distanceManager).hasCubePlayersNearby(cubePosAsLong) || this.playerMap.getPlayers(cubePosAsLong).noneMatch(
            (serverPlayer) -> !serverPlayer.isSpectator() && euclideanDistanceSquared(cubePos, serverPlayer) < (TICK_UPDATE_DISTANCE * TICK_UPDATE_DISTANCE));
    }

    // euclideanDistanceSquared
    private static double euclideanDistanceSquared(CubePos cubePos, Entity entity) {
        double x = Coords.cubeToCenterBlock(cubePos.getX());
        double y = Coords.cubeToCenterBlock(cubePos.getY());
        double z = Coords.cubeToCenterBlock(cubePos.getZ());
        double dX = x - entity.getX();
        double dY = y - entity.getY();
        double dZ = z - entity.getZ();
        return dX * dX + dY * dY + dZ * dZ;
    }

    private void updatePlayerHeightmap(ServerPlayer player, ChunkPos pos) {
        ChunkHolder chunkHolder = visibleChunkMap.get(pos.toLong());
        if (chunkHolder == null) {
            // todo: is this ever going to be null?
            return;
        }
        Either<LevelChunk, ChunkHolder.ChunkLoadingFailure> chunkOrError = chunkHolder.getFullChunkFuture().getNow(null);
        if (chunkOrError == null) {
            return;
        }
        chunkOrError.ifLeft(chunk -> PacketDispatcher.sendTo(PacketHeightmap.forChunk(chunk), player));
    }

    @Override
    public int getTickingGeneratedCubes() {
        return this.tickingGeneratedCubes.get();
    }

    @Override
    public int sizeCubes() {
        return this.visibleCubeMap.size();
    }

    @Override
    public IntSupplier getCubeQueueLevel(long cubePosIn) {
        return () -> {
            ChunkHolder chunkholder = this.getVisibleCubeIfPresent(cubePosIn);
            return chunkholder == null ? CubeTaskPriorityQueue.LEVEL_COUNT - 1 : Math.min(chunkholder.getQueueLevel(),
                CubeTaskPriorityQueue.LEVEL_COUNT - 1);
        };
    }

    @Inject(method = "close", at = @At("HEAD"), remap = false)
    public void closeCubeIO(CallbackInfo ci) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }
        regionCubeIO.flush();
    }
}