package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.placement;

import java.util.OptionalInt;
import java.util.Random;

import io.github.opencubicchunks.cubicchunks.levelgen.CubeWorldGenRegion;
import io.github.opencubicchunks.cubicchunks.levelgen.placement.CubicHeightProvider;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.world.level.levelgen.feature.configurations.RangeDecoratorConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.placement.DecorationContext;
import net.minecraft.world.level.levelgen.placement.RangeDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RangeDecorator.class)
public class MixinRangeDecorator {

    @Inject(method = "y", at = @At("RETURN"), cancellable = true)
    private void handleCubicRangeDecorator(DecorationContext decorationContext, Random random, RangeDecoratorConfiguration rangeDecoratorConfiguration, int i,
                                     CallbackInfoReturnable<Integer> cir) {
        if (!((CubicLevelHeightAccessor) decorationContext.getLevel()).isCubic()) {
            return;
        }

        CubeWorldGenRegion level = (CubeWorldGenRegion) decorationContext.getLevel();
        HeightProvider heightProvider = rangeDecoratorConfiguration.height;

        if (heightProvider instanceof CubicHeightProvider) {
            OptionalInt optionalInt = ((CubicHeightProvider) heightProvider).sampleCubic(random, decorationContext,
                Coords.cubeToMinBlock(level.getMainCubeY()), Coords.cubeToMaxBlock(level.getMainCubeY()));
            if (optionalInt.isPresent()) {
                cir.setReturnValue(optionalInt.getAsInt());
            } else {
                cir.setReturnValue(Integer.MIN_VALUE);
            }
        } else {
            if (!level.insideCubeHeight(cir.getReturnValue())) {
                cir.setReturnValue(Coords.cubeToMinBlock(level.getMainCubeY()));
            }
        }
    }
}
