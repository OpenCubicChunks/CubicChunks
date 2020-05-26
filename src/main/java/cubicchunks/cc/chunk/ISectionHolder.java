package cubicchunks.cc.chunk;

import com.mojang.datafixers.util.Either;
import cubicchunks.cc.chunk.section.SectionPrimerWrapper;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;

public interface ISectionHolder {
    Either<ISection, ChunkHolder.IChunkLoadingError> MISSING_SECTION = Either.right(ChunkHolder.IChunkLoadingError.UNLOADED);
    CompletableFuture<Either<ISection, ChunkHolder.IChunkLoadingError>> MISSING_CHUNK_FUTURE = CompletableFuture.completedFuture(MISSING_SECTION);
    Either<ChunkSection, ChunkHolder.IChunkLoadingError> UNLOADED_SECTION = Either.right(ChunkHolder.IChunkLoadingError.UNLOADED);
    CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> UNLOADED_SECTION_FUTURE = CompletableFuture.completedFuture(UNLOADED_SECTION);

    ChunkSection getSectionIfComplete();

    void setYPos(int yPos);
    SectionPos getSectionPos();
    int getYPos();

    void chainSection(CompletableFuture<? extends Either<? extends ISection,
            ChunkHolder.IChunkLoadingError>> eitherChunk);

    // func_219276_a
    CompletableFuture<Either<ISection, ChunkHolder.IChunkLoadingError>> createSectionFuture(ChunkStatus chunkStatus, ChunkManager chunkManager);

    CompletableFuture<Either<ISection, ChunkHolder.IChunkLoadingError>> getSectionFuture(ChunkStatus chunkStatus);

    CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> getSectionEntityTickingFuture();

    // func_219294_a
    void onSectionWrapperCreated(SectionPrimerWrapper primer);
}
