package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.placement;

import java.util.Random;
import java.util.stream.Stream;

import io.github.opencubicchunks.cubicchunks.levelgen.CubeWorldGenRegion;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.DecorationContextAccess;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.configurations.DecoratorConfiguration;
import net.minecraft.world.level.levelgen.placement.DecorationContext;
import net.minecraft.world.level.levelgen.placement.VerticalDecorator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//TODO: Actually do correct calculations.
@Mixin(VerticalDecorator.class)
public abstract class MixinVerticalDecorator<DC extends DecoratorConfiguration> {

    @Shadow protected abstract int y(DecorationContext decorationContext, Random random, DC decoratorConfiguration, int i);

    @Inject(method = "getPositions", at = @At("HEAD"), cancellable = true)
    private void fitToCubeRange(DecorationContext context, Random random, DC config, BlockPos pos, CallbackInfoReturnable<Stream<BlockPos>> cir) {
        if (!((CubicLevelHeightAccessor) context).isCubic()) {
            return;
        }

        int y = this.y(context, random, config, pos.getY());

        if (y == Integer.MIN_VALUE) {
            cir.setReturnValue(Stream.empty());
            return;
        }

        CubeWorldGenRegion cubeWorldGenRegion = (CubeWorldGenRegion) ((DecorationContextAccess) context).getLevel();

        if (!cubeWorldGenRegion.insideCubeHeight(y)) {
            cir.setReturnValue(Stream.empty());
        } else {
            cir.setReturnValue(Stream.of(new BlockPos(pos.getX(), y, pos.getZ())));
        }
    }
}
