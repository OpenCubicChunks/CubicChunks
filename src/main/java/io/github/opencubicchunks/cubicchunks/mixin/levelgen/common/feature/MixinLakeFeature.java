package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.feature;

import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LakeFeature.class)
public class MixinLakeFeature {


    @Redirect(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;getMinBuildHeight()I", ordinal = 0))
    private int cubicLakeFeature(WorldGenLevel worldGenLevel, FeaturePlaceContext<BlockStateConfiguration> featurePlaceContext) {
        if (!((CubicLevelHeightAccessor) featurePlaceContext.level()).isCubic()) {
            return featurePlaceContext.level().getMinBuildHeight();
        }

        return featurePlaceContext.origin().getY();
    }
}
