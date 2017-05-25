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
import cubicchunks.worldgen.generator.custom.ConversionUtils;
import cubicchunks.worldgen.generator.custom.builder.IBuilder;
import cubicchunks.worldgen.generator.custom.builder.NoiseSource;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockSand;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;

import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SurfaceDefaultReplacer implements IBiomeBlockReplacer {
    protected static final IBlockState GRAVEL = Blocks.GRAVEL.getDefaultState();
    protected static final IBlockState RED_SANDSTONE = Blocks.RED_SANDSTONE.getDefaultState();
    protected static final IBlockState SANDSTONE = Blocks.SANDSTONE.getDefaultState();

    private final IBuilder depthNoise;
    private final int maxPossibleDepth;
    private IBlockState topBlock;
    private IBlockState fillerBlock;
    private final double horizontalGradientCutoffWeight;
    private final double horizontalGradientDepthDecreaseWeight;
    private final double oceanHeight;

    public SurfaceDefaultReplacer(IBlockState topBlock, IBlockState fillerBlock,
            double horizontalGradientCutoffWeight, double horizontalGradientDepthDecreaseWeight, double oceanHeight) {
        this.topBlock = topBlock;
        this.fillerBlock = fillerBlock;
        this.horizontalGradientCutoffWeight = horizontalGradientCutoffWeight;
        this.horizontalGradientDepthDecreaseWeight = horizontalGradientDepthDecreaseWeight;
        this.oceanHeight = oceanHeight;
        this.depthNoise = NoiseSource.perlin()
                .frequency(ConversionUtils.frequencyFromVanilla(0.0625f, 4)).octaves(4).create()
                .mul((1 << 3) - 1) // TODO: do it properly, currently this value is just temporary until I figure out the right one
                .mul(1.0 / 3.0).add(3)
                .cached2d(256, v -> v.getX() + v.getZ() * 16);
        this.maxPossibleDepth = 9;
    }

    /**
     * Replaces a few top non-air blocks with biome surface and filler blocks
     */
    @Override
    public IBlockState getReplacedBlock(IBlockState previousBlock, int x, int y, int z, double dx, double dy, double dz, double density) {
        // skip everything below if there is no chance it will actually do something
        if (density > maxPossibleDepth || density < 0) {
            return previousBlock;
        }
        if (previousBlock.getBlock() == Blocks.AIR) {
            return previousBlock;
        }
        double depth = depthNoise.get(x, 0, z);
        if (density + dy <= 0) { // if air above
            if (y < oceanHeight - 7 - depth) { // if we are deep into the ocean
                return GRAVEL;
            }
            if (y < oceanHeight - 1) { // if just below the ocean level
                return filler(depth);
            }
            return top(previousBlock, depth);
        } else {
            double xzSize = Math.sqrt(dx * dx + dz * dz);
            double dyAdjusted = dy + horizontalGradientCutoffWeight * xzSize;
            if (dyAdjusted < 0 && density < depth + 1 - horizontalGradientDepthDecreaseWeight * xzSize) {
                return fillerBlock;
            }

            if (fillerBlock.getBlock() == Blocks.SAND && depth > 1 && y > oceanHeight - depth) {
                return fillerBlock.getValue(BlockSand.VARIANT) == BlockSand.EnumType.RED_SAND ? RED_SANDSTONE : SANDSTONE;
            } else if (depth <=1){
                System.out.println("x=" + x + ", z=" + z);
            }
        }
        return previousBlock;
    }

    private IBlockState filler(double depth) {
        return depth > 0 ? fillerBlock : Blocks.AIR.getDefaultState();
    }

    private IBlockState top(IBlockState prev, double depth) {
        return depth > 0 ? topBlock : prev;
    }

    public static IBiomeBlockReplacerProvider provider() {
        return new IBiomeBlockReplacerProvider() {
            private final ResourceLocation HORIZONTAL_GRADIENT_WEIGHT = CubicChunks.location("horizontal_gradient_cutoff_weight");
            private final ResourceLocation HORIZONTAL_GRADIENT_DEC = CubicChunks.location("horizontal_gradient_depth_decrease_weight");
            private final ResourceLocation OCEAN_LEVEL = CubicChunks.location("ocean_level");

            @Override
            public IBiomeBlockReplacer create(ICubicWorld world, CubicBiome cubicBiome, BiomeBlockReplacerConfig conf) {
                double gradWeight = conf.getDouble(HORIZONTAL_GRADIENT_WEIGHT);
                double gradientDec = conf.getDouble(HORIZONTAL_GRADIENT_DEC);
                double oceanY = conf.getInt(OCEAN_LEVEL);
                Biome biome = cubicBiome.getBiome();
                return new SurfaceDefaultReplacer(biome.topBlock, biome.fillerBlock, gradWeight, gradientDec, oceanY);
            }

            @Override public Set<ConfigOptionInfo> getPossibleConfigOptions() {
                return Sets.newHashSet(
                        new ConfigOptionInfo(HORIZONTAL_GRADIENT_WEIGHT, -0.2),
                        new ConfigOptionInfo(HORIZONTAL_GRADIENT_DEC, 0.2)
                );
            }
        };
    }
}
