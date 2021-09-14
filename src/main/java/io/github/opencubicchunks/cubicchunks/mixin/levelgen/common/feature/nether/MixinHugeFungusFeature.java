package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.feature.nether;

import java.util.Random;

import io.github.opencubicchunks.cubicchunks.levelgen.CubeWorldGenRegion;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.HugeFungusConfiguration;
import net.minecraft.world.level.levelgen.feature.HugeFungusFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(HugeFungusFeature.class)
public class MixinHugeFungusFeature {

    @Redirect(method = "place", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/ChunkGenerator;getGenDepth()I"))
    private int useRegionBounds(ChunkGenerator chunkGenerator, FeaturePlaceContext<HugeFungusConfiguration> context) {
        if (!((CubicLevelHeightAccessor) context.level()).isCubic()) {
            return chunkGenerator.getGenDepth();
        }
        return Coords.cubeToMaxBlock(((CubeWorldGenRegion) context.level()).getMaxCubeY());
    }

    @Inject(method = "place", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/feature/HugeFungusConfiguration;planted:Z"), locals = LocalCapture.CAPTURE_FAILHARD,
        cancellable = true)
    private void cancelInLava(FeaturePlaceContext<?> context, CallbackInfoReturnable<Boolean> cir, WorldGenLevel worldGenLevel, BlockPos blockPos, Random random,
                              ChunkGenerator chunkGenerator, HugeFungusConfiguration hugeFungusConfiguration, Block block, BlockPos blockPos2, int i) {
        if (worldGenLevel.getFluidState(blockPos2).is(FluidTags.LAVA)) {
            cir.setReturnValue(false);
        }
    }
}
