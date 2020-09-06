package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import com.mojang.datafixers.util.Either;
import io.github.opencubicchunks.cubicchunks.chunk.cube.CubePrimer;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.ServerWorldLightManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Mixin(targets = "net.minecraft.world.chunk.ChunkStatus$ISelectiveWorker")
public interface MixinISelectiveWorker {

    @Shadow void doWork(ServerWorld p_doWork_1_, ChunkGenerator p_doWork_2_, List<IChunk> p_doWork_3_, IChunk p_doWork_4_);

    /**
     * @author Batrteks2x
     * @reason inject is not supported, SectionPrimer check
     */
    @Overwrite
    default CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> doWork(
            ChunkStatus status, ServerWorld p_doWork_2_, ChunkGenerator p_doWork_3_,
            TemplateManager p_doWork_4_, ServerWorldLightManager p_doWork_5_,
            Function<IChunk, CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>>> p_doWork_6_,
            List<IChunk> p_doWork_7_, IChunk chunk) {

        if (!chunk.getStatus().isOrAfter(status)) {
            this.doWork(p_doWork_2_, p_doWork_3_, p_doWork_7_, chunk);
            if (chunk instanceof ChunkPrimer) {
                ((ChunkPrimer)chunk).setStatus(status);
            } else if (chunk instanceof CubePrimer) {
                ((CubePrimer) chunk).setCubeStatus(status);
            }
        }
        return CompletableFuture.completedFuture(Either.left(chunk));
    }
}