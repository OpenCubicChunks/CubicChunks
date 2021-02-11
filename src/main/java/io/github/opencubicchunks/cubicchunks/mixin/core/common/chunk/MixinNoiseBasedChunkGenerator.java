package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import net.minecraft.util.Mth;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class MixinNoiseBasedChunkGenerator {

    @Shadow @Final private int cellHeight;

    @Mutable @Shadow @Final private int cellCountY;

    @Mutable @Shadow @Final private int height;


    @Redirect(method = "fillFromNoise", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(II)I"))
    private int alwaysUseChunkMinBuildHeight(int a, int b) {
        return b;
    }

    @Redirect(method = "fillFromNoise", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseSettings;minY()I"))
    private int modifyMinY(NoiseSettings noiseSettings, Executor executor, StructureFeatureManager structureFeatureManager, ChunkAccess chunkAccess) {
        return chunkAccess.getMinBuildHeight();
    }

    @Inject(method = "fillFromNoise", at = @At("HEAD"))
    private void changeCellSize(Executor executor, StructureFeatureManager structureFeatureManager, ChunkAccess chunkAccess, CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        this.height = chunkAccess.getHeight();
        this.cellCountY = chunkAccess.getHeight() / this.cellHeight;
    }

    @Inject(method = "doFill", at = @At("HEAD"))
    private void changeCellSize2(StructureFeatureManager structureFeatureManager, ChunkAccess chunkAccess, int i, int j, CallbackInfoReturnable<ChunkAccess> cir) {
        this.height = chunkAccess.getHeight();
        this.cellCountY = chunkAccess.getHeight() / this.cellHeight;
    }

    @Redirect(method = "doFill", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseSettings;minY()I"))
    private int changeMinY(NoiseSettings noiseSettings, StructureFeatureManager structureFeatureManager, ChunkAccess chunkAccess, int i, int j) {
        return chunkAccess.getMinBuildHeight();
    }

    @Redirect(method = "fillFromNoise", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;intFloorDiv(II)I", ordinal = 1))
    private int alwaysUseChunkMaxHeight(int i, int j, Executor executor, StructureFeatureManager structureFeatureManager, ChunkAccess chunkAccess) {
        return Mth.intFloorDiv(chunkAccess.getMaxBuildHeight() - chunkAccess.getMinBuildHeight(), cellHeight);
    }

    @Inject(method = "setBedrock", at = @At(value = "HEAD"), cancellable = true)
    private void cancelBedrockPlacement(ChunkAccess chunk, Random random, CallbackInfo ci) {
        ci.cancel();
    }
}
