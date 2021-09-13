package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.feature;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.IcebergFeature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(IcebergFeature.class)
public class MixinIcebergFeature {

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void checkIfSeaLevelIsInCube(FeaturePlaceContext<BlockStateConfiguration> featurePlaceContext, CallbackInfoReturnable<Boolean> cir) {
        if (!((CubicLevelHeightAccessor) featurePlaceContext.level()).isCubic()) {
            return;
        }

        if (featurePlaceContext.origin().getY() > featurePlaceContext.chunkGenerator().getSeaLevel()
            || featurePlaceContext.origin().getY() + IBigCube.DIAMETER_IN_BLOCKS <= featurePlaceContext.chunkGenerator().getSeaLevel()) {
            cir.setReturnValue(true);
        }
    }
}
