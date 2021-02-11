package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk.cave.legacy;

import java.util.BitSet;
import java.util.Random;
import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.carver.CanyonWorldCarver;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CanyonWorldCarver.class)
public class MixinCanyonWorldCarver {


    @Inject(method = "carve", at = @At("HEAD"), cancellable = true)
    private void cancelCanyons(ChunkAccess chunkAccess, Function<BlockPos, Biome> function, Random random, int i, int j, int k, int l, int m, BitSet bitSet,
                               ProbabilityFeatureConfiguration probabilityFeatureConfiguration, CallbackInfoReturnable<Boolean> cir) {

        cir.setReturnValue(false);
    }
}
