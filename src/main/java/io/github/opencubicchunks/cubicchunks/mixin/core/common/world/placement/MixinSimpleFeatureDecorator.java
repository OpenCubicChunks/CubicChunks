package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.placement;


import java.util.Random;
import java.util.stream.Stream;

import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.world.placement.CubicSimpleFeatureDecorator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.configurations.DecoratorConfiguration;
import net.minecraft.world.level.levelgen.placement.DecorationContext;
import net.minecraft.world.level.levelgen.placement.SimpleFeatureDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleFeatureDecorator.class)
public abstract class MixinSimpleFeatureDecorator<DC extends DecoratorConfiguration> implements CubicSimpleFeatureDecorator<DC> {

    @Shadow protected abstract Stream<BlockPos> place(Random random, DC decoratorConfiguration, BlockPos blockPos);

    @Inject(at = @At("HEAD"), method = "getPositions", cancellable = true)
    private void getCubicPositions(DecorationContext decorationContext, Random random, DC decoratorConfiguration, BlockPos blockPos, CallbackInfoReturnable<Stream<BlockPos>> cir) {
        if (!((CubicLevelHeightAccessor) decorationContext).isCubicWorld()) {
            return;
        }

        cir.setReturnValue(placeCubic(random, decoratorConfiguration, blockPos));
    }

    @Override
    public Stream<BlockPos> placeCubic(Random rand, DC config, BlockPos minCubePos) {
        return place(rand, config, minCubePos);
    }
}
