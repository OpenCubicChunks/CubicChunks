package io.github.opencubicchunks.cubicchunks.mixin.core.common;

import com.mojang.datafixers.util.Either;
import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.IChunkManager;
import io.github.opencubicchunks.cubicchunks.chunk.ICube;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeHolder;
import io.github.opencubicchunks.cubicchunks.chunk.cube.Cube;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubeStatus;
import io.github.opencubicchunks.cubicchunks.chunk.graph.CCTicketType;
import io.github.opencubicchunks.cubicchunks.chunk.ticket.ITicketManager;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.ChunkManagerAccess;
import io.github.opencubicchunks.cubicchunks.server.IServerChunkProvider;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
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

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

@Mixin(ServerChunkProvider.class)
public abstract class MixinServerChunkProvider implements IServerChunkProvider {
    @Final @Shadow private TicketManager ticketManager;
    @Final @Shadow public ChunkManager chunkManager;

    @Shadow @Final public ServerWorld world;

    @Shadow @Final private ServerChunkProvider.ChunkExecutor executor;

    @Shadow @Final private Thread mainThread;

    @Shadow protected abstract boolean func_217224_a(@Nullable ChunkHolder chunkHolderIn, int p_217224_2_);

    @Shadow public abstract int getLoadedChunkCount();

    private final long[] recentCubePositions = new long[4];
    private final ChunkStatus[] recentCubeStatuses = new ChunkStatus[4];
    private final ICube[] recentCubes = new ICube[4];

    @Override
    public <T> void registerTicket(TicketType<T> type, CubePos pos, int distance, T value) {
        ((ITicketManager) this.ticketManager).register(type, pos, distance, value);
    }

    public <T> void releaseTicket(TicketType<T> type, CubePos pos, int distance, T value)
    {
        ((ITicketManager) this.ticketManager).release(type, pos, distance, value);
    }

    @Override public int getLoadedCubesCount() {
        return ((IChunkManager) chunkManager).getLoadedCubesCount();
    }

    @Nullable
    @Override
    public ICube getCube(int cubeX, int cubeY, int cubeZ, ChunkStatus requiredStatus, boolean load) {
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
                    ICube icube = this.recentCubes[j];
                    if (icube != null || !load) {
                        return icube;
                    }
                }
            }

            iprofiler.func_230035_c_("getChunkCacheMiss");
            CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> completablefuture = this.getCubeFuture(cubeX, cubeY, cubeZ,
                    requiredStatus,
                    load);
            this.executor.driveUntil(completablefuture::isDone);
            ICube icube = completablefuture.join().map((p_222874_0_) -> {
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
    public Cube getCubeNow(int cubeX, int cubeY, int cubeZ) {
        if (Thread.currentThread() != this.mainThread) {
            return null;
        } else {
            this.world.getProfiler().func_230035_c_("getChunkNow");
            long posAsLong = CubePos.asLong(cubeX, cubeY, cubeZ);

            for(int j = 0; j < 4; ++j) {
                if (posAsLong == this.recentCubePositions[j] && this.recentCubeStatuses[j] == ChunkStatus.FULL) {
                    ICube icube = this.recentCubes[j];
                    return icube instanceof Cube ? (Cube)icube : null;
                }
            }

            ChunkHolder chunkholder = this.getImmutableCubeHolder(posAsLong);
            if (chunkholder == null) {
                return null;
            } else {
                Either<ICube, ChunkHolder.IChunkLoadingError> either =
                        ((ICubeHolder)chunkholder).getFutureHigherThanCubeStatus(ChunkStatus.FULL).getNow(null);
                if (either == null) {
                    return null;
                } else {
                    ICube icube1 = either.left().orElse(null);
                    if (icube1 != null) {
                        this.addRecents(posAsLong, icube1, ChunkStatus.FULL);
                        if (icube1 instanceof Cube) {
                            return (Cube)icube1;
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
    private CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> getCubeFuture(int cubeX, int cubeY, int cubeZ,
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
    private void addRecents(long newPositionIn, ICube newCubeIn, ChunkStatus newStatusIn) {
        for(int i = 3; i > 0; --i) {
            this.recentCubePositions[i] = this.recentCubePositions[i - 1];
            this.recentCubeStatuses[i] = this.recentCubeStatuses[i - 1];
            this.recentCubes[i] = this.recentCubes[i - 1];
        }

        this.recentCubePositions[0] = newPositionIn;
        this.recentCubeStatuses[0] = newStatusIn;
        this.recentCubes[0] = newCubeIn;
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

    /**
     * @author Barteks2x
     * @reason sections
     */
    @Overwrite
    public void markBlockChanged(BlockPos pos) {
        ChunkHolder chunkholder = ((IChunkManager) this.chunkManager).getCubeHolder(CubePos.from(pos).asLong());
        if (chunkholder != null) {
            chunkholder.markBlockChanged(Coords.localX(pos), Coords.localY(pos), Coords.localZ(pos));
        }
    }

    @Inject(method = "tickChunks",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ChunkManager;getLoadedChunksIterable()Ljava/lang/Iterable;"))
    private void tickSections(CallbackInfo ci) {

        ((IChunkManager) this.chunkManager).getLoadedCubeIterable().forEach((cubeHolder) -> {
            Optional<Cube> optional =
                    ((ICubeHolder) cubeHolder).getCubeEntityTickingFuture().getNow(ICubeHolder.UNLOADED_CUBE).left();
            if (optional.isPresent()) {
                Cube section = (Cube) optional.get();
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
        return "ServerChunkCache: " + this.getLoadedChunkCount() + " | " + this.getLoadedCubesCount();
    }

}
