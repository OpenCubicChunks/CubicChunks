package cubicchunks.cc.mixin.core.common;

import com.mojang.datafixers.util.Either;
import cubicchunks.cc.CubicChunks;
import cubicchunks.cc.chunk.IChunkManager;
import cubicchunks.cc.chunk.ICube;
import cubicchunks.cc.chunk.ICubeHolder;
import cubicchunks.cc.chunk.cube.Cube;
import cubicchunks.cc.chunk.graph.CCTicketType;
import cubicchunks.cc.chunk.ticket.ITicketManager;
import cubicchunks.cc.chunk.util.CubePos;
import cubicchunks.cc.mixin.core.common.chunk.interfaces.InvokeChunkManager;
import cubicchunks.cc.server.IServerChunkProvider;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.ChunkSection;
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
    @Shadow TicketManager ticketManager;
    @Shadow ChunkManager chunkManager;

    @Shadow @Final public ServerWorld world;

    @Shadow @Final private ServerChunkProvider.ChunkExecutor executor;

    @Shadow @Final private Thread mainThread;

    @Shadow protected abstract boolean func_217224_a(@Nullable ChunkHolder chunkHolderIn, int p_217224_2_);

    private final long[] recentPositions = new long[4];
    private final ChunkStatus[] recentStatuses = new ChunkStatus[4];
    private final ICube[] recentCubes = new ICube[4];

    @Override
    public <T> void registerTicket(TicketType<T> type, CubePos pos, int distance, T value) {
        ((ITicketManager) this.ticketManager).register(type, pos, distance, value);
    }

    @Override public int getLoadedSectionsCount() {
        return ((IChunkManager) chunkManager).getLoadedSectionsCount();
    }

    @Nullable
    public ICube getCube(int cubeX, int cubeY, int cubeZ, ChunkStatus requiredStatus, boolean load) {
        if (Thread.currentThread() != this.mainThread) {
            return CompletableFuture.supplyAsync(() -> {
                return this.getCube(cubeX, cubeY, cubeZ, requiredStatus, load);
            }, this.executor).join();
        } else {
            IProfiler iprofiler = this.world.getProfiler();
            iprofiler.func_230035_c_("getCube");
            long i = SectionPos.asLong(cubeX, cubeY, cubeZ);

            for(int j = 0; j < 4; ++j) {
                if (i == this.recentPositions[j] && requiredStatus == this.recentStatuses[j]) {
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
            long posAsLong = SectionPos.asLong(cubeX, cubeY, cubeZ);

            for(int j = 0; j < 4; ++j) {
                if (posAsLong == this.recentPositions[j] && this.recentStatuses[j] == ChunkStatus.FULL) {
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

    // func_217233_c
    private CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> getCubeFuture(int cubeX, int cubeY, int cubeZ,
            ChunkStatus requiredStatus, boolean load) {
        CubePos cubePos = CubePos.of(cubeX, cubeY, cubeZ);
        long i = cubePos.asLong();
        int j = 33 + ChunkStatus.getDistance(requiredStatus);
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

        return this.func_217224_a(chunkholder, j) ? ICubeHolder.MISSING_CUBE_FUTURE : ((ICubeHolder)chunkholder).createFuture(requiredStatus,
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
            this.recentPositions[i] = this.recentPositions[i - 1];
            this.recentStatuses[i] = this.recentStatuses[i - 1];
            this.recentCubes[i] = this.recentCubes[i - 1];
        }

        this.recentPositions[0] = newPositionIn;
        this.recentStatuses[0] = newStatusIn;
        this.recentCubes[0] = newCubeIn;
    }

    private boolean refreshAndInvalidate() {
        boolean flag = this.ticketManager.processUpdates(this.chunkManager);
        boolean flag1 = ((InvokeChunkManager)this.chunkManager).refreshOffThreadCacheSection();
        if (!flag && !flag1) {
            return false;
        } else {
            this.invalidateCaches();
            return true;
        }
    }

    private void invalidateCaches() {
        Arrays.fill(this.recentPositions, CubicChunks.SECTIONPOS_SENTINEL);
        Arrays.fill(this.recentStatuses, (Object)null);
        Arrays.fill(this.recentCubes, (Object)null);
    }

    /**
     * @author Barteks2x
     * @reason sections
     */
    @Overwrite
    public void markBlockChanged(BlockPos pos) {
        int x = pos.getX() >> 4;
        int y = pos.getY() >> 4;
        int z = pos.getZ() >> 4;
        ChunkHolder chunkholder = ((IChunkManager) this.chunkManager).getCubeHolder(SectionPos.asLong(x, y, z));
        if (chunkholder != null) {
            chunkholder.markBlockChanged(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
        }
    }

    @Inject(method = "tickChunks",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ChunkManager;getLoadedChunksIterable()Ljava/lang/Iterable;"))
    private void tickSections(CallbackInfo ci) {

        ((IChunkManager) this.chunkManager).getLoadedSectionsIterable().forEach((cubeHolder) -> {
            Optional<Cube> optional =
                    ((ICubeHolder) cubeHolder).getSectionEntityTickingFuture().getNow(ICubeHolder.UNLOADED_CUBE).left();
            if (optional.isPresent()) {
                Cube section = (Cube) optional.get();
                this.world.getProfiler().startSection("broadcast");
                ((ICubeHolder) cubeHolder).sendChanges(section);
                this.world.getProfiler().endSection();
            }
        });
    }
}
