package io.github.opencubicchunks.cubicchunks.mixin.core.common;

import static io.github.opencubicchunks.cubicchunks.utils.Utils.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.VerticalViewDistanceListener;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.ChunkMapAccess;
import io.github.opencubicchunks.cubicchunks.server.level.CubeHolder;
import io.github.opencubicchunks.cubicchunks.server.level.CubeMap;
import io.github.opencubicchunks.cubicchunks.server.level.CubicDistanceManager;
import io.github.opencubicchunks.cubicchunks.server.level.CubicTicketType;
import io.github.opencubicchunks.cubicchunks.server.level.ServerCubeCache;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.CubicNaturalSpawner;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeStatus;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.LevelCube;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.LightCubeGetter;
import io.github.opencubicchunks.cubicchunks.world.server.CubicServerLevel;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ServerChunkCache.class)
public abstract class MixinServerChunkCache implements ServerCubeCache, LightCubeGetter, VerticalViewDistanceListener {
    @Shadow @Final private static List<ChunkStatus> CHUNK_STATUSES;

    @Shadow @Final public ChunkMap chunkMap;
    @Shadow @Final private ServerLevel level;
    @Shadow @Final private DistanceManager distanceManager;
    @Shadow @Final private ServerChunkCache.MainThreadExecutor mainThreadProcessor;
    @Shadow @Final private Thread mainThread;

    @Shadow private boolean spawnEnemies;
    @Shadow private boolean spawnFriendlies;

    private final long[] lastCubePos = new long[4];
    private final ChunkStatus[] lastCubeStatus = new ChunkStatus[4];
    private final CubeAccess[] lastCube = new CubeAccess[4];

    @Shadow protected abstract boolean chunkAbsent(@Nullable ChunkHolder chunkHolderIn, int i);

    @Shadow public abstract int getLoadedChunksCount();

    @Shadow @Nullable protected abstract ChunkHolder getVisibleChunkIfPresent(long pos);

    @Shadow abstract boolean runDistanceManagerUpdates();

    @Override
    public <T> void addCubeRegionTicket(TicketType<T> type, CubePos pos, int distance, T value) {
        ((CubicDistanceManager) this.distanceManager).addCubeRegionTicket(type, pos, distance, value);
    }

    public <T> void removeCubeRegionTicket(TicketType<T> type, CubePos pos, int distance, T value) {
        ((CubicDistanceManager) this.distanceManager).removeCubeRegionTicket(type, pos, distance, value);
    }

    @Override public int getTickingGeneratedCubes() {
        return ((CubeMap) chunkMap).getTickingGeneratedCubes();
    }

    @Nullable
    @Override
    public CubeAccess getCube(int cubeX, int cubeY, int cubeZ, ChunkStatus requiredStatus, boolean load) {
        if (Thread.currentThread() != this.mainThread) {
            return CompletableFuture.supplyAsync(() -> {
                return this.getCube(cubeX, cubeY, cubeZ, requiredStatus, load);
            }, this.mainThreadProcessor).join();
        } else {
            ProfilerFiller profiler = this.level.getProfiler();
            profiler.incrementCounter("getCube");
            long i = CubePos.asLong(cubeX, cubeY, cubeZ);

            for (int j = 0; j < 4; ++j) {
                if (i == this.lastCubePos[j] && requiredStatus == this.lastCubeStatus[j]) {
                    CubeAccess cube = this.lastCube[j];
                    if (cube != null || !load) {
                        return cube;
                    }
                }
            }

            profiler.incrementCounter("getChunkCacheMiss");
            CompletableFuture<Either<CubeAccess, ChunkHolder.ChunkLoadingFailure>> futureCube = this.getCubeFutureMainThread(cubeX, cubeY, cubeZ, requiredStatus, load);
            this.mainThreadProcessor.managedBlock(futureCube::isDone);
            CubeAccess cube = futureCube.join().map(Function.identity(), (chunkLoadingFailure) -> {
                if (load) {
                    throw Util.pauseInIde(new IllegalStateException("Cube not there when requested: " + chunkLoadingFailure));
                } else {
                    return null;
                }
            });
            this.storeCubeInCache(i, cube, requiredStatus);
            return cube;
        }
    }

    @Nullable
    public LevelCube getCubeNow(int cubeX, int cubeY, int cubeZ) {
        if (Thread.currentThread() != this.mainThread) {
            return null;
        } else {
            this.level.getProfiler().incrementCounter("getChunkNow");
            long posAsLong = CubePos.asLong(cubeX, cubeY, cubeZ);

            for (int j = 0; j < 4; ++j) {
                if (posAsLong == this.lastCubePos[j] && this.lastCubeStatus[j] == ChunkStatus.FULL) {
                    CubeAccess icube = this.lastCube[j];
                    return icube instanceof LevelCube ? (LevelCube) icube : null;
                }
            }

            ChunkHolder chunkholder = this.getVisibleCubeIfPresent(posAsLong);
            if (chunkholder == null) {
                return null;
            } else {
                Either<CubeAccess, ChunkHolder.ChunkLoadingFailure> either =
                    ((CubeHolder) chunkholder).getCubeFutureIfPresent(ChunkStatus.FULL).getNow(null);
                if (either == null) {
                    return null;
                } else {
                    CubeAccess cube = either.left().orElse(null);
                    if (cube != null) {
                        this.storeCubeInCache(posAsLong, cube, ChunkStatus.FULL);
                        if (cube instanceof LevelCube) {
                            return (LevelCube) cube;
                        }
                    }
                    return null;
                }
            }
        }
    }

    // forceChunk
    @Override
    public void forceCube(CubePos pos, boolean add) {
        ((CubicDistanceManager) this.distanceManager).updateCubeForced(pos, add);
    }


    @Override public CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getColumnFutureForCube(CubePos cubepos, int chunkX, int chunkZ, ChunkStatus leastStatus,
                                                                                                                    boolean create) {
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        long l = chunkPos.toLong();
        int i = 33 + ChunkStatus.getDistance(leastStatus);
        ChunkHolder chunkHolder = this.getVisibleChunkIfPresent(l);
        if (create) {
            this.distanceManager.addTicket(CubicTicketType.COLUMN, chunkPos, i, cubepos);
            if (this.chunkAbsent(chunkHolder, i)) {
                ProfilerFiller profilerFiller = this.level.getProfiler();
                profilerFiller.push("chunkLoad");
                this.runDistanceManagerUpdates();
                chunkHolder = this.getVisibleChunkIfPresent(l);
                profilerFiller.pop();
                if (this.chunkAbsent(chunkHolder, i)) {
                    throw Util.pauseInIde(new IllegalStateException("No chunk holder after ticket has been added"));
                }
            }
        }
        return this.chunkAbsent(chunkHolder, i) ? ChunkHolder.UNLOADED_CHUNK_FUTURE : chunkHolder.getOrScheduleFuture(leastStatus, this.chunkMap);
    }

    // getChunkFutureMainThread
    private CompletableFuture<Either<CubeAccess, ChunkHolder.ChunkLoadingFailure>> getCubeFutureMainThread(int cubeX, int cubeY, int cubeZ,
                                                                                                           ChunkStatus requiredStatus, boolean load) {
        CubePos cubePos = CubePos.of(cubeX, cubeY, cubeZ);
        long i = cubePos.asLong();
        int j = 33 + CubeStatus.getDistance(requiredStatus);
        ChunkHolder chunkholder = this.getVisibleCubeIfPresent(i);
        if (load) {
            ((CubicDistanceManager) this.distanceManager).addCubeTicket(CubicTicketType.UNKNOWN, cubePos, j, cubePos);
            if (this.chunkAbsent(chunkholder, j)) {
                ProfilerFiller profiler = this.level.getProfiler();
                profiler.push("chunkLoad");
                this.runCubeDistanceManagerUpdates();
                chunkholder = this.getVisibleCubeIfPresent(i);
                profiler.pop();
                if (this.chunkAbsent(chunkholder, j)) {
                    throw Util.pauseInIde(new IllegalStateException("No cube holder after ticket has been added"));
                }
            }
        }

        return this.chunkAbsent(chunkholder, j) ? CubeHolder.MISSING_CUBE_FUTURE : ((CubeHolder) chunkholder).getOrScheduleCubeFuture(requiredStatus,
            this.chunkMap);
    }

    // getVisibleChunkIfPresent
    @Nullable
    private ChunkHolder getVisibleCubeIfPresent(long cubePosIn) {
        return ((CubeMap) this.chunkMap).getVisibleCubeIfPresent(cubePosIn);
    }

    // storeInCache
    private void storeCubeInCache(long newPositionIn, CubeAccess newCubeIn, ChunkStatus newStatusIn) {
        for (int i = 3; i > 0; --i) {
            this.lastCubePos[i] = this.lastCubePos[i - 1];
            this.lastCubeStatus[i] = this.lastCubeStatus[i - 1];
            this.lastCube[i] = this.lastCube[i - 1];
        }
        this.lastCubePos[0] = newPositionIn;
        this.lastCubeStatus[0] = newStatusIn;
        this.lastCube[0] = newCubeIn;
    }

    @Override public void setIncomingVerticalViewDistance(int verticalDistance) {
        ((VerticalViewDistanceListener) this.chunkMap).setIncomingVerticalViewDistance(verticalDistance);
    }

    @Inject(method = "runDistanceManagerUpdates", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;clearCache()V"))
    private void onClearCache(CallbackInfoReturnable<Boolean> cir) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }
        this.clearCubeCache();
    }

    // runDistanceManagerUpdates
    private boolean runCubeDistanceManagerUpdates() {
        boolean flag = this.distanceManager.runAllUpdates(this.chunkMap);
        boolean flag1 = ((ChunkMapAccess) this.chunkMap).invokePromoteChunkMap();
        if (!flag && !flag1) {
            return false;
        } else {
            this.clearCubeCache();
            return true;
        }
    }

    private void clearCubeCache() {
        Arrays.fill(this.lastCubePos, CubicChunks.SECTIONPOS_SENTINEL);
        Arrays.fill(this.lastCubeStatus, null);
        Arrays.fill(this.lastCube, null);
    }

    @Override
    @Nullable
    public BlockGetter getCubeForLighting(int sectionX, int sectionY, int sectionZ) {
        long cubePosAsLong = CubePos.of(Coords.sectionToCube(sectionX), Coords.sectionToCube(sectionY), Coords.sectionToCube(sectionZ)).asLong();
        ChunkHolder chunkholder = ((CubeMap) this.chunkMap).getVisibleCubeIfPresent(cubePosAsLong);
        if (chunkholder == null) {
            return null;
        } else {
            int j = CHUNK_STATUSES.size() - 1;

            while (true) {
                ChunkStatus chunkstatus = CHUNK_STATUSES.get(j);
                Optional<CubeAccess> optional = ((CubeHolder) chunkholder).getCubeFutureIfPresentUnchecked(chunkstatus).getNow(CubeHolder.MISSING_CUBE).left();
                if (optional.isPresent()) {
                    return optional.get();
                }
                if (chunkstatus == ChunkStatus.LIGHT.getParent()) {
                    return null;
                }
                --j;
            }
        }
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void initChunkMapForCC(ServerLevel serverLevel, LevelStorageSource.LevelStorageAccess levelStorageAccess, DataFixer dataFixer, StructureManager structureManager,
                                   Executor executor, ChunkGenerator chunkGenerator, int i, boolean bl, ChunkProgressListener chunkProgressListener,
                                   ChunkStatusUpdateListener chunkStatusUpdateListener, Supplier<DimensionDataStorage> supplier, CallbackInfo ci) {
        ((CubeMap) this.chunkMap).setServerChunkCache((ServerChunkCache) (Object) this);
    }

    /**
     * @author Barteks2x
     * @reason sections
     */
    @Inject(method = "blockChanged", at = @At("RETURN"))
    public void onBlockChanged(BlockPos pos, CallbackInfo ci) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }
        ChunkHolder chunkholder = ((CubeMap) this.chunkMap).getUpdatingCubeIfPresent(CubePos.from(pos).asLong());
        if (chunkholder != null) {
            // markBlockChanged
            chunkholder.blockChanged(new BlockPos(Coords.localX(pos), Coords.localY(pos), Coords.localZ(pos)));
        }
    }

    @Redirect(method = "tickChunks", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/NaturalSpawner;createState(ILjava/lang/Iterable;Lnet/minecraft/world/level/NaturalSpawner$ChunkGetter;)"
            + "Lnet/minecraft/world/level/NaturalSpawner$SpawnState;"))
    private NaturalSpawner.SpawnState cubicChunksSpawnState(int spawningChunkCount, Iterable<Entity> entities, NaturalSpawner.ChunkGetter chunkGetter) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return NaturalSpawner.createState(spawningChunkCount, entities, chunkGetter);
        }
        int naturalSpawnCountForColumns = ((CubicDistanceManager) this.distanceManager).getNaturalSpawnCubeCount()
            * CubeAccess.DIAMETER_IN_SECTIONS * CubeAccess.DIAMETER_IN_SECTIONS / (CubicNaturalSpawner.SPAWN_RADIUS * 2 / CubeAccess.DIAMETER_IN_BLOCKS + 1);

        return CubicNaturalSpawner.createState(naturalSpawnCountForColumns, entities, this::getFullCube);
    }

    @Inject(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap;getChunks()Ljava/lang/Iterable;"),
        locals = LocalCapture.CAPTURE_FAILHARD)
    private void tickCubes(CallbackInfo ci, long l, long timePassed, LevelData levelData, boolean bl, boolean doMobSpawning, int randomTicks, boolean bl3, int j,
                           NaturalSpawner.SpawnState cubeSpawnState) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }

        ((CubeMap) this.chunkMap).getCubes().forEach((cubeHolder) -> {
            Optional<LevelCube> optional =
                ((CubeHolder) cubeHolder).getCubeEntityTickingFuture().getNow(CubeHolder.UNLOADED_CUBE).left();
            if (optional.isPresent()) {
                LevelCube cube = optional.get();
                this.level.getProfiler().push("broadcast");
                ((CubeHolder) cubeHolder).broadcastChanges(cube);
                this.level.getProfiler().pop();

                if (!((CubeMap) this.chunkMap).noPlayersCloseForSpawning(cube.getCubePos())) {
                    // TODO probably want to make sure column-based inhabited time works too
                    cube.setCubeInhabitedTime(cube.getCubeInhabitedTime() + timePassed);

                    if (this.level.random.nextInt(((CubicNaturalSpawner.SPAWN_RADIUS / CubeAccess.DIAMETER_IN_BLOCKS) * 2) + 1) == 0) {
                        if (doMobSpawning && (this.spawnEnemies || this.spawnFriendlies) && this.level.getWorldBorder().isWithinBounds(cube.getCubePos().asChunkPos())) {
                            CubicNaturalSpawner.spawnForCube(this.level, cube, cubeSpawnState, this.spawnFriendlies, this.spawnEnemies, bl3);
                        }
                    }
                    ((CubicServerLevel) this.level).tickCube(cube, randomTicks);
                }
            }
        });
    }

    private void getFullCube(long pos, Consumer<ChunkAccess> chunkConsumer) {
        ChunkHolder chunkHolder = this.getVisibleCubeIfPresent(pos);
        if (chunkHolder != null) {
            CompletableFuture<Either<LevelCube, ChunkHolder.ChunkLoadingFailure>> o = unsafeCast((chunkHolder.getFullChunkFuture()));
            o.getNow(CubeHolder.UNLOADED_CUBE).left().ifPresent(chunkConsumer);
        }

    }

    @Override public boolean checkCubeFuture(long cubePosLong, Function<ChunkHolder, CompletableFuture<Either<LevelCube, ChunkHolder.ChunkLoadingFailure>>> futureFunction) {
        ChunkHolder chunkHolder = this.getVisibleCubeIfPresent(cubePosLong);
        if (chunkHolder == null) {
            return false;
        } else {
            return futureFunction.apply(chunkHolder).getNow(unsafeCast(ChunkHolder.UNLOADED_LEVEL_CHUNK)).left().isPresent();
        }
    }

    /**
     * @author Barteks2x
     * @reason debug string
     */
    @Inject(method = "gatherStats", at = @At("HEAD"), cancellable = true)
    public void gatherStatsForCubicChunks(CallbackInfoReturnable<String> cir) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }
        cir.setReturnValue("ServerChunkCache: " + this.getLoadedChunksCount() + " | " + ((CubeMap) chunkMap).sizeCubes());
    }

    @Inject(method = "isPositionTicking", at = @At("HEAD"), cancellable = true)
    private void isCubeAtPositionTicking(long pos, CallbackInfoReturnable<Boolean> cir) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }
        cir.setReturnValue(this.checkCubeFuture(pos, (chunkHolder) -> unsafeCast(chunkHolder.getTickingChunkFuture())));
    }

    @Nullable
    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "lambda$onLightUpdate$7(Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/LightLayer;)V", at = @At(value = "HEAD"), cancellable = true)
    private void onlyCubes(SectionPos pos, LightLayer type, CallbackInfo ci) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }
        ci.cancel();
        ChunkHolder chunkHolder = this.getVisibleCubeIfPresent(pos.asLong());
        if (chunkHolder != null) {
            chunkHolder.sectionLightChanged(type, Coords.sectionToIndex(pos.getX(), pos.getY(), pos.getZ()));
        }
    }
}