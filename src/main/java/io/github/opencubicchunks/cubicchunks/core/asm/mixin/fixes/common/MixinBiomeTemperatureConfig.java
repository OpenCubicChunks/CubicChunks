/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.core.CubicChunksConfig;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Biome.class)
public abstract class MixinBiomeTemperatureConfig {

    @Shadow @Final protected static NoiseGeneratorPerlin TEMPERATURE_NOISE;

    @Shadow public abstract float getDefaultTemperature();

    /**
     * @param pos block position
     * @return temperature
     * @author Barteks2x
     * @reason Configure how it changes with height
     */
    @SuppressWarnings("OverwriteModifiers")
    @Overwrite
    public float getTemperature(BlockPos pos) { // this must be non-final for galacticraft compatibility https://github.com/SpongePowered/Mixin/issues/347
        if (pos.getY() > CubicChunksConfig.biomeTemperatureCenterY) {
            float noise = (float) (TEMPERATURE_NOISE.getValue((double) ((float) pos.getX() / 8.0F), (double) ((float) pos.getZ() / 8.0F)) * 4.0D);
            int y = Math.min(pos.getY(), CubicChunksConfig.biomeTemperatureScaleMaxY);
            return this.getDefaultTemperature() +
                (noise + y - CubicChunksConfig.biomeTemperatureCenterY) * CubicChunksConfig.biomeTemperatureHeightFactor;
        } else {
            return this.getDefaultTemperature();
        }
    }
}
