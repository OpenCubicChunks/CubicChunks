package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
}
