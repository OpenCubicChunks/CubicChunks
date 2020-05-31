package cubicchunks.cc.mixin.core.common.chunk;

import static cubicchunks.cc.chunk.util.Utils.unsafeCast;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import cubicchunks.cc.CubeSerializer;
import cubicchunks.cc.chunk.IChunkManager;
import cubicchunks.cc.chunk.ICube;
import cubicchunks.cc.chunk.ICubeHolder;
import cubicchunks.cc.chunk.ICubeStatusListener;
import cubicchunks.cc.chunk.cube.CubeStatus;
import cubicchunks.cc.chunk.graph.CCTicketType;
import cubicchunks.cc.chunk.cube.CubePrimer;
import cubicchunks.cc.chunk.cube.CubePrimerWrapper;
import cubicchunks.cc.chunk.cube.Cube;
import cubicchunks.cc.chunk.ticket.CubeTaskPriorityQueueSorter;
import cubicchunks.cc.chunk.ticket.ITicketManager;
import cubicchunks.cc.chunk.util.CubePos;
import cubicchunks.cc.network.PacketCubes;
import cubicchunks.cc.network.PacketDispatcher;
import cubicchunks.cc.network.PacketUnloadCube;
import cubicchunks.cc.network.PacketUpdateCubePosition;
import cubicchunks.cc.utils.Coords;
import cubicchunks.cc.utils.MathUtil;
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
import net.minecraft.network.play.server.SMountEntityPacket;
import net.minecraft.network.play.server.SSetPassengersPacket;
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
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.ServerWorldLightManager;
import net.minecraft.world.storage.SessionLockException;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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

    private final PlayerGenerationTracker playerCubeGenerationTracker = new PlayerGenerationTracker();

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

    @Shadow private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> immutableLoadedChunks;

    @Shadow @Final private PlayerGenerationTracker playerGenerationTracker;

    @Shadow protected abstract boolean cannotGenerateChunks(ServerPlayerEntity player);

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

    @Nullable
    @Override
    public ChunkHolder setCubeLevel(long cubePosIn, int newLevel, @Nullable ChunkHolder holder, int oldLevel) {
        if (oldLevel > MAX_CUBE_LOADED_LEVEL && newLevel > MAX_CUBE_LOADED_LEVEL) {
            return holder;
        } else {
            if (holder != null) {
                holder.setChunkLevel(newLevel);
            }

            if (holder != null) {
                if (newLevel > MAX_CUBE_LOADED_LEVEL) {
                    this.unloadableCubes.add(cubePosIn);
                } else {
                    this.unloadableCubes.remove(cubePosIn);
                }
            }

            if (newLevel <= MAX_CUBE_LOADED_LEVEL && holder == null) {
                holder = this.cubesToUnload.remove(cubePosIn);
                if (holder != null) {
                    holder.setChunkLevel(newLevel);
                } else {

                    holder = new ChunkHolder(new ChunkPos(CubePos.extractX(cubePosIn), CubePos.extractZ(cubePosIn)), newLevel, this.lightManager,
                            this.cubeTaskPriorityQueueSorter, (ChunkHolder.IPlayerProvider) this);
                    ((ICubeHolder) holder).setYPos(CubePos.extractY(cubePosIn));
                }
                this.loadedCubes.put(cubePosIn, holder);
                this.immutableLoadedChunksDirty = true;
            }
            return holder;
        }
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
        if (!cube.isModified()) {
            return false;
        } else {
            try {
                this.world.checkSessionLock();
            } catch (SessionLockException sessionlockexception) {
                LOGGER.error("Couldn't save chunk; already in use by another instance of Minecraft?", (Throwable)sessionlockexception);
                return false;
            }

            // cube.setLastSaveTime(this.world.getGameTime());
            cube.setModified(false);
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
                LOGGER.error("Failed to save chunk " + chunkHolderIn.getPosition(), p_223171_2_);
            }
        });
    }

    @Override
    public LongSet getUnloadableCubes() {
        return this.unloadableCubes;
    }

    // func_219220_a
    @Override
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
            CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> completablefuture = unsafeCast(
                    ((ICubeHolder) chunkHolderIn).createCubeFuture(chunkStatusIn.getParent(), (ChunkManager) (Object) this)
            );
            return unsafeCast(completablefuture.thenComposeAsync(
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
                                    completablefuture1 = this.sectionGenerate(chunkHolderIn, chunkStatusIn);
                                } else {
                                    completablefuture1 = unsafeCast(
                                            chunkStatusIn.doLoadingWork(this.world, this.templateManager, this.lightManager, (chunk) -> {
                                                return unsafeCast(this.makeCubeInstance(chunkHolderIn));
                                            }, (IChunk) cube));
                                }

                                ((ICubeStatusListener) this.statusListener).cubeStatusChanged(cubePos, chunkStatusIn);
                                return completablefuture1;
                            } else {
                                return unsafeCast(this.sectionGenerate(chunkHolderIn, chunkStatusIn));
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
    private CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> sectionGenerate(ChunkHolder chunkHolderIn, ChunkStatus chunkStatusIn) {
        CubePos cubePos = ((ICubeHolder) chunkHolderIn).getCubePos();
        CompletableFuture<Either<List<ICube>, ChunkHolder.IChunkLoadingError>> future =
                this.makeFutureForStatusNeighbors(cubePos, CubeStatus.getCubeTaskRange(chunkStatusIn), (count) -> {
                    return this.getParentStatus(chunkStatusIn, count);
                });
        this.world.getProfiler().func_230036_c_(() -> {
            return "chunkGenerate " + chunkStatusIn.getName();
        });
        return future.thenComposeAsync((sectionOrError) -> {
            return sectionOrError.map((neighborSections) -> {
                try {
                    CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> finalFuture = unsafeCast(
                            chunkStatusIn.doGenerationWork(this.world, this.generator, this.templateManager, this.lightManager, (chunk) -> {
                                return unsafeCast(this.makeCubeInstance(chunkHolderIn));
                            }, unsafeCast(neighborSections)));
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
                            ((ICubeHolder) chunkholder).createCubeFuture(parentStatus, unsafeCast(this));
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
    protected void releaseLightTicket(CubePos cubePos) {
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
                //TODO: implement rescheduleTicks for chunkSection
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
                        //TODO: implement this later
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
                    this.sendCubeData(serverPlayerEntity, objects, cube, cubePos);
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
            for (int dy = -r; dy < r; ++dy) {
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
        return this.playerGenerationTracker.getGeneratingPlayers(pos.asLong()).filter((serverPlayerEntity) -> {
            int i = IChunkManager.getCubeChebyshevDistance(pos, serverPlayerEntity, true);
            int viewDistanceCubes = MathUtil.ceilDiv(this.viewDistance, 2);
            if (i > viewDistanceCubes) {
                return false;
            } else {
                return !boundaryOnly || i == viewDistanceCubes;
            }
        });
    }

    @Inject(method = "setPlayerTracking", at = @At("RETURN"))
    void setPlayerTracking(ServerPlayerEntity player, boolean track, CallbackInfo ci) {
        boolean cannotGenerateChunks = this.cannotGenerateChunks(player);
        boolean cannotGenerateChunksTracker = this.playerCubeGenerationTracker.cannotGenerateChunks(player);
        int xFloor = Coords.getCubeXForEntity(player);
        int yFloor = Coords.getCubeYForEntity(player);
        int zFloor = Coords.getCubeZForEntity(player);
        if (track) {
            this.playerCubeGenerationTracker.addPlayer(CubePos.asLong(xFloor, yFloor, zFloor), player, cannotGenerateChunks);
            this.sendPlayerCubePositionPacket(player);
            if (!cannotGenerateChunks) {
                //This is ok, as we mixin into this method:
                this.ticketManager.updatePlayerPosition(SectionPos.from(player), player);
            }
        } else {
            CubePos cubePos = CubePos.from(player.getManagedSectionPos());
            this.playerCubeGenerationTracker.removePlayer(cubePos.asLong(), player);
            if (!cannotGenerateChunksTracker) {
                ((ITicketManager)this.ticketManager).removePlayer(cubePos, player);
            }
        }

        int viewDistanceCubes = MathUtil.ceilDiv(this.viewDistance, 2);
        for(int ix = xFloor - viewDistanceCubes; ix <= xFloor + viewDistanceCubes; ++ix) {
            for (int iy = yFloor - viewDistanceCubes; iy <= yFloor + viewDistanceCubes; ++iy) {
                for (int iz = zFloor - viewDistanceCubes; iz <= zFloor + viewDistanceCubes; ++iz) {
                    CubePos cubePos = CubePos.of(ix, iy, iz);
                    this.setCubeLoadedAtClient(player, cubePos, new Object[2], !track, track);
                }
            }
        }
    }

    @Inject(method = "updatePlayerPosition", at = @At("RETURN"))
    public void updatePlayerPosition(ServerPlayerEntity player, CallbackInfo ci)
    {
        int xFloor = Coords.getCubeXForEntity(player);
        int yFloor = Coords.getCubeYForEntity(player);
        int zFloor = Coords.getCubeZForEntity(player);

        CubePos cubePosManaged = CubePos.from(player.getManagedSectionPos());
        CubePos cubePos = CubePos.from(player.getPosition());
        long managedPosAsLong = cubePosManaged.asLong();
        long posAsLong = cubePos.asLong();
        boolean isPlayerTracking = this.playerCubeGenerationTracker.func_225419_d(player);
        boolean cannotGenerateChunks = this.cannotGenerateChunks(player);
        boolean positionsAreEqual = cubePosManaged.asLong() != cubePos.asLong();
        if (positionsAreEqual || isPlayerTracking != cannotGenerateChunks) {
            this.sendPlayerCubePositionPacket(player);
            if (!isPlayerTracking) {
                ((ITicketManager)this.ticketManager).removePlayer(cubePosManaged, player);
            }

            if (!cannotGenerateChunks) {
                // we are mixin into this method, so it should work as this:
                this.ticketManager.updatePlayerPosition(cubePos.asSectionPos(), player);
            }

            if (!isPlayerTracking && cannotGenerateChunks) {
                this.playerCubeGenerationTracker.disableGeneration(player);
            }

            if (isPlayerTracking && !cannotGenerateChunks) {
                this.playerCubeGenerationTracker.enableGeneration(player);
            }

            if (managedPosAsLong != posAsLong) {
                this.playerCubeGenerationTracker.updatePlayerPosition(managedPosAsLong, posAsLong, player);
            }
        }
        int viewDistanceCubes = MathUtil.ceilDiv(this.viewDistance, 2);

        int managedX = cubePosManaged.getX();
        int managedY = cubePosManaged.getY();
        int managedZ = cubePosManaged.getZ();
        if (Math.abs(managedX - xFloor) <= viewDistanceCubes * 2 &&
                Math.abs(managedY - yFloor) <= viewDistanceCubes * 2 &&
                Math.abs(managedZ - zFloor) <= viewDistanceCubes * 2) {
            int minX = Math.min(xFloor, managedX) - viewDistanceCubes;
            int minY = Math.min(yFloor, managedY) - viewDistanceCubes;
            int minZ = Math.min(zFloor, managedZ) - viewDistanceCubes;
            int maxX = Math.max(xFloor, managedX) + viewDistanceCubes;
            int maxY = Math.max(yFloor, managedY) + viewDistanceCubes;
            int maxZ = Math.max(zFloor, managedZ) + viewDistanceCubes;

            for(int ix = minX; ix <= maxX; ++ix) {
                for(int iy = minY; iy <= maxY; ++iy) {
                    for (int iz = minZ; iz <= maxZ; ++iz) {
                        CubePos cubePos1 = CubePos.of(ix, iy, iz);
                        boolean flag5 = IChunkManager.getCubeDistance(cubePos1, managedX, managedY, managedZ) <= viewDistanceCubes;
                        boolean flag6 = IChunkManager.getCubeDistance(cubePos1, xFloor, yFloor, zFloor) <= viewDistanceCubes;
                        this.setCubeLoadedAtClient(player, cubePos1, new Object[2], flag5, flag6);
                    }
                }
            }
        } else {
            for(int ix = managedX - viewDistanceCubes; ix <= managedX + viewDistanceCubes; ++ix) {
                for(int iy = managedY - viewDistanceCubes; iy <= managedY + viewDistanceCubes; ++iy) {
                    for (int iz = managedZ - viewDistanceCubes; iz <= managedZ + viewDistanceCubes; ++iz) {
                        CubePos cubePos2 = CubePos.of(ix, iy, iz);
                        this.setCubeLoadedAtClient(player, cubePos2, new Object[2], true, false);
                    }
                }
            }

            for(int ix = xFloor - viewDistanceCubes; ix <= xFloor + viewDistanceCubes; ++ix) {
                for (int iy = yFloor - viewDistanceCubes; iy <= yFloor + viewDistanceCubes; ++iy) {
                    for (int iz = zFloor - viewDistanceCubes; iz <= zFloor + viewDistanceCubes; ++iz) {
                        CubePos cubePos3 = CubePos.of(ix, iy, iz);
                        this.setCubeLoadedAtClient(player, cubePos3, new Object[2], false, true);
                    }
                }
            }
        }
    }

    @Inject(method = "setViewDistance", at = @At("RETURN"))
    protected void setViewDistance(int viewDistance, CallbackInfo ci) {
        viewDistance = MathUtil.ceilDiv(viewDistance, 2);
        int viewDistanceCubes = MathUtil.ceilDiv(this.viewDistance, 2);
        int i = MathHelper.clamp(viewDistance + 1, 3, 33);
        if (i != viewDistanceCubes) {
            for(ChunkHolder chunkholder : this.loadedCubes.values()) {
                CubePos cubePos = ((ICubeHolder)chunkholder).getCubePos();
                Object[] objects = new Object[2];
                this.getCubeTrackingPlayers(cubePos, false).forEach((serverPlayerEntity) -> {
                    int k = getDistanceToPlayer(cubePos, serverPlayerEntity, true);
                    boolean flag = k <= viewDistanceCubes;
                    boolean flag1 = k <= viewDistanceCubes;
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
                        this.sendCubeData(player, packetCache, cube, cube.getCubePos());
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

    //TODO: remove CubePos param, cube contains a cubepos
    // sendChunkData
    private void sendCubeData(ServerPlayerEntity player, Object[] packetCache, Cube cubeIn, CubePos pos) {
        if (packetCache[0] == null) {
            packetCache[0] = new PacketCubes(Collections.singletonList(cubeIn));
            //packetCache[1] = new SUpdateLightPacket(pos, this.lightManager);
        }

        PacketDispatcher.sendTo(packetCache[0], player);
        List<Entity> leashedEntities = Lists.newArrayList();
        List<Entity> passengerEntities = Lists.newArrayList();

        for (ChunkManager.EntityTracker entityTracker : this.entities.values()) {
            Entity entity = ((EntityTrackerAccess) entityTracker).getEntity();
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

    //func_219215_b
    private static int getDistanceToPlayer(CubePos pos, ServerPlayerEntity player, boolean useManagedPos) {
        int x;
        int y;
        int z;
        if (useManagedPos) {
            CubePos cubePos = CubePos.from(player.getManagedSectionPos());
            x = cubePos.getX();
            y = cubePos.getY();
            z = cubePos.getZ();
        } else {
            x = Coords.getCubeXForEntity(player);
            y = Coords.getCubeYForEntity(player);
            z = Coords.getCubeZForEntity(player);
        }
        return IChunkManager.getCubeDistance(pos, x, y, z);
    }
}
