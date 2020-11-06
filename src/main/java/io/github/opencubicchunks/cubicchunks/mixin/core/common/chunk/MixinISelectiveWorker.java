package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import com.mojang.datafixers.util.Either;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

@Mixin(targets = "net.minecraft.world.level.chunk.ChunkStatus$SimpleGenerationTask")
public interface MixinISelectiveWorker {

    @Shadow void doWork(ServerLevel p_doWork_1_, ChunkGenerator p_doWork_2_, List<ChunkAccess> p_doWork_3_, ChunkAccess p_doWork_4_);

    /**
     * @author Batrteks2x
     * @reason inject is not supported, SectionPrimer check
     */
    @Overwrite
    default CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> doWork(
            ChunkStatus status, ServerLevel p_doWork_2_, ChunkGenerator p_doWork_3_,
            StructureManager p_doWork_4_, ThreadedLevelLightEngine p_doWork_5_,
            Function<ChunkAccess, CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>>> p_doWork_6_,
            List<ChunkAccess> p_doWork_7_, ChunkAccess chunk) {

        if (!chunk.getStatus().isOrAfter(status)) {
            this.doWork(p_doWork_2_, p_doWork_3_, p_doWork_7_, chunk);
            if (chunk instanceof ProtoChunk) {
                ((ProtoChunk)chunk).setStatus(status);
            } else if (chunk instanceof CubePrimer) {
                ((CubePrimer) chunk).setCubeStatus(status);
            }
        }
        return CompletableFuture.completedFuture(Either.left(chunk));
    }
}