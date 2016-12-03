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
package cubicchunks.worldgen.generator.custom;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.ChunkProviderSettings;

import org.lwjgl.input.Keyboard;

import java.util.Random;
import java.util.function.ToIntFunction;

import javax.annotation.ParametersAreNonnullByDefault;

import cubicchunks.CubicChunks;
import cubicchunks.world.ICubicWorld;
import cubicchunks.worldgen.generator.ICubePrimer;
import cubicchunks.worldgen.generator.custom.builder.BiomeHeightVolatilitySource;
import cubicchunks.worldgen.generator.custom.builder.IBuilder;
import cubicchunks.worldgen.generator.custom.builder.NoiseSource;
import mcp.MethodsReturnNonnullByDefault;

import static cubicchunks.util.Coords.blockToLocal;
import static cubicchunks.worldgen.generator.custom.builder.IBuilder.NEGATIVE;
import static cubicchunks.worldgen.generator.custom.builder.IBuilder.POSITIVE;

/**
 * A terrain generator that supports infinite(*) worlds
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CustomTerrainGenerator {
	private static final int CACHE_SIZE_2D = 16*16;
	private static final int CACHE_SIZE_3D = 16*16*16;
	private static final ToIntFunction<Vec3i> HASH_2D = (v) -> v.getX() + v.getZ()*5;
	private static final ToIntFunction<Vec3i> HASH_3D = (v) -> v.getX() + v.getZ()*5 + v.getY()*25;
	// Number of octaves for the noise function
	private IBuilder terrainBuilder;
	private final BiomeHeightVolatilitySource biomeSource;
	private CustomGeneratorSettings conf;

	public CustomTerrainGenerator(ICubicWorld world, final long seed) {
		this.biomeSource = new BiomeHeightVolatilitySource(world.getBiomeProvider(), 2);
		initGenerator(seed);
	}

	private void initGenerator(long seed) {
		Random rnd = new Random(seed);

		ChunkProviderSettings.Factory factoryVanilla = new ChunkProviderSettings.Factory();
		factoryVanilla.setDefaults();
		ChunkProviderSettings confVanilla = factoryVanilla.build();

		conf = CustomGeneratorSettings.fromVanilla(confVanilla);

		IBuilder selector = NoiseSource.perlin()
			.seed(rnd.nextLong())
			.normalizeTo(-1, 1)
			.frequency(conf.selectorNoiseFrequencyX, conf.selectorNoiseFrequencyY, conf.selectorNoiseFrequencyZ)
			.octaves(conf.selectorNoiseOctaves)
			.create()
			.mul(conf.selectorNoiseFactor).add(conf.selectorNoiseOffset).clamp(0, 1);

		IBuilder low = NoiseSource.perlin()
			.seed(rnd.nextLong())
			.normalizeTo(-1, 1)
			.frequency(conf.lowNoiseFrequencyX, conf.lowNoiseFrequencyY, conf.lowNoiseFrequencyZ)
			.octaves(conf.lowNoiseOctaves)
			.create()
			.mul(conf.lowNoiseFactor).add(conf.lowNoiseOffset);

		IBuilder high = NoiseSource.perlin()
			.seed(rnd.nextLong())
			.normalizeTo(-1, 1)
			.frequency(conf.highNoiseFrequencyX, conf.highNoiseFrequencyY, conf.highNoiseFrequencyZ)
			.octaves(conf.highNoiseOctaves)
			.create()
			.mul(conf.highNoiseFactor).add(conf.highNoiseOffset);

		IBuilder randomHeight2d = NoiseSource.perlin()
			.seed(rnd.nextLong())
			.normalizeTo(-1, 1)
			.frequency(conf.depthNoiseFrequencyX, 0, conf.depthNoiseFrequencyZ)
			.octaves(conf.depthNoiseOctaves)
			.create()
			.mul(conf.depthNoiseFactor).add(conf.depthNoiseOffset)
			.mulIf(NEGATIVE, -0.3).mul(3).sub(2).clamp(-2, 1)
			.divIf(NEGATIVE, 2*2*1.4).divIf(POSITIVE, 8)
			.mul(0.2*17/64.0)
			.cached2d(CACHE_SIZE_2D, HASH_2D);

		IBuilder height = ((IBuilder) biomeSource::getHeight)
			.mul(conf.heightFactor)
			.add(conf.heightOffset);

		double specialVariationFactor = conf.specialHeightVariationFactorBelowAverageY;
		IBuilder volatility = ((IBuilder) biomeSource::getVolatility)
			.mul((x, y, z) -> height.get(x, y, z) > y ? specialVariationFactor : 1)
			.mul(conf.heightVariationFactor)
			.add(conf.heightVariationOffset);

		this.terrainBuilder = selector
			.lerp(low, high).mul(volatility).add(height).add(randomHeight2d)
			.sub((x, y, z) -> y)
			.cached(CACHE_SIZE_3D, HASH_3D);
	}

	/**
	 * Generate the cube as the specified location
	 *
	 * @param cubePrimer cube primer to use
	 * @param cubeX cube x location
	 * @param cubeY cube y location
	 * @param cubeZ cube z location
	 */
	public void generate(final ICubePrimer cubePrimer, int cubeX, int cubeY, int cubeZ) {
		// when debugging is enabled, allow reloading generator settings after pressing L
		// no need to restart after applying changes.
		// Seed it changed to some constant because world isn't easily accessible here
		if (CubicChunks.DEBUG_ENABLED && Keyboard.isKeyDown(Keyboard.KEY_L)) {
			initGenerator(42);
		}

		BlockPos start = new BlockPos(cubeX*4, cubeY*2, cubeZ*4);
		BlockPos end = start.add(4, 2, 4);
		terrainBuilder.forEachScaled(start, end, new Vec3i(4, 8, 4),
			(x, y, z, dx, dy, dz, v) ->
				cubePrimer.setBlockState(
					blockToLocal(x), blockToLocal(y), blockToLocal(z),
					getBlock(x, y, z, dx, dy, dz, v))
		);

	}

	/**
	 * Retrieve the blockstate appropriate for the specified builder entry
	 *
	 * @return The block state
	 */
	private IBlockState getBlock(int x, int y, int z, double dx, double dy, double dz, double density) {
		Biome biome = biomeSource.getBiome(x, y, z);

		double dir = dy + 0.25*Math.sqrt(dx*dx + dz*dz);
		final double dirtDepth = 4;
		IBlockState state = Blocks.AIR.getDefaultState();
		if (density > 0) {
			state = Blocks.STONE.getDefaultState();
			//if the block above would be empty:
			if (density + dy <= 0) {
				if (y < conf.waterLevel - 1) {
					state = biome.fillerBlock;
				} else {
					state = biome.topBlock;
				}
				//if density decreases as we go up && density < dirtDepth
			} else if (dir < 0 && density < dirtDepth) {
				state = biome.fillerBlock;
			}
		} else if (y < conf.waterLevel) {
			// TODO replace check with GlobalGeneratorConfig.SEA_LEVEL
			state = Blocks.WATER.getDefaultState();
		}
		return state;
	}

}
