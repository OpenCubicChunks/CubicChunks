package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlendedNoise.class)
public class MixinBlendedNoise {
    @Shadow private PerlinNoise mainNoise;
    @Shadow private PerlinNoise minLimitNoise;
    @Shadow private PerlinNoise maxLimitNoise;

    /**
     * @reason optimization: the main noise is often clamped, causing only one of the min limit or max limit to be actually used
     * @author Gegy
     */
    @Overwrite
    public double sampleAndClampNoise(int ix, int iy, int iz, double horizontalScale, double verticalScale, double horizontalStretch, double verticalStretch) {
        double amplitude = 1.0;

        double x = ix;
        double y = iy;
        double z = iz;

        double main = 0.0;

        for (int octave = 0; octave < 8; octave++) {
            double unwrappedY = y * verticalStretch;
            double sampleX = PerlinNoise.wrap(x * horizontalStretch);
            double sampleY = PerlinNoise.wrap(unwrappedY);
            double sampleZ = PerlinNoise.wrap(z * horizontalStretch);
            main += mainNoise.getOctaveNoise(octave).noise(sampleX, sampleY, sampleZ, verticalStretch, unwrappedY) * amplitude;

            horizontalStretch *= 0.5;
            verticalStretch *= 0.5;
            amplitude *= 2.0;
        }

        double mix = (main / 10.0 + 1.0) / 2.0;

        amplitude = 1.0 / 512.0;

        if (mix <= 0.0) {
            double minLimit = 0.0;

            for (int octave = 0; octave < 16; octave++) {
                double unwrappedY = y * verticalScale;
                double sampleX = PerlinNoise.wrap(x * horizontalScale);
                double sampleY = PerlinNoise.wrap(unwrappedY);
                double sampleZ = PerlinNoise.wrap(z * horizontalScale);

                minLimit += minLimitNoise.getOctaveNoise(octave).noise(sampleX, sampleY, sampleZ, verticalScale, unwrappedY) * amplitude;

                horizontalScale *= 0.5;
                verticalScale *= 0.5;
                amplitude *= 2.0;
            }

            return minLimit;
        } else if (mix >= 1.0) {
            double maxLimit = 0.0;

            for (int octave = 0; octave < 16; octave++) {
                double unwrappedY = y * verticalScale;
                double sampleX = PerlinNoise.wrap(x * horizontalScale);
                double sampleY = PerlinNoise.wrap(unwrappedY);
                double sampleZ = PerlinNoise.wrap(z * horizontalScale);

                maxLimit += maxLimitNoise.getOctaveNoise(octave).noise(sampleX, sampleY, sampleZ, verticalScale, unwrappedY) * amplitude;

                horizontalScale *= 0.5;
                verticalScale *= 0.5;
                amplitude *= 2.0;
            }

            return maxLimit;
        } else {
            double minLimit = 0.0;
            double maxLimit = 0.0;

            for (int octave = 0; octave < 16; octave++) {
                double unwrappedY = y * verticalScale;
                double sampleX = PerlinNoise.wrap(x * horizontalScale);
                double sampleY = PerlinNoise.wrap(unwrappedY);
                double sampleZ = PerlinNoise.wrap(z * horizontalScale);

                minLimit += minLimitNoise.getOctaveNoise(octave).noise(sampleX, sampleY, sampleZ, verticalScale, unwrappedY) * amplitude;
                maxLimit += maxLimitNoise.getOctaveNoise(octave).noise(sampleX, sampleY, sampleZ, verticalScale, unwrappedY) * amplitude;

                horizontalScale *= 0.5;
                verticalScale *= 0.5;
                amplitude *= 2.0;
            }

            return Mth.lerp(mix, minLimit, maxLimit);
        }
    }
}
