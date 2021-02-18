package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import java.util.Iterator;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import io.github.opencubicchunks.cubicchunks.world.CubicNaturalSpawner;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.PotentialCalculator;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = NaturalSpawner.class, priority = 0)// Assume absolute priority because of Y checks found here, we should always WANT to run first
public abstract class MixinNaturalSpawner {

    @Inject(method = "spawnForChunk", at = @At("HEAD"), cancellable = true)
    private static void cancelSpawnForChunk(ServerLevel serverLevel, LevelChunk levelChunk, NaturalSpawner.SpawnState spawnState, boolean bl,
                                            boolean bl2, boolean bl3, CallbackInfo ci) {
        if (!((CubicLevelHeightAccessor) serverLevel).isCubic()) {
            return;
        }

        ci.cancel();
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

    //Called from ASM
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


    @Inject(method = "isRightDistanceToPlayerAndSpawnPoint", at = @At("HEAD"), cancellable = true)
    private static void useCubePos(ServerLevel world, ChunkAccess chunk, BlockPos.MutableBlockPos pos, double squaredDistance, CallbackInfoReturnable<Boolean> cir) {
        if (!((CubicLevelHeightAccessor) world).isCubic()) {
            return;
        }

        cir.setReturnValue(CubicNaturalSpawner.isRightDistanceToPlayerAndSpawnPoint(world, chunk, pos, squaredDistance));
    }


    @Inject(method = "getRoughBiome", at = @At("HEAD"), cancellable = true)
    private static void getRoughCubicBiome(BlockPos pos, ChunkAccess chunk, CallbackInfoReturnable<Biome> cir) {
        if (!((CubicLevelHeightAccessor) chunk).isCubic() || !(chunk instanceof IBigCube)) {
            return;
        }

        cir.setReturnValue(CubicNaturalSpawner.getRoughBiomeForCube(pos, chunk));
    }

    private static ThreadLocal<BlockPos> capturedPos = new ThreadLocal<>();

    @Dynamic
    @Inject(method = "createCubicState", at = @At(value = "INVOKE", target = "Lio/github/opencubicchunks/cubicchunks/chunk/util/CubePos;asLong(II)J"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void createCubicState(int spawningChunkCount, Iterable<?> entities, CubicNaturalSpawner.CubeGetter chunkSource, CallbackInfoReturnable<NaturalSpawner.SpawnState> cir,
                                         PotentialCalculator potentialCalculator, Object2IntOpenHashMap<?> object2IntOpenHashMap, Iterator<?> var5, Entity entity, MobCategory mobCategory,
                                         BlockPos blockPos) {
        capturedPos.set(blockPos);
    }

    @Dynamic
    @Redirect(method = "createCubicState", at = @At(value = "INVOKE", target = "Lio/github/opencubicchunks/cubicchunks/chunk/util/CubePos;asLong(II)J"))
    private static long packCubePosLongNoSectionPos(int x, int z) {
        BlockPos pos = capturedPos.get();
        return CubePos.asLong(
            Coords.blockToCube(pos.getX()),
            Coords.blockToCube(pos.getY()),
            Coords.blockToCube(pos.getZ())
        );
    }

    // Mixin AP doesn't see mappings for the target because this method doesn't actually exist anywhere
    // so it can't remap the class names, so we have to provide redirects with both intermediary and mapped names
    // Group is used to ensure that one of them applies
    @Dynamic
    @Group(name = "isRightDistanceToPlayerAndSpawnPointForCube", min = 1, max = 1)
    @Redirect(method = "isRightDistanceToPlayerAndSpawnPointForCube", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkAccess;getPos()"
        + "Lio/github/opencubicchunks/cubicchunks/chunk/util/CubePos;", remap = false), require = 0, remap = false)
    private static CubePos useGetCubePos(ChunkAccess chunkAccess) {
        return ((IBigCube) chunkAccess).getCubePos();
    }

    @Dynamic
    @Group(name = "isRightDistanceToPlayerAndSpawnPointForCube", min = 1, max = 1)
    @Redirect(method = "isRightDistanceToPlayerAndSpawnPointForCube", at = @At(value = "INVOKE", target = "Lnet/minecraft/class_2791;method_12004()"
        + "Lio/github/opencubicchunks/cubicchunks/chunk/util/CubePos;", remap = false), require = 0, remap = false)
    private static CubePos useGetCubePosMapping(ChunkAccess chunkAccess) {
        return ((IBigCube) chunkAccess).getCubePos();
    }
}
