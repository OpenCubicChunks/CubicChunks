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
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class OceanWaterReplacer implements IBiomeBlockReplacer {

    private final IBlockState oceanBlock;
    private final int oceanLevel;

    public OceanWaterReplacer(IBlockState oceanBlock, int oceanLevel) {
        this.oceanBlock = oceanBlock;
        this.oceanLevel = oceanLevel;
    }

    /**
     * Replaces air blocks below configurable sea level with configurable block (usually water)
     */
    @Override
    public IBlockState getReplacedBlock(IBlockState previousBlock, int x, int y, int z, double dx, double dy, double dz, double density) {
        if (previousBlock.getBlock() == Blocks.AIR && y < oceanLevel) {
            return oceanBlock;
        }
        return previousBlock;
    }

    public static IBiomeBlockReplacerProvider provider() {
        return new IBiomeBlockReplacerProvider() {
            private final ResourceLocation OCEAN_BLOCK = CubicChunks.location("ocean_block");
            private final ResourceLocation OCEAN_LEVEL = CubicChunks.location("ocean_level");

            @Override public IBiomeBlockReplacer create(ICubicWorld world, CubicBiome biome, BiomeBlockReplacerConfig conf) {
                IBlockState oceanBlock = Block.getBlockFromName(conf.getString(OCEAN_BLOCK)).getDefaultState();
                int oceanHeight = conf.getInt(OCEAN_LEVEL);
                return new OceanWaterReplacer(oceanBlock, oceanHeight);
            }

            @Override public Set<ConfigOptionInfo> getPossibleConfigOptions() {
                return Sets.newHashSet(
                        new ConfigOptionInfo(OCEAN_BLOCK, Blocks.WATER.getRegistryName().toString()),
                        new ConfigOptionInfo(OCEAN_LEVEL, 63)
                );
            }
        };
    }
}
