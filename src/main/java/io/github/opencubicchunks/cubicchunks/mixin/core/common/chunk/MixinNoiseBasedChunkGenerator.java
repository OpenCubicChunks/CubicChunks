package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import java.util.Iterator;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.structures.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.StructureStart;
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

    @Shadow @Final protected Supplier<NoiseGeneratorSettings> settings;
    private ChunkAccess access;

    @Inject(method = "fillFromNoise", at = @At("HEAD"))
    private void captureChunkAccess(LevelAccessor world, StructureFeatureManager accessor, ChunkAccess chunk, CallbackInfo ci) {
        this.height = chunk.getHeight();
        this.chunkCountY = chunk.getHeight() / this.chunkHeight;
        access = chunk;
    }


    @Redirect(method = "fillFromNoise", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseSettings;minY()I"))
    private int useMinBuildHeight(NoiseSettings noiseSettings) {
        return access.getMinBuildHeight();
    }

    @Inject(method = "fillFromNoise", at = @At("RETURN"))
    private void nullAccess(LevelAccessor world, StructureFeatureManager accessor, ChunkAccess chunk, CallbackInfo ci) {
        access = null;
        this.height = this.settings.get().noiseSettings().height();
        this.chunkCountY = this.height / this.chunkHeight;
    }

    @Redirect(method = "fillFromNoise", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/StructureFeatureManager;startsForFeature(Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/levelgen/feature/StructureFeature;)"
            + "Ljava/util/stream/Stream;"))
    private Stream<?> doNotHandleStructureNoise(StructureFeatureManager featureManager, SectionPos pos, StructureFeature<?> feature, LevelAccessor world, StructureFeatureManager accessor,
                                                ChunkAccess chunk) {
        return featureManager.startsForFeature(SectionPos.of(pos.chunk(), chunk.getMinSection()), feature); //TODO: Handle Structure noise
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Redirect(method = "lambda$fillFromNoise$6", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/structures/JigsawJunction;getSourceX()I"))
    private static int checkYBounds(JigsawJunction junction, ChunkPos pos, ObjectList list, int number, int number2, ObjectList list2, StructureStart structureStart) {
        ChunkAccess chunkAccess = (ChunkAccess) list.get(0);
        int jigsawJunctionSourceY = junction.getSourceGroundY();
        int minY = chunkAccess.getMinBuildHeight();
        int maxY = chunkAccess.getMaxBuildHeight() - 1;
        boolean isInYBounds = jigsawJunctionSourceY > minY - 12 && jigsawJunctionSourceY < maxY + 12;

        if (isInYBounds) {
            return junction.getSourceX();
        } else {
            return Integer.MIN_VALUE;
        }
    }

    @Inject(method = "fillFromNoise", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;forEach(Ljava/util/function/Consumer;)V", shift = At.Shift.AFTER), locals =
        LocalCapture.CAPTURE_FAILHARD)
    private void removeChunkAccessElement(LevelAccessor world, StructureFeatureManager accessor, ChunkAccess chunk, CallbackInfo ci, ObjectList objectList, ObjectList objectList2,
                                          ChunkPos chunkPos, int i, int j, int k, int l, Iterator var11, StructureFeature structureFeature) {
        objectList.removeIf(x -> x instanceof ChunkAccess);
    }

    @Inject(method = "fillFromNoise", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/StructureFeatureManager;startsForFeature(Lnet/minecraft/core/SectionPos;"
        + "Lnet/minecraft/world/level/levelgen/feature/StructureFeature;)Ljava/util/stream/Stream;"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void getLocals(LevelAccessor world, StructureFeatureManager accessor, ChunkAccess chunk, CallbackInfo ci, ObjectList objectList, ObjectList objectList2, ChunkPos chunkPos, int i,
                           int j, int k, int l, Iterator var11, StructureFeature structureFeature) {
        objectList.add(0, chunk);
    }

    @Inject(method = "setBedrock", at = @At(value = "HEAD"), cancellable = true)
    private void cancelBedrockPlacement(ChunkAccess chunk, Random random, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "fillNoiseColumn", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;intFloorDiv(II)I"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void doNotTransformNoiseToVanillaYBounds(double buffer[], int x, int z, CallbackInfo ci, double ac, double ad, NoiseSettings noiseSettings, double ae, double af, double ag,
                                                     double ah, double ai, double aj, double ak, double al, double am, double an, double ao, double ap, double aq) {
        ci.cancel();

        int ySize = Mth.intFloorDiv((access != null) ? access.getMinBuildHeight() : noiseSettings.minY(), this.chunkHeight);

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
