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
import cubicchunks.api.worldgen.biome.CubicBiome;
import cubicchunks.util.cache.HashCacheDoubles;
import cubicchunks.world.ICubicWorld;
import cubicchunks.worldgen.generator.custom.builder.IBuilder;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.BiomeMesa;
import net.minecraft.world.gen.NoiseGeneratorPerlin;

import java.util.Arrays;
import java.util.Random;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MesaSurfaceReplacer implements IBiomeBlockReplacer {

    private static final ResourceLocation HEIGHT_OFFSET = CubicChunks.location("height_offset");
    private static final ResourceLocation HEIGHT_SCALE = CubicChunks.location("height_scale");
    private static final ResourceLocation OCEAN_LEVEL = CubicChunks.location("water_level");

    private final double heightOffset;
    private final double heightScale;
    private final double waterHeight;

    private final IBuilder depthNoise = SurfaceDefaultReplacer.makeDepthNoise();

    private final BiomeMesa biomeMesa;

    private final IBlockState[] clayBands;
    private final HashCacheDoubles<BlockPos> clayBandsOffsetNoise;
    private final HashCacheDoubles<BlockPos> pillarNoise;
    private final HashCacheDoubles<BlockPos> pillarRoofNoise;

    protected static final IBlockState STAINED_HARDENED_CLAY = Blocks.STAINED_HARDENED_CLAY.getDefaultState();
    protected static final IBlockState AIR = Blocks.AIR.getDefaultState();
    protected static final IBlockState STONE = Blocks.STONE.getDefaultState();
    protected static final IBlockState COARSE_DIRT = Blocks.DIRT.getDefaultState().withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT);
    protected static final IBlockState GRASS = Blocks.GRASS.getDefaultState();
    protected static final IBlockState HARDENED_CLAY = Blocks.HARDENED_CLAY.getDefaultState();
    protected static final IBlockState ORANGE_STAINED_HARDENED_CLAY = STAINED_HARDENED_CLAY.withProperty(BlockColored.COLOR, EnumDyeColor.ORANGE);


    public MesaSurfaceReplacer(ICubicWorld world, CubicBiome biome, double heightOffset, double heightScale, double waterHeight) {
        this.biomeMesa = (BiomeMesa) biome.getBiome();
        this.heightOffset = heightOffset;
        this.heightScale = heightScale;
        this.waterHeight = waterHeight;

        if (biomeMesa.clayBands == null || biomeMesa.worldSeed != world.getSeed()) {
            biomeMesa.generateBands(world.getSeed());
        }
        // so that we don't cause issues when we replace clayBands and scrollOffset noise
        biomeMesa.worldSeed = world.getSeed();
        this.clayBands = Arrays.copyOf(biomeMesa.clayBands, biomeMesa.clayBands.length);
        this.clayBandsOffsetNoise = HashCacheDoubles.create(
                256, p -> p.getX() * 16 + p.getZ(), p -> biomeMesa.clayBandsOffsetNoise.getValue(p.getX() / 512.0, p.getZ() / 512.0)
        );

        Random random = new Random(world.getSeed());
        NoiseGeneratorPerlin pillasPerlin = new NoiseGeneratorPerlin(random, 4);
        this.pillarNoise = HashCacheDoubles.create(
                256, p -> p.getX() * 16 + p.getZ(), p -> pillasPerlin.getValue(p.getX(), p.getZ())
        );
        NoiseGeneratorPerlin pillarRoofPerlin = new NoiseGeneratorPerlin(random, 1);
        this.pillarRoofNoise = HashCacheDoubles.create(
                256, p -> p.getX() * 16 + p.getZ(), p -> pillarRoofPerlin.getValue(p.getX(), p.getZ())
        );
    }

    @Override public IBlockState getReplacedBlock(IBlockState previousBlock, int x, int y, int z, double dx, double dy, double dz, double density) {
        if (density < 0) {
            return previousBlock;
        }
        double depth = depthNoise.get(x, 0, z);
        double origDepthNoise = depth - 3;
        double pillarHeight = getPillarHeightVanilla(x, z, origDepthNoise);
        pillarHeight = convertYFromVanilla(pillarHeight);
        if (y < pillarHeight) {
            // simulate pillar density ORed with te terrain
            density = Math.max(density, pillarHeight - y);
        }

        boolean coarse = Math.cos(origDepthNoise * Math.PI) > 0.0D;

        IBlockState top = STAINED_HARDENED_CLAY;
        IBlockState filler = biomeMesa.fillerBlock;

        if (depth < 0) {
            top = AIR;
            filler = STONE;
        }

        if (y >= waterHeight - 1) {
            if (biomeMesa.hasForest && y >= convertYFromVanilla(86) + depth * 2) {
                top = coarse ? COARSE_DIRT : GRASS;
                filler = getBand(x, y, z);
            } else if (y > waterHeight + 3 + depth) {
                filler = getBand(x, y, z);
                top = coarse ? HARDENED_CLAY : filler;
            } else {
                top = filler = ORANGE_STAINED_HARDENED_CLAY;
            }
        }

        if (density + dy <= 0) { // if air above
            return top;
        }
        if (density < 16) {
            return filler;
        }
        return previousBlock;
    }

    private double convertYFromVanilla(double y) {
        y = (y - 64.0) / 64.0;
        y *= heightScale;
        y += heightOffset;
        return y;
    }

    private double getPillarHeightVanilla(int x, int z, double depth) {
        double pillarHeight = 0.0;
        if (biomeMesa.brycePillars) {
            double pillarScale = Math.min(Math.abs(depth),
                    this.pillarNoise.get(new BlockPos(x * 0.25D, 0, z * 0.25D)));

            if (pillarScale > 0.0D) {
                double xzScale = 0.001953125D;
                double pillarRoofVal = Math.abs(this.pillarRoofNoise.get(new BlockPos(x * xzScale, 0, z * xzScale)));
                pillarHeight = pillarScale * pillarScale * 2.5D;
                double cutoffHeight = Math.ceil(pillarRoofVal * 50.0D) + 14.0D;

                if (pillarHeight > cutoffHeight) {
                    pillarHeight = cutoffHeight;
                }

                pillarHeight = pillarHeight + 64.0D;
            }
        }
        return pillarHeight;
    }

    private IBlockState getBand(int blockX, int blockY, int blockZ) {
        int offset = (int) Math.round(this.clayBandsOffsetNoise.get(new BlockPos(blockX, 0, blockX)) * 2.0D);
        return clayBands[(blockY + offset + 64) & 63];
    }

    public static IBiomeBlockReplacerProvider provider() {
        return IBiomeBlockReplacerProvider.of((world, biome, conf) ->
                new MesaSurfaceReplacer(world, biome, conf.getDouble(HEIGHT_OFFSET), conf.getDouble(HEIGHT_SCALE), conf.getDouble(OCEAN_LEVEL))
        );
    }
}
