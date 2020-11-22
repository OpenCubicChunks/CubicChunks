package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.feature;

import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.SeagrassFeature;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Random;

import static io.github.opencubicchunks.cubicchunks.utils.Coords.blockToCube;

@Mixin(SeagrassFeature.class)
public class MixinSeagrassFeature {

    /**
     * CAPTURE_FAILHARD because it causes crash on loading into an existing world
     */
    @Inject(method = "place",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/world/level/WorldGenLevel;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"
        ),
        locals = LocalCapture.CAPTURE_FAILHARD,
        cancellable = true
    )
    private void checkGenRegionOnPlace(WorldGenLevel worldGenLevel, ChunkGenerator chunkGenerator, Random random, BlockPos blockPos, ProbabilityFeatureConfiguration probabilityFeatureConfiguration, CallbackInfoReturnable<Boolean> cir, boolean bl, int i, int j, int k) {
        if(worldGenLevel instanceof CubeWorldGenRegion) {
            if(!((CubeWorldGenRegion) worldGenLevel).cubeExists(blockToCube(blockPos.getX() + i), blockToCube(k), blockToCube(blockPos.getZ() + j)))
                cir.setReturnValue(false);
        }
    }
}
