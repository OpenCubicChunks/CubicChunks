package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import io.github.opencubicchunks.cubicchunks.chunk.ICubeAquifer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.Heightmap;
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
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class MixinNoiseBasedChunkGenerator {

    @Shadow @Final private int cellHeight;

    @Mutable @Shadow @Final private int cellCountY;

    @Mutable @Shadow @Final private int height;


    @Shadow @Final protected BlockState defaultBlock;

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

    @Inject(method = "doFill", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseBasedChunkGenerator;updateNoiseAndGenerateBaseState"
        + "(Lnet/minecraft/world/level/levelgen/Beardifier;Lnet/minecraft/world/level/levelgen/Aquifer;IIID)Lnet/minecraft/world/level/block/state/BlockState;", shift = At.Shift.BEFORE),
        locals = LocalCapture.CAPTURE_FAILHARD)
    private void attachAquiferChunk(StructureFeatureManager structureFeatureManager, ChunkAccess chunkAccess, int i, int j, CallbackInfoReturnable<ChunkAccess> cir,
                                    NoiseSettings noiseSettings, int k, Heightmap heightmap, Heightmap heightmap2, ChunkPos chunkPos, int l, int m, int n, int o, Beardifier beardifier,
                                    Aquifer aquifer, double ds[][][], BlockPos.MutableBlockPos mutableBlockPos, int s, int w, LevelChunkSection levelChunkSection, int x, double d, double e,
                                    double f, double g, double h, double y, double z, double aa, int ab, int ac, int ad, double af, double ag, double ah, double ai, double aj, int ak,
                                    int al, int am, double an, double ao, double ap, int aq, int ar, int as, double at, double au) {

        ((ICubeAquifer) aquifer).prepareLocalWaterLevelForCube(chunkAccess);
    }

    @Inject(method = "setBedrock", at = @At(value = "HEAD"), cancellable = true)
    private void cancelBedrockPlacement(ChunkAccess chunk, Random random, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(
        method = "updateNoiseAndGenerateBaseState",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/Aquifer;computeAt(III)V", shift = At.Shift.BEFORE),
        locals = LocalCapture.CAPTURE_FAILHARD,
        cancellable = true
    )
    private void computeAquifer(Beardifier beardifier, Aquifer aquifer, int x, int y, int z, double noise, CallbackInfoReturnable<BlockState> ci, double density) {
        // optimization: we don't need to compute aquifer if we know that this block is already solid
        if (density > 0.0) {
            ci.cancel();
            ci.setReturnValue(this.defaultBlock);
        }
    }
}
