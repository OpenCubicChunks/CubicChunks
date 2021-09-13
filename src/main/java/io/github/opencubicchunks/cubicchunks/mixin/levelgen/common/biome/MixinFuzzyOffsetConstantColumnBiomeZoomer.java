package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.biome;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FuzzyOffsetBiomeZoomer;
import net.minecraft.world.level.biome.FuzzyOffsetConstantColumnBiomeZoomer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FuzzyOffsetConstantColumnBiomeZoomer.class)
public class MixinFuzzyOffsetConstantColumnBiomeZoomer {
    @Inject(method = "getBiome", at = @At("HEAD"), cancellable = true)
    private void replaceGetBiome(long l, int i, int j, int k, BiomeManager.NoiseBiomeSource noiseBiomeSource, CallbackInfoReturnable<Biome> cir) {
        cir.setReturnValue(FuzzyOffsetBiomeZoomer.INSTANCE.getBiome(l, i, j, k, noiseBiomeSource));
    }

}
