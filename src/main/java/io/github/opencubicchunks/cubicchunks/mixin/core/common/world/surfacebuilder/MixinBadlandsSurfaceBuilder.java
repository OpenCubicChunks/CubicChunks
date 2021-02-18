package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.surfacebuilder;

import java.util.Random;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.surfacebuilders.BadlandsSurfaceBuilder;
import net.minecraft.world.level.levelgen.surfacebuilders.SurfaceBuilderBaseConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = BadlandsSurfaceBuilder.class)
public class MixinBadlandsSurfaceBuilder {

    @ModifyConstant(method = "apply", constant = @Constant(expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO))
    private int doNotLoopOutsideCube(int arg0, Random random, ChunkAccess chunk, Biome biome, int i, int j, int height, double d, BlockState blockState, BlockState blockState2, int l,
                                     long m, SurfaceBuilderBaseConfiguration surfaceBuilderBaseConfiguration) {
        if (chunk.getMinBuildHeight() > arg0) {
            return chunk.getMinBuildHeight();
        } else if (chunk.getMaxBuildHeight() < arg0) {
            return Integer.MAX_VALUE;
        } else {
            return arg0;
        }
    }
}