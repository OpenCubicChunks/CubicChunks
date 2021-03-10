package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import io.github.opencubicchunks.cubicchunks.chunk.NonAtomicWorldgenRandom;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.NoiseGeneratorSettingsAccess;
import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.BaseStoneSource;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSampler;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class MixinNoiseBasedChunkGenerator {
    @Mutable @Shadow @Final protected Supplier<NoiseGeneratorSettings> settings;

    @Shadow @Final private int cellHeight;

    @Mutable @Shadow @Final private int cellCountY;

    @Mutable @Shadow @Final private int height;

    @Inject(method = "<init>(Lnet/minecraft/world/level/biome/BiomeSource;Lnet/minecraft/world/level/biome/BiomeSource;JLjava/util/function/Supplier;)V", at = @At("RETURN"))
    private void init(BiomeSource biomeSource, BiomeSource biomeSource2, long l, Supplier<NoiseGeneratorSettings> supplier, CallbackInfo ci) {
        // access to through the registry is slow: vanilla accesses settings directly from the supplier in the constructor anyway
        NoiseGeneratorSettings settings = this.settings.get();
        this.settings = () -> settings;
    }

    @Redirect(method = "fillFromNoise", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I"))
    private int alwaysUseChunkMinBuildHeight(int a, int b, Executor executor, StructureFeatureManager accessor, ChunkAccess chunk) {
        if (((CubicLevelHeightAccessor) chunk).generates2DChunks()) {
            return Math.max(a, b);
        }
        return b;
    }

    @Redirect(method = "fillFromNoise", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseSettings;minY()I"))
    private int modifyMinY(NoiseSettings noiseSettings, Executor executor, StructureFeatureManager structureFeatureManager, ChunkAccess chunkAccess) {
        if (((CubicLevelHeightAccessor) chunkAccess).generates2DChunks()) {
            return noiseSettings.minY();
        }

        return chunkAccess.getMinBuildHeight();
    }

    @Inject(method = "fillFromNoise", at = @At("HEAD"))
    private void changeCellSize(Executor executor, StructureFeatureManager structureFeatureManager, ChunkAccess chunkAccess, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        if (((CubicLevelHeightAccessor) chunkAccess).generates2DChunks()) {
            return;
        }
        this.height = chunkAccess.getHeight();
        this.cellCountY = chunkAccess.getHeight() / this.cellHeight;
    }

    @Inject(method = "doFill", at = @At("HEAD"))
    private void changeCellSize2(StructureFeatureManager structureFeatureManager, ChunkAccess chunkAccess, int i, int j, CallbackInfoReturnable<ChunkAccess> cir) {
        if (((CubicLevelHeightAccessor) chunkAccess).generates2DChunks()) {
            return;
        }
        this.height = chunkAccess.getHeight();
        this.cellCountY = chunkAccess.getHeight() / this.cellHeight;
    }

    @Redirect(method = "doFill", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseSettings;minY()I"))
    private int changeMinY(NoiseSettings noiseSettings, StructureFeatureManager structureFeatureManager, ChunkAccess chunkAccess, int i, int j) {
        if (((CubicLevelHeightAccessor) chunkAccess).generates2DChunks()) {
            return noiseSettings.minY();
        }

        return chunkAccess.getMinBuildHeight();
    }

    @Redirect(method = "fillFromNoise", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;intFloorDiv(II)I", ordinal = 1))
    private int alwaysUseChunkMaxHeight(int i, int j, Executor executor, StructureFeatureManager structureFeatureManager, ChunkAccess chunkAccess) {
        if (((CubicLevelHeightAccessor) chunkAccess).generates2DChunks()) {
            return Mth.intFloorDiv(i, j);
        }

        return Mth.intFloorDiv(chunkAccess.getMaxBuildHeight() - chunkAccess.getMinBuildHeight(), cellHeight);
    }

    @Redirect(method = "doFill", at = @At(value = "NEW", target = "net/minecraft/world/level/levelgen/Aquifer"))
    private Aquifer createAquifier(int i, int j, NormalNoise normalNoise, NormalNoise normalNoise2, NoiseGeneratorSettings generatorSettings, NoiseSampler noiseSampler, int k,
                                   StructureFeatureManager structureFeatureManager, ChunkAccess chunk, int chunkX, int chunkZ) {

        if (((CubicLevelHeightAccessor) chunk).generates2DChunks()) {
            return new Aquifer(i, j, normalNoise, normalNoise2, generatorSettings, noiseSampler, k);
        }


        // copy noise generator settings with the minimum noise value set for aquifier initialization
        NoiseGeneratorSettingsAccess generatorSettingsAccess = (NoiseGeneratorSettingsAccess) (Object) generatorSettings;
        NoiseSettings noise = generatorSettings.noiseSettings();
        noise = NoiseSettings.create(
            chunk.getMinBuildHeight(), noise.height(), noise.noiseSamplingSettings(), noise.topSlideSettings(), noise.bottomSlideSettings(), noise.noiseSizeHorizontal(),
            noise.noiseSizeVertical(), noise.densityFactor(), noise.densityOffset(), noise.useSimplexSurfaceNoise(), noise.randomDensityOffset(), noise.islandNoiseOverride(),
            noise.isAmplified()
        );

        NoiseGeneratorSettings cubeGeneratorSettings = NoiseGeneratorSettingsAccess.create(
            generatorSettings.structureSettings(), noise, generatorSettings.getDefaultBlock(), generatorSettings.getDefaultFluid(),
            generatorSettings.getBedrockRoofPosition(), generatorSettings.getBedrockFloorPosition(), generatorSettings.seaLevel(),
            generatorSettingsAccess.isDisableMobGeneration(),
            generatorSettingsAccess.isAquifersEnabled(),
            generatorSettingsAccess.isNoiseCavesEnabled(), generatorSettingsAccess.isDeepSlateEnabled()
        );

        return new Aquifer(i, j, normalNoise, normalNoise2, cubeGeneratorSettings, noiseSampler, k);
    }

    @Inject(method = "setBedrock", at = @At(value = "HEAD"), cancellable = true)
    private void cancelBedrockPlacement(ChunkAccess chunk, Random random, CallbackInfo ci) {
        if (((CubicLevelHeightAccessor) chunk).generates2DChunks()) {
            return;
        }
        ci.cancel();
    }

    @Inject(
        method = "updateNoiseAndGenerateBaseState",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/Aquifer;computeAt(III)V", shift = At.Shift.BEFORE),
        locals = LocalCapture.CAPTURE_FAILHARD,
        cancellable = true
    )
    private void computeAquifer(Beardifier beardifier, Aquifer aquifer, BaseStoneSource stoneSource, int x, int y, int z, double noise, CallbackInfoReturnable<BlockState> ci,
                                double density) {
        // optimization: we don't need to compute aquifer if we know that this block is already solid
        if (density > 0.0) {
            ci.setReturnValue(stoneSource.getBaseStone(x, y, z, this.settings.get()));
        }
    }

    // replace with non-atomic random for optimized random number generation
    @Redirect(method = "buildSurfaceAndBedrock", at = @At(value = "NEW", target = "net/minecraft/world/level/levelgen/WorldgenRandom"))
    private WorldgenRandom createCarverRandom() {
        return new NonAtomicWorldgenRandom();
    }
}
