package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class MixinNoiseBasedChunkGenerator {

    @Mutable @Shadow @Final private int chunkCountY;
    @Shadow @Final private int chunkHeight;

    @Shadow protected abstract double sampleAndClampNoise(int x, int y, int z, double horizontalScale, double verticalScale, double horizontalStretch, double verticalStretch);

    @Mutable @Shadow @Final private int height;

    @Inject(method = "<init>(Lnet/minecraft/world/level/biome/BiomeSource;Lnet/minecraft/world/level/biome/BiomeSource;JLjava/util/function/Supplier;)V", at = @At("RETURN"))
    private void transfromClassFields(BiomeSource biomeSource, BiomeSource biomeSource2, long l, Supplier<NoiseGeneratorSettings> supplier, CallbackInfo ci) {
        this.height = IBigCube.DIAMETER_IN_BLOCKS;
        this.chunkCountY = IBigCube.DIAMETER_IN_BLOCKS / this.chunkHeight;
    }

    private ChunkAccess access;

    @Inject(method = "fillFromNoise", at = @At("HEAD"))
    private void captureChunkAccess(LevelAccessor world, StructureFeatureManager accessor, ChunkAccess chunk, CallbackInfo ci) {
        access = chunk;
    }

    @Redirect(method = "fillFromNoise", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseSettings;minY()I"))
    private int useMinBuildHeight(NoiseSettings noiseSettings) {
        return access.getMinBuildHeight();
    }

    @Redirect(method = "fillFromNoise", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/StructureFeatureManager;startsForFeature(Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/levelgen/feature/StructureFeature;)"
            + "Ljava/util/stream/Stream;"))
    private Stream<?> doNotHandleStructureNoise(StructureFeatureManager featureManager, SectionPos pos, StructureFeature<?> feature) {
        return Stream.empty(); //TODO: Handle Structure noise
    }

    @Inject(method = "setBedrock", at = @At(value = "HEAD"), cancellable = true)
    private void cancelBedrockPlacement(ChunkAccess chunk, Random random, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "fillNoiseColumn", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;intFloorDiv(II)I"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void doNotTransformNoiseToVanillaYBounds(double buffer[], int x, int z, CallbackInfo ci, double ac, double ad, NoiseSettings noiseSettings, double ae, double af, double ag,
                                                     double ah, double ai, double aj, double ak, double al, double am, double an, double ao, double ap, double aq) {
        ci.cancel();

        int ySize = Mth.intFloorDiv(access.getMinBuildHeight(), this.chunkHeight);

        for (int ySection = 0; ySection <= this.chunkCountY; ++ySection) {
            int y = ySection + ySize;
            double height = this.sampleAndClampNoise(x, y, z, ae, af, ag, ah);
            double baseYGradient = 1.0D - (double) y * 2.0D / (double) 32 + ao; //We need to use 32 here.
            double configuredYGradient = baseYGradient * ap + aq;
            double biomeYGradient = (configuredYGradient + ac) * ad;
            if (biomeYGradient > 0.0D) {
                height += biomeYGradient * 4.0D;
            } else {
                height += biomeYGradient;
            }

            buffer[ySection] = height;
        }
    }
}
