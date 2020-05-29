package cubicchunks.cc.mixin.core.common.chunk;

import static cubicchunks.cc.chunk.util.Utils.unsafeCast;
import static net.minecraft.world.server.ChunkManager.MAX_LOADED_LEVEL;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import cubicchunks.cc.CubeSerializer;
import cubicchunks.cc.chunk.IChunkManager;
import cubicchunks.cc.chunk.ICube;
import cubicchunks.cc.chunk.ICubeHolder;
import cubicchunks.cc.chunk.ICubeStatusListener;
import cubicchunks.cc.chunk.graph.CCTicketType;
import cubicchunks.cc.chunk.cube.CubePrimer;
import cubicchunks.cc.chunk.cube.CubePrimerWrapper;
import cubicchunks.cc.chunk.cube.Cube;
import cubicchunks.cc.chunk.ticket.CubeTaskPriorityQueueSorter;
import cubicchunks.cc.chunk.ticket.ITicketManager;
import cubicchunks.cc.chunk.util.CubePos;
import cubicchunks.cc.network.PacketCubes;
import cubicchunks.cc.network.PacketDispatcher;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
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
import net.minecraft.world.chunk.ChunkSection;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.function.Supplier;
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

    private final PlayerGenerationTracker playerSectionGenerationTracker = new PlayerGenerationTracker();

    // field_219264_r
    private ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> worldgenExecutor;
    // field_219265_s
    private ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> mainExecutor;

    private final AtomicInteger cubesLoaded = new AtomicInteger();


    @Shadow @Final private static Logger LOGGER;

    @Shadow @Final private ServerWorldLightManager lightManager;

    @Shadow private boolean immutableLoadedChunksDirty;

    @Shadow @Final private ChunkManager.ProxyTicketManager ticketManager;

    @Shadow @Final private ServerWorld world;

    @Shadow @Final private TemplateManager templateManager;

    @Shadow @Final private ThreadTaskExecutor<Runnable> mainThread;

    @Shadow @Final private PointOfInterestManager pointOfInterestManager;

    @Shadow @Final private IChunkStatusListener field_219266_t;

    @Shadow(aliases = "func_219205_a") protected abstract ChunkStatus getParentStatus(ChunkStatus status, int upCount);

    @Shadow @Final private ChunkGenerator<?> generator;

    @Shadow protected abstract CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> chunkGenerate(ChunkHolder chunkHolderIn,
            ChunkStatus chunkStatusIn);

    @Shadow @Final private File dimensionDirectory;

    @Shadow private int viewDistance;

    @Shadow @Final private Int2ObjectMap<ChunkManager.EntityTracker> entities;

    @Shadow @Final private Long2ObjectLinkedOpenHashMap<ChunkHolder> loadedChunks;

    @Shadow private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> immutableLoadedChunks;

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
        if (oldLevel > MAX_LOADED_LEVEL && newLevel > MAX_LOADED_LEVEL) {
            return holder;
        } else {
            if (holder != null) {
                holder.setChunkLevel(newLevel);
            }

            if (holder != null) {
                if (newLevel > MAX_LOADED_LEVEL) {
                    this.unloadableCubes.add(cubePosIn);
                } else {
                    this.unloadableCubes.remove(cubePosIn);
                }
            }

            if (newLevel <= MAX_LOADED_LEVEL && holder == null) {
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
        return this.immutableLoadedChunks.get(cubePosIn);
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
            ((ICubeStatusListener) field_219266_t).cubeStatusChanged(
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
            ((ICubeStatusListener) field_219266_t).cubeStatusChanged(
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
            ((ICubeStatusListener) field_219266_t).cubeStatusChanged(
                    ((ICubeHolder) chunkHolderIn).getCubePos(), null);
        }
    }

    @Inject(method = "refreshOffThreadCache", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap;clone()Lit/unimi/dsi/fastutil/longs/Long2ObjectLinkedOpenHashMap;"))
    private void onRefreshCache(CallbackInfoReturnable<Boolean> cir) {
        this.immutableLoadedCubes = loadedChunks.clone();
    }

    @Override
    public Iterable<ChunkHolder> getLoadedSectionsIterable() {
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
            CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> completablefuture = unsafeCast(
                    ((ICubeHolder) chunkHolderIn).createCubeFuture(chunkStatusIn.getParent(), (ChunkManager) (Object) this)
            );
            return unsafeCast(completablefuture.thenComposeAsync(
                    (Either<IChunk, ChunkHolder.IChunkLoadingError> inputSection) -> {
                        Optional<IChunk> optional = inputSection.left();
                        if (!optional.isPresent()) {
                            return CompletableFuture.completedFuture(inputSection);
                        } else {
                            if (chunkStatusIn == ChunkStatus.LIGHT) {
                                ((ITicketManager) this.ticketManager).registerWithLevel(CCTicketType.CCLIGHT, cubePos,
                                        33 + ChunkStatus.getDistance(ChunkStatus.FEATURES), cubePos);
                            }

                            IChunk iChunk = optional.get();
                            if (iChunk.getStatus().isAtLeast(chunkStatusIn)) {
                                CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> completablefuture1;
                                if (chunkStatusIn == ChunkStatus.LIGHT) {
                                    completablefuture1 = this.chunkGenerate(chunkHolderIn, chunkStatusIn);
                                } else {
                                    completablefuture1 =
                                            chunkStatusIn.doLoadingWork(this.world, this.templateManager, this.lightManager, (chunk) -> {
                                                return unsafeCast(this.makeChunkInstance(chunkHolderIn));
                                            }, iChunk);
                                }

                                ((ICubeStatusListener) this.field_219266_t).cubeStatusChanged(cubePos, chunkStatusIn);
                                return completablefuture1;
                            } else {
                                return unsafeCast(this.sectionGenerate(chunkHolderIn, chunkStatusIn));
                            }
                        }
                    }, this.mainThread));
        }
    }

    //chunkGenerate
    private CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> sectionGenerate(ChunkHolder chunkHolderIn, ChunkStatus chunkStatusIn) {
        CubePos cubePos = ((ICubeHolder) chunkHolderIn).getCubePos();
        CompletableFuture<Either<List<ICube>, ChunkHolder.IChunkLoadingError>> future =
                this.makeFutureForStatusNeighbors(cubePos, chunkStatusIn.getTaskRange(), (count) -> {
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
                                return unsafeCast(this.makeChunkInstance(chunkHolderIn));
                            }, unsafeCast(neighborSections)));
                    ((ICubeStatusListener) this.field_219266_t).cubeStatusChanged(cubePos, chunkStatusIn);
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
                    cubePos, 33 + ChunkStatus.getDistance(ChunkStatus.FEATURES), cubePos);
        }, () -> {
            return "release light ticket " + cubePos;
        }));
    }

    // func_219200_b
    private CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> makeChunkInstance(ChunkHolder holder) {
        CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> fullFuture =
                ((ICubeHolder) holder).getSectionFuture(ChunkStatus.FULL.getParent());
        return fullFuture.thenApplyAsync((sectionOrError) -> {
            ChunkStatus chunkstatus = ChunkHolder.getChunkStatusFromLevel(holder.getChunkLevel());
            return !chunkstatus.isAtLeast(ChunkStatus.FULL) ? ICubeHolder.MISSING_CUBE : sectionOrError.mapLeft((section) -> {
                CubePos cubePos = ((ICubeHolder) holder).getCubePos();
                Cube cube;
                if (section instanceof CubePrimerWrapper) {
                        cube = ((CubePrimerWrapper)section).getCube();
                } else {
                    cube = new Cube(this.world, cubePos, ((CubePrimer) section).getChunkSections(), null);
                    ((ICubeHolder) holder).onSectionWrapperCreated(new CubePrimerWrapper(cube));
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
    public CompletableFuture<Either<Cube, ChunkHolder.IChunkLoadingError>> createSectionBorderFuture(ChunkHolder chunkHolder) {
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
    public CompletableFuture<Either<Cube, ChunkHolder.IChunkLoadingError>> createSectionTickingFuture(ChunkHolder chunkHolder) {
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
                Object[] ipacket = new Object[2];
                this.getSectionTrackingPlayers(cubePos, false).forEach((serverPlayerEntity) -> {
                    this.sendSectionData(serverPlayerEntity, ipacket, cube, cubePos);
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

                ICube icube = CubeSerializer.loadCube(cubePos, this.dimensionDirectory.toPath());
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
    protected ChunkHolder getLoadedSection(long sectionPosIn) {
        return this.loadedCubes.get(sectionPosIn);
    }

    // getTrackingPlayers
    public Stream<ServerPlayerEntity> getSectionTrackingPlayers(CubePos pos, boolean boundaryOnly) {
        return this.playerSectionGenerationTracker.getGeneratingPlayers(pos.asLong()).filter((p_219192_3_) -> {
            int i = this.getSectionChebyshevDistance(pos, p_219192_3_, true);
            if (i > this.viewDistance) {
                return false;
            } else {
                return !boundaryOnly || i == this.viewDistance;
            }
        });
    }

    // func_219215_b
    private static int getSectionChebyshevDistance(CubePos pos, ServerPlayerEntity player, boolean p_219215_2_) {
        int x;
        int y;
        int z;
        if (p_219215_2_) {
            //THIS IS FINE AS SECTION POS, AS IT IS CONVERTED TO CUBE POS WITH THE >> 1
            SectionPos sectionpos = player.getManagedSectionPos();
            x = sectionpos.getSectionX() >> 1;
            y = sectionpos.getSectionY() >> 1;
            z = sectionpos.getSectionZ() >> 1;
        } else {
            x = MathHelper.floor(player.getPosX() / 16.0D) >> 1;
            y = MathHelper.floor(player.getPosY() / 16.0D) >> 1;
            z = MathHelper.floor(player.getPosZ() / 16.0D) >> 1;
        }

        return getCubeDistance(pos, x, y, z);
    }

    //getChunkDistance
    private static int getCubeDistance(CubePos cubePosIn, int x, int y, int z) {
        int dX = cubePosIn.getX() - x;
        int dY = cubePosIn.getY() - y;
        int dZ = cubePosIn.getZ() - z;
        return Math.max(Math.max(Math.abs(dX), Math.abs(dZ)), Math.abs(dY));
    }

    // func_222973_a
    @Override
    public CompletableFuture<Void> saveCubeScheduleTicks(Cube sectionIn) {
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
    public CompletableFuture<Either<Cube, ChunkHolder.IChunkLoadingError>> createSectionEntityTickingFuture(CubePos pos) {
        return this.createCubeRegionFuture(pos, 2, (index) -> {
            return ChunkStatus.FULL;
        }).thenApplyAsync((eitherSectionError) -> {
            return eitherSectionError.mapLeft((cubes) -> {
                return (Cube) cubes.get(cubes.size() / 2);
            });
        }, this.mainThread);
    }

    // sendChunkData
    private void sendSectionData(ServerPlayerEntity player, Object[] packetCache, Cube cubeIn, CubePos pos) {
        if (packetCache[0] == null) {
            packetCache[0] = new PacketCubes(Collections.singletonMap(pos, cubeIn));
            //packetCache[1] = new SUpdateLightPacket(pos, this.lightManager);
        }

        PacketDispatcher.sendTo(packetCache[0], player);
        List<Entity> leashedEntities = Lists.newArrayList();
        List<Entity> passengerEntities = Lists.newArrayList();

        for (ChunkManager.EntityTracker entityTracker : this.entities.values()) {
            Entity entity = ((IEntityTracker) entityTracker).getEntity();
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

    public int getLoadedSectionsCount()
    {
        return this.cubesLoaded.get();
    }
}
