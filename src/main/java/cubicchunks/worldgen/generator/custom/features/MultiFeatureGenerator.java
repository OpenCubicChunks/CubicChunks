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
package cubicchunks.worldgen.generator.custom.features;

import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import net.minecraft.world.biome.Biome;

import java.util.Random;

public class MultiFeatureGenerator extends FeatureGenerator {
	private final FeatureGenerator gen;
	private final int attempts;

	public MultiFeatureGenerator(ICubicWorld world, FeatureGenerator gen, int attempts) {
		super(world);
		this.gen = gen;
		this.attempts = attempts;
	}

	@Override
	public void generate(Random rand, Cube cube, Biome biome) {
		for (int i = 0; i < this.attempts; i++) {
			this.gen.generate(rand, cube, biome);
		}
	}
}
