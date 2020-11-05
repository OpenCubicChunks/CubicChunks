package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.*;
import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimer;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimerWrapper;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubeStatus;
import io.github.opencubicchunks.cubicchunks.chunk.graph.CCTicketType;
import io.github.opencubicchunks.cubicchunks.chunk.ticket.CubeTaskPriorityQueue;
import io.github.opencubicchunks.cubicchunks.chunk.ticket.CubeTaskPriorityQueueSorter;
import io.github.opencubicchunks.cubicchunks.chunk.ticket.ITicketManager;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.chunk.util.Utils;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.EntityTrackerAccess;
import io.github.opencubicchunks.cubicchunks.network.*;
import io.github.opencubicchunks.cubicchunks.world.storage.RegionCubeIO;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.server.IServerWorld;
import io.github.opencubicchunks.cubicchunks.world.server.IServerWorldLightManager;
import io.github.opencubicchunks.cubicchunks.world.storage.CubeSerializer;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SMountEntityPacket;
import net.minecraft.network.play.server.SSetPassengersPacket;
import net.minecraft.network.play.server.SUpdateChunkPositionPacket;
import net.minecraft.network.play.server.SUpdateLightPacket;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.Util;
import net.minecraft.util.concurrent.DelegatedTaskExecutor;
import net.minecraft.util.concurrent.ITaskExecutor;
import net.minecraft.util.concurrent.ThreadTaskExecutor;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.chunk.IChunkLightProvider;
import net.minecraft.world.chunk.PlayerGenerationTracker;
import net.minecraft.world.chunk.listener.IChunkStatusListener;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.ServerWorldLightManager;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraft.world.storage.SaveFormat;
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

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
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

import static io.github.opencubicchunks.cubicchunks.CubicChunks.LOGGER;
import static io.github.opencubicchunks.cubicchunks.utils.Coords.sectionToCube;

@Mixin(ChunkManager.class)
public abstract class MixinChunkManager implements IChunkManager {

    private CubeTaskPriorityQueueSorter cubeQueueSorter;

    private final Long2ObjectLinkedOpenHashMap<ChunkHolder> updatingCubeMap = new Long2ObjectLinkedOpenHashMap<>();
    private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleCubeMap = this.updatingCubeMap.clone();

    private final LongSet cubesToDrop = new LongOpenHashSet();
    private final LongSet cubeEntitiesInLevel = new LongOpenHashSet();
    private final Long2ObjectLinkedOpenHashMap<ChunkHolder> pendingCubeUnloads = new Long2ObjectLinkedOpenHashMap<>();

    // field_219264_r, worldgenMailbox
    private ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> cubeWorldgenMailbox;
    // field_219265_s, mainThreadMailbox
    private ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> cubeMainThreadMailbox;

    private final AtomicInteger tickingGeneratedCubes = new AtomicInteger();

    private final Queue<Runnable> saveCubeTasks = Queues.newConcurrentLinkedQueue();

    private RegionCubeIO regionCubeIO;

    @Shadow @Final private ServerWorldLightManager lightEngine;

    @Shadow private boolean modified;

    @Shadow @Final private ChunkManager.ProxyTicketManager distanceManager;

    @Shadow @Final private ServerWorld level;

    @Shadow @Final private TemplateManager structureManager;

    @Shadow @Final private ThreadTaskExecutor<Runnable> mainThreadExecutor;

    @Shadow @Final private IChunkStatusListener progressListener;

    @Shadow @Final private ChunkGenerator generator;

    @Shadow @Final private File storageFolder;

    @Shadow private int viewDistance;

    @Shadow @Final private Int2ObjectMap<ChunkManager.EntityTracker> entityMap;

    @Shadow @Final private PlayerGenerationTracker playerMap;

    @Shadow protected abstract boolean skipPlayer(ServerPlayerEntity player);

    @Shadow protected abstract void updateChunkTracking(ServerPlayerEntity player, ChunkPos chunkPosIn, IPacket<?>[] packetCache,
            boolean wasLoaded, boolean load);

    @Shadow private static int checkerboardDistance(ChunkPos chunkPosIn, int x, int y) {
        throw new Error("Mixin didn't apply");
    }

    @Inject(method = "<init>", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onConstruct(ServerWorld worldIn,
            SaveFormat.LevelSave levelSave,
            DataFixer p_i51538_3_,
            TemplateManager templateManagerIn,
            Executor p_i51538_5_,
            ThreadTaskExecutor<Runnable> mainThreadIn,
            IChunkLightProvider p_i51538_7_,
            ChunkGenerator generatorIn,
            IChunkStatusListener p_i51538_9_,
            Supplier<DimensionSavedDataManager> p_i51538_10_,
            int p_i51538_11_,
            boolean p_i232602_12_,
            CallbackInfo ci, DelegatedTaskExecutor delegatedtaskexecutor,
            ITaskExecutor itaskexecutor, DelegatedTaskExecutor delegatedtaskexecutor1) {

        this.cubeQueueSorter = new CubeTaskPriorityQueueSorter(ImmutableList.of(delegatedtaskexecutor,
                itaskexecutor, delegatedtaskexecutor1), p_i51538_5_, Integer.MAX_VALUE);
        this.cubeWorldgenMailbox = this.cubeQueueSorter.createExecutor(delegatedtaskexecutor, false);
        this.cubeMainThreadMailbox = this.cubeQueueSorter.createExecutor(itaskexecutor, false);

        ((IServerWorldLightManager)this.lightEngine).postConstructorSetup(this.cubeQueueSorter,
                this.cubeQueueSorter.createExecutor(delegatedtaskexecutor1, false));

        try {
            regionCubeIO = new RegionCubeIO(worldIn, storageFolder);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Inject(method = "tick(Ljava/util/function/BooleanSupplier;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ChunkManager;processUnloads(Ljava/util/function/BooleanSupplier;)V"))
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

                    IBigCube cube = cubeFuture.join();
                    return cube;
                }).filter((cube) -> cube instanceof CubePrimerWrapper || cube instanceof BigCube)
                    .filter(this::cubeSave).forEach((unsavedCube) -> savedAny.setTrue());
            } while (savedAny.isTrue());

            this.processCubeUnloads(() -> true);
            //this.flushWorker();
            LOGGER.info("ThreadedAnvilChunkStorage ({}): All cubes are saved", this.storageFolder.getName());
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
//        this.poiManager.flush(cube.getCubePos());
        if (!cube.isDirty()) {
            return false;
        } else {
            cube.setCubeLastSaveTime(level.getGameTime());
            cube.setDirty(false);
            CubePos cubePos = cube.getCubePos();

            try {
                //TODO: implement isExistingCubeFull and by extension cubeTypeCache
                ChunkStatus status = cube.getCubeStatus();
//                if (status.getChunkType() != ChunkStatus.Type.LEVELCHUNK) {
//                    if (this.isExistingChunkFull(cubePos)) {
//                        return false;
//                    }
//
//                    if (status == ChunkStatus.EMPTY && p_219229_1_.getAllStarts().values().stream().noneMatch(StructureStart::isValid)) {
//                        return false;
//                    }
//                }

                if (status.getChunkType() != ChunkStatus.Type.LEVELCHUNK) {
                    CompoundNBT compoundnbt = this.readCube(cubePos);
                    if (compoundnbt != null && CubeSerializer.getChunkStatus(compoundnbt) == ChunkStatus.Type.LEVELCHUNK) {
                        return false;
                    }

                    //TODO: SAVE FORMAT : reimplement structures
//                    if (status == ChunkStatus.EMPTY && cube.getStructureStarts().values().stream().noneMatch(StructureStart::isValid)) {
//                        return false;
//                    }
                }

                CompoundNBT compoundnbt = CubeSerializer.write(this.level, cube);
                //TODO: FORGE EVENT : reimplement ChunkDataEvent#Save
//                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkDataEvent.Save(p_219229_1_, p_219229_1_.getWorldForge() != null ? p_219229_1_.getWorldForge() : this.level, compoundnbt));
                this.regionCubeIO.storeCubeNBT(cubePos, compoundnbt);
//                this.markPosition(cubePos, status.getChunkType());
//                LOGGER.info("Saving cube {}", cubePos);
                return true;

            } catch (Exception exception) {
                LOGGER.error("Failed to save chunk {},{},{}", cubePos.getX(), cubePos.getY(), cubePos.getZ(), exception);
                return false;
            }
        }
    }

    // processUnloads
    private void processCubeUnloads(BooleanSupplier hasMoreTime) {
        LongIterator longiterator = this.cubesToDrop.iterator();

        for(int i = 0; longiterator.hasNext() && (hasMoreTime.getAsBoolean() || i < 200 || this.cubesToDrop.size() > 2000); longiterator.remove()) {
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
        while((hasMoreTime.getAsBoolean() || this.saveCubeTasks.size() > 2000) && (runnable = this.saveCubeTasks.poll()) != null) {
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
                        ((BigCube)icube).setLoaded(false);
                        //TODO: reimplement forge event ChunkEvent#Unload.
                        //net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Unload((Chunk)cube));
                    }

                    this.cubeSave(icube);
                    if (this.cubeEntitiesInLevel.remove(cubePos) && icube instanceof BigCube) {
                        ((IServerWorld)this.level).onCubeUnloading((BigCube)icube);
                    }

                    ((IServerWorldLightManager)this.lightEngine).setCubeStatusEmpty(icube.getCubePos());
                    this.lightEngine.tryScheduleUpdate();
                    ((ICubeStatusListener) this.progressListener).onCubeStatusChange(icube.getCubePos(), (ChunkStatus)null);
                }

            }
        }, this.saveCubeTasks::add).whenComplete((p_223171_1_, p_223171_2_) -> {
            if (p_223171_2_ != null) {
                LOGGER.error("Failed to save chunk " + ((ICubeHolder) chunkHolderIn).getCubePos(), p_223171_2_);
            }
        });
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
    public ChunkHolder getImmutableCubeHolder(long cubePosIn)
    {
        return this.visibleCubeMap.get(cubePosIn);
    }

    // TODO: remove when cubic chunks versions are done
    @SuppressWarnings("UnresolvedMixinReference")
    // lambda$func_219244_a$13 or lambda$schedule$13 or lambda$null$13
    @Inject(method = "*", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/listener/IChunkStatusListener;onStatusChange(Lnet/minecraft/util/math/ChunkPos;"
                    + "Lnet/minecraft/world/chunk/ChunkStatus;)V")
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
            target = "Lnet/minecraft/world/chunk/listener/IChunkStatusListener;onStatusChange(Lnet/minecraft/util/math/ChunkPos;"
                    + "Lnet/minecraft/world/chunk/ChunkStatus;)V")
    )
    @Group(name = "MixinChunkManager.onScheduleSaveStatusChange", min = 1, max = 1)
    private void onScheduleSaveStatusChange(ChunkHolder chunkHolderIn, CompletableFuture<?> completablefuture,
            long chunkPosIn, IChunk p_219185_5_, CallbackInfo ci) {
        if (((ICubeHolder) chunkHolderIn).getCubePos() != null) {
            ((ICubeStatusListener) progressListener).onCubeStatusChange(
                    ((ICubeHolder) chunkHolderIn).getCubePos(), null);
        }
    }

    @SuppressWarnings({"UnresolvedMixinReference", "target"})
    @Inject(method = "lambda$null$18(Lnet/minecraft/world/chunk/ChunkStatus;Lnet/minecraft/world/server/ChunkHolder;Lnet/minecraft/util/math/ChunkPos;Ljava/util/List;)Ljava/util/concurrent/CompletableFuture;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/chunk/listener/IChunkStatusListener;onStatusChange(Lnet/minecraft/util/math/ChunkPos;"
                    + "Lnet/minecraft/world/chunk/ChunkStatus;)V"
            )
    )
    private void onGenerateStatusChange(ChunkStatus chunkStatusIn, ChunkHolder chunkHolderIn, ChunkPos chunkpos, List<?> p_223148_4_,
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
    public CompletableFuture<Either<IBigCube, ChunkHolder.IChunkLoadingError>> scheduleCube(ChunkHolder chunkHolderIn, ChunkStatus chunkStatusIn) {
        CubePos cubePos = ((ICubeHolder) chunkHolderIn).getCubePos();
        if (chunkStatusIn == ChunkStatus.EMPTY) {
            return this.scheduleCubeLoad(cubePos);
        } else {
            CompletableFuture<Either<IBigCube, ChunkHolder.IChunkLoadingError>> completablefuture = Utils.unsafeCast(
                    ((ICubeHolder) chunkHolderIn).getOrScheduleCubeFuture(chunkStatusIn.getParent(), (ChunkManager) (Object) this)
            );
            return Utils.unsafeCast(completablefuture.thenComposeAsync(
                    (Either<IBigCube, ChunkHolder.IChunkLoadingError> inputSection) -> {
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
                                CompletableFuture<Either<IBigCube, ChunkHolder.IChunkLoadingError>> completablefuture1;
                                if (chunkStatusIn == ChunkStatus.LIGHT) {
                                    completablefuture1 = this.scheduleCubeGeneration(chunkHolderIn, chunkStatusIn);
                                } else {
                                    completablefuture1 = Utils.unsafeCast(
                                            chunkStatusIn.load(this.level, this.structureManager, this.lightEngine, (chunk) -> {
                                                return Utils.unsafeCast(this.protoCubeToFullCube(chunkHolderIn));
                                            }, (IChunk) cube));
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
    private CompletableFuture<Either<IBigCube, ChunkHolder.IChunkLoadingError>> scheduleCubeGeneration(ChunkHolder chunkHolderIn, ChunkStatus chunkStatusIn) {
        CubePos cubePos = ((ICubeHolder) chunkHolderIn).getCubePos();
        CompletableFuture<Either<List<IBigCube>, ChunkHolder.IChunkLoadingError>> future =
                this.getCubeRangeFuture(cubePos, CubeStatus.getCubeTaskRange(chunkStatusIn), (count) -> {
                    return this.getCubeDependencyStatus(chunkStatusIn, count);
                });
        this.level.getProfiler().incrementCounter(() -> {
            return "cubeGenerate " + chunkStatusIn.getName();
        });
        return future.thenComposeAsync((sectionOrError) -> {
            return sectionOrError.map((neighborSections) -> {
                try {
                    CompletableFuture<Either<IBigCube, ChunkHolder.IChunkLoadingError>> finalFuture = Utils.unsafeCast(
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
    public CompletableFuture<Either<List<IBigCube>, ChunkHolder.IChunkLoadingError>> getCubeRangeFuture(
            CubePos pos, int radius, IntFunction<ChunkStatus> getParentStatus) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        int requiredAreaLength = (2*radius + 1);
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
                        return CompletableFuture.completedFuture(Either.right(new ChunkHolder.IChunkLoadingError() {
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
                        CompletableFuture<Either<IBigCube, ChunkHolder.IChunkLoadingError>> future =
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

            for (final Either<IBigCube, ChunkHolder.IChunkLoadingError> either : cubeEithers) {
                Optional<IBigCube> optional = either.left();
                if (!optional.isPresent()) {
                    final int d = radius * 2 + 1;
                    final int idx = i;
                    //noinspection MixinInnerClass
                    return Either.right(new ChunkHolder.IChunkLoadingError() {
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
    private CompletableFuture<Either<IBigCube, ChunkHolder.IChunkLoadingError>> protoCubeToFullCube(ChunkHolder holder) {
        CompletableFuture<Either<IBigCube, ChunkHolder.IChunkLoadingError>> fullFuture =
                ((ICubeHolder) holder).getCubeFutureIfPresentUnchecked(ChunkStatus.FULL.getParent());
        return fullFuture.thenApplyAsync((sectionOrError) -> {
            ChunkStatus chunkstatus = ICubeHolder.getCubeStatusFromLevel(holder.getTicketLevel());
            return !chunkstatus.isOrAfter(ChunkStatus.FULL) ? ICubeHolder.MISSING_CUBE : sectionOrError.mapLeft((prevCube) -> {
                CubePos cubePos = ((ICubeHolder) holder).getCubePos();
                BigCube cube;
                if (prevCube instanceof CubePrimerWrapper) {
                        cube = ((CubePrimerWrapper)prevCube).getCube();
                } else {
                    cube = new BigCube(this.level, (CubePrimer) prevCube);
                    ((ICubeHolder) holder).replaceProtoCube(new CubePrimerWrapper(cube));
                }

                // TODO: reimplement
                //chunkSection.setLocationType(() -> {
                //    return ChunkHolder.getLocationTypeFromLevel(holder.getChunkLevel());
                //});
                cube.postLoad();
                if (this.cubeEntitiesInLevel.add(cubePos.asLong())) {
                    cube.setLoaded(true);
                    this.level.addAllPendingBlockEntities(cube.getTileEntityMap().values());
                    List<Entity> entities = null;
                    ClassInheritanceMultiMap<Entity>[] entityLists = cube.getEntityLists();

                    for(int idxList = 0; idxList < entityLists.length; ++idxList) {
                        //TODO: reimplement forge ChunkEvent#Load
                        //net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Load(chunk));
                        for (Entity entity : entityLists[idxList]) {
                            if (entity instanceof PlayerEntity || this.level.loadFromChunk(entity)) {
                                continue;
                            }
                            if (entities == null) {
                                entities = Lists.newArrayList(entity);
                            } else {
                                entities.add(entity);
                            }
                        }
                    }
                    if (entities != null) {
                        entities.forEach(cube::removeEntity);
                    }
                    // TODO: reimplement forge ChunkEvent#Load
                    // net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Load(chunkSection));
                }
                return (IBigCube) cube;
            });
        }, (runnable) -> {
            this.cubeMainThreadMailbox.tell(CubeTaskPriorityQueueSorter.createMsg(
                    runnable, ((ICubeHolder) holder).getCubePos().asLong(), holder::getTicketLevel));
        });
    }

    // func_222961_b, unpackTicks
    @Override
    public CompletableFuture<Either<BigCube, ChunkHolder.IChunkLoadingError>> unpackCubeTicks(ChunkHolder chunkHolder) {
        return ((ICubeHolder) chunkHolder).getOrScheduleCubeFuture(ChunkStatus.FULL, (ChunkManager) (Object) this).thenApplyAsync((p_222976_0_) -> {
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
    public CompletableFuture<Either<BigCube, ChunkHolder.IChunkLoadingError>> postProcessCube(ChunkHolder chunkHolder) {
        CubePos cubePos = ((ICubeHolder) chunkHolder).getCubePos();
        CompletableFuture<Either<List<IBigCube>, ChunkHolder.IChunkLoadingError>> completablefuture = this.getCubeRangeFuture(cubePos, 1,
                (p_219172_0_) -> {
                    return ChunkStatus.FULL;
                });
        CompletableFuture<Either<BigCube, ChunkHolder.IChunkLoadingError>> completablefuture1 =
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
                return Either.left(cube);
            });
        }, (p_219202_2_) -> {
            this.cubeMainThreadMailbox.tell(CubeTaskPriorityQueueSorter.createMsg(chunkHolder, p_219202_2_));
        });
        return completablefuture1;
    }

    //readChunk
    @Nullable
    private CompoundNBT readCube(CubePos cubePos) throws IOException {
        return this.regionCubeIO.loadCubeNBT(cubePos);
//        return partialCubeData == null ? null : partialCubeData.getNbt(); // == null ? null : this.upgradeChunkTag(this.level.dimension(), this.overworldDataStorage, compoundnbt);
    }

    //scheduleChunkLoad
    private CompletableFuture<Either<IBigCube, ChunkHolder.IChunkLoadingError>> scheduleCubeLoad(CubePos cubePos) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.level.getProfiler().incrementCounter("cubeLoad");

                CompoundNBT compoundnbt = this.readCube(cubePos);
                if (compoundnbt != null) {
                    boolean flag = compoundnbt.contains("Level", 10) && compoundnbt.getCompound("Level").contains("Status", 8);
                    if (flag) {
                        IBigCube iBigCube = CubeSerializer.read(this.level, this.structureManager, null, cubePos, compoundnbt);
                        //TODO: reimplement
//                        iBigCube.setLastSaveTime(this.level.getGameTime());
//                        this.markPosition(p_223172_1_, iBigCube.getStatus().getChunkType());
                        return Either.left(iBigCube);
                    }

                    LOGGER.error("Chunk file at {} is missing level data, skipping", cubePos);
                }

//                IBigCube icube = CubeSerializer.loadCube(level, cubePos, this.storageFolder.toPath());
//                if(icube != null)
//                    return Either.left(icube);

            } catch (ReportedException reportedexception) {
                Throwable throwable = reportedexception.getCause();
                if (!(throwable instanceof IOException)) {
                    throw reportedexception;
                }

                LOGGER.error("Couldn't load cube {}", cubePos, throwable);
            } catch (Exception exception) {
                LOGGER.error("Couldn't load cube {}", cubePos, exception);
            }

            return Either.left(new CubePrimer(cubePos, null, null, null, null));
        }, this.mainThreadExecutor);
    }

    // func_219220_a, getUpdatingChunkIfPresent
    @Nullable
    protected ChunkHolder getUpdatingCubeIfPresent(long cubePosIn) {
        return this.updatingCubeMap.get(cubePosIn);
    }

    // getPlayers
    public Stream<ServerPlayerEntity> getPlayers(CubePos pos, boolean boundaryOnly) {
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
    void updatePlayerStatus(ServerPlayerEntity player, boolean track) {
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
                ((ITicketManager)this.distanceManager).addCubePlayer(CubePos.from(SectionPos.of(player)), player);
            }
        } else {
            SectionPos managedSectionPos = player.getLastSectionPos(); //Vanilla
            CubePos cubePos = CubePos.from(managedSectionPos);
            this.playerMap.removePlayer(cubePos.asChunkPos().toLong(), player);
            if (!cannotGenerateChunksTracker) {
                this.distanceManager.removePlayer(managedSectionPos, player); //Vanilla
                ((ITicketManager)this.distanceManager).removeCubePlayer(cubePos, player);
            }
        }

        //Vanilla
        int i = MathHelper.floor(player.getX()) >> 4;
        int j = MathHelper.floor(player.getZ()) >> 4;

        for(int l = i - this.viewDistance; l <= i + this.viewDistance; ++l) {
            for(int k = j - this.viewDistance; k <= j + this.viewDistance; ++k) {
                ChunkPos chunkpos = new ChunkPos(l, k);
                this.updateChunkTracking(player, chunkpos, new IPacket[2], !track, track);
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
    public void move(ServerPlayerEntity player) {

        for(ChunkManager.EntityTracker chunkmanager$entitytracker : this.entityMap.values()) {
            if (((EntityTrackerAccess)chunkmanager$entitytracker).getEntity() == player) {
                chunkmanager$entitytracker.updatePlayers(this.level.players());
            } else {
                chunkmanager$entitytracker.updatePlayer(player);
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
                ((ITicketManager)this.distanceManager).addCubePlayer(newCubePos, player);
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

        int newSectionX = MathHelper.floor(player.getX()) >> 4;
        int newSectionZ = MathHelper.floor(player.getZ()) >> 4;

        int oldSectionX = managedSectionPos.x();
        int oldSectionZ = managedSectionPos.z();
        if (Math.abs(oldSectionX - newSectionX) <= this.viewDistance * 2 && Math.abs(oldSectionZ - newSectionZ) <= this.viewDistance * 2) {
            int k2 = Math.min(newSectionX, oldSectionX) - this.viewDistance;
            int i3 = Math.min(newSectionZ, oldSectionZ) - this.viewDistance;
            int j3 = Math.max(newSectionX, oldSectionX) + this.viewDistance;
            int k3 = Math.max(newSectionZ, oldSectionZ) + this.viewDistance;

            for(int l3 = k2; l3 <= j3; ++l3) {
                for(int k1 = i3; k1 <= k3; ++k1) {
                    ChunkPos chunkpos1 = new ChunkPos(l3, k1);
                    boolean flag5 = checkerboardDistance(chunkpos1, oldSectionX, oldSectionZ) <= this.viewDistance;
                    boolean flag6 = checkerboardDistance(chunkpos1, newSectionX, newSectionZ) <= this.viewDistance;
                    this.updateChunkTracking(player, chunkpos1, new IPacket[2], flag5, flag6);
                }
            }
        } else {
            for(int i1 = oldSectionX - this.viewDistance; i1 <= oldSectionX + this.viewDistance; ++i1) {
                for(int j1 = oldSectionZ - this.viewDistance; j1 <= oldSectionZ + this.viewDistance; ++j1) {
                    ChunkPos chunkpos = new ChunkPos(i1, j1);
                    this.updateChunkTracking(player, chunkpos, new IPacket[2], true, false);
                }
            }
            for(int j2 = newSectionX - this.viewDistance; j2 <= newSectionX + this.viewDistance; ++j2) {
                for(int l2 = newSectionZ - this.viewDistance; l2 <= newSectionZ + this.viewDistance; ++l2) {
                    ChunkPos chunkpos2 = new ChunkPos(j2, l2);
                    this.updateChunkTracking(player, chunkpos2, new IPacket[2], false, true);
                }
            }
        }
    }

    // this needs to be at HEAD, otherwise we are not going to see the view distance being different
    @Inject(method = "setViewDistance", at = @At("HEAD"))
    protected void setViewDistance(int viewDistance, CallbackInfo ci) {
        int viewDistanceSections = MathHelper.clamp(viewDistance + 1, 3, 33);
        int newViewDistanceCubes = Coords.sectionToCubeRenderDistance(viewDistanceSections);
        int viewDistanceCubes = Coords.sectionToCubeRenderDistance(this.viewDistance);
        if (newViewDistanceCubes != viewDistanceCubes) {
            for(ChunkHolder chunkholder : this.updatingCubeMap.values()) {
                CubePos cubePos = ((ICubeHolder)chunkholder).getCubePos();
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
    private SectionPos updatePlayerCubePos(ServerPlayerEntity serverPlayerEntityIn) {
        SectionPos sectionpos = SectionPos.of(serverPlayerEntityIn);
        serverPlayerEntityIn.setLastSectionPos(sectionpos);
        PacketDispatcher.sendTo(new PacketUpdateCubePosition(sectionpos), serverPlayerEntityIn);
        serverPlayerEntityIn.connection.send(new SUpdateChunkPositionPacket(sectionpos.x(), sectionpos.z()));
        return sectionpos;
    }

    protected void updateCubeTracking(ServerPlayerEntity player, CubePos cubePosIn, Object[] packetCache, boolean wasLoaded, boolean load) {
        if (player.level == this.level) {
            //TODO: reimplement forge event
            //net.minecraftforge.event.ForgeEventFactory.fireChunkWatch(wasLoaded, load, player, cubePosIn, this.world);
            if (load && !wasLoaded) {
                ChunkHolder chunkholder = ((IChunkManager)this).getImmutableCubeHolder(cubePosIn.asLong());
                if (chunkholder != null) {
                    BigCube cube = ((ICubeHolder)chunkholder).getCubeIfComplete();
                    if (cube != null) {
                        this.playerLoadedCube(player, packetCache, cube);
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
    public CompletableFuture<Either<BigCube, ChunkHolder.IChunkLoadingError>> getCubeEntityTickingRangeFuture(CubePos pos) {
        return this.getCubeRangeFuture(pos, 2, (index) -> {
            return ChunkStatus.FULL;
        }).thenApplyAsync((eitherSectionError) -> {
            return eitherSectionError.mapLeft((cubes) -> {
                return (BigCube) cubes.get(cubes.size() / 2);
            });
        }, this.mainThreadExecutor);
    }

    private void untrackPlayerChunk(ServerPlayerEntity player, CubePos cubePosIn) {
        if (player.isAlive()) {
            PacketDispatcher.sendTo(new PacketUnloadCube(cubePosIn), player);
        }
    }

    @Nullable
    @Redirect(method = "playerLoadedChunk", at = @At(value = "NEW", target = "net/minecraft/network/play/server/SUpdateLightPacket"))
    private SUpdateLightPacket onVanillaLightPacketConstruct(ChunkPos pos, WorldLightManager lightManager, boolean bool) {
        return new SUpdateLightPacket();
    }

    // playerLoadedChunk
    private void playerLoadedCube(ServerPlayerEntity player, Object[] packetCache, BigCube cubeIn) {
        if (packetCache[0] == null) {
            packetCache[0] = new PacketCubes(Collections.singletonList(cubeIn));
            packetCache[1] = new PacketUpdateLight(cubeIn.getCubePos(), this.lightEngine, true);
        }

        CubePos pos = cubeIn.getCubePos();

        PacketDispatcher.sendTo(packetCache[1], player);
        PacketDispatcher.sendTo(packetCache[0], player);
        List<Entity> leashedEntities = Lists.newArrayList();
        List<Entity> passengerEntities = Lists.newArrayList();

        for (ChunkManager.EntityTracker entityTracker : this.entityMap.values()) {
            Entity entity = ((EntityTrackerAccess) entityTracker).getEntity();
            if ((entity != player) && (sectionToCube(entity.xChunk) == pos.getX()) && (sectionToCube(entity.yChunk) == pos.getY()) && (
                    sectionToCube(entity.zChunk) == pos.getZ())) {
                entityTracker.updatePlayer(player);
                if (entity instanceof MobEntity && ((MobEntity) entity).getLeashHolder() != null) {
                    leashedEntities.add(entity);
                }

                if (!entity.getPassengers().isEmpty()) {
                    passengerEntities.add(entity);
                }
            }
        }

        if (!leashedEntities.isEmpty()) {
            for (Entity entity1 : leashedEntities) {
                player.connection.send(new SMountEntityPacket(entity1, ((MobEntity) entity1).getLeashHolder()));
            }
        }

        if (!passengerEntities.isEmpty()) {
            for (Entity entity2 : passengerEntities) {
                player.connection.send(new SSetPassengersPacket(entity2));
            }
        }
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
            return chunkholder == null ? CubeTaskPriorityQueue.levelCount - 1 : Math.min(chunkholder.getQueueLevel(),
                    CubeTaskPriorityQueue.levelCount - 1);
        };
    }
}