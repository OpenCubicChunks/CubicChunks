package cubicchunks.cc.chunk;

import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.server.ChunkHolder;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;

import javax.annotation.Nullable;

public interface IChunkManager {
    ChunkHolder setSectionLevel(long sectionPosIn, int newLevel, @Nullable ChunkHolder holder, int oldLevel);

    LongSet getUnloadableSections();

    ChunkHolder getSectionHolder(long sectionPosIn);

    CompletableFuture<Either<ISection, ChunkHolder.IChunkLoadingError>> createSectionFuture(ChunkHolder chunkHolderIn,
            ChunkStatus chunkStatusIn);

    CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> createBorderFuture(ChunkHolder chunkHolder);


    CompletableFuture<Either<ChunkSection, ChunkHolder.IChunkLoadingError>> createTickingFuture(ChunkHolder chunkHolder);

    CompletableFuture<Either<List<ISection>, ChunkHolder.IChunkLoadingError>> getSectionRegionFuture(ChunkPos pos, int p_219236_2_,
            IntFunction<ChunkStatus> p_219236_3_);

}
