package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.feature.range;

import java.util.Random;

import io.github.opencubicchunks.cubicchunks.levelgen.CubeWorldGenRegion;
import io.github.opencubicchunks.cubicchunks.levelgen.util.BlockPosHeightMapDoubleMarker;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.RandomPatchFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

//TODO: Either this feature's decorator(HeightmapDoubleDecorator) is wrong on cube edges or this feature itself.
@Mixin(RandomPatchFeature.class)
public class MixinRandomPatchFeature {

    @Inject(method = "place", at = @At(value = "INVOKE_ASSIGN",
        target = "Lnet/minecraft/world/level/WorldGenLevel;getHeightmapPos(Lnet/minecraft/world/level/levelgen/Heightmap$Types;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;",
        ordinal = 0, shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void cancelPlacement(FeaturePlaceContext<RandomPatchConfiguration> context, CallbackInfoReturnable<Boolean> cir, RandomPatchConfiguration randomPatchConfiguration,
                                 Random random, BlockPos blockPos, WorldGenLevel worldGenLevel, BlockState blockState, BlockPos blockPos2) {
        if (!((CubicLevelHeightAccessor) worldGenLevel).isCubic()) {
            return;
        }
        if (blockPos instanceof BlockPosHeightMapDoubleMarker) {
            if (random.nextInt(2) == 0) {
                cir.setReturnValue(false);
                return;
            }
        }
        CubeWorldGenRegion cubeWorldGenRegion = (CubeWorldGenRegion) worldGenLevel;
        if (!cubeWorldGenRegion.insideCubeHeight(blockPos2.getY())) {
            cir.setReturnValue(false);
        }
    }
}
