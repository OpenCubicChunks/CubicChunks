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

import cubicchunks.CubicChunks;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.NoiseGeneratorPerlin;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SwampWaterWithLilypadReplacer implements BiomeBlockReplacer {

    private static final ResourceLocation OCEAN_LEVEL = CubicChunks.location("water_level");

    private final NoiseGeneratorPerlin noiseGen;
    private final int seaLevel;

    private SwampWaterWithLilypadReplacer(NoiseGeneratorPerlin noiseGen, int seaLevel) {
        this.noiseGen = noiseGen;
        this.seaLevel = seaLevel;
    }

    @Override public IBlockState getReplacedBlock(IBlockState previousBlock, int x, int y, int z, double dx, double dy, double dz, double density) {
        if (y == seaLevel - 1 && previousBlock.getBlock() != Blocks.AIR && density + dy < 0) {
            double noise = noiseGen.getValue(x * 0.25D, z * 0.25D);
            if (noise > 0) { // if noise < 0 && isSurface
                return Blocks.WATER.getDefaultState();
            }
        }
        if (y == seaLevel - 1 && previousBlock.getBlock() == Blocks.AIR && density - dy >= 0) {
            double noise = noiseGen.getValue(x * 0.25D, z * 0.25D);
            if (noise > 0 && noise < 0.12) {
                return Blocks.WATERLILY.getDefaultState();
            }
        }
        return previousBlock;
    }

    public static BiomeBlockReplacerProvider provider() {
        return BiomeBlockReplacerProvider.of((world, biome, conf) ->
                new SwampWaterWithLilypadReplacer(Biome.GRASS_COLOR_NOISE, MathHelper.floor(conf.getDouble(OCEAN_LEVEL)))
        );
    }
}
