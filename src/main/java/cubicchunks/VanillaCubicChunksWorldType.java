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
package cubicchunks;

import cubicchunks.server.ServerCubeCache;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.worldgen.GeneratorPipeline;
import cubicchunks.worldgen.GeneratorStage;
import cubicchunks.worldgen.IndependentGeneratorStage;
import cubicchunks.worldgen.generator.vanilla.VanillaFirstLightProcessor;
import cubicchunks.worldgen.generator.vanilla.VanillaPopulationProcessor;
import cubicchunks.worldgen.generator.vanilla.VanillaTerrainProcessor;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.ChunkProviderOverworld;

import static cubicchunks.worldgen.GeneratorStage.LIGHTING;

public class VanillaCubicChunksWorldType extends WorldType implements ICubicChunksWorldType {

	public VanillaCubicChunksWorldType() {
		super("VanillaCubic");
	}

	@Override public void registerWorldGen(ICubicWorldServer world, GeneratorPipeline pipeline) {
		ServerCubeCache cubeCache = world.getCubeCache();

		ChunkProviderOverworld vanillaGen = new ChunkProviderOverworld((World) world, world.getSeed(), true, "");
		
		// init the worldgen pipeline
		GeneratorStage terrain = new IndependentGeneratorStage("terrain");
		GeneratorStage population = new IndependentGeneratorStage("population");		
		
		pipeline.addStage(terrain, new VanillaTerrainProcessor(LIGHTING, world, vanillaGen, 5));
		pipeline.addStage(LIGHTING, new VanillaFirstLightProcessor(LIGHTING, population, cubeCache, 5));
		pipeline.addStage(population, new VanillaPopulationProcessor(population, world, vanillaGen, 5));

	}

	/**
	 * Return Double.NaN to remove void fog and fix night vision potion below Y=0.
	 * <p>
	 * In EntityRenderer.updateFogColor entity Y position is multiplied by
	 * value returned by this method.
	 * <p>
	 * If this method returns any real number - then the void fog factor can be <= 0.
	 * But if this method returns NaN - the result is always NaN. And Minecraft enables void fog only of the value is < 1.
	 * And since any comparison with NaN returns false - void fog is effectively disabled.
	 */
	@Override
	public double voidFadeMagnitude() {
		return Double.NaN;
	}

	public static void create() {
		new VanillaCubicChunksWorldType();
	}
}
