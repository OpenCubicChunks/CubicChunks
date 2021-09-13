package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.biome;

import net.minecraft.data.worldgen.BiomeDefaultFeatures;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BiomeDefaultFeatures.class)
public class MixinBiomeDefaultFeatures {

    @Inject(at = @At("HEAD"), method = "addDefaultSprings(Lnet/minecraft/world/level/biome/BiomeGenerationSettings$Builder;)V", cancellable = true)
    private static void cancelSprings(BiomeGenerationSettings.Builder builder, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(at = @At("HEAD"), method = "addDefaultSeagrass(Lnet/minecraft/world/level/biome/BiomeGenerationSettings$Builder;)V", cancellable = true)
    private static void cancelKelp(BiomeGenerationSettings.Builder builder, CallbackInfo ci) {
        ci.cancel();
    }
}
