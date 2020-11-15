package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.placement;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.NoneDecoratorConfiguration;
import net.minecraft.world.level.levelgen.placement.DarkOakTreePlacementDecorator;
import net.minecraft.world.level.levelgen.placement.DecorationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Mixin(DarkOakTreePlacementDecorator.class)
public abstract class MixinDarkOakTreePlacementDecorator {

    @Shadow protected abstract Heightmap.Types type(NoneDecoratorConfiguration noneDecoratorConfiguration);

    @Inject(at = @At("HEAD"), method = "getPositions(Lnet/minecraft/world/level/levelgen/placement/DecorationContext;Ljava/util/Random;Lnet/minecraft/world/level/levelgen/feature/configurations/NoneDecoratorConfiguration;Lnet/minecraft/core/BlockPos;)Ljava/util/stream/Stream;", cancellable = true)
    private void placeCubic(DecorationContext decorationContext, Random random, NoneDecoratorConfiguration noneDecoratorConfiguration, BlockPos blockPos, CallbackInfoReturnable<Stream<BlockPos>> cir) {
        CubicLevelHeightAccessor cubicLevelHeightAccessor = (CubicLevelHeightAccessor) decorationContext;
        if (!cubicLevelHeightAccessor.isCubicWorld())
            return;

        int blockMaxRange = IBigCube.DIAMETER_IN_BLOCKS / 4;
        cir.setReturnValue(IntStream.range(0, blockMaxRange * blockMaxRange).mapToObj((idx) -> {
            int dx = idx / blockMaxRange;
            int dz = idx % blockMaxRange;
            int blockX = dx * 4 + 1 + random.nextInt(3) + blockPos.getX();
            int blockZ = dz * 4 + 1 + random.nextInt(3) + blockPos.getZ();
            int heightMapY = decorationContext.getHeight(this.type(noneDecoratorConfiguration), blockX, blockZ);
            return new BlockPos(blockX, heightMapY, blockZ);
        }));
    }
}
