package cubicchunks.cc.chunk;

import com.mojang.datafixers.util.Either;
import cubicchunks.cc.chunk.cube.CubePrimerWrapper;
import cubicchunks.cc.chunk.cube.Cube;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;

import java.util.concurrent.CompletableFuture;

public interface ICubeHolder {
    Either<ICube, ChunkHolder.IChunkLoadingError> MISSING_SECTION = Either.right(ChunkHolder.IChunkLoadingError.UNLOADED);
    CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> MISSING_CHUNK_FUTURE = CompletableFuture.completedFuture(MISSING_SECTION);
    Either<ChunkSection, ChunkHolder.IChunkLoadingError> UNLOADED_SECTION = Either.right(ChunkHolder.IChunkLoadingError.UNLOADED);
    CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> UNLOADED_SECTION_FUTURE = CompletableFuture.completedFuture(UNLOADED_SECTION);

    ChunkSection getSectionIfComplete();

    void setYPos(int yPos);
    SectionPos getSectionPos();
    int getYPos();

    void chainSection(CompletableFuture<? extends Either<? extends ICube,
            ChunkHolder.IChunkLoadingError>> eitherChunk);

    // func_219276_a
    CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> createSectionFuture(ChunkStatus chunkStatus, ChunkManager chunkManager);

    CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> getSectionFuture(ChunkStatus chunkStatus);

    CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> getSectionEntityTickingFuture();

    // func_219294_a
    void onSectionWrapperCreated(CubePrimerWrapper primer);

    // func_225410_b
    CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> getFutureHigherThanCubeStatus(ChunkStatus chunkStatus);

    CompletableFuture<Either<ICube, ChunkHolder.IChunkLoadingError>> createFuture(ChunkStatus p_219276_1_, ChunkManager p_219276_2_);

    void sendChanges(Cube cube);

    CompletableFuture<ICube> getCurrentCubeFuture();
}
