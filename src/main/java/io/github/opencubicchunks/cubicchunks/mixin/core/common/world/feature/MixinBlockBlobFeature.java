package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.feature;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.BlockBlobFeature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//TODO: Configure this properly
@Mixin(BlockBlobFeature.class)
public class MixinBlockBlobFeature {

    private int storedY;

    @Inject(at = @At(value = "HEAD"), method = "place")
    private void storeMinCubeY(WorldGenLevel worldGenLevel, ChunkGenerator chunkGenerator, Random random, BlockPos blockPos, BlockStateConfiguration blockStateConfiguration,
                               CallbackInfoReturnable<Boolean> cir) {
        storedY = blockPos.getY();
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;isEmptyBlock(Lnet/minecraft/core/BlockPos;)Z", ordinal = 0), method = "place", cancellable = true)
    private void checkIfInCubeBounds(WorldGenLevel worldGenLevel, ChunkGenerator chunkGenerator, Random random, BlockPos blockPos, BlockStateConfiguration blockStateConfiguration,
                                     CallbackInfoReturnable<Boolean> cir) {
        if (blockPos.getY() < storedY) {
            cir.setReturnValue(true);
        }
    }
}
