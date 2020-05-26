package cubicchunks.cc.chunk;

import com.mojang.datafixers.util.Either;
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
    ChunkSection getSectionIfComplete();

    void setYPos(int yPos);
    SectionPos getSectionPos();
    int getYPos();

    void chainSection(CompletableFuture<? extends Either<? extends ISection,
            ChunkHolder.IChunkLoadingError>> eitherChunk);

    // func_219276_a
    CompletableFuture<Either<ISection, ChunkHolder.IChunkLoadingError>> createFuture(ChunkStatus chunkStatus, ChunkManager chunkManager);

    CompletableFuture<Either<ISection, ChunkHolder.IChunkLoadingError>> getSectionFuture(ChunkStatus chunkStatus);
}
