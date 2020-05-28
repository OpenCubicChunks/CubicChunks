package cubicchunks.cc.mixin.core.common.chunk;

import com.mojang.datafixers.util.Either;
import cubicchunks.cc.chunk.ICube;
import cubicchunks.cc.chunk.cube.CubePrimer;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.template.TemplateManager;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.ServerWorldLightManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Mixin(ChunkStatus.class)
public class MixinChunkStatus {
    // lambda$static$0 == NOOP_LOADING_WORKER
    // lambda$static$2 == STRUCTURE_STARTS
    // lambda$static$9 == FEATURES

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "lambda$static$0", at = @At("HEAD"))
    private static void noopLoadingWorker(
            ChunkStatus status, ServerWorld world, TemplateManager templateManager,
            ServerWorldLightManager lightManager,
            Function<IChunk, CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>>> func,
            IChunk chunk,
            CallbackInfoReturnable<CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>>> ci) {
        if (chunk instanceof CubePrimer && !chunk.getStatus().isAtLeast(status)) {
            ((CubePrimer) chunk).setStatus(status);
        }
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "lambda$static$2", at = @At("HEAD"), cancellable = true)
    private static void generateStructureStatus(
            ChunkStatus status, ServerWorld world, ChunkGenerator<?> generator,
            TemplateManager templateManager, ServerWorldLightManager lightManager,
            Function<IChunk, CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>>> func,
            List<IChunk> chunks, IChunk chunk,
            CallbackInfoReturnable<CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>>> cir) {

        cir.setReturnValue(CompletableFuture.completedFuture(Either.left(chunk)));
        if(!(chunk instanceof ICube))
        {
            //vanilla
            return;
        }
        //cc
        if (!((ICube) chunk).getCubeStatus().isAtLeast(status)) {
            if (world.getWorldInfo().isMapFeaturesEnabled()) {
                generator.generateStructures(world.getBiomeManager().copyWithProvider(generator.getBiomeProvider()), chunk, generator, templateManager);
            }

            if (chunk instanceof CubePrimer) {
                ((CubePrimer)chunk).setStatus(status);
            }
        }
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "lambda$static$9", at = @At(value = "HEAD"), cancellable = true)
    private static void featuresSetStatus(
            ChunkStatus status, ServerWorld world, ChunkGenerator<?> generator,
            TemplateManager templateManager, ServerWorldLightManager lightManager,
            Function<IChunk, CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>>> func,
            List<IChunk> chunks, IChunk chunk,
            CallbackInfoReturnable<CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>>> cir) {

        if (!(chunk instanceof CubePrimer)) {
            return;
        }
        CubePrimer chunkprimer = (CubePrimer) chunk;
        // chunkprimer.setLightManager(lightManager);
        if (!chunk.getStatus().isAtLeast(status)) {
            //Heightmap.updateChunkHeightmaps(chunk, EnumSet
            //        .of(Heightmap.Type.MOTION_BLOCKING, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, Heightmap.Type.OCEAN_FLOOR,
            //        Heightmap.Type.WORLD_SURFACE));
            // TODO worldgen
            // generator.decorate(new WorldGenRegion(world, chunks));
            chunkprimer.setStatus(status);
        }
        cir.setReturnValue(CompletableFuture.completedFuture(Either.left(chunk)));
    }

    @Inject(method = "lightChunk", at = @At("HEAD"), cancellable = true)
    private static void lightChunkCC(ChunkStatus status, ServerWorldLightManager lightManager, IChunk chunk,
            CallbackInfoReturnable<CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>>> cir) {
        if (!(chunk instanceof CubePrimer)) {
            return;
        }
        if (!chunk.getStatus().isAtLeast(status)) {
            ((CubePrimer) chunk).setStatus(status);
        }
        cir.setReturnValue(CompletableFuture.completedFuture(Either.left(chunk)));
    }


}
