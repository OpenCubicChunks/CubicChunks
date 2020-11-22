package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.feature;

import java.util.Random;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.IcebergFeature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IcebergFeature.class)
public class MixinIcebergFeature {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void checkIfSeaLevelIsInCube(WorldGenLevel worldGenLevel, ChunkGenerator chunkGenerator, Random random, BlockPos blockPos, BlockStateConfiguration blockStateConfiguration,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (blockPos.getY() > chunkGenerator.getSeaLevel() || blockPos.getY() + IBigCube.DIAMETER_IN_BLOCKS <= chunkGenerator.getSeaLevel()) {
            cir.setReturnValue(true);
        }
    }
}
