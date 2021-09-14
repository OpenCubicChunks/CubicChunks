package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.feature;

import io.github.opencubicchunks.cubicchunks.levelgen.CubeWorldGenRegion;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.FossilFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FossilFeature.class)
public class MixinFossilFeature {

    @Redirect(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;getMaxBuildHeight()I"))
    private int useTopYMainCube(WorldGenLevel level) {
        if (!((CubicLevelHeightAccessor) level).isCubic()) {
            return level.getMinBuildHeight();
        }
        return Coords.cubeToMaxBlock(((CubeWorldGenRegion) level).getMainCubeY());
    }

    @Redirect(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;getMinBuildHeight()I"))
    private int useBottomYMainCube(WorldGenLevel level) {
        if (!((CubicLevelHeightAccessor) level).isCubic()) {
            return level.getMinBuildHeight();
        }
        return Coords.cubeToMinBlock(((CubeWorldGenRegion) level).getMainCubeY());
    }
}
