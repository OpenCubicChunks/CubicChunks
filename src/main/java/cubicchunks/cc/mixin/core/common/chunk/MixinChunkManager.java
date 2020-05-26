package cubicchunks.cc.mixin.core.common.chunk;

import static net.minecraft.util.math.SectionPos.extractX;
import static net.minecraft.util.math.SectionPos.extractY;
import static net.minecraft.util.math.SectionPos.extractZ;
import static net.minecraft.world.server.ChunkManager.MAX_LOADED_LEVEL;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import cubicchunks.cc.SectionSerializer;
import cubicchunks.cc.chunk.IChunkManager;
import cubicchunks.cc.chunk.IChunkSection;
import cubicchunks.cc.chunk.ISection;
import cubicchunks.cc.chunk.ISectionHolder;
import cubicchunks.cc.chunk.ISectionStatusListener;
import cubicchunks.cc.chunk.graph.CCTicketType;
import cubicchunks.cc.chunk.section.SectionPrimer;
import cubicchunks.cc.chunk.ticket.CubeTaskPriorityQueueSorter;
import cubicchunks.cc.chunk.ticket.ITicketManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.crash.ReportedException;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
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

    private final Long2ObjectLinkedOpenHashMap<ChunkHolder> loadedSections = new Long2ObjectLinkedOpenHashMap<>();
    private final LongSet unloadableSections = new LongOpenHashSet();
    private final Long2ObjectLinkedOpenHashMap<ChunkHolder> sectionsToUnload = new Long2ObjectLinkedOpenHashMap<>();

    private final PlayerGenerationTracker playerSectionGenerationTracker = new PlayerGenerationTracker();

    // field_219265_s
    private ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> mainExecutor;

    private final AtomicInteger sectionsLoaded = new AtomicInteger();

    @Shadow @Final private static Logger LOGGER;

    @Shadow @Final private ServerWorldLightManager lightManager;

    @Shadow private boolean immutableLoadedChunksDirty;

    @Shadow @Final private ChunkManager.ProxyTicketManager ticketManager;

    @Shadow @Final private ServerWorld world;

    @Shadow @Final private TemplateManager templateManager;

    @Shadow @Final private ThreadTaskExecutor<Runnable> mainThread;

    @Shadow @Final private PointOfInterestManager pointOfInterestManager;

    @Shadow @Final private IChunkStatusListener field_219266_t;

    @Shadow protected abstract ChunkStatus func_219205_a(ChunkStatus p_219205_1_, int p_219205_2_);

    @Shadow @Final private ChunkGenerator<?> generator;

    @Shadow protected abstract CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> func_219200_b(ChunkHolder p_219200_1_);

    @Shadow protected abstract CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> chunkGenerate(ChunkHolder chunkHolderIn,
            ChunkStatus chunkStatusIn);

    @Shadow @Final private File dimensionDirectory;

    @Shadow private int viewDistance;

    @Inject(method = "<init>", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void onConstruct(ServerWorld worldIn, File worldDirectory, DataFixer p_i51538_3_, TemplateManager templateManagerIn,
            Executor p_i51538_5_, ThreadTaskExecutor mainThreadIn, IChunkLightProvider p_i51538_7_,
            ChunkGenerator generatorIn, IChunkStatusListener p_i51538_9_, Supplier p_i51538_10_,
            int p_i51538_11_, CallbackInfo ci, DelegatedTaskExecutor delegatedtaskexecutor,
            ITaskExecutor itaskexecutor, DelegatedTaskExecutor delegatedtaskexecutor1) {

        this.cubeTaskPriorityQueueSorter = new CubeTaskPriorityQueueSorter(ImmutableList.of(delegatedtaskexecutor,
                itaskexecutor, delegatedtaskexecutor1), p_i51538_5_, Integer.MAX_VALUE);
        this.mainExecutor = this.cubeTaskPriorityQueueSorter.createExecutor(itaskexecutor, false);
    }

    @Nullable
    @Override
    public ChunkHolder setSectionLevel(long sectionPosIn, int newLevel, @Nullable ChunkHolder holder, int oldLevel) {
        if (oldLevel > MAX_LOADED_LEVEL && newLevel > MAX_LOADED_LEVEL) {
            return holder;
        } else {
            if (holder != null) {
                holder.setChunkLevel(newLevel);
            }

            if (holder != null) {
                if (newLevel > MAX_LOADED_LEVEL) {
                    this.unloadableSections.add(sectionPosIn);
                } else {
                    this.unloadableSections.remove(sectionPosIn);
                }
            }

            if (newLevel <= MAX_LOADED_LEVEL && holder == null) {
                holder = this.sectionsToUnload.remove(sectionPosIn);
                if (holder != null) {
                    holder.setChunkLevel(newLevel);
                } else {

                    holder = new ChunkHolder(new ChunkPos(extractX(sectionPosIn), extractZ(sectionPosIn)), newLevel, this.lightManager,
                            this.cubeTaskPriorityQueueSorter, (ChunkHolder.IPlayerProvider) this);
                    ((ISectionHolder) holder).setYPos(extractY(sectionPosIn));
                }
                this.loadedSections.put(sectionPosIn, holder);
                this.immutableLoadedChunksDirty = true;
            }

            return holder;
        }
    }

    @Override
    public LongSet getUnloadableSections() {
        return this.unloadableSections;
    }

    @Override
    public ChunkHolder getSectionHolder(long sectionPosIn) {
        return loadedSections.get(sectionPosIn);
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
        if (((ISectionHolder) chunkHolderIn).getSectionPos() != null) {
            ((ISectionStatusListener) field_219266_t).sectionStatusChanged(
                    ((ISectionHolder) chunkHolderIn).getSectionPos(),
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
        if (((ISectionHolder) chunkHolderIn).getSectionPos() != null) {
            ((ISectionStatusListener) field_219266_t).sectionStatusChanged(
                    ((ISectionHolder) chunkHolderIn).getSectionPos(), null);
        }
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "lambda$null$18", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/listener/IChunkStatusListener;statusChanged(Lnet/minecraft/util/math/ChunkPos;"
                    + "Lnet/minecraft/world/chunk/ChunkStatus;)V"))
    private void onGenerateStatusChange(ChunkStatus chunkStatusIn, ChunkHolder chunkHolderIn, ChunkPos chunkpos, List<?> p_223148_4_,
            CallbackInfoReturnable<CompletableFuture<?>> cir) {
        if (((ISectionHolder) chunkHolderIn).getSectionPos() != null) {
            ((ISectionStatusListener) field_219266_t).sectionStatusChanged(
                    ((ISectionHolder) chunkHolderIn).getSectionPos(), null);
        }
    }

    //func_219244_a
    @SuppressWarnings("unchecked")
    @Override
    public CompletableFuture<Either<ISection, ChunkHolder.IChunkLoadingError>> createSectionFuture(ChunkHolder chunkHolderIn,
            ChunkStatus chunkStatusIn) {
        SectionPos sectionPos = ((ISectionHolder) chunkHolderIn).getSectionPos();
        if (chunkStatusIn == ChunkStatus.EMPTY) {
            return this.sectionLoad(sectionPos);
        } else {
            CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> completablefuture = (CompletableFuture<Either<IChunk,
                    ChunkHolder.IChunkLoadingError>>) (Object)
                    ((ISectionHolder) chunkHolderIn).createFuture(chunkStatusIn.getParent(), (ChunkManager) (Object) this);
            return (CompletableFuture<Either<ISection, ChunkHolder.IChunkLoadingError>>)
                    (Object) completablefuture.thenComposeAsync((Either<IChunk,
                            ChunkHolder.IChunkLoadingError> inputSection) -> {
                        Optional<IChunk> optional = inputSection.left();
                        if (!optional.isPresent()) {
                            return CompletableFuture.completedFuture(inputSection);
                        } else {
                            if (chunkStatusIn == ChunkStatus.LIGHT) {
                                ((ITicketManager) this.ticketManager).registerWithLevel(CCTicketType.CCLIGHT, sectionPos,
                                        33 + ChunkStatus.getDistance(ChunkStatus.FEATURES), sectionPos);
                            }

                            IChunk iChunk = optional.get();
                            if (((IChunkSection) iChunk).getStatus().isAtLeast(chunkStatusIn)) {
                                CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> completablefuture1;
                                if (chunkStatusIn == ChunkStatus.LIGHT) {
                                    completablefuture1 = this.chunkGenerate(chunkHolderIn, chunkStatusIn);
                                } else {
                                    completablefuture1 =
                                            chunkStatusIn.doLoadingWork(this.world, this.templateManager, this.lightManager, (p_223175_2_) -> {
                                                return this.func_219200_b(chunkHolderIn);
                                            }, iChunk);
                                }

                                ((ISectionStatusListener) this.field_219266_t).sectionStatusChanged(sectionPos, chunkStatusIn);
                                return completablefuture1;
                            } else {
                                return this.chunkGenerate(chunkHolderIn, chunkStatusIn);
                            }
                        }
                    }, this.mainThread);
        }
    }

    @Override
    public CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> createBorderFuture(ChunkHolder chunkHolder) {
        return ((ISectionHolder)chunkHolder).createFuture(ChunkStatus.FULL, (ChunkManager)(Object)this).thenApplyAsync((p_222976_0_) -> {
            return p_222976_0_.mapLeft((p_222955_0_) -> {
                ChunkSection chunkSection = (ChunkSection)p_222955_0_;
                //TODO: implement rescheduleTicks for chunkSection
                //chunkSection.rescheduleTicks();
                return chunkSection;
            });
        }, (p_222962_2_) -> {
            this.mainExecutor.enqueue(CubeTaskPriorityQueueSorter.createMsg(chunkHolder, p_222962_2_));
        });
    }

    // func_219179_a
    @Override
    public CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> createTickingFuture(ChunkHolder chunkHolder) {
        SectionPos sectionPos = ((ISectionHolder)chunkHolder).getSectionPos();
        CompletableFuture<Either<List<ISection>, ChunkHolder.IChunkLoadingError>> completablefuture = this.getSectionRegionFuture(sectionPos, 1,
                (p_219172_0_) -> {
            return ChunkStatus.FULL;
        });
        CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> completablefuture1 = completablefuture.thenApplyAsync((p_219239_0_) -> {
            return p_219239_0_.flatMap((p_219208_0_) -> {
                ChunkSection chunkSection = (ChunkSection)p_219208_0_.get(p_219208_0_.size() / 2);
                //TODO: implement this later
                //chunkSection.postProcess();
                return Either.left(chunkSection);
            });
        }, (p_219230_2_) -> {
            this.mainExecutor.enqueue(CubeTaskPriorityQueueSorter.createMsg(chunkHolder, p_219230_2_));
        });
        completablefuture1.thenAcceptAsync((chunkSectionLoadingErrorEither) -> {
            chunkSectionLoadingErrorEither.mapLeft((chunkSection) -> {
                this.sectionsLoaded.getAndIncrement();
                IPacket<?>[] ipacket = new IPacket[2];
                this.getSectionTrackingPlayers(sectionPos, false).forEach((serverPlayerEntity) -> {
                    this.sendChunkData(serverPlayerEntity, ipacket, chunkSection);
                });
                return Either.left(chunkSection);
            });
        }, (p_219202_2_) -> {
            this.mainExecutor.enqueue(CubeTaskPriorityQueueSorter.createMsg(chunkHolder, p_219202_2_));
        });
        return completablefuture1;
    }

    //chunkLoad
    private CompletableFuture<Either<ISection, ChunkHolder.IChunkLoadingError>> sectionLoad(SectionPos sectionPos) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.world.getProfiler().func_230035_c_("chunkLoad");

                ISection iSection = SectionSerializer.loadSection(sectionPos, this.dimensionDirectory.toPath());
                return Either.left(iSection);

            } catch (ReportedException reportedexception) {
                Throwable throwable = reportedexception.getCause();
                if (!(throwable instanceof IOException)) {
                    throw reportedexception;
                }

                LOGGER.error("Couldn't load chunk {}", sectionPos, throwable);
            } catch (Exception exception) {
                LOGGER.error("Couldn't load chunk {}", sectionPos, exception);
            }

            return Either.left(new SectionPrimer(sectionPos, null));
        }, this.mainThread);
    }

    // func_219236_a
    @Override
    public CompletableFuture<Either<List<ISection>, ChunkHolder.IChunkLoadingError>> getSectionRegionFuture(ChunkPos pos, int p_219236_2_,
            IntFunction<ChunkStatus> p_219236_3_) {
        List<CompletableFuture<Either<ISection, ChunkHolder.IChunkLoadingError>>> list = Lists.newArrayList();
        int i = pos.x;
        int j = pos.z;

        for (int k = -p_219236_2_; k <= p_219236_2_; ++k) {
            for (int l = -p_219236_2_; l <= p_219236_2_; ++l) {
                int i1 = Math.max(Math.abs(l), Math.abs(k));
                final ChunkPos chunkpos = new ChunkPos(i + l, j + k);
                long j1 = chunkpos.asLong();
                ChunkHolder chunkholder = this.getLoadedSection(j1);
                if (chunkholder == null) {
                    //noinspection MixinInnerClass
                    return CompletableFuture.completedFuture(Either.right(new ChunkHolder.IChunkLoadingError() {
                        public String toString() {
                            return "Unloaded " + chunkpos.toString();
                        }
                    }));
                }

                ChunkStatus chunkstatus = p_219236_3_.apply(i1);
                CompletableFuture<Either<ISection, ChunkHolder.IChunkLoadingError>> completablefuture =
                        ((ISectionHolder)chunkholder).createFuture(chunkstatus,
                        (ChunkManager)(Object)this);
                list.add(completablefuture);
            }
        }

        CompletableFuture<List<Either<ISection, ChunkHolder.IChunkLoadingError>>> completablefuture1 = Util.gather(list);
        return completablefuture1.thenApply((p_219227_4_) -> {
            List<ISection> list1 = Lists.newArrayList();
            int k1 = 0;

            for (final Either<ISection, ChunkHolder.IChunkLoadingError> either : p_219227_4_) {
                Optional<ISection> optional = either.left();
                if (!optional.isPresent()) {
                    final int l1 = k1;
                    //noinspection MixinInnerClass
                    return Either.right(new ChunkHolder.IChunkLoadingError() {
                        public String toString() {
                            return "Unloaded " + new ChunkPos(i + l1 % (p_219236_2_ * 2 + 1), j + l1 / (p_219236_2_ * 2 + 1)) + " " + either.right()
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
        return this.loadedSections.get(sectionPosIn);
    }

    // getTrackingPlayers
    /**
     * Returns the players tracking the given chunk.
     */
    public Stream<ServerPlayerEntity> getSectionTrackingPlayers(SectionPos pos, boolean boundaryOnly) {
        return this.playerSectionGenerationTracker.getGeneratingPlayers(pos.asLong()).filter((p_219192_3_) -> {
            int i = this.getChebyshevDistance(pos, p_219192_3_, true);
            if (i > this.viewDistance) {
                return false;
            } else {
                return !boundaryOnly || i == this.viewDistance;
            }
        });
    }

    // func_219215_b
    private static int getChebyshevDistance(SectionPos pos, ServerPlayerEntity player, boolean p_219215_2_) {
        int x;
        int y;
        int z;
        if (p_219215_2_) {
            SectionPos sectionpos = player.getManagedSectionPos();
            x = sectionpos.getSectionX();
            y = sectionpos.getSectionY();
            z = sectionpos.getSectionZ();
        } else {
            x = MathHelper.floor(player.getPosX() / 16.0D);
            y = MathHelper.floor(player.getPosY() / 16.0D);
            z = MathHelper.floor(player.getPosZ() / 16.0D);
        }

        return getSectionDistance(pos, x, y, z);
    }

    //getChunkDistance
    private static int getSectionDistance(SectionPos sectionPosIn, int x, int y, int z) {
        int dX = sectionPosIn.getX() - x;
        int dY = sectionPosIn.getY() - y;
        int dZ = sectionPosIn.getZ() - z;
        return Math.max(Math.max(Math.abs(dX), Math.abs(dZ)), Math.abs(dY));
    }
}
