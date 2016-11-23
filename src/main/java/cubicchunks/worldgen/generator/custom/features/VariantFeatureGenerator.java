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

import net.minecraft.world.biome.Biome;

import org.eclipse.collections.api.list.primitive.DoubleList;
import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class VariantFeatureGenerator extends FeatureGenerator {

	@Nonnull private final FeatureGenerator[] generators;
	@Nonnull private final double[] probabilities;

	private VariantFeatureGenerator(ICubicWorld world, List<FeatureGenerator> generators, DoubleList probabilities) {
		super(world);
		assert generators.size() == probabilities.size();
		int size = generators.size();
		this.generators = new FeatureGenerator[size];
		this.probabilities = new double[size];
		for (int i = 0; i < size; i++) {
			this.generators[i] = generators.get(i);
			this.probabilities[i] = probabilities.get(i);
		}
	}

	@Override
	public void generate(Random rand, Cube cube, Biome biome) {
		for (int i = 0; i < probabilities.length; i++) {
			if (rand.nextDouble() <= this.probabilities[i]) {
				this.generators[i].generate(rand, cube, biome);
				break;
			}
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		// use lists because order is important
		@Nonnull private final List<FeatureGenerator> generators;
		@Nonnull private final DoubleArrayList probabilities;
		@Nullable private ICubicWorld world;

		private Builder() {
			this.generators = new ArrayList<FeatureGenerator>(2);
			this.probabilities = new DoubleArrayList(2);
		}

		@Nonnull public Builder world(ICubicWorld world) {
			this.world = world;
			return this;
		}

		@Nonnull public Builder nextVariant(FeatureGenerator gen, double probability) {
			this.generators.add(gen);
			this.probabilities.add(probability);
			assert this.generators.size() == this.probabilities.size();
			return this;
		}

		@Nonnull public VariantFeatureGenerator build() {
			return new VariantFeatureGenerator(this.world, this.generators, this.probabilities);
		}
	}
}
