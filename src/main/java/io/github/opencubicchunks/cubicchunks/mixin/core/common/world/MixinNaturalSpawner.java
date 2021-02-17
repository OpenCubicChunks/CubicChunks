package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import java.util.Objects;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.server.IServerChunkProvider;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import io.github.opencubicchunks.cubicchunks.world.CubicNaturalSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NaturalSpawner.class, priority = 0)// Assume absolute priority because of Y checks found here, we should always WANT to run first
public abstract class MixinNaturalSpawner {

    @Inject(method = "spawnForChunk", at = @At("HEAD"), cancellable = true)
    private static void cancelSpawnForChunk(ServerLevel serverLevel, LevelChunk levelChunk, NaturalSpawner.SpawnState spawnState, boolean bl,
                                            boolean bl2, boolean bl3, CallbackInfo ci) {
        if (!((CubicLevelHeightAccessor) serverLevel).isCubic()) {
            return;
        }

        ci.cancel(); // TODO: mob spawning
    }


    @Inject(method = "isSpawnPositionOk", at = @At(value = "HEAD"), cancellable = true)
    private static void isSpawnPositionOkForCubeWorldGenRegion(SpawnPlacements.Type location, LevelReader reader, BlockPos pos, @Nullable EntityType<?> entityType,
                                                               CallbackInfoReturnable<Boolean> cir) {
        if (!((CubicLevelHeightAccessor) reader).isCubic()) {
            return;
        }

        if (reader instanceof CubeWorldGenRegion) {
            CubeWorldGenRegion world = (CubeWorldGenRegion) reader;
            int lowestAllowedY = Coords.cubeToMinBlock(world.getMainCubeY());
            int highestAllowedY = Coords.cubeToMaxBlock(world.getMainCubeY());
            if (pos.getY() < lowestAllowedY + 1 || pos.getY() > highestAllowedY - 1) {
                cir.setReturnValue(false);
            }

        }
    }

    @Inject(method = "getTopNonCollidingPos", at = @At("HEAD"), cancellable = true)
    private static void returnOnBrokenPosition(LevelReader reader, EntityType<?> entityType, int x, int z, CallbackInfoReturnable<BlockPos> cir) {
        if (!((CubicLevelHeightAccessor) reader).isCubic()) {
            return;
        }

        if (reader instanceof CubeWorldGenRegion) {
            CubeWorldGenRegion world = (CubeWorldGenRegion) reader;
            BlockPos newPos = new BlockPos(x, world.getHeight(SpawnPlacements.getHeightmapType(entityType), x, z), z);
            int lowestAllowedY = Coords.cubeToMinBlock(world.getMainCubeY());
            int highestAllowedY = Coords.cubeToMaxBlock(world.getMainCubeY());

            if (newPos.getY() < lowestAllowedY + 1 || newPos.getY() > highestAllowedY - 1) {
                cir.setReturnValue(newPos);
            }
        }
    }

    @Dynamic
    @Redirect(method = "spawnCategoryForCube", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/NaturalSpawner;getRandomPosWithin(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/chunk/ChunkAccess;)Lnet/minecraft/core/BlockPos;"))
    private static BlockPos getRandomPosWithinCube(Level level, ChunkAccess chunkAccess) {
        CubePos pos = ((IBigCube) chunkAccess).getCubePos();
        int blockX = pos.minCubeX() + level.random.nextInt(IBigCube.DIAMETER_IN_BLOCKS);
        int blockZ = pos.minCubeZ() + level.random.nextInt(IBigCube.DIAMETER_IN_BLOCKS);

        int height = level.getHeight(Heightmap.Types.WORLD_SURFACE, blockX, blockZ) + 1; //This is wrong, we need to use the one from the BigCube(ChunkAccess)

        int minY = pos.minCubeY();
        if (minY > height) {
            return new BlockPos(blockX, Integer.MIN_VALUE, blockZ);
        }

        if (pos.maxCubeY() <= height) {
            int blockY = minY + level.random.nextInt(IBigCube.DIAMETER_IN_BLOCKS);
            return new BlockPos(blockX, blockY, blockZ);
        }

        return new BlockPos(blockX, Mth.randomBetweenInclusive(level.random, minY, height), blockZ);
    }


    @Inject(method = "isRightDistanceToPlayerAndSpawnPoint", at = @At(value = "NEW", target = "net/minecraft/world/level/ChunkPos"), cancellable = true)
    private static void useCubePos(ServerLevel world, ChunkAccess chunk, BlockPos.MutableBlockPos pos, double squaredDistance, CallbackInfoReturnable<Boolean> cir) {
        if (!((CubicLevelHeightAccessor) world).isCubic()) {
            return;
        }
        CubePos cubePos = CubePos.from(pos);

        cir.setReturnValue(Objects.equals(cubePos, ((BigCube) chunk).getCubePos()) || ((IServerChunkProvider) world.getChunkSource()).isEntityTickingCube(cubePos));
    }

    @Inject(method = "getRoughBiome", at = @At("HEAD"), cancellable = true)
    private static void getRoughCubicBiome(BlockPos pos, ChunkAccess chunk, CallbackInfoReturnable<Biome> cir) {
        if (!((CubicLevelHeightAccessor) chunk).isCubic() || !(chunk instanceof IBigCube)) {
            return;
        }

        cir.setReturnValue(CubicNaturalSpawner.getRoughBiomeForCube(pos, chunk));
    }
}
