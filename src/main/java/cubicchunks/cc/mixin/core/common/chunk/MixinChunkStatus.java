package cubicchunks.cc.mixin.core.common.chunk;

import com.mojang.datafixers.util.Either;
import cubicchunks.cc.chunk.ICube;
import cubicchunks.cc.chunk.cube.CubePrimer;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Mixin(ChunkStatus.class)
public class MixinChunkStatus {
    // lambda$static$0 == NOOP_LOADING_WORKER
    // lambda$static$1 == EMPTY
    // lambda$static$2 == STRUCTURE_STARTS
    // lambda$static$3 == STRUCTURE_REFERENCES
    // lambda$static$4 == BIOMES
    // lambda$static$5 == NOISE
    // lambda$static$6 == SURFACE
    // lambda$static$7 == CARVERS
    // lambda$static$8 == LIQUID_CARVERS
    // lambda$static$9 == FEATURES
    // lambda$static$10 == LIGHT
    // lambda$static$11 == LIGHT (loading worker)
    // lambda$static$12 == SPAWM
    // lambda$static$13 == HEIGHTMAPS
    // lambda$static$14 == FULL
    // lambda$static$15 == FULL (loading worker)

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

    // EMPTY -> does nothing already

    // structure starts - replace setStatus, handled by MixinChunkGenerator
    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "lambda$static$2", at = @At("HEAD"), cancellable = true)
    private static void generateStructureStatus(
            ChunkStatus status, ServerWorld world, ChunkGenerator<?> generator,
            TemplateManager templateManager, ServerWorldLightManager lightManager,
            Function<IChunk, CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>>> func,
            List<IChunk> chunks, IChunk chunk,
            CallbackInfoReturnable<CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>>> cir) {

        cir.setReturnValue(CompletableFuture.completedFuture(Either.left(chunk)));
        if (!(chunk instanceof ICube)) {
            //vanilla
            return;
        }
        //cc
        if (!((ICube) chunk).getCubeStatus().isAtLeast(status)) {
            if (world.getWorldInfo().isMapFeaturesEnabled()) {
                generator.generateStructures(world.getBiomeManager().copyWithProvider(generator.getBiomeProvider()), chunk, generator,
                        templateManager);
            }

            if (chunk instanceof CubePrimer) {
                ((CubePrimer) chunk).setStatus(status);
            }
        }
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "lambda$static$3", at = @At("HEAD"), cancellable = true)
    private static void cubicChunksStructureReferences(ServerWorld world, ChunkGenerator<?> generator, List<IChunk> neighbors, IChunk chunk,
            CallbackInfo ci) {

        ci.cancel();
        if (chunk instanceof ICube) {
            // generator.generateStructureStarts(new CubeWorldGenRegion(world, unsafeCast(neighbors)), chunk);
        }
    }
    // biomes -> handled by MixinChunkGenerator
    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "lambda$static$5", at = @At("HEAD"), cancellable = true)
    private static void cubicChunksNoise(ServerWorld world, ChunkGenerator<?> generator, List<IChunk> neighbors, IChunk chunk,
            CallbackInfo ci) {

        ci.cancel();
        if (chunk instanceof ICube) {
            // generator.makeBase(new CubeWorldGenRegion(world, unsafeCast(neighbors)), chunk);
            ICube cube = (ICube) chunk;
            int cubeY = cube.getCubePos().getY();
            if (cubeY < 5) {
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        for (int z = 0; z < 16; z++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            if (cubeY == 4 && y == 15) {
                                cube.setBlockState(pos, Blocks.GRASS.getDefaultState(), false);
                            } else if (cubeY == 0 && y == 0) {
                                cube.setBlockState(pos, Blocks.BEDROCK.getDefaultState(), false);
                            } else {
                                cube.setBlockState(pos, Blocks.STONE.getDefaultState(), false);
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "lambda$static$6", at = @At("HEAD"), cancellable = true)
    private static void cubicChunksSurface(ServerWorld world, ChunkGenerator<?> generator, List<IChunk> neighbors, IChunk chunk,
            CallbackInfo ci) {

        ci.cancel();
        if (chunk instanceof ICube) {
            // generator.generateSurface(new CubeWorldGenRegion(world, unsafeCast(neighbors)), chunk);
        }
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "lambda$static$7", at = @At("HEAD"), cancellable = true)
    private static void cubicChunksCarvers(ServerWorld world, ChunkGenerator<?> generator, List<IChunk> neighbors, IChunk chunk,
            CallbackInfo ci) {

        ci.cancel();
        if (chunk instanceof ICube) {
            // generator.func_225550_a_(world.getBiomeManager().copyWithProvider(generator.getBiomeProvider()), chunk, GenerationStage.Carving.AIR);
        }
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "lambda$static$8", at = @At("HEAD"), cancellable = true)
    private static void cubicChunksLiquidCarvers(ServerWorld world, ChunkGenerator<?> generator, List<IChunk> neighbors, IChunk chunk,
            CallbackInfo ci) {

        ci.cancel();
        if (chunk instanceof ICube) {
            // generator.func_225550_a_(world.getBiomeManager().copyWithProvider(generator.getBiomeProvider()), chunk, GenerationStage.Carving.LIQUID);
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

    //lambda$static$12
    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "lambda$static$12", at = @At("HEAD"), cancellable = true)
    private static void cubicChunksSpawnMobs(ServerWorld world, ChunkGenerator<?> generator, List<IChunk> neighbors, IChunk chunk,
            CallbackInfo ci) {

        ci.cancel();
        if (chunk instanceof ICube) {
            // generator.spawnMobs(new CubeWorldGenRegion(world, unsafeCast(neighbors)));
        }
    }
}
