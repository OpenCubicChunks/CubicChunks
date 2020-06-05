package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import io.github.opencubicchunks.cubicchunks.chunk.ticket.CubeTaskPriorityQueue;
import io.github.opencubicchunks.cubicchunks.world.storage.CubeSerializer;
import io.github.opencubicchunks.cubicchunks.chunk.IChunkManager;
import io.github.opencubicchunks.cubicchunks.chunk.ICube;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeHolder;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeStatusListener;
import io.github.opencubicchunks.cubicchunks.chunk.cube.Cube;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimer;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimerWrapper;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubeStatus;
import io.github.opencubicchunks.cubicchunks.chunk.graph.CCTicketType;
import io.github.opencubicchunks.cubicchunks.chunk.ticket.CubeTaskPriorityQueueSorter;
import io.github.opencubicchunks.cubicchunks.chunk.ticket.ITicketManager;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.EntityTrackerAccess;
import io.github.opencubicchunks.cubicchunks.network.PacketCubes;
import io.github.opencubicchunks.cubicchunks.network.PacketDispatcher;
import io.github.opencubicchunks.cubicchunks.network.PacketUnloadCube;
import io.github.opencubicchunks.cubicchunks.network.PacketUpdateCubePosition;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.chunk.util.Utils;
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
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SMountEntityPacket;
import net.minecraft.network.play.server.SSetPassengersPacket;
import net.minecraft.network.play.server.SUpdateChunkPositionPacket;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.Util;
import net.minecraft.util.concurrent.DelegatedTaskExecutor;
import net.minecraft.util.concurrent.ITaskExecutor;
import net.minecraft.util.concurrent.ThreadTaskExecutor;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.SectionPos;
import net.minecraft.village.PointOfInterestManager;
import net.minecraft.world.chunk.Chunk;
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
import net.minecraft.world.storage.SessionLockException;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.File;
import java.io.IOException;
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

@Mixin(ChunkManager.class)
public abstract class MixinChunkManager implements IChunkManager {

    private CubeTaskPriorityQueueSorter cubeTaskPriorityQueueSorter;

    private final Long2ObjectLinkedOpenHashMap<ChunkHolder> loadedCubes = new Long2ObjectLinkedOpenHashMap<>();
    private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> immutableLoadedCubes = this.loadedCubes.clone();

    private final LongSet unloadableCubes = new LongOpenHashSet();
    private final LongSet loadedCubePositions = new LongOpenHashSet();
    private final Long2ObjectLinkedOpenHashMap<ChunkHolder> cubesToUnload = new Long2ObjectLinkedOpenHashMap<>();

    // field_219264_r
    private ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> worldgenExecutor;
    // field_219265_s
    private ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> mainExecutor;

    private final AtomicInteger cubesLoaded = new AtomicInteger();

    private final Queue<Runnable> saveCubeTasks = Queues.newConcurrentLinkedQueue();

    @Shadow @Final private static Logger LOGGER;

    @Shadow @Final private ServerWorldLightManager lightManager;

    @Shadow private boolean immutableLoadedChunksDirty;

    @Shadow @Final private ChunkManager.ProxyTicketManager ticketManager;

    @Shadow @Final private ServerWorld world;

    @Shadow @Final private TemplateManager templateManager;

    @Shadow @Final private ThreadTaskExecutor<Runnable> mainThread;

    @Shadow @Final private PointOfInterestManager pointOfInterestManager;

    @Shadow(aliases = "field_219266_t") @Final private IChunkStatusListener statusListener;

    @Shadow @Final private ChunkGenerator<?> generator;

    @Shadow protected abstract CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> chunkGenerate(ChunkHolder chunkHolderIn,
            ChunkStatus chunkStatusIn);

    @Shadow @Final private File dimensionDirectory;

    @Shadow private int viewDistance;

    @Shadow @Final private Int2ObjectMap<ChunkManager.EntityTracker> entities;

    @Shadow @Final private Long2ObjectLinkedOpenHashMap<ChunkHolder> loadedChunks;

    @Shadow @Final private PlayerGenerationTracker playerGenerationTracker;

    @Shadow protected abstract boolean cannotGenerateChunks(ServerPlayerEntity player);

    @Shadow protected abstract void setChunkLoadedAtClient(ServerPlayerEntity player, ChunkPos chunkPosIn, IPacket<?>[] packetCache,
            boolean wasLoaded, boolean load);

    @Shadow protected static int getChunkDistance(ChunkPos chunkPosIn, int x, int y) {
        throw new Error("Mixin didn't apply");
    }

    @Inject(method = "<init>", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onConstruct(ServerWorld worldIn, File worldDirectory, DataFixer p_i51538_3_, TemplateManager templateManagerIn,
            Executor p_i51538_5_, ThreadTaskExecutor mainThreadIn, IChunkLightProvider p_i51538_7_,
            ChunkGenerator generatorIn, IChunkStatusListener p_i51538_9_, Supplier p_i51538_10_,
            int p_i51538_11_, CallbackInfo ci, DelegatedTaskExecutor delegatedtaskexecutor,
            ITaskExecutor itaskexecutor, DelegatedTaskExecutor delegatedtaskexecutor1) {

        this.cubeTaskPriorityQueueSorter = new CubeTaskPriorityQueueSorter(ImmutableList.of(delegatedtaskexecutor,
                itaskexecutor, delegatedtaskexecutor1), p_i51538_5_, Integer.MAX_VALUE);
        this.worldgenExecutor = this.cubeTaskPriorityQueueSorter.createExecutor(delegatedtaskexecutor, false);
        this.mainExecutor = this.cubeTaskPriorityQueueSorter.createExecutor(itaskexecutor, false);
    }

    @Dynamic
    @Redirect(method = "setCubeLevel", at = @At(
            value = "NEW",
            target = "net/minecraft/world/server/ChunkHolder")
    )
    private ChunkHolder onCubeHolderConstruct(ChunkPos chunkPosIn, int levelIn, WorldLightManager lightManagerIn, ChunkHolder.IListener listener,
            ChunkHolder.IPlayerProvider playerProviderIn) {
        long pos = chunkPosIn.asLong(); // this is actually a cube pos encodes as chunk pos
        ChunkHolder holder = new ChunkHolder(CubePos.from(pos).asChunkPos(), levelIn, lightManagerIn, listener, playerProviderIn);
        //noinspection ConstantConditions
        ((ICubeHolder) holder).setYPos(CubePos.extractY(pos));
        return holder;
    }

    @Inject(method = "save", at = @At("HEAD"))
    protected void save(boolean flush, CallbackInfo ci) {
        if (flush) {
            List<ChunkHolder> list =
                    this.immutableLoadedCubes.values().stream().filter(ChunkHolder::isAccessible).peek(ChunkHolder::updateAccessible).collect(
                    Collectors.toList());
            MutableBoolean savedAny = new MutableBoolean();

            do {
                savedAny.setFalse();
                list.stream().map((cubeHolder) -> {
                    CompletableFuture<ICube> cubeFuture;
                    do {
                        cubeFuture = ((ICubeHolder) cubeHolder).getCurrentCubeFuture();
                        this.mainThread.driveUntil(cubeFuture::isDone);
                    } while (cubeFuture != ((ICubeHolder) cubeHolder).getCurrentCubeFuture());

                    return cubeFuture.join();
                }).filter((cube) -> {
                    return cube instanceof CubePrimerWrapper || cube instanceof Cube;
                }).filter(this::cubeSave).forEach((unsavedCube) -> {
                    savedAny.setTrue();
                });
            } while (savedAny.isTrue());

            this.scheduleCubeUnloads(() -> true);
            //this.func_227079_i_();
            LOGGER.info("ThreadedAnvilChunkStorage ({}): All cubes are saved", this.dimensionDirectory.getName());
        } else {
            this.immutableLoadedCubes.values().stream().filter(ChunkHolder::isAccessible).forEach((cubeHolder) -> {
                ICube cube = ((ICubeHolder) cubeHolder).getCurrentCubeFuture().getNow(null);
                if (cube instanceof CubePrimerWrapper || cube instanceof Cube) {
                    this.cubeSave(cube);
                    cubeHolder.updateAccessible();
                }
            });
        }

    }

    // chunkSave
    private boolean cubeSave(ICube cube) {
        if (!cube.isDirty()) {
            return false;
        } else {
            try {
                this.world.checkSessionLock();
            } catch (SessionLockException sessionlockexception) {
                LOGGER.error("Couldn't save chunk; already in use by another instance of Minecraft?", (Throwable)sessionlockexception);
                return false;
            }

            // cube.setLastSaveTime(this.world.getGameTime());
            cube.setDirty(false);
            CubePos chunkpos = cube.getCubePos();

            try {
                //ChunkStatus status = cube.getCubeStatus();
                //if (status.getType() != ChunkStatus.Type.LEVELCHUNK) {
                //    CompoundNBT compoundnbt = this.loadChunkData(chunkpos);
                //    if (compoundnbt != null && ChunkSerializer.getChunkStatus(compoundnbt) == ChunkStatus.Type.LEVELCHUNK) {
                //        return false;
                //    }
//
                //    if (status == ChunkStatus.EMPTY && chunkIn.getStructureStarts().values().stream().noneMatch(StructureStart::isValid)) {
                //        return false;
                //    }
                //}

                this.world.getProfiler().func_230035_c_("chunkSave");
                CubeSerializer.writeCube(world, cube, dimensionDirectory.toPath());
                return true;
            } catch (Exception exception) {
                LOGGER.error("Failed to save chunk {},{},{}", chunkpos.getX(), chunkpos.getY(), chunkpos.getZ(), exception);
                return false;
            }
        }
    }

    private void scheduleCubeUnloads(BooleanSupplier hasMoreTime) {
        LongIterator longiterator = this.unloadableCubes.iterator();

        for(int i = 0; longiterator.hasNext() && (hasMoreTime.getAsBoolean() || i < 200 || this.unloadableCubes.size() > 2000); longiterator.remove()) {
            long j = longiterator.nextLong();
            ChunkHolder chunkholder = this.loadedChunks.remove(j);
            if (chunkholder != null) {
                this.cubesToUnload.put(j, chunkholder);
                this.immutableLoadedChunksDirty = true;
                ++i;
                this.scheduleCubeSave(j, chunkholder);
            }
        }

        Runnable runnable;
        while((hasMoreTime.getAsBoolean() || this.saveCubeTasks.size() > 2000) && (runnable = this.saveCubeTasks.poll()) != null) {
            runnable.run();
        }
    }


    private void scheduleCubeSave(long cubePos, ChunkHolder chunkHolderIn) {
        CompletableFuture<ICube> completablefuture = ((ICubeHolder) chunkHolderIn).getCurrentCubeFuture();
        completablefuture.thenAcceptAsync((cube) -> {
            CompletableFuture<ICube> completablefuture1 = ((ICubeHolder) chunkHolderIn).getCurrentCubeFuture();
            if (completablefuture1 != completablefuture) {
                this.scheduleCubeSave(cubePos, chunkHolderIn);
            } else {
                if (this.cubesToUnload.remove(cubePos, chunkHolderIn) && cube != null) {
                    if (cube instanceof Chunk) {
                        ((Chunk)cube).setLoaded(false);
                        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Unload((Chunk)cube));
                    }

                    this.cubeSave(cube);
                    if (this.loadedCubePositions.remove(cubePos) && cube instanceof Chunk) {
                        Chunk chunk = (Chunk)cube;
                        this.world.onChunkUnloading(chunk);
                    }

                    //this.lightManager.updateChunkStatus(cube.getCubeStatus());
                    //this.lightManager.func_215588_z_();
                    ((ICubeStatusListener) this.statusListener).cubeStatusChanged(cube.getCubePos(), (ChunkStatus)null);
                }

            }
        }, this.saveCubeTasks::add).whenComplete((p_223171_1_, p_223171_2_) -> {
            if (p_223171_2_ != null) {
                LOGGER.error("Failed to save chunk " + ((ICubeHolder) chunkHolderIn).getCubePos(), p_223171_2_);
            }
        });
    }

    @Override
    public LongSet getUnloadableCubes() {
        return this.unloadableCubes;
    }

    // func_219220_a
    @Override
    @Nullable
    public ChunkHolder getCubeHolder(long cubePosIn) {
        return loadedCubes.get(cubePosIn);
    }
    // func_219219_b
    @Override
    public ChunkHolder getImmutableCubeHolder(long cubePosIn)
    {
        return this.immutableLoadedCubes.get(cubePosIn);
    }

    // TODO: remove when cubic chunks versions are done
    @SuppressWarnings({"UnresolvedMixinReference"})
    @Inject(method = "lambda$func_219244_a$13", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/listener/IChunkStatusListener;statusChanged(Lnet/minecraft/util/math/ChunkPos;"
                    + "Lnet/minecraft/world/chunk/ChunkStatus;)V")
    )
    private void on_func_219244_a_StatusChange(ChunkStatus chunkStatusIn, ChunkPos chunkpos,
            ChunkHolder chunkHolderIn, Either<?, ?> p_223180_4_, CallbackInfoReturnable<CompletionStage<?>> cir) {
        if (((ICubeHolder) chunkHolderIn).getCubePos() != null) {
            ((ICubeStatusListener) statusListener).cubeStatusChanged(
                    ((ICubeHolder) chunkHolderIn).getCubePos(),
                    chunkStatusIn);
        }
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "lambda$scheduleSave$10", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/listener/IChunkStatusListener;statusChanged(Lnet/minecraft/util/math/ChunkPos;"
                    + "Lnet/minecraft/world/chunk/ChunkStatus;)V")
    )
    private void onScheduleSaveStatusChange(ChunkHolder chunkHolderIn, CompletableFuture<?> completablefuture,
            long chunkPosIn, IChunk p_219185_5_, CallbackInfo ci) {
        if (((ICubeHolder) chunkHolderIn).getCubePos() != null) {
            ((ICubeStatusListener) statusListener).cubeStatusChanged(
                    ((ICubeHolder) chunkHolderIn).getCubePos(), null);
        }
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "lambda$null$18", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/listener/IChunkStatusListener;statusChanged(Lnet/minecraft/util/math/ChunkPos;"
                    + "Lnet/minecraft/world/chunk/ChunkStatus;)V"))
    private void onGenerateStatusChange(ChunkStatus chunkStatusIn, ChunkHolder chunkHolderIn, ChunkPos chunkpos, List<?> p_223148_4_,
            CallbackInfoReturnable<CompletableFuture<?>> cir) {
        if (((ICubeHolder) chunkHolderIn).getCubePos() != null) {
            ((ICubeStatusListener) statusListener).cubeStatusChanged(
                    ((ICubeHolder) chunkHolderIn).getCubePos(), null);
        }
    }

    @Inject(method = "refreshOffThreadCache", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap;clone()Lit/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap;"))
    private void onRefreshCache(CallbackInfoReturnable<Boolean> cir) {
        this.immutableLoadedCubes = loadedCubes.clone();
    }

    @Override
    public Iterable<ChunkHolder> getLoadedCubeIterable() {
        return Iterables.unmodifiableIterable(this.immutableLoadedCubes.values());
    }

    //func_219244_a
    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> createCubeFuture(ChunkHolder chunkHolderIn,
            ChunkStatus chunkStatusIn) {
        CubePos cubePos = ((ICubeHolder) chunkHolderIn).getCubePos();
        if (chunkStatusIn == ChunkStatus.EMPTY) {
            return this.cubeLoad(cubePos);
        } else {
            CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> completablefuture = Utils.unsafeCast(
                    ((ICubeHolder) chunkHolderIn).createCubeFuture(chunkStatusIn.getParent(), (ChunkManager) (Object) this)
            );
            return Utils.unsafeCast(completablefuture.thenComposeAsync(
                    (Either<ICube, ChunkHolder.IChunkLoadingError> inputSection) -> {
                        Optional<ICube> optional = inputSection.left();
                        if (!optional.isPresent()) {
                            return CompletableFuture.completedFuture(inputSection);
                        } else {
                            if (chunkStatusIn == ChunkStatus.LIGHT) {
                                ((ITicketManager) this.ticketManager).registerWithLevel(CCTicketType.CCLIGHT, cubePos,
                                        33 + CubeStatus.getDistance(ChunkStatus.FEATURES), cubePos);
                            }

                            ICube cube = optional.get();
                            if (cube.getCubeStatus().isAtLeast(chunkStatusIn)) {
                                CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> completablefuture1;
                                if (chunkStatusIn == ChunkStatus.LIGHT) {
                                    completablefuture1 = this.cubeGenerate(chunkHolderIn, chunkStatusIn);
                                } else {
                                    completablefuture1 = Utils.unsafeCast(
                                            chunkStatusIn.doLoadingWork(this.world, this.templateManager, this.lightManager, (chunk) -> {
                                                return Utils.unsafeCast(this.makeCubeInstance(chunkHolderIn));
                                            }, (IChunk) cube));
                                }

                                ((ICubeStatusListener) this.statusListener).cubeStatusChanged(cubePos, chunkStatusIn);
                                return completablefuture1;
                            } else {
                                return this.cubeGenerate(chunkHolderIn, chunkStatusIn);
                            }
                        }
                    }, this.mainThread));
        }
    }

    // func_219205_a
    private ChunkStatus getParentStatus(ChunkStatus status, int distance) {
        ChunkStatus parent;
        if (distance == 0) {
            parent = status.getParent();
        } else {
            parent = CubeStatus.getStatus(CubeStatus.getDistance(status) + distance);
        }

        return parent;
    }

    //chunkGenerate
    private CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> cubeGenerate(ChunkHolder chunkHolderIn, ChunkStatus chunkStatusIn) {
        CubePos cubePos = ((ICubeHolder) chunkHolderIn).getCubePos();
        CompletableFuture<Either<List<ICube>, ChunkHolder.IChunkLoadingError>> future =
                this.makeFutureForStatusNeighbors(cubePos, CubeStatus.getCubeTaskRange(chunkStatusIn), (count) -> {
                    return this.getParentStatus(chunkStatusIn, count);
                });
        this.world.getProfiler().func_230036_c_(() -> {
            return "cubeGenerate " + chunkStatusIn.getName();
        });
        return future.thenComposeAsync((sectionOrError) -> {
            return sectionOrError.map((neighborSections) -> {
                try {
                    CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> finalFuture = Utils.unsafeCast(
                            chunkStatusIn.doGenerationWork(this.world, this.generator, this.templateManager, this.lightManager, (chunk) -> {
                                return Utils.unsafeCast(this.makeCubeInstance(chunkHolderIn));
                            }, Utils.unsafeCast(neighborSections)));
                    ((ICubeStatusListener) this.statusListener).cubeStatusChanged(cubePos, chunkStatusIn);
                    return finalFuture;
                } catch (Exception exception) {
                    CrashReport crashreport = CrashReport.makeCrashReport(exception, "Exception generating new chunk");
                    CrashReportCategory crashreportcategory = crashreport.makeCategory("Chunk to be generated");
                    crashreportcategory.addDetail("Location", String.format("%d,%d,%d", cubePos.getX(), cubePos.getY(), cubePos.getZ()));
                    crashreportcategory.addDetail("Position hash", CubePos.asLong(cubePos.getX(), cubePos.getY(), cubePos.getZ()));
                    crashreportcategory.addDetail("Generator", this.generator);
                    throw new ReportedException(crashreport);
                }
            }, (p_219211_2_) -> {
                this.releaseLightTicket(cubePos);
                return CompletableFuture.completedFuture(Either.right(p_219211_2_));
            });
        }, (runnable) -> {
            this.worldgenExecutor.enqueue(CubeTaskPriorityQueueSorter.createMsg(chunkHolderIn, runnable));
        });
    }

    // func_219236_a
    private CompletableFuture<Either<List<ICube>, ChunkHolder.IChunkLoadingError>> makeFutureForStatusNeighbors(
            CubePos pos, int radius, IntFunction<ChunkStatus> getParentStatus) {
        List<CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>>> list = Lists.newArrayList();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        // to index: x*d*d + y*d + z
        // extract x: index/(d*d)
        // extract y: (index/d) % d
        // extract z: index % d
        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dy = -radius; dy <= radius; ++dy) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    int distance = Math.max(Math.max(Math.abs(dz), Math.abs(dx)), Math.abs(dy));
                    final CubePos cubePos = CubePos.of(x + dx, y + dy, z + dz);
                    long posLong = cubePos.asLong();
                    ChunkHolder chunkholder = this.getLoadedSection(posLong);
                    if (chunkholder == null) {
                        //noinspection MixinInnerClass
                        return CompletableFuture.completedFuture(Either.right(new ChunkHolder.IChunkLoadingError() {
                            public String toString() {
                                return "Unloaded " + cubePos.toString();
                            }
                        }));
                    }

                    ChunkStatus parentStatus = getParentStatus.apply(distance);
                    CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> future =
                            ((ICubeHolder) chunkholder).createCubeFuture(parentStatus, Utils.unsafeCast(this));
                    list.add(future);
                }
            }
        }

        CompletableFuture<List<Either<ICube, ChunkHolder.IChunkLoadingError>>> futures = Util.gather(list);
        return futures.thenApply((p_219227_4_) -> {
            List<ICube> returnFutures = Lists.newArrayList();
            int i = 0;

            for (final Either<ICube, ChunkHolder.IChunkLoadingError> either : p_219227_4_) {
                Optional<ICube> optional = either.left();
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

    // func_219209_c
    @Override
    public void releaseLightTicket(CubePos cubePos) {
        this.mainThread.enqueue(Util.namedRunnable(() -> {
            ((ITicketManager) this.ticketManager).releaseWithLevel(CCTicketType.CCLIGHT,
                    cubePos, 33 + CubeStatus.getDistance(ChunkStatus.FEATURES), cubePos);
        }, () -> {
            return "release light ticket " + cubePos;
        }));
    }

    // func_219200_b
    private CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> makeCubeInstance(ChunkHolder holder) {
        CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> fullFuture =
                ((ICubeHolder) holder).getCubeFuture(ChunkStatus.FULL.getParent());
        return fullFuture.thenApplyAsync((sectionOrError) -> {
            ChunkStatus chunkstatus = ICubeHolder.getCubeStatusFromLevel(holder.getChunkLevel());
            return !chunkstatus.isAtLeast(ChunkStatus.FULL) ? ICubeHolder.MISSING_CUBE : sectionOrError.mapLeft((prevCube) -> {
                CubePos cubePos = ((ICubeHolder) holder).getCubePos();
                Cube cube;
                if (prevCube instanceof CubePrimerWrapper) {
                        cube = ((CubePrimerWrapper)prevCube).getCube();
                } else {
                    cube = new Cube(this.world, cubePos, prevCube.getCubeSections(), null);
                    cube.setCubeStatus(prevCube.getCubeStatus());
                    ((ICubeHolder) holder).onCubeWrapperCreated(new CubePrimerWrapper(cube));
                }

                // TODO: reimplement
                //chunkSection.setLocationType(() -> {
                //    return ChunkHolder.getLocationTypeFromLevel(holder.getChunkLevel());
                //});
                //chunkSection.postLoad();
                if (this.loadedCubePositions.add(cubePos.asLong())) {
                    // TODO: reimplement setLoaded
                    // worldSection.setLoaded(true);
                    this.world.addTileEntities(cube.getTileEntityMap().values());
                    List<Entity> entities = null;
                    ClassInheritanceMultiMap<Entity> entityLists = cube.getEntityList();


                    for (Entity entity : entityLists) {
                        if (entity instanceof PlayerEntity || this.world.addEntityIfNotDuplicate(entity)) {
                            continue;
                        }
                        if (entities == null) {
                            entities = Lists.newArrayList(entity);
                        } else {
                            entities.add(entity);
                        }
                    }

                    if (entities != null) {
                        entities.forEach(cube::removeEntity);
                    }
                    // TODO: events
                    // net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Load(chunkSection));
                }

                return (ICube) cube;
            });
        }, (runnable) -> {
            this.mainExecutor.enqueue(CubeTaskPriorityQueueSorter.createMsg(
                    runnable, ((ICubeHolder) holder).getCubePos().asLong(), holder::getChunkLevel));
        });
    }

    // func_222961_b
    @Override
    public CompletableFuture<Either<Cube, ChunkHolder.IChunkLoadingError>> createCubeBorderFuture(ChunkHolder chunkHolder) {
        return ((ICubeHolder) chunkHolder).createCubeFuture(ChunkStatus.FULL, (ChunkManager) (Object) this).thenApplyAsync((p_222976_0_) -> {
            return p_222976_0_.mapLeft((icube) -> {
                Cube cube = (Cube) icube;
                //TODO: implement rescheduleTicks for cube
                //cube.rescheduleTicks();
                return cube;
            });
        }, (p_222962_2_) -> {
            this.mainExecutor.enqueue(CubeTaskPriorityQueueSorter.createMsg(chunkHolder, p_222962_2_));
        });
    }

    // func_219179_a
    @Override
    public CompletableFuture<Either<Cube, ChunkHolder.IChunkLoadingError>> createCubeTickingFuture(ChunkHolder chunkHolder) {
        CubePos cubePos = ((ICubeHolder) chunkHolder).getCubePos();
        CompletableFuture<Either<List<ICube>, ChunkHolder.IChunkLoadingError>> completablefuture = this.createCubeRegionFuture(cubePos, 1,
                (p_219172_0_) -> {
                    return ChunkStatus.FULL;
                });
        CompletableFuture<Either<Cube, ChunkHolder.IChunkLoadingError>> completablefuture1 =
                completablefuture.thenApplyAsync((p_219239_0_) -> {
                    return p_219239_0_.flatMap((p_219208_0_) -> {
                        Cube cube = (Cube) p_219208_0_.get(p_219208_0_.size() / 2);
                        //TODO: implement cube#postProcess
                        //cube.postProcess();
                        return Either.left(cube);
                    });
                }, (p_219230_2_) -> {
                    this.mainExecutor.enqueue(CubeTaskPriorityQueueSorter.createMsg(chunkHolder, p_219230_2_));
                });
        completablefuture1.thenAcceptAsync((cubeLoadingErrorEither) -> {
            cubeLoadingErrorEither.mapLeft((cube) -> {
                this.cubesLoaded.getAndIncrement();
                Object[] objects = new Object[2];
                this.getCubeTrackingPlayers(cubePos, false).forEach((serverPlayerEntity) -> {
                    this.sendCubeData(serverPlayerEntity, objects, cube);
                });
                return Either.left(cube);
            });
        }, (p_219202_2_) -> {
            this.mainExecutor.enqueue(CubeTaskPriorityQueueSorter.createMsg(chunkHolder, p_219202_2_));
        });
        return completablefuture1;
    }

    //chunkLoad
    private CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> cubeLoad(CubePos cubePos) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.world.getProfiler().func_230035_c_("cubeLoad");

                ICube icube = CubeSerializer.loadCube(world, cubePos, this.dimensionDirectory.toPath());
                if(icube != null)
                    return Either.left(icube);

            } catch (ReportedException reportedexception) {
                Throwable throwable = reportedexception.getCause();
                if (!(throwable instanceof IOException)) {
                    throw reportedexception;
                }

                LOGGER.error("Couldn't load cube {}", cubePos, throwable);
            } catch (Exception exception) {
                LOGGER.error("Couldn't load cube {}", cubePos, exception);
            }

            return Either.left(new CubePrimer(cubePos, null));
        }, this.mainThread);
    }

    // func_219236_a
    @Override
    public CompletableFuture<Either<List<ICube>, ChunkHolder.IChunkLoadingError>> createCubeRegionFuture(CubePos pos, int r,
            IntFunction<ChunkStatus> getTargetStatus) {
        List<CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>>> list = Lists.newArrayList();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();


        for (int dx = -r; dx <= r; ++dx) {
            for (int dy = -r; dy <= r; ++dy) {
                for (int dz = -r; dz <= r; ++dz) {
                    int distance = Math.max(Math.max(Math.abs(dz), Math.abs(dx)), Math.abs(dy));
                    final CubePos cubePos = CubePos.of(x + dz, y + dy, z + dx);
                    long posLong = cubePos.asLong();
                    ChunkHolder chunkholder = this.getLoadedSection(posLong);
                    if (chunkholder == null) {
                        //noinspection MixinInnerClass
                        return CompletableFuture.completedFuture(Either.right(new ChunkHolder.IChunkLoadingError() {
                            public String toString() {
                                return "Unloaded " + cubePos.toString();
                            }
                        }));
                    }

                    ChunkStatus chunkstatus = getTargetStatus.apply(distance);
                    CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> completablefuture =
                            ((ICubeHolder) chunkholder).createCubeFuture(chunkstatus,
                                    (ChunkManager) (Object) this);
                    list.add(completablefuture);
                }
            }
        }

        CompletableFuture<List<Either<ICube, ChunkHolder.IChunkLoadingError>>> completablefuture1 = Util.gather(list);
        return completablefuture1.thenApply((eitherISectionError) -> {
            List<ICube> list1 = Lists.newArrayList();
            int k1 = 0;

            for (final Either<ICube, ChunkHolder.IChunkLoadingError> either : eitherISectionError) {
                Optional<ICube> optional = either.left();
                if (!optional.isPresent()) {
                    final int l1 = k1;
                    //noinspection MixinInnerClass
                    return Either.right(new ChunkHolder.IChunkLoadingError() {
                        public String toString() {
                            int d = r * 2 + 1;
                            return "Unloaded " + CubePos.of(
                                    x + l1 / (d * d),
                                    y + (l1 / d) % d,
                                    z + l1 % d) + " " + either.right()
                                    .get().toString();
                        }
                    });
                }
                list1.add(optional.get());
                ++k1;
            }

            return Either.left(list1);
        });
    }

    // func_219220_a
    @Nullable
    protected ChunkHolder getLoadedSection(long cubePosIn) {
        return this.loadedCubes.get(cubePosIn);
    }

    // getTrackingPlayers
    public Stream<ServerPlayerEntity> getCubeTrackingPlayers(CubePos pos, boolean boundaryOnly) {
        int viewDistanceCubes = Coords.sectionToCubeRenderDistance(this.viewDistance);
        return this.playerGenerationTracker.getGeneratingPlayers(pos.asLong()).filter((serverPlayerEntity) -> {
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
     * @reason Due to vanilla calling ChunkManager#func_223489_c, which updates player#managedSectionPos, this is required.
     */
    @Overwrite
    void setPlayerTracking(ServerPlayerEntity player, boolean track) {
        boolean cannotGenerateChunks = this.cannotGenerateChunks(player);
        boolean cannotGenerateChunksTracker = this.playerGenerationTracker.cannotGenerateChunks(player);
        int xFloor = Coords.getCubeXForEntity(player);
        int yFloor = Coords.getCubeYForEntity(player);
        int zFloor = Coords.getCubeZForEntity(player);
        if (track) {
            this.playerGenerationTracker.addPlayer(CubePos.of(xFloor, yFloor, zFloor).asChunkPos().asLong(), player, cannotGenerateChunks);
            this.sendPlayerCubePositionPacket(player); //This also sends the vanilla packet, as player#ManagedSectionPos is changed in this method.
            if (!cannotGenerateChunks) {
                this.ticketManager.updatePlayerPosition(SectionPos.from(player), player); //Vanilla
                ((ITicketManager)this.ticketManager).updateCubePlayerPosition(CubePos.from(SectionPos.from(player)), player);
            }
        } else {
            SectionPos managedSectionPos = player.getManagedSectionPos(); //Vanilla
            CubePos cubePos = CubePos.from(managedSectionPos);
            this.playerGenerationTracker.removePlayer(cubePos.asChunkPos().asLong(), player);
            if (!cannotGenerateChunksTracker) {
                this.ticketManager.removePlayer(managedSectionPos, player); //Vanilla
                ((ITicketManager)this.ticketManager).removeCubePlayer(cubePos, player);
            }
        }

        //Vanilla
        int i = MathHelper.floor(player.getPosX()) >> 4;
        int j = MathHelper.floor(player.getPosZ()) >> 4;

        for(int l = i - this.viewDistance; l <= i + this.viewDistance; ++l) {
            for(int k = j - this.viewDistance; k <= j + this.viewDistance; ++k) {
                ChunkPos chunkpos = new ChunkPos(l, k);
                this.setChunkLoadedAtClient(player, chunkpos, new IPacket[2], !track, track);
            }
        }
        //CC
        int viewDistanceCubes = Coords.sectionToCubeRenderDistance(this.viewDistance);
        for (int ix = xFloor - viewDistanceCubes; ix <= xFloor + viewDistanceCubes; ++ix) {
            for (int iy = yFloor - viewDistanceCubes; iy <= yFloor + viewDistanceCubes; ++iy) {
                for (int iz = zFloor - viewDistanceCubes; iz <= zFloor + viewDistanceCubes; ++iz) {
                    CubePos cubePos = CubePos.of(ix, iy, iz);
                    this.setCubeLoadedAtClient(player, cubePos, new Object[2], !track, track);
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
    public void updatePlayerPosition(ServerPlayerEntity player) {

        for(ChunkManager.EntityTracker chunkmanager$entitytracker : this.entities.values()) {
            if (((EntityTrackerAccess)chunkmanager$entitytracker).getEntity() == player) {
                chunkmanager$entitytracker.updateTrackingState(this.world.getPlayers());
            } else {
                chunkmanager$entitytracker.updateTrackingState(player);
            }
        }

        SectionPos managedSectionPos = player.getManagedSectionPos();
        SectionPos newSectionPos = SectionPos.from(player);

        CubePos cubePosManaged = CubePos.from(managedSectionPos);
        CubePos newCubePos = CubePos.from(player.getPosition());

        long managedPosAsLong = cubePosManaged.asLong();
        long posAsLong = newCubePos.asLong();

        long managedSectionPosLong = managedSectionPos.asChunkPos().asLong();
        long newSectionPosLong = newSectionPos.asChunkPos().asLong();

        boolean prevNoGenerate = this.playerGenerationTracker.func_225419_d(player);
        boolean nowNoGenerate = this.cannotGenerateChunks(player);

        boolean sectionPosChanged = managedSectionPos.asLong() != newSectionPos.asLong();

        if (sectionPosChanged || prevNoGenerate != nowNoGenerate) {
            this.sendPlayerCubePositionPacket(player);
            // remove player is generation was allowed on last update
            if (!prevNoGenerate) {
                this.ticketManager.removePlayer(managedSectionPos, player);
                ((ITicketManager) this.ticketManager).removeCubePlayer(cubePosManaged, player);

            }

            // update the position if generation is allowed now
            if (!nowNoGenerate) {
                // we are mixin into this method, so it should work as this:
                this.ticketManager.updatePlayerPosition(newSectionPos, player); //Vanilla
                ((ITicketManager)this.ticketManager).updateCubePlayerPosition(newCubePos, player);
            }

            if (!prevNoGenerate && nowNoGenerate) {
                this.playerGenerationTracker.disableGeneration(player);
            }

            if (prevNoGenerate && !nowNoGenerate) {
                this.playerGenerationTracker.enableGeneration(player);
            }

            if (managedPosAsLong != posAsLong) {
                // THIS IS FINE
                // this method is actually empty, positions don't actually matter
                this.playerGenerationTracker.updatePlayerPosition(managedSectionPosLong, newSectionPosLong, player);
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
                        this.setCubeLoadedAtClient(player, cubePos1, new Object[2], loadedBefore, loadedNow);
                    }
                }
            }
        } else {
            for (int ix = managedX - viewDistanceCubes; ix <= managedX + viewDistanceCubes; ++ix) {
                for (int iz = managedZ - viewDistanceCubes; iz <= managedZ + viewDistanceCubes; ++iz) {
                    for (int iy = managedY - viewDistanceCubes; iy <= managedY + viewDistanceCubes; ++iy) {
                        CubePos cubePos2 = CubePos.of(ix, iy, iz);
                        this.setCubeLoadedAtClient(player, cubePos2, new Object[2], true, false);
                    }
                }
            }

            for (int ix = newCubeX - viewDistanceCubes; ix <= newCubeX + viewDistanceCubes; ++ix) {
                for (int iz = newCubeZ - viewDistanceCubes; iz <= newCubeZ + viewDistanceCubes; ++iz) {
                    for (int iy = newCubeY - viewDistanceCubes; iy <= newCubeY + viewDistanceCubes; ++iy) {
                        CubePos cubePos3 = CubePos.of(ix, iy, iz);
                        this.setCubeLoadedAtClient(player, cubePos3, new Object[2], false, true);
                    }
                }
            }
        }

        int newSectionX = MathHelper.floor(player.getPosX()) >> 4;
        int newSectionZ = MathHelper.floor(player.getPosZ()) >> 4;

        int oldSectionX = managedSectionPos.getSectionX();
        int oldSectionZ = managedSectionPos.getSectionZ();
        if (Math.abs(oldSectionX - newSectionX) <= this.viewDistance * 2 && Math.abs(oldSectionZ - newSectionZ) <= this.viewDistance * 2) {
            int k2 = Math.min(newSectionX, oldSectionX) - this.viewDistance;
            int i3 = Math.min(newSectionZ, oldSectionZ) - this.viewDistance;
            int j3 = Math.max(newSectionX, oldSectionX) + this.viewDistance;
            int k3 = Math.max(newSectionZ, oldSectionZ) + this.viewDistance;

            for(int l3 = k2; l3 <= j3; ++l3) {
                for(int k1 = i3; k1 <= k3; ++k1) {
                    ChunkPos chunkpos1 = new ChunkPos(l3, k1);
                    boolean flag5 = getChunkDistance(chunkpos1, oldSectionX, oldSectionZ) <= this.viewDistance;
                    boolean flag6 = getChunkDistance(chunkpos1, newSectionX, newSectionZ) <= this.viewDistance;
                    this.setChunkLoadedAtClient(player, chunkpos1, new IPacket[2], flag5, flag6);
                }
            }
        } else {
            for(int i1 = oldSectionX - this.viewDistance; i1 <= oldSectionX + this.viewDistance; ++i1) {
                for(int j1 = oldSectionZ - this.viewDistance; j1 <= oldSectionZ + this.viewDistance; ++j1) {
                    ChunkPos chunkpos = new ChunkPos(i1, j1);
                    this.setChunkLoadedAtClient(player, chunkpos, new IPacket[2], true, false);
                }
            }
            for(int j2 = newSectionX - this.viewDistance; j2 <= newSectionX + this.viewDistance; ++j2) {
                for(int l2 = newSectionZ - this.viewDistance; l2 <= newSectionZ + this.viewDistance; ++l2) {
                    ChunkPos chunkpos2 = new ChunkPos(j2, l2);
                    this.setChunkLoadedAtClient(player, chunkpos2, new IPacket[2], false, true);
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
            for(ChunkHolder chunkholder : this.loadedCubes.values()) {
                CubePos cubePos = ((ICubeHolder)chunkholder).getCubePos();
                Object[] objects = new Object[2];
                this.getCubeTrackingPlayers(cubePos, false).forEach((serverPlayerEntity) -> {
                    int k = IChunkManager.getCubeChebyshevDistance(cubePos, serverPlayerEntity, true);
                    boolean flag = k <= viewDistanceCubes;
                    boolean flag1 = k <= newViewDistanceCubes;
                    this.setCubeLoadedAtClient(serverPlayerEntity, cubePos, objects, flag, flag1);
                });
            }
        }

    }

    // func_223489_c
    private SectionPos sendPlayerCubePositionPacket(ServerPlayerEntity serverPlayerEntityIn) {
        SectionPos sectionpos = SectionPos.from(serverPlayerEntityIn);
        serverPlayerEntityIn.setManagedSectionPos(sectionpos);
        PacketDispatcher.sendTo(new PacketUpdateCubePosition(sectionpos), serverPlayerEntityIn);
        serverPlayerEntityIn.connection.sendPacket(new SUpdateChunkPositionPacket(sectionpos.getSectionX(), sectionpos.getSectionZ()));
        return sectionpos;
    }

    protected void setCubeLoadedAtClient(ServerPlayerEntity player, CubePos cubePosIn, Object[] packetCache, boolean wasLoaded, boolean load) {
        if (player.world == this.world) {
            //TODO: reimplement forge event
            //net.minecraftforge.event.ForgeEventFactory.fireChunkWatch(wasLoaded, load, player, cubePosIn, this.world);
            if (load && !wasLoaded) {
                ChunkHolder chunkholder = ((IChunkManager)this).getImmutableCubeHolder(cubePosIn.asLong());
                if (chunkholder != null) {
                    Cube cube = ((ICubeHolder)chunkholder).getCubeIfComplete();
                    if (cube != null) {
                        this.sendCubeData(player, packetCache, cube);
                    }
                    //TODO: reimplement debugpacket
                    //DebugPacketSender.sendChuckPos(this.world, cubePosIn);
                }
            }
            if (!load && wasLoaded) {
                //Vanilla: //player.sendChunkUnload(chunkPosIn)
                //I moved to MixinChunkManager to be in the same place as sendCubeLoad
                this.sendCubeUnload(player, cubePosIn);
            }
        }
    }

    // func_219215_b is in ICubeManager now
    //getChunkDistance is in ICubeManager now

    // func_222973_a
    @Override
    public CompletableFuture<Void> saveCubeScheduleTicks(Cube cubeIn) {
        return this.mainThread.runAsync(() -> {
            //TODO: implement saveCubeScheduleTicks
            /*
            This means adding a method to MixinChunkSection to handle the ticking of blocks,
            chunksection needs a blocksToBeTickedField, and that will probably need to be initialised in a secondary afterInit method
             */
            //sectionIn.saveScheduledTicks(this.world);
        });
    }

    // func_219188_b
    public CompletableFuture<Either<Cube, ChunkHolder.IChunkLoadingError>> createCubeEntityTickingFuture(CubePos pos) {
        return this.createCubeRegionFuture(pos, 2, (index) -> {
            return ChunkStatus.FULL;
        }).thenApplyAsync((eitherSectionError) -> {
            return eitherSectionError.mapLeft((cubes) -> {
                return (Cube) cubes.get(cubes.size() / 2);
            });
        }, this.mainThread);
    }

    private void sendCubeUnload(ServerPlayerEntity player, CubePos cubePosIn)
    {
        if (player.isAlive()) {
            PacketDispatcher.sendTo(new PacketUnloadCube(cubePosIn), player);
        }
    }

    // sendChunkData
    private void sendCubeData(ServerPlayerEntity player, Object[] packetCache, Cube cubeIn) {
        if (packetCache[0] == null) {
            packetCache[0] = new PacketCubes(Collections.singletonList(cubeIn));
            //packetCache[1] = new SUpdateLightPacket(pos, this.lightManager);
        }

        CubePos pos = cubeIn.getCubePos();

        PacketDispatcher.sendTo(packetCache[0], player);
        List<Entity> leashedEntities = Lists.newArrayList();
        List<Entity> passengerEntities = Lists.newArrayList();

        for (ChunkManager.EntityTracker entityTracker : this.entities.values()) {
            Entity entity = ((EntityTrackerAccess) entityTracker).getEntity();
            // TODO: entity chunk coords fix
            if (entity != player && entity.chunkCoordX == pos.getX() && entity.chunkCoordY == pos.getY() && entity.chunkCoordZ == pos.getZ()) {
                entityTracker.updateTrackingState(player);
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
                player.connection.sendPacket(new SMountEntityPacket(entity1, ((MobEntity) entity1).getLeashHolder()));
            }
        }

        if (!passengerEntities.isEmpty()) {
            for (Entity entity2 : passengerEntities) {
                player.connection.sendPacket(new SSetPassengersPacket(entity2));
            }
        }
    }

    public int getLoadedCubesCount()
    {
        return this.cubesLoaded.get();
    }

    // func_219191_c
    public IntSupplier getCompletedLevel(long cubePosIn) {
        return () -> {
            ChunkHolder chunkholder = this.getImmutableCubeHolder(cubePosIn);
            return chunkholder == null ? CubeTaskPriorityQueue.levelCount - 1 : Math.min(chunkholder.func_219281_j(),
                    CubeTaskPriorityQueue.levelCount - 1);
        };
    }
}
