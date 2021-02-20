package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk.cave;

import io.github.opencubicchunks.cubicchunks.chunk.AquiferRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSampler;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Aquifer.class)
public abstract class MixinAquifer {
    @Shadow @Final private NormalNoise barrierNoise;

    @Shadow @Final private int minGridY;
    @Shadow @Final private long[] aquiferLocationCache;
    @Mutable @Shadow @Final private int[] aquiferCache;

    @Shadow private boolean shouldScheduleWaterUpdate;
    @Shadow private int lastWaterLevel;
    @Shadow private double lastBarrierDensity;

    @Shadow protected abstract int gridY(int i);
    @Shadow protected abstract int getIndex(int x, int y, int z);
    @Shadow protected abstract double similarity(int i, int j);
    @Shadow protected abstract int computeAquifer(int x, int y, int z);

    private static final double NOISE_MIN = transformBarrierNoise(-1.5);
    private static final double NOISE_MAX = transformBarrierNoise(1.5);

    private static double transformBarrierNoise(double noise) {
        return 1.0 + (noise + 0.1) / 4.0;
    }

    private final AquiferRandom random = new AquiferRandom();

    private double barrierNoiseCache = Double.NaN;

    private int minLevelGridX;
    private int minLevelGridZ;
    private int levelGridSizeX;
    private int levelGridSizeZ;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/Arrays;fill([II)V", shift = At.Shift.BEFORE, remap = false))
    private void init(int chunkX, int chunkZ, NormalNoise barrierNoise, NormalNoise waterLevelNoise, NoiseGeneratorSettings noiseSettings, NoiseSampler sampler, int sizeY, CallbackInfo ci) {
        int minX = chunkX << 4;
        int maxX = (chunkX << 4) + 15;
        int minZ = chunkZ << 4;
        int maxZ = (chunkZ << 4) + 15;
        int minY = noiseSettings.noiseSettings().minY();

        int minGridX = levelGridX(minX) - 1;
        int maxGridX = levelGridX(maxX) + 1;
        int minGridY = this.gridY(minY) - 1;
        int maxGridY = this.gridY(minY + sizeY) + 1;
        int minGridZ = levelGridZ(minZ) - 1;
        int maxGridZ = levelGridZ(maxZ) + 1;

        this.minLevelGridX = minGridX;
        this.levelGridSizeX = maxGridX - minGridX + 1;
        this.minLevelGridZ = minGridZ;
        this.levelGridSizeZ = maxGridZ - minGridZ + 1;

        int gridSizeY = maxGridY - minGridY + 1;
        int levelCacheSize = this.levelGridSizeX * gridSizeY * this.levelGridSizeZ;

        // TODO: we have to reinitialize the array because we can't redirect int array construction.. what other options?
        this.aquiferCache = new int[levelCacheSize];
    }

    private static int levelGridX(int x) {
        return x >> 6;
    }

    private static int levelGridZ(int z) {
        return z >> 6;
    }

    // we aren't able to reduce the grid on the y-axis because they don't match; can we change this?
    private int getLevelIndex(int x, int y, int z) {
        int localX = x - this.minLevelGridX;
        int localY = y - this.minGridY;
        int localZ = z - this.minLevelGridZ;
        return (localY * this.levelGridSizeZ + localZ) * this.levelGridSizeX + localX;
    }

    /**
     * @reason lazily compute barrier noise value to avoid excessive sampling
     * @author Gegy
     */
    @Overwrite
    protected void computeAt(int x, int y, int z) {
        int gridX = this.gridX(x - 5);
        int gridY = this.gridY(y + 1);
        int gridZ = this.gridZ(z - 5);

        // find closest 3 aquifer sources
        int closestDistance2 = Integer.MAX_VALUE;
        int secondDistance2 = Integer.MAX_VALUE;
        int thirdDistance2 = Integer.MAX_VALUE;
        long closestSource = 0;
        long secondSource = 0;
        long thirdSource = 0;

        for (int offsetX = 0; offsetX <= 1; offsetX++) {
            for (int offsetY = -1; offsetY <= 1; offsetY++) {
                for (int offsetZ = 0; offsetZ <= 1; offsetZ++) {
                    long sourcePos = this.getAquiferSourceIn(gridX + offsetX, gridY + offsetY, gridZ + offsetZ);

                    int deltaX = BlockPos.getX(sourcePos) - x;
                    int deltaY = BlockPos.getY(sourcePos) - y;
                    int deltaZ = BlockPos.getZ(sourcePos) - z;

                    int distance2 = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
                    if (distance2 <= closestDistance2) {
                        thirdSource = secondSource;
                        secondSource = closestSource;
                        closestSource = sourcePos;
                        thirdDistance2 = secondDistance2;
                        secondDistance2 = closestDistance2;
                        closestDistance2 = distance2;
                    } else if (distance2 <= secondDistance2) {
                        thirdSource = secondSource;
                        secondSource = sourcePos;
                        thirdDistance2 = secondDistance2;
                        secondDistance2 = distance2;
                    } else if (distance2 <= thirdDistance2) {
                        thirdSource = sourcePos;
                        thirdDistance2 = distance2;
                    }
                }
            }
        }

        int closestWaterLevel = this.getWaterLevel(closestSource);
        int secondWaterLevel = this.getWaterLevel(secondSource);
        int thirdWaterLevel = this.getWaterLevel(thirdSource);

        double closestToSecond = this.similarity(closestDistance2, secondDistance2);

        this.lastWaterLevel = closestWaterLevel;
        this.shouldScheduleWaterUpdate = closestToSecond > 0.0;

        if (closestToSecond <= -1.0) {
            this.lastBarrierDensity = 0.0;
            return;
        }

        // find barrier density
        double closestToThird = this.similarity(closestDistance2, thirdDistance2);
        double secondToThird = this.similarity(secondDistance2, thirdDistance2);

        // early exit: we know density will = 0
        if (closestToSecond <= 0.0 && secondToThird <= 0.0 && closestToThird <= 0.0) {
            this.lastBarrierDensity = 0.0;
            return;
        }

        this.barrierNoiseCache = Double.NaN;

        double closestToSecondPressure = this.getPressureLazyNoise(x, y, z, closestWaterLevel, secondWaterLevel);
        double closestToThirdPressure = this.getPressureLazyNoise(x, y, z, closestWaterLevel, thirdWaterLevel);
        double secondToThirdPressure = this.getPressureLazyNoise(x, y, z, secondWaterLevel, thirdWaterLevel);

        // early exit: we know density will = 0
        if (Double.isNaN(this.barrierNoiseCache) || (closestToSecondPressure <= 0.0 && closestToThirdPressure <= 0.0 && secondToThirdPressure <= 0.0)) {
            this.lastBarrierDensity = 0.0;
            return;
        }

        double closestToSecondFactor = Math.max(0.0, closestToSecond);
        double closestToThirdFactor = Math.max(0.0, closestToThird);
        double secondClosestToThirdFactor = Math.max(0.0, secondToThird);

        double toThirdPressure = Math.max(
            closestToThirdPressure * closestToThirdFactor,
            secondToThirdPressure * secondClosestToThirdFactor
        );

        this.lastBarrierDensity = Math.max(0.0, 2.0 * closestToSecondFactor * Math.max(closestToSecondPressure, toThirdPressure));
    }

    private double getPressureLazyNoise(int x, int y, int z, int firstLevel, int secondLevel) {
        double meanLevel = 0.5 * (firstLevel + secondLevel);
        double distanceFromMean = Math.abs(meanLevel - y - 0.5);

        double targetDistanceFromMean = 0.5 * Math.abs(firstLevel - secondLevel);

        // avoid sampling noise if we don't need it
        double noise = this.barrierNoiseCache;
        if (Double.isNaN(noise)) {
            if (targetDistanceFromMean * NOISE_MIN <= distanceFromMean && targetDistanceFromMean * NOISE_MAX <= distanceFromMean) {
                return 0.0;
            }

            this.barrierNoiseCache = noise = transformBarrierNoise(this.barrierNoise.getValue(x, y, z));
        }

        return (targetDistanceFromMean * noise) - distanceFromMean;
    }

    private long getAquiferSourceIn(int x, int y, int z) {
        int index = this.getIndex(x, y, z);

        long sourcePos = this.aquiferLocationCache[index];
        if (sourcePos == Long.MAX_VALUE) {
            AquiferRandom random = this.random;
            random.setSeed(Mth.getSeed(x, y * 3, z) + 1L);
            sourcePos = BlockPos.asLong(
                x * 16 + random.nextInt(10),
                y * 12 + random.nextInt(9),
                z * 16 + random.nextInt(10)
            );
            this.aquiferLocationCache[index] = sourcePos;
        }

        return sourcePos;
    }

    /**
     * @reason replace with smaller cache to avoid recomputing the same values
     * @author Gegy
     */
    @Overwrite
    private int getWaterLevel(long pos) {
        int x = BlockPos.getX(pos);
        int y = BlockPos.getY(pos);
        int z = BlockPos.getZ(pos);
        int gridX = levelGridX(x);
        int gridY = this.gridY(y);
        int gridZ = levelGridZ(z);

        int cacheIndex = this.getLevelIndex(gridX, gridY, gridZ);
        int level = this.aquiferCache[cacheIndex];
        if (level == Integer.MAX_VALUE) {
            level = this.computeAquifer(x, y, z);
            this.aquiferCache[cacheIndex] = level;
        }

        return level;
    }

    /**
     * @reason optimization: shift is faster than floorDiv
     * @author Gegy
     */
    @Overwrite
    private int gridX(int x) {
        return x >> 4;
    }

    /**
     * @reason optimization: shift is faster than floorDiv
     * @author Gegy
     */
    @Overwrite
    private int gridZ(int z) {
        return z >> 4;
    }
}
