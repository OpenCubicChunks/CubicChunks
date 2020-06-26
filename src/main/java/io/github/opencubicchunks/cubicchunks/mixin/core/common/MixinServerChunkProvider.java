package io.github.opencubicchunks.cubicchunks.mixin.core.common;

import com.mojang.datafixers.util.Either;
import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.IChunkManager;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeHolder;
import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubeStatus;
import io.github.opencubicchunks.cubicchunks.chunk.graph.CCTicketType;
import io.github.opencubicchunks.cubicchunks.chunk.ticket.ITicketManager;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.ChunkManagerAccess;
import io.github.opencubicchunks.cubicchunks.server.IServerChunkProvider;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.lighting.ICubeLightProvider;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.server.TicketType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

@Mixin(ServerChunkProvider.class)
public abstract class MixinServerChunkProvider implements IServerChunkProvider, ICubeLightProvider {
    @Final @Shadow private TicketManager ticketManager;
    @Final @Shadow public ChunkManager chunkManager;

    @Shadow @Final public ServerWorld world;

    @Shadow @Final private ServerChunkProvider.ChunkExecutor executor;

    @Shadow @Final private Thread mainThread;

    @Shadow protected abstract boolean func_217224_a(@Nullable ChunkHolder chunkHolderIn, int p_217224_2_);

    @Shadow public abstract int getLoadedChunkCount();

    @Shadow @Final private static List<ChunkStatus> field_217239_c;
    private final long[] recentCubePositions = new long[4];
    private final ChunkStatus[] recentCubeStatuses = new ChunkStatus[4];
    private final IBigCube[] recentCubes = new IBigCube[4];

    @Override
    public <T> void registerTicket(TicketType<T> type, CubePos pos, int distance, T value) {
        ((ITicketManager) this.ticketManager).register(type, pos, distance, value);
    }

    public <T> void releaseTicket(TicketType<T> type, CubePos pos, int distance, T value)
    {
        ((ITicketManager) this.ticketManager).release(type, pos, distance, value);
    }

    @Override public int getCubeLoadCounter() {
        return ((IChunkManager) chunkManager).getCubeLoadCounter();
    }

    @Nullable
    @Override
    public IBigCube getCube(int cubeX, int cubeY, int cubeZ, ChunkStatus requiredStatus, boolean load) {
        if (Thread.currentThread() != this.mainThread) {
            return CompletableFuture.supplyAsync(() -> {
                return this.getCube(cubeX, cubeY, cubeZ, requiredStatus, load);
            }, this.executor).join();
        } else {
            IProfiler iprofiler = this.world.getProfiler();
            iprofiler.func_230035_c_("getCube");
            long i = CubePos.asLong(cubeX, cubeY, cubeZ);

            for(int j = 0; j < 4; ++j) {
                if (i == this.recentCubePositions[j] && requiredStatus == this.recentCubeStatuses[j]) {
                    IBigCube icube = this.recentCubes[j];
                    if (icube != null || !load) {
                        return icube;
                    }
                }
            }

            iprofiler.func_230035_c_("getChunkCacheMiss");
            CompletableFuture<Either<IBigCube, ChunkHolder.IChunkLoadingError>> completablefuture = this.getCubeFuture(cubeX, cubeY, cubeZ,
                    requiredStatus,
                    load);
            this.executor.driveUntil(completablefuture::isDone);
            IBigCube icube = completablefuture.join().map((p_222874_0_) -> {
                return p_222874_0_;
            }, (p_222870_1_) -> {
                if (load) {
                    throw Util.pauseDevMode(new IllegalStateException("Chunk not there when requested: " + p_222870_1_));
                } else {
                    return null;
                }
            });
            this.addRecents(i, icube, requiredStatus);
            return icube;
        }
    }

    @Nullable
    public BigCube getCubeNow(int cubeX, int cubeY, int cubeZ) {
        if (Thread.currentThread() != this.mainThread) {
            return null;
        } else {
            this.world.getProfiler().func_230035_c_("getChunkNow");
            long posAsLong = CubePos.asLong(cubeX, cubeY, cubeZ);

            for(int j = 0; j < 4; ++j) {
                if (posAsLong == this.recentCubePositions[j] && this.recentCubeStatuses[j] == ChunkStatus.FULL) {
                    IBigCube icube = this.recentCubes[j];
                    return icube instanceof BigCube ? (BigCube)icube : null;
                }
            }

            ChunkHolder chunkholder = this.getImmutableCubeHolder(posAsLong);
            if (chunkholder == null) {
                return null;
            } else {
                Either<IBigCube, ChunkHolder.IChunkLoadingError> either =
                        ((ICubeHolder)chunkholder).getFutureHigherThanCubeStatus(ChunkStatus.FULL).getNow(null);
                if (either == null) {
                    return null;
                } else {
                    IBigCube icube1 = either.left().orElse(null);
                    if (icube1 != null) {
                        this.addRecents(posAsLong, icube1, ChunkStatus.FULL);
                        if (icube1 instanceof BigCube) {
                            return (BigCube)icube1;
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
        ((ITicketManager)this.ticketManager).forceCube(pos, add);
    }

    // func_217233_c
    private CompletableFuture<Either<IBigCube, ChunkHolder.IChunkLoadingError>> getCubeFuture(int cubeX, int cubeY, int cubeZ,
                                                                                              ChunkStatus requiredStatus, boolean load) {
        CubePos cubePos = CubePos.of(cubeX, cubeY, cubeZ);
        long i = cubePos.asLong();
        int j = 33 + CubeStatus.getDistance(requiredStatus);
        ChunkHolder chunkholder = this.getImmutableCubeHolder(i);
        if (load) {
            ((ITicketManager)this.ticketManager).registerWithLevel(CCTicketType.CCUNKNOWN, cubePos, j, cubePos);
            if (this.func_217224_a(chunkholder, j)) {
                IProfiler iprofiler = this.world.getProfiler();
                iprofiler.startSection("chunkLoad");
                this.refreshAndInvalidate();
                chunkholder = this.getImmutableCubeHolder(i);
                iprofiler.endSection();
                if (this.func_217224_a(chunkholder, j)) {
                    throw Util.pauseDevMode(new IllegalStateException("No chunk holder after ticket has been added"));
                }
            }
        }

        return this.func_217224_a(chunkholder, j) ? ICubeHolder.MISSING_CUBE_FUTURE : ((ICubeHolder)chunkholder).createCubeFuture(requiredStatus,
                this.chunkManager);
    }

    //func_217213_a
    private ChunkHolder getImmutableCubeHolder(long cubePosIn)
    {
        return ((IChunkManager)this.chunkManager).getImmutableCubeHolder(cubePosIn);
    }

    // func_225315_a
    private void addRecents(long newPositionIn, IBigCube newCubeIn, ChunkStatus newStatusIn) {
        for(int i = 3; i > 0; --i) {
            this.recentCubePositions[i] = this.recentCubePositions[i - 1];
            this.recentCubeStatuses[i] = this.recentCubeStatuses[i - 1];
            this.recentCubes[i] = this.recentCubes[i - 1];
        }

        this.recentCubePositions[0] = newPositionIn;
        this.recentCubeStatuses[0] = newStatusIn;
        this.recentCubes[0] = newCubeIn;
    }

    @Inject(method = "func_217235_l", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerChunkProvider;invalidateCaches()V"))
    private void onRefeshAndInvalidate(CallbackInfoReturnable<Boolean> cir)
    {
        this.invalidateCubeCaches();
    }

    // func_217235_l
    private boolean refreshAndInvalidate() {
        boolean flag = this.ticketManager.processUpdates(this.chunkManager);
        boolean flag1 = ((ChunkManagerAccess)this.chunkManager).refreshOffThreadCacheSection();
        if (!flag && !flag1) {
            return false;
        } else {
            this.invalidateCubeCaches();
            return true;
        }
    }

    private void invalidateCubeCaches() {
        Arrays.fill(this.recentCubePositions, CubicChunks.SECTIONPOS_SENTINEL);
        Arrays.fill(this.recentCubeStatuses, (Object)null);
        Arrays.fill(this.recentCubes, (Object)null);
    }

    @Override
    @Nullable
    public IBlockReader getCubeForLight(int sectionX, int sectionY, int sectionZ) {
        long cubePosAsLong = CubePos.of(Coords.sectionToCube(sectionX), Coords.sectionToCube(sectionY), Coords.sectionToCube(sectionZ)).asLong();
        ChunkHolder chunkholder = ((IChunkManager)this.chunkManager).getImmutableCubeHolder(cubePosAsLong);
        if (chunkholder == null) {
            return null;
        } else {
            int j = field_217239_c.size() - 1;

            while(true) {
                ChunkStatus chunkstatus = field_217239_c.get(j);
                Optional<IBigCube> optional = ((ICubeHolder)chunkholder).getCubeFuture(chunkstatus).getNow(ICubeHolder.MISSING_CUBE).left();
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

    /**
     * @author Barteks2x
     * @reason sections
     */
    @Overwrite
    public void markBlockChanged(BlockPos pos) {
        ChunkHolder chunkholder = ((IChunkManager) this.chunkManager).getCubeHolder(CubePos.from(pos).asLong());
        if (chunkholder != null) {
            // markBlockChanged
            chunkholder.func_241819_a(Coords.localX(pos), Coords.localY(pos), Coords.localZ(pos));
        }
    }

    @Inject(method = "tickChunks",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ChunkManager;getLoadedChunksIterable()Ljava/lang/Iterable;"))
    private void tickSections(CallbackInfo ci) {

        ((IChunkManager) this.chunkManager).getLoadedCubeIterable().forEach((cubeHolder) -> {
            Optional<BigCube> optional =
                    ((ICubeHolder) cubeHolder).getCubeEntityTickingFuture().getNow(ICubeHolder.UNLOADED_CUBE).left();
            if (optional.isPresent()) {
                BigCube section = (BigCube) optional.get();
                this.world.getProfiler().startSection("broadcast");
                ((ICubeHolder) cubeHolder).sendChanges(section);
                this.world.getProfiler().endSection();
            }
        });
    }

    /**
     * @author Barteks2x
     * @reason debug string
     */
    @Overwrite
    public String makeString() {
        return "ServerChunkCache: " + this.getLoadedChunkCount() + " | " + ((IChunkManager) chunkManager).getLoadedCubeCount();
    }

}
