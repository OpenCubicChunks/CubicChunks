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
		private Perlin perlin = new Perlin();
		private ScalePoint scaled = new ScalePoint();

		public PerlinBuilder seed(long seed) {
			perlin.setSeed((int) ((seed & 0xFFFFFFFF) ^ (seed >>> 32)));
			return this;
		}

		public PerlinBuilder frequency(double fx, double fy, double fz) {
			scaled.setXScale(fx);
			scaled.setYScale(fy);
			scaled.setZScale(fz);
			return this;
		}

		public PerlinBuilder frequency(double f) {
			return frequency(f, f, f);
		}

		public PerlinBuilder octaves(int count) {
			perlin.setOctaveCount(count);
			return this;
		}

		public NoiseSource create() {
			scaled.setSourceModule(0, perlin);
			return new NoiseSource(scaled);
		}
	}
}
