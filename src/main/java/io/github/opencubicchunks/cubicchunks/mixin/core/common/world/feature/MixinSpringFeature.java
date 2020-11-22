package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.feature;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.SpringFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SpringConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//Enable this
@Mixin(SpringFeature.class)
public class MixinSpringFeature {

    @Inject(at = @At("HEAD"), method = "place", cancellable = true)
    private void cancel(WorldGenLevel worldGenLevel, ChunkGenerator chunkGenerator, Random random, BlockPos blockPos, SpringConfiguration springConfiguration,
                        CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
}
