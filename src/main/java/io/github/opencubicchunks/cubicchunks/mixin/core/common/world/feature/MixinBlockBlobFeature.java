package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.feature;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.BlockBlobFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
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
    private void storeMinCubeY(FeaturePlaceContext<BlockStateConfiguration> featurePlaceContext, CallbackInfoReturnable<Boolean> cir) {
        storedY = featurePlaceContext.origin().getY();
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;isEmptyBlock(Lnet/minecraft/core/BlockPos;)Z", ordinal = 0), method = "place", cancellable = true)
    private void checkIfInCubeBounds(FeaturePlaceContext<BlockStateConfiguration> featurePlaceContext, CallbackInfoReturnable<Boolean> cir) {
        if (featurePlaceContext.origin().getY() < storedY) {
            cir.setReturnValue(true);
        }
    }
}
