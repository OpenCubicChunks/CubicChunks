package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.feature;

import io.github.opencubicchunks.cubicchunks.levelgen.CubeWorldGenRegion;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.BlockBlobFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//TODO: Configure this properly
@Mixin(BlockBlobFeature.class)
public class MixinBlockBlobFeature {

    @Redirect(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;getMinBuildHeight()I"))
    private int useCubeMinY(WorldGenLevel worldGenLevel) {
        if (!((CubicLevelHeightAccessor) worldGenLevel).isCubic()) {
            return worldGenLevel.getMinBuildHeight();
        }
        return Coords.cubeToMinBlock(((CubeWorldGenRegion) worldGenLevel).getMainCubeY());

    }
}
