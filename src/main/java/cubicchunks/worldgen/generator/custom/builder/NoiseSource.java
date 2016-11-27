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
package cubicchunks.worldgen.generator.custom.builder;

import com.flowpowered.noise.module.Module;
import com.flowpowered.noise.module.modifier.ScaleBias;
import com.flowpowered.noise.module.modifier.ScalePoint;
import com.flowpowered.noise.module.source.Perlin;

public class NoiseSource implements IBuilder {

	private Module module;

	public NoiseSource(Module module) {
		this.module = module;
	}

	@Override public double get(int x, int y, int z) {
		return module.getValue(x, y, z);
	}

	public static PerlinBuilder perlin() {
		return new PerlinBuilder();
	}

	public static class PerlinBuilder {
		private boolean normalized = false;
		private double minNorm, maxNorm;
		private double fx;
		private double fy;
		private double fz;
		private long seed;
		private int octaves;

		public PerlinBuilder seed(long seed) {
			this.seed = seed;
			return this;
		}

		public PerlinBuilder frequency(double fx, double fy, double fz) {
			this.fx = fx;
			this.fy = fy;
			this.fz = fz;
			return this;
		}

		public PerlinBuilder frequency(double f) {
			return frequency(f, f, f);
		}

		public PerlinBuilder octaves(int octaves) {
			this.octaves = octaves;
			return this;
		}

		public PerlinBuilder normalizeTo(double min, double max) {
			this.minNorm = min;
			this.maxNorm = max;
			normalized = true;
			return this;
		}

		public NoiseSource create() {
			Module mod;
			Perlin perlin = new Perlin();
			perlin.setSeed((int) ((seed & 0xFFFFFFFF) ^ (seed >>> 32)));
			perlin.setOctaveCount(octaves);
			mod = perlin;
			if (normalized) {
				// Max perlin noise value with N octaves and persistance p is
				// 1 + 1/p + 1/(p^2) + ... + 1/(p^(N-1))
				// It's equal to (1 - p^N) / (1 - p)
				// Divide result by it, multiply by 2 and subtract 1
				// to make sure that result is between -1 and 1 and that the mean value is at 0
				final double persistance = 0.5;
				ScaleBias scaleBias = new ScaleBias();
				scaleBias.setScale(2*(1 - persistance)/(1 - Math.pow(persistance, octaves)));
				scaleBias.setBias(-1);
				scaleBias.setSourceModule(0, mod);
				mod = scaleBias;

				scaleBias = new ScaleBias();
				scaleBias.setScale((maxNorm - minNorm)/2);
				scaleBias.setBias((maxNorm + minNorm)/2);
				scaleBias.setSourceModule(0, mod);
				mod = scaleBias;
			}
			ScalePoint scaled = new ScalePoint();
			scaled.setXScale(fx);
			scaled.setYScale(fy);
			scaled.setZScale(fz);
			scaled.setSourceModule(0, mod);
			mod = scaled;
			return new NoiseSource(mod);
		}
	}
}
