package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import static io.github.opencubicchunks.cubicchunks.CubicChunks.LOGGER;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
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
import io.github.opencubicchunks.cubicchunks.chunk.CubeCollectorFuture;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.IChunkManager;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeHolder;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeStatusListener;
import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimer;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimerWrapper;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubeStatus;
import io.github.opencubicchunks.cubicchunks.chunk.graph.CCTicketType;
import io.github.opencubicchunks.cubicchunks.chunk.storage.ISectionStorage;
import io.github.opencubicchunks.cubicchunks.chunk.ticket.CubeTaskPriorityQueue;
import io.github.opencubicchunks.cubicchunks.chunk.ticket.CubeTaskPriorityQueueSorter;
import io.github.opencubicchunks.cubicchunks.chunk.ticket.ITicketManager;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.chunk.util.Utils;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.EntityTrackerAccess;
import io.github.opencubicchunks.cubicchunks.network.PacketCubes;
import io.github.opencubicchunks.cubicchunks.network.PacketDispatcher;
import io.github.opencubicchunks.cubicchunks.network.PacketHeightmap;
import io.github.opencubicchunks.cubicchunks.network.PacketUnloadCube;
import io.github.opencubicchunks.cubicchunks.network.PacketUpdateCubePosition;
import io.github.opencubicchunks.cubicchunks.network.PacketUpdateLight;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.server.IServerWorld;
import io.github.opencubicchunks.cubicchunks.world.server.IServerWorldLightManager;
import io.github.opencubicchunks.cubicchunks.world.storage.CubeSerializer;
import io.github.opencubicchunks.cubicchunks.world.storage.RegionCubeIO;
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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.PlayerMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ThreadedLevelLightEngine;
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
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ChunkMap.class)
public abstract class MixinChunkManager implements IChunkManager {

    private static final double TICK_UPDATE_DISTANCE = 128.0;

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

    private RegionCubeIO regionCubeIO;

    @Shadow @Final private ThreadedLevelLightEngine lightEngine;

    @Shadow private boolean modified;

    @Shadow @Final private ChunkMap.DistanceManager distanceManager;

    @Shadow @Final private ServerLevel level;

    @Shadow @Final private StructureManager structureManager;

    @Shadow @Final private BlockableEventLoop<Runnable> mainThreadExecutor;

    @Shadow @Final private ChunkProgressListener progressListener;

    @Shadow @Final private ChunkGenerator generator;

    @Shadow @Final private File storageFolder;

    @Shadow private int viewDistance;

    @Shadow @Final private Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;

    @Shadow @Final private PlayerMap playerMap;

    @Shadow private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap;

    @Shadow protected abstract boolean skipPlayer(ServerPlayer player);

    @Shadow protected abstract void updateChunkTracking(ServerPlayer player, ChunkPos chunkPosIn, Packet<?>[] packetCache,
                                                        boolean wasLoaded, boolean load);

    @Shadow private static int checkerboardDistance(ChunkPos chunkPosIn, int x, int y) {
        throw new Error("Mixin didn't apply");
    }

    @Shadow public abstract Stream<ServerPlayer> getPlayers(ChunkPos chunkPos, boolean bl);

    @Shadow @Final private PoiManager poiManager;

    @Inject(method = "<init>", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onConstruct(ServerLevel worldIn,
                             LevelStorageSource.LevelStorageAccess levelSave,
                             DataFixer p_i51538_3_,
                             StructureManager templateManagerIn,
                             Executor p_i51538_5_,
                             BlockableEventLoop<Runnable> mainThreadIn,
                             LightChunkGetter p_i51538_7_,
                             ChunkGenerator generatorIn,
                             ChunkProgressListener p_i51538_9_,
                             ChunkStatusUpdateListener chunkStatusUpdateListener,
                             Supplier<DimensionDataStorage> p_i51538_10_,
                             int p_i51538_11_,
                             boolean p_i232602_12_,
                             CallbackInfo ci, ProcessorMailbox delegatedtaskexecutor,
                             ProcessorHandle itaskexecutor, ProcessorMailbox delegatedtaskexecutor1) {

        this.cubeQueueSorter = new CubeTaskPriorityQueueSorter(ImmutableList.of(delegatedtaskexecutor,
            itaskexecutor, delegatedtaskexecutor1), p_i51538_5_, Integer.MAX_VALUE);
        this.cubeWorldgenMailbox = this.cubeQueueSorter.createExecutor(delegatedtaskexecutor, false);
        this.cubeMainThreadMailbox = this.cubeQueueSorter.createExecutor(itaskexecutor, false);

        ((IServerWorldLightManager) this.lightEngine).postConstructorSetup(this.cubeQueueSorter,
            this.cubeQueueSorter.createExecutor(delegatedtaskexecutor1, false));

        try {
            regionCubeIO = new RegionCubeIO(storageFolder, "chunk", "cube");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;processUnloads(Ljava/util/function/BooleanSupplier;)V"))
    protected void onTickScheduleUnloads(BooleanSupplier hasMoreTime, CallbackInfo ci) {
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
        if (flush) {
            List<ChunkHolder> list =
                this.visibleCubeMap.values().stream().filter(ChunkHolder::wasAccessibleSinceLastSave).peek(ChunkHolder::refreshAccessibility).collect(
                    Collectors.toList());
            MutableBoolean savedAny = new MutableBoolean();

            do {
                savedAny.setFalse();
                list.stream().map((cubeHolder) -> {
                    CompletableFuture<IBigCube> cubeFuture;
                    do {
                        cubeFuture = ((ICubeHolder) cubeHolder).getCubeToSave();
                        this.mainThreadExecutor.managedBlock(cubeFuture::isDone);
                    } while (cubeFuture != ((ICubeHolder) cubeHolder).getCubeToSave());

                    return cubeFuture.join();
                }).filter((cube) -> cube instanceof CubePrimerWrapper || cube instanceof BigCube)
                    .filter(this::cubeSave).forEach((unsavedCube) -> savedAny.setTrue());
            } while (savedAny.isTrue());

            this.processCubeUnloads(() -> true);
            regionCubeIO.flush();
            LOGGER.info("Cube Storage ({}): All cubes are saved", this.storageFolder.getName());
        } else {
            this.visibleCubeMap.values().stream().filter(ChunkHolder::wasAccessibleSinceLastSave).forEach((cubeHolder) -> {
                IBigCube cube = ((ICubeHolder) cubeHolder).getCubeToSave().getNow(null);
                if (cube instanceof CubePrimerWrapper || cube instanceof BigCube) {
                    this.cubeSave(cube);
                    cubeHolder.refreshAccessibility();
                }
            });
        }

    }

    // chunkSave
    private boolean cubeSave(IBigCube cube) {
        ((ISectionStorage) this.poiManager).flush(cube.getCubePos());
        if (!cube.isDirty()) {
            return false;
        } else {
            cube.setDirty(false);
            CubePos cubePos = cube.getCubePos();

            try {
                //TODO: implement isExistingCubeFull and by extension cubeTypeCache
                ChunkStatus status = cube.getCubeStatus();
                if (status.getChunkType() != ChunkStatus.ChunkType.LEVELCHUNK) {
                    if (isExistingCubeFull(cubePos)) {
                        return false;
                    }
//                    if (status == ChunkStatus.EMPTY && p_219229_1_.getAllStarts().values().stream().noneMatch(StructureStart::isValid)) {
//                        return false;
//                    }
                }

                if (status.getChunkType() != ChunkStatus.ChunkType.LEVELCHUNK) {
                    CompoundTag compoundnbt = regionCubeIO.loadCubeNBT(cubePos);
                    if (compoundnbt != null && CubeSerializer.getChunkStatus(compoundnbt) == ChunkStatus.ChunkType.LEVELCHUNK) {
                        return false;
                    }

                    //TODO: SAVE FORMAT : reimplement structures
//                    if (status == ChunkStatus.EMPTY && cube.getStructureStarts().values().stream().noneMatch(StructureStart::isValid)) {
//                        return false;
//                    }
                }

                CompoundTag compoundnbt = CubeSerializer.write(this.level, cube);
                //TODO: FORGE EVENT : reimplement ChunkDataEvent#Save
//                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkDataEvent.Save(p_219229_1_, p_219229_1_.getWorldForge() != null ?
//                p_219229_1_.getWorldForge() : this.level, compoundnbt));
                regionCubeIO.saveCubeNBT(cubePos, compoundnbt);
                this.markCubePosition(cubePos, status.getChunkType());
                return true;

            } catch (Exception exception) {
                LOGGER.error("Failed to save chunk {},{},{}", cubePos.getX(), cubePos.getY(), cubePos.getZ(), exception);
                return false;
            }
        }
    }

    private boolean isExistingCubeFull(CubePos cubePos) {
        byte b0 = cubeTypeCache.get(cubePos.asLong());
        if (b0 != 0) {
            return b0 == 1;
        } else {
            CompoundTag compoundnbt;
            try {
                compoundnbt = regionCubeIO.loadCubeNBT(cubePos);
                if (compoundnbt == null) {
                    this.markCubePositionReplaceable(cubePos);
                    return false;
                }
            } catch (Exception exception) {
                LOGGER.error("Failed to read chunk {}", cubePos, exception);
                this.markCubePositionReplaceable(cubePos);
                return false;
            }

            ChunkStatus.ChunkType status = ChunkSerializer.getChunkTypeFromTag(compoundnbt);
            return this.markCubePosition(cubePos, status) == 1;
        }
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


    private void scheduleCubeUnload(long cubePos, ChunkHolder chunkHolderIn) {
        CompletableFuture<IBigCube> completablefuture = ((ICubeHolder) chunkHolderIn).getCubeToSave();
        completablefuture.thenAcceptAsync((icube) -> {
            CompletableFuture<IBigCube> completablefuture1 = ((ICubeHolder) chunkHolderIn).getCubeToSave();
            if (completablefuture1 != completablefuture) {
                this.scheduleCubeUnload(cubePos, chunkHolderIn);
            } else {
                if (this.pendingCubeUnloads.remove(cubePos, chunkHolderIn) && icube != null) {
                    if (icube instanceof BigCube) {
                        ((BigCube) icube).setLoaded(false);
                        //TODO: reimplement forge event ChunkEvent#Unload.
                        //net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Unload((Chunk)cube));
                    }

                    this.cubeSave(icube);
                    if (this.cubeEntitiesInLevel.remove(cubePos) && icube instanceof BigCube) {
                        ((IServerWorld) this.level).onCubeUnloading((BigCube) icube);
                    }

                    ((IServerWorldLightManager) this.lightEngine).setCubeStatusEmpty(icube.getCubePos());
                    this.lightEngine.tryScheduleUpdate();
                    ((ICubeStatusListener) this.progressListener).onCubeStatusChange(icube.getCubePos(), null);
                }

            }
        }, this.cubeUnloadQueue::add).whenComplete((p_223171_1_, p_223171_2_) -> {
            if (p_223171_2_ != null) {
                LOGGER.error("Failed to save cube " + ((ICubeHolder) chunkHolderIn).getCubePos(), p_223171_2_);
            }
        });
    }

    private void markCubePositionReplaceable(CubePos cubePos) {
        this.cubeTypeCache.put(cubePos.asLong(), (byte) -1);
    }

    private byte markCubePosition(CubePos cubePos, ChunkStatus.ChunkType status) {
        return this.cubeTypeCache.put(cubePos.asLong(), (byte) (status == ChunkStatus.ChunkType.PROTOCHUNK ? -1 : 1));
    }

    @Override
    public LongSet getCubesToDrop() {
        return this.cubesToDrop;
    }

    // func_219220_a, getUpdatingChunkIfPresent
    @Override
    @Nullable
    public ChunkHolder getCubeHolder(long cubePosIn) {
        return updatingCubeMap.get(cubePosIn);
    }

    // func_219219_b, getVisibleChunkIfPresent
    @Override
    public ChunkHolder getImmutableCubeHolder(long cubePosIn) {
        return this.visibleCubeMap.get(cubePosIn);
    }

    // TODO: remove when cubic chunks versions are done
    @SuppressWarnings("UnresolvedMixinReference")
    // lambda$func_219244_a$13 or lambda$schedule$13 or lambda$null$13
    @Inject(method = "*", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/server/level/progress/ChunkProgressListener;onStatusChange(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/ChunkStatus;)V")
    )
    @Group(name = "MixinChunkManager.on_func_219244_a_StatusChange", min = 1, max = 1)
    private void on_func_219244_a_StatusChange(ChunkStatus chunkStatusIn, ChunkPos chunkpos,
                                               ChunkHolder chunkHolderIn, Either<?, ?> p_223180_4_, CallbackInfoReturnable<CompletionStage<?>> cir) {
        if (((ICubeHolder) chunkHolderIn).getCubePos() != null) {
            ((ICubeStatusListener) progressListener).onCubeStatusChange(
                ((ICubeHolder) chunkHolderIn).getCubePos(),
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
        if (((ICubeHolder) chunkHolderIn).getCubePos() != null) {
            ((ICubeStatusListener) progressListener).onCubeStatusChange(
                ((ICubeHolder) chunkHolderIn).getCubePos(), null);
        }
    }

    @SuppressWarnings({ "UnresolvedMixinReference", "target" })
    @Inject(
        method = "lambda$null$18(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/server/level/ChunkHolder;Lnet/minecraft/world/level/chunk/ChunkStatus;Ljava/util/List;)"
            + "Ljava/util/concurrent/CompletableFuture;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/progress/ChunkProgressListener;onStatusChange(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/ChunkStatus;)V"
        )
    )
    private void onGenerateStatusChange(ChunkPos chunkpos, ChunkHolder chunkHolderIn, ChunkStatus chunkStatusIn, List<?> p_223148_4_,
                                        CallbackInfoReturnable<CompletableFuture<?>> cir) {
        if (((ICubeHolder) chunkHolderIn).getCubePos() != null) {
            ((ICubeStatusListener) progressListener).onCubeStatusChange(
                ((ICubeHolder) chunkHolderIn).getCubePos(), null);
        }
    }

    @Inject(method = "promoteChunkMap",
        at = @At(value = "INVOKE",
            target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap;clone()Lit/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap;",
            remap = false
        )
    )
    private void onPromoteChunkMap(CallbackInfoReturnable<Boolean> cir) {
        this.visibleCubeMap = updatingCubeMap.clone();
    }

    @Override
    public Iterable<ChunkHolder> getCubes() {
        return Iterables.unmodifiableIterable(this.visibleCubeMap.values());
    }

    // func_219244_a, schedule
    @Override
    public CompletableFuture<Either<IBigCube, ChunkHolder.ChunkLoadingFailure>> scheduleCube(ChunkHolder chunkHolderIn, ChunkStatus chunkStatusIn) {
        CubePos cubePos = ((ICubeHolder) chunkHolderIn).getCubePos();
        if (chunkStatusIn == ChunkStatus.EMPTY) {
            return this.scheduleCubeLoad(cubePos);
        } else {
            CompletableFuture<Either<IBigCube, ChunkHolder.ChunkLoadingFailure>> completablefuture = Utils.unsafeCast(
                ((ICubeHolder) chunkHolderIn).getOrScheduleCubeFuture(chunkStatusIn.getParent(), (ChunkMap) (Object) this)
            );
            return Utils.unsafeCast(completablefuture.thenComposeAsync(
                (Either<IBigCube, ChunkHolder.ChunkLoadingFailure> inputSection) -> {
                    Optional<IBigCube> optional = inputSection.left();
                    if (!optional.isPresent()) {
                        return CompletableFuture.completedFuture(inputSection);
                    } else {
                        if (chunkStatusIn == ChunkStatus.LIGHT) {
                            ((ITicketManager) this.distanceManager).addCubeTicket(CCTicketType.CCLIGHT, cubePos,
                                33 + CubeStatus.getDistance(ChunkStatus.FEATURES), cubePos);
                        }

                        IBigCube cube = optional.get();
                        if (cube.getCubeStatus().isOrAfter(chunkStatusIn)) {
                            CompletableFuture<Either<IBigCube, ChunkHolder.ChunkLoadingFailure>> completablefuture1;
                            if (chunkStatusIn == ChunkStatus.LIGHT) {
                                completablefuture1 = this.scheduleCubeGeneration(chunkHolderIn, chunkStatusIn);
                            } else {
                                completablefuture1 = Utils.unsafeCast(
                                    chunkStatusIn.load(this.level, this.structureManager, this.lightEngine, (chunk) -> {
                                        return Utils.unsafeCast(this.protoCubeToFullCube(chunkHolderIn));
                                    }, (ChunkAccess) cube));
                            }

                            ((ICubeStatusListener) this.progressListener).onCubeStatusChange(cubePos, chunkStatusIn);
                            return completablefuture1;
                        } else {
                            return this.scheduleCubeGeneration(chunkHolderIn, chunkStatusIn);
                        }
                    }
                }, this.mainThreadExecutor));
        }
    }

    // func_219205_a, getDependencyStatus
    private ChunkStatus getCubeDependencyStatus(ChunkStatus status, int distance) {
        ChunkStatus parent;
        if (distance == 0) {
            parent = status.getParent();
        } else {
            parent = CubeStatus.getStatus(CubeStatus.getDistance(status) + distance);
        }

        return parent;
    }

    //scheduleChunkGeneration
    private CompletableFuture<Either<IBigCube, ChunkHolder.ChunkLoadingFailure>> scheduleCubeGeneration(ChunkHolder chunkHolderIn, ChunkStatus chunkStatusIn) {
        CubePos cubePos = ((ICubeHolder) chunkHolderIn).getCubePos();
        CompletableFuture<Either<List<IBigCube>, ChunkHolder.ChunkLoadingFailure>> future =
            this.getCubeRangeFuture(cubePos, CubeStatus.getCubeTaskRange(chunkStatusIn), (count) -> {
                return this.getCubeDependencyStatus(chunkStatusIn, count);
            });
        this.level.getProfiler().incrementCounter(() -> {
            return "cubeGenerate " + chunkStatusIn.getName();
        });
        return future.thenComposeAsync((sectionOrError) -> {
            return sectionOrError.map((neighborSections) -> {
                try {
                    CompletableFuture<Either<IBigCube, ChunkHolder.ChunkLoadingFailure>> finalFuture = Utils.unsafeCast(
                        chunkStatusIn.generate(this.level, this.generator, this.structureManager, this.lightEngine, (chunk) -> {
                            return Utils.unsafeCast(this.protoCubeToFullCube(chunkHolderIn));
                        }, Utils.unsafeCast(neighborSections)));
                    ((ICubeStatusListener) this.progressListener).onCubeStatusChange(cubePos, chunkStatusIn);
                    return finalFuture;
                } catch (Exception exception) {
                    CrashReport crashreport = CrashReport.forThrowable(exception, "Exception generating new chunk");
                    CrashReportCategory crashreportcategory = crashreport.addCategory("Chunk to be generated");
                    crashreportcategory.setDetail("Location", String.format("%d,%d,%d", cubePos.getX(), cubePos.getY(), cubePos.getZ()));
                    crashreportcategory.setDetail("Position hash", CubePos.asLong(cubePos.getX(), cubePos.getY(), cubePos.getZ()));
                    crashreportcategory.setDetail("Generator", this.generator);
                    throw new ReportedException(crashreport);
                }
            }, (p_219211_2_) -> {
                this.releaseLightTicket(cubePos);
                return CompletableFuture.completedFuture(Either.right(p_219211_2_));
            });
        }, (runnable) -> {
            this.cubeWorldgenMailbox.tell(CubeTaskPriorityQueueSorter.createMsg(chunkHolderIn, runnable));
        });
    }

    // func_219236_a, getChunkRangeFuture
    public CompletableFuture<Either<List<IBigCube>, ChunkHolder.ChunkLoadingFailure>> getCubeRangeFuture(
        CubePos pos, int radius, IntFunction<ChunkStatus> getParentStatus) {
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
                    ICubeHolder chunkholder = (ICubeHolder) this.getUpdatingCubeIfPresent(posLong);
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
                        CompletableFuture<Either<IBigCube, ChunkHolder.ChunkLoadingFailure>> future =
                            chunkholder.getOrScheduleCubeFuture(parentStatus, Utils.unsafeCast(this));
                        future.whenComplete((either, error) -> collectorFuture.add(idx2, either, error));
                    }
                    ++cubeIdx;
                }
            }
        }

//        CompletableFuture<List<Either<IBigCube, ChunkHolder.IChunkLoadingError>>> futures = Util.gather(list);
        return collectorFuture.thenApply((cubeEithers) -> {
            List<IBigCube> returnFutures = Lists.newArrayList();
            int i = 0;

            for (final Either<IBigCube, ChunkHolder.ChunkLoadingFailure> either : cubeEithers) {
                Optional<IBigCube> optional = either.left();
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

    // func_219209_c, releaseLightTicket
    @Override
    public void releaseLightTicket(CubePos cubePos) {
        this.mainThreadExecutor.tell(Util.name(() -> {
            ((ITicketManager) this.distanceManager).removeCubeTicket(CCTicketType.CCLIGHT,
                cubePos, 33 + CubeStatus.getDistance(ChunkStatus.FEATURES), cubePos);
        }, () -> {
            return "release light ticket " + cubePos;
        }));
    }

    // func_219200_b, protoChunkToFullChunk
    private CompletableFuture<Either<IBigCube, ChunkHolder.ChunkLoadingFailure>> protoCubeToFullCube(ChunkHolder holder) {
        CompletableFuture<Either<IBigCube, ChunkHolder.ChunkLoadingFailure>> fullFuture =
            ((ICubeHolder) holder).getCubeFutureIfPresentUnchecked(ChunkStatus.FULL.getParent());
        return fullFuture.thenApplyAsync((sectionOrError) -> {
            ChunkStatus chunkstatus = ICubeHolder.getCubeStatusFromLevel(holder.getTicketLevel());
            return !chunkstatus.isOrAfter(ChunkStatus.FULL) ? ICubeHolder.MISSING_CUBE : sectionOrError.mapLeft((prevCube) -> {
                CubePos cubePos = ((ICubeHolder) holder).getCubePos();
                BigCube cube;
                if (prevCube instanceof CubePrimerWrapper) {
                    cube = ((CubePrimerWrapper) prevCube).getCube();
                } else {
                    cube = new BigCube(this.level, (CubePrimer) prevCube, (bigCube) -> {
                        //TODO: implement new entities consumer for cube
//                        postLoadProtoChunk(this.level, protoChunk.getEntities());
                    });
                    ((ICubeHolder) holder).replaceProtoCube(new CubePrimerWrapper(cube, level));
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
                runnable, ((ICubeHolder) holder).getCubePos().asLong(), holder::getTicketLevel));
        });
    }

    // func_222961_b, unpackTicks
    @Override
    public CompletableFuture<Either<BigCube, ChunkHolder.ChunkLoadingFailure>> unpackCubeTicks(ChunkHolder chunkHolder) {
        return ((ICubeHolder) chunkHolder).getOrScheduleCubeFuture(ChunkStatus.FULL, (ChunkMap) (Object) this).thenApplyAsync((p_222976_0_) -> {
            return p_222976_0_.mapLeft((icube) -> {
                BigCube cube = (BigCube) icube;
                //TODO: implement rescheduleTicks for cube
                //cube.unpackTicks();
                return cube;
            });
        }, (p_222962_2_) -> {
            this.cubeMainThreadMailbox.tell(CubeTaskPriorityQueueSorter.createMsg(chunkHolder, p_222962_2_));
        });
    }

    // func_219179_a, postProcess
    @Override
    public CompletableFuture<Either<BigCube, ChunkHolder.ChunkLoadingFailure>> postProcessCube(ChunkHolder chunkHolder) {
        CubePos cubePos = ((ICubeHolder) chunkHolder).getCubePos();
        CompletableFuture<Either<List<IBigCube>, ChunkHolder.ChunkLoadingFailure>> completablefuture = this.getCubeRangeFuture(cubePos, 1,
            (p_219172_0_) -> {
                return ChunkStatus.FULL;
            });
        CompletableFuture<Either<BigCube, ChunkHolder.ChunkLoadingFailure>> completablefuture1 =
            completablefuture.thenApplyAsync((p_219239_0_) -> {
                return p_219239_0_.flatMap((p_219208_0_) -> {
                    BigCube cube = (BigCube) p_219208_0_.get(p_219208_0_.size() / 2);
                    //TODO: implement cube#postProcess
                    //cube.postProcess();
                    return Either.left(cube);
                });
            }, (p_219230_2_) -> {
                this.cubeMainThreadMailbox.tell(CubeTaskPriorityQueueSorter.createMsg(chunkHolder, p_219230_2_));
            });
        completablefuture1.thenAcceptAsync((cubeLoadingErrorEither) -> {
            cubeLoadingErrorEither.mapLeft((cube) -> {
                this.tickingGeneratedCubes.getAndIncrement();
                Object[] objects = new Object[2];
                this.getPlayers(cubePos, false).forEach((serverPlayerEntity) -> {
                    this.playerLoadedCube(serverPlayerEntity, objects, cube);
                });
                for (int dx = 0; dx < IBigCube.DIAMETER_IN_SECTIONS; dx++) {
                    for (int dz = 0; dz < IBigCube.DIAMETER_IN_SECTIONS; dz++) {
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
        return completablefuture1;
    }

    @Redirect(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;write(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/nbt/CompoundTag;)V"))
    private void on$writeChunk(ChunkMap chunkManager, ChunkPos chunkPos, CompoundTag chunkNBT) {
        regionCubeIO.saveChunkNBT(chunkPos, chunkNBT);
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Redirect(method = "lambda$scheduleChunkLoad$14(Lnet/minecraft/world/level/ChunkPos;)Lcom/mojang/datafixers/util/Either;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;readChunk(Lnet/minecraft/world/level/ChunkPos;)Lnet/minecraft/nbt/CompoundTag;"))
    private CompoundTag on$readChunk(ChunkMap chunkManager, ChunkPos chunkPos) {
        try {
            //noinspection ConstantConditions
            return regionCubeIO.loadChunkNBT(chunkPos);
        } catch (IOException e) {
            LOGGER.error("Couldn't load chunk {}", chunkPos, e);
            //noinspection ConstantConditions
            return null;
        }
    }

    //readChunk
    @Nullable
    private CompoundTag readCube(CubePos cubePos) throws IOException {
        return this.regionCubeIO.loadCubeNBT(cubePos);
//        return partialCubeData == null ? null : partialCubeData.getNbt(); // == null ? null : this.upgradeChunkTag(this.level.dimension(), this.overworldDataStorage, compoundnbt);
    }

    //scheduleChunkLoad
    private CompletableFuture<Either<IBigCube, ChunkHolder.ChunkLoadingFailure>> scheduleCubeLoad(CubePos cubePos) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.level.getProfiler().incrementCounter("cubeLoad");

                CompoundTag compoundnbt = regionCubeIO.loadCubeNBT(cubePos);
                if (compoundnbt != null) {
                    boolean flag = compoundnbt.contains("Level", 10) && compoundnbt.getCompound("Level").contains("Status", 8);
                    if (flag) {
                        IBigCube iBigCube = CubeSerializer.read(this.level, this.structureManager, null, cubePos, compoundnbt);
                        this.markCubePosition(cubePos, iBigCube.getCubeStatus().getChunkType());
                        return Either.left(iBigCube);
                    }
                    LOGGER.error("Cube file at {} is missing level data, skipping", cubePos);
                }
            } catch (ReportedException reportedexception) {
                Throwable throwable = reportedexception.getCause();
                if (!(throwable instanceof IOException)) {
                    throw reportedexception;
                }

                LOGGER.error("Couldn't load cube {}", cubePos, throwable);
            } catch (Exception exception) {
                LOGGER.error("Couldn't load cube {}", cubePos, exception);
            }

            return Either.left(new CubePrimer(cubePos, UpgradeData.EMPTY, level));
        }, this.mainThreadExecutor);
    }

    // func_219220_a, getUpdatingChunkIfPresent
    @Nullable
    protected ChunkHolder getUpdatingCubeIfPresent(long cubePosIn) {
        return this.updatingCubeMap.get(cubePosIn);
    }

    // getPlayers
    public Stream<ServerPlayer> getPlayers(CubePos pos, boolean boundaryOnly) {
        int viewDistanceCubes = Coords.sectionToCubeRenderDistance(this.viewDistance);
        return this.playerMap.getPlayers(pos.asLong()).filter((serverPlayerEntity) -> {
            int i = IChunkManager.getCubeChebyshevDistance(pos, serverPlayerEntity, true);
            if (i > viewDistanceCubes) {
                return false;
            } else {
                return !boundaryOnly || i == viewDistanceCubes;
            }
        });
    }

    /**
     * @author NotStirred
     * @reason Due to vanilla calling ChunkManager#updatePlayerPos, which updates player#managedSectionPos, this is required.
     */
    @Overwrite
    void updatePlayerStatus(ServerPlayer player, boolean track) {
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
                ((ITicketManager) this.distanceManager).addCubePlayer(CubePos.from(SectionPos.of(player)), player);
            }
        } else {
            SectionPos managedSectionPos = player.getLastSectionPos(); //Vanilla
            CubePos cubePos = CubePos.from(managedSectionPos);
            this.playerMap.removePlayer(cubePos.asChunkPos().toLong(), player);
            if (!cannotGenerateChunksTracker) {
                this.distanceManager.removePlayer(managedSectionPos, player); //Vanilla
                ((ITicketManager) this.distanceManager).removeCubePlayer(cubePos, player);
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
        int viewDistanceCubes = Coords.sectionToCubeRenderDistance(this.viewDistance);
        for (int ix = xFloor - viewDistanceCubes; ix <= xFloor + viewDistanceCubes; ++ix) {
            for (int iy = yFloor - viewDistanceCubes; iy <= yFloor + viewDistanceCubes; ++iy) {
                for (int iz = zFloor - viewDistanceCubes; iz <= zFloor + viewDistanceCubes; ++iz) {
                    CubePos cubePos = CubePos.of(ix, iy, iz);
                    this.updateCubeTracking(player, cubePos, new Object[2], !track, track);
                }
            }
        }
    }

    //@Inject(method = "updatePlayerPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;floor(D)I", ordinal = 0),
    // cancellable = true)

    /**
     * @author NotStirred
     * @reason To fix crash when vanilla updated player#managedSectionPos
     */
    @Overwrite
    public void move(ServerPlayer player) {

        for (ChunkMap.TrackedEntity trackedEntity : this.entityMap.values()) {
            if (((EntityTrackerAccess) trackedEntity).getEntity() == player) {
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
                ((ITicketManager) this.distanceManager).removeCubePlayer(cubePosManaged, player);

            }

            // update the position if generation is allowed now
            if (!nowNoGenerate) {
                // we are mixin into this method, so it should work as this:
                this.distanceManager.addPlayer(newSectionPos, player); //Vanilla
                ((ITicketManager) this.distanceManager).addCubePlayer(newCubePos, player);
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
        int viewDistanceCubes = Coords.sectionToCubeRenderDistance(this.viewDistance);

        int newCubeX = Coords.getCubeXForEntity(player);
        int newCubeY = Coords.getCubeYForEntity(player);
        int newCubeZ = Coords.getCubeZForEntity(player);

        int managedX = cubePosManaged.getX();
        int managedY = cubePosManaged.getY();
        int managedZ = cubePosManaged.getZ();

        if (Math.abs(managedX - newCubeX) <= viewDistanceCubes * 2 &&
            Math.abs(managedY - newCubeY) <= viewDistanceCubes * 2 &&
            Math.abs(managedZ - newCubeZ) <= viewDistanceCubes * 2) {
            int minX = Math.min(newCubeX, managedX) - viewDistanceCubes;
            int minY = Math.min(newCubeY, managedY) - viewDistanceCubes;
            int minZ = Math.min(newCubeZ, managedZ) - viewDistanceCubes;
            int maxX = Math.max(newCubeX, managedX) + viewDistanceCubes;
            int maxY = Math.max(newCubeY, managedY) + viewDistanceCubes;
            int maxZ = Math.max(newCubeZ, managedZ) + viewDistanceCubes;

            for (int ix = minX; ix <= maxX; ++ix) {
                for (int iz = minZ; iz <= maxZ; ++iz) {
                    for (int iy = minY; iy <= maxY; ++iy) {
                        CubePos cubePos1 = CubePos.of(ix, iy, iz);
                        boolean loadedBefore = IChunkManager.getCubeDistance(cubePos1, managedX, managedY, managedZ) <= viewDistanceCubes;
                        boolean loadedNow = IChunkManager.getCubeDistance(cubePos1, newCubeX, newCubeY, newCubeZ) <= viewDistanceCubes;
                        this.updateCubeTracking(player, cubePos1, new Object[2], loadedBefore, loadedNow);
                    }
                }
            }
        } else {
            for (int ix = managedX - viewDistanceCubes; ix <= managedX + viewDistanceCubes; ++ix) {
                for (int iz = managedZ - viewDistanceCubes; iz <= managedZ + viewDistanceCubes; ++iz) {
                    for (int iy = managedY - viewDistanceCubes; iy <= managedY + viewDistanceCubes; ++iy) {
                        CubePos cubePos2 = CubePos.of(ix, iy, iz);
                        this.updateCubeTracking(player, cubePos2, new Object[2], true, false);
                    }
                }
            }

            for (int ix = newCubeX - viewDistanceCubes; ix <= newCubeX + viewDistanceCubes; ++ix) {
                for (int iz = newCubeZ - viewDistanceCubes; iz <= newCubeZ + viewDistanceCubes; ++iz) {
                    for (int iy = newCubeY - viewDistanceCubes; iy <= newCubeY + viewDistanceCubes; ++iy) {
                        CubePos cubePos3 = CubePos.of(ix, iy, iz);
                        this.updateCubeTracking(player, cubePos3, new Object[2], false, true);
                    }
                }
            }
        }

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

    // this needs to be at HEAD, otherwise we are not going to see the view distance being different
    @Inject(method = "setViewDistance", at = @At("HEAD"))
    protected void setViewDistance(int newViewDistance, CallbackInfo ci) {
        int viewDistanceSections = Mth.clamp(newViewDistance + 1, 3, 33);
        int newViewDistanceCubes = Coords.sectionToCubeRenderDistance(viewDistanceSections);
        int viewDistanceCubes = Coords.sectionToCubeRenderDistance(this.viewDistance);
        if (newViewDistanceCubes != viewDistanceCubes) {
            for (ChunkHolder chunkholder : this.updatingCubeMap.values()) {
                CubePos cubePos = ((ICubeHolder) chunkholder).getCubePos();
                Object[] objects = new Object[2];
                this.getPlayers(cubePos, false).forEach((serverPlayerEntity) -> {
                    int k = IChunkManager.getCubeChebyshevDistance(cubePos, serverPlayerEntity, true);
                    boolean wasLoaded = k <= viewDistanceCubes;
                    boolean isLoaded = k <= newViewDistanceCubes;
                    this.updateCubeTracking(serverPlayerEntity, cubePos, objects, wasLoaded, isLoaded);
                });
            }
        }

    }

    // func_223489_c, updatePlayerPos
    private SectionPos updatePlayerCubePos(ServerPlayer serverPlayerEntityIn) {
        SectionPos sectionpos = SectionPos.of(serverPlayerEntityIn);
        serverPlayerEntityIn.setLastSectionPos(sectionpos);
        PacketDispatcher.sendTo(new PacketUpdateCubePosition(sectionpos), serverPlayerEntityIn);
        serverPlayerEntityIn.connection.send(new ClientboundSetChunkCacheCenterPacket(sectionpos.x(), sectionpos.z()));
        return sectionpos;
    }

    protected void updateCubeTracking(ServerPlayer player, CubePos cubePosIn, Object[] packetCache, boolean wasLoaded, boolean load) {
        if (player.level == this.level) {
            //TODO: reimplement forge event
            //net.minecraftforge.event.ForgeEventFactory.fireChunkWatch(wasLoaded, load, player, cubePosIn, this.world);
            if (load && !wasLoaded) {
                ChunkHolder chunkholder = ((IChunkManager) this).getImmutableCubeHolder(cubePosIn.asLong());
                if (chunkholder != null) {
                    BigCube cube = ((ICubeHolder) chunkholder).getCubeIfComplete();
                    if (cube != null) {
                        this.playerLoadedCube(player, packetCache, cube);
                        for (int dx = 0; dx < IBigCube.DIAMETER_IN_SECTIONS; dx++) {
                            for (int dz = 0; dz < IBigCube.DIAMETER_IN_SECTIONS; dz++) {
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

    // func_222973_a, packTicks
    @Override
    public CompletableFuture<Void> packCubeTicks(BigCube cubeIn) {
        return this.mainThreadExecutor.submit(() -> {
            //TODO: implement saveCubeScheduleTicks
            /*
            This means adding a method to MixinChunkSection to handle the ticking of blocks,
            chunksection needs a blocksToBeTickedField, and that will probably need to be initialised in a secondary afterInit method
             */
            //sectionIn.saveScheduledTicks(this.world);
        });
    }

    // func_219188_b, getEntityTickingRangeFuture
    public CompletableFuture<Either<BigCube, ChunkHolder.ChunkLoadingFailure>> getCubeEntityTickingRangeFuture(CubePos pos) {
        return this.getCubeRangeFuture(pos, 2, (index) -> {
            return ChunkStatus.FULL;
        }).thenApplyAsync((eitherSectionError) -> {
            return eitherSectionError.mapLeft((cubes) -> {
                return (BigCube) cubes.get(cubes.size() / 2);
            });
        }, this.mainThreadExecutor);
    }

    private void untrackPlayerChunk(ServerPlayer player, CubePos cubePosIn) {
        if (player.isAlive()) {
            PacketDispatcher.sendTo(new PacketUnloadCube(cubePosIn), player);
        }
    }

    @Nullable
    @Redirect(method = "playerLoadedChunk", at = @At(value = "NEW", target = "net/minecraft/network/protocol/game/ClientboundLightUpdatePacket"))
    private ClientboundLightUpdatePacket onVanillaLightPacketConstruct(ChunkPos pos, LevelLightEngine lightManager, BitSet bits1, BitSet bits2, boolean bool) {
        return new ClientboundLightUpdatePacket();
    }

    // playerLoadedChunk
    private void playerLoadedCube(ServerPlayer player, Object[] packetCache, BigCube cubeIn) {
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
            Entity entity = ((EntityTrackerAccess) entityTracker).getEntity();
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
        return !((ITicketManager) this.distanceManager).hasCubePlayersNearby(cubePosAsLong) || this.playerMap.getPlayers(cubePosAsLong).noneMatch(
            (serverPlayer) -> !serverPlayer.isSpectator() && euclideanDistanceSquared(cubePos, serverPlayer) < (TICK_UPDATE_DISTANCE * TICK_UPDATE_DISTANCE));
    }

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

    // func_219174_c, getTickingGenerated
    @Override
    public int getTickingGeneratedCubes() {
        return this.tickingGeneratedCubes.get();
    }

    @Override
    public int sizeCubes() {
        return this.visibleCubeMap.size();
    }

    // func_219191_c, getChunkQueueLevel
    public IntSupplier getCubeQueueLevel(long cubePosIn) {
        return () -> {
            ChunkHolder chunkholder = this.getImmutableCubeHolder(cubePosIn);
            return chunkholder == null ? CubeTaskPriorityQueue.LEVEL_COUNT - 1 : Math.min(chunkholder.getQueueLevel(),
                CubeTaskPriorityQueue.LEVEL_COUNT - 1);
        };
    }

    @Inject(method = "close", at = @At("HEAD"))
    public void on$close(CallbackInfo ci) {
        regionCubeIO.flush();
    }
}