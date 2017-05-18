/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
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
package cubicchunks.worldgen.generator.custom.biome.replacer;

import com.google.common.collect.Sets;
import cubicchunks.CubicChunks;
import cubicchunks.world.ICubicWorld;
import cubicchunks.api.worldgen.biome.CubicBiome;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;

import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SurfaceDefaultReplacer implements IBiomeBlockReplacer {

    private IBlockState topBlock;
    private IBlockState fillerBlock;
    private final double horizontalGradientWeight;
    private final double depth;
    private final double oceanHeight;

    public SurfaceDefaultReplacer(IBlockState topBlock, IBlockState fillerBlock, double horizontalGradientWeight, double depthMultiplier,
            double oceanHeight) {
        this.topBlock = topBlock;
        this.fillerBlock = fillerBlock;
        this.horizontalGradientWeight = horizontalGradientWeight;
        this.depth = depthMultiplier;
        this.oceanHeight = oceanHeight;
    }

    /**
     * Replaces a few top non-air blocks with biome surface and filler blocks
     */
    @Override
    public IBlockState getReplacedBlock(IBlockState previousBlock, int x, int y, int z, double dx, double dy, double dz, double density) {
        if (previousBlock.getBlock() == Blocks.AIR) {
            return previousBlock;
        }
        if (density + dy <= 0) {
            if (y < oceanHeight - 1) {
                return fillerBlock;
            } else {
                return topBlock;
            }
        } else {
            // I don't have a good idea for variable name here
            double d = dy + horizontalGradientWeight * Math.sqrt(dx * dx + dz * dz);
            if (d < 0 && density < depth) {
                return fillerBlock;
            }
        }
        return previousBlock;
    }

    public static IBiomeBlockReplacerProvider provider() {
        return new IBiomeBlockReplacerProvider() {
            private final ResourceLocation HORIZONTAL_GRADIENT_WEIGHT = CubicChunks.location("horizontal_gradient_weight");
            private final ResourceLocation FILLER_DEPTH_MULTIPLIER = CubicChunks.location("filler_depth_multiplier");
            private final ResourceLocation OCEAN_LEVEL = CubicChunks.location("ocean_level");

            @Override
            public IBiomeBlockReplacer create(ICubicWorld world, CubicBiome cubicBiome, BiomeBlockReplacerConfig conf) {
                double gradWeight = conf.getDouble(HORIZONTAL_GRADIENT_WEIGHT);
                double depthMult = conf.getDouble(FILLER_DEPTH_MULTIPLIER);
                double oceanY = conf.getInt(OCEAN_LEVEL);
                Biome biome = cubicBiome.getBiome();
                return new SurfaceDefaultReplacer(biome.topBlock, biome.fillerBlock, gradWeight, depthMult, oceanY);
            }

            @Override public Set<ConfigOptionInfo> getPossibleConfigOptions() {
                return Sets.newHashSet(
                        new ConfigOptionInfo(HORIZONTAL_GRADIENT_WEIGHT, 0.25),
                        new ConfigOptionInfo(FILLER_DEPTH_MULTIPLIER, 4.0)
                );
            }
        };
    }
}
