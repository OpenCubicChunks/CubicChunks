package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.placement;

import io.github.opencubicchunks.cubicchunks.levelgen.CubeWorldGenRegion;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import net.minecraft.world.level.levelgen.placement.DecorationContext;
import net.minecraft.world.level.levelgen.placement.nether.CountMultiLayerDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CountMultiLayerDecorator.class)
public class MixinCountMultiLayerDecorator {

    @Redirect(method = "findOnGroundYPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/placement/DecorationContext;getMinBuildHeight()I"))
    private static int loopToBottomOfCube(DecorationContext decorationContext) {
        if (!((CubicLevelHeightAccessor) decorationContext.getLevel()).isCubic()) {
            return decorationContext.getMinBuildHeight();
        }

        return Coords.cubeToMinBlock(((CubeWorldGenRegion) decorationContext.getLevel()).getMainCubeY());
    }
}
