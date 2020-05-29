package cubicchunks.cc.chunk;

import com.mojang.datafixers.util.Either;
import cubicchunks.cc.chunk.cube.Cube;
import cubicchunks.cc.chunk.util.CubePos;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.server.ChunkHolder;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;

import javax.annotation.Nullable;

public interface IChunkManager {
    int getLoadedSectionsCount();

    ChunkHolder setCubeLevel(long sectionPosIn, int newLevel, @Nullable ChunkHolder holder, int oldLevel);

    LongSet getUnloadableCubes();

    ChunkHolder getCubeHolder(long sectionPosIn);
    ChunkHolder getImmutableCubeHolder(long sectionPosIn);

    CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> createCubeFuture(ChunkHolder chunkHolderIn,
            ChunkStatus chunkStatusIn);

    CompletableFuture<Either<Cube, ChunkHolder.IChunkLoadingError>> createSectionBorderFuture(ChunkHolder chunkHolder);


    CompletableFuture<Either<Cube, ChunkHolder.IChunkLoadingError>> createSectionTickingFuture(ChunkHolder chunkHolder);

    CompletableFuture<Either<List<ICube>, ChunkHolder.IChunkLoadingError>> createCubeRegionFuture(CubePos pos, int p_219236_2_,
            IntFunction<ChunkStatus> p_219236_3_);

    CompletableFuture<Void> saveCubeScheduleTicks(Cube sectionIn);

    CompletableFuture<Either<Cube, ChunkHolder.IChunkLoadingError>> createSectionEntityTickingFuture(CubePos pos);

    Iterable<ChunkHolder> getLoadedSectionsIterable();
}
