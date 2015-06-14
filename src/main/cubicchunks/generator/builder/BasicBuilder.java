/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2014 Tall Worlds
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
package cubicchunks.generator.builder;

import com.flowpowered.noise.module.Module;
import com.flowpowered.noise.module.modifier.Clamp;
import com.flowpowered.noise.module.modifier.ScaleBias;
import com.flowpowered.noise.module.modifier.ScalePoint;
import com.flowpowered.noise.module.source.Perlin;

@SuppressWarnings("unused")
public class BasicBuilder implements IBuilder {
	protected Module finalModule;

	// Planet seed. Change this to generate a different planet.
	int SEED = 0;

	// Maximum elevation, in meters. This value is approximate.
	double MAX_ELEV = 8192.0;

	// Specifies the sea level. This value must be between -1.0
	// (minimum elevation) and +1.0 (maximum planet elevation.)
	double SEA_LEVEL = 0.0;

	int NUM_OCTAVES = 10;

	double SCALE_X = 1;
	double SCALE_Y = 1;
	double SCALE_Z = 1;

	double persistance = 0.5;
	double clampMin = -Double.MAX_VALUE, clampMax = Double.MAX_VALUE;
	double lacunarity = 2;
	private double scaleOctaves;

	@Override
	public void setSeed(int seed) {
		this.SEED = seed;
	}

	public void setMaxElev(double maxElev) {
		this.MAX_ELEV = maxElev;
	}

	@Override
	public void setSeaLevel(double seaLevel) {
		this.SEA_LEVEL = seaLevel;
		clampMin = -MAX_ELEV + SEA_LEVEL;
		clampMax = MAX_ELEV + SEA_LEVEL;
	}

	public void setOctaves(int numOctaves) {
		this.NUM_OCTAVES = numOctaves;
		this.scaleOctaves = 1 / (2 - Math.pow(2, numOctaves - 1));
	}

	public void setFreq(double scale) {
		this.SCALE_X = this.SCALE_Y = this.SCALE_Z = scale;
	}

	public void setFreq(double scaleX, double scaleY, double scaleZ) {
		this.SCALE_X = scaleX;
		this.SCALE_Y = scaleY;
		this.SCALE_Z = scaleZ;
	}

	public void setPersistance(double p) {
		this.persistance = p;
	}

	public void setlacunarity(double l) {
		this.lacunarity = l;
	}

	public void setClamp(double min, double max) {
		this.clampMin = min;
		this.clampMax = max;
	}

	@Override
	public void build() {
		Perlin perlin = new Perlin();
		perlin.setSeed(SEED);
		perlin.setFrequency(1.0);
		perlin.setPersistence(persistance);
		perlin.setLacunarity(lacunarity);
		perlin.setOctaveCount(NUM_OCTAVES);
//		perlin.build();

		ScalePoint scalePoint = new ScalePoint();
		scalePoint.setSourceModule(0, perlin);
		scalePoint.setXScale(SCALE_X);
		scalePoint.setYScale(SCALE_Y);
		scalePoint.setZScale(SCALE_Z);

		ScaleBias scaleBias = new ScaleBias();
		scaleBias.setSourceModule(0, scalePoint);
		// Max perlin noise value with N octaves and persistance p is
		// 1 + 1/p + 1/(p^2) + ... + 1/(p^(N-1))
		// It's equal to (1 - p^N) / (1 - p)
		// Divide result by it to make sure that result is between -1 and 1
		scaleBias.setScale(MAX_ELEV * (1 - persistance) / (1 - Math.pow(persistance, NUM_OCTAVES)));
		scaleBias.setBias(SEA_LEVEL);

		Clamp clamp = new Clamp();
		clamp.setSourceModule(0, scaleBias);
		clamp.setLowerBound(clampMin);
		clamp.setUpperBound(clampMax);

		finalModule = clamp;
	}

	@Override
	public double getValue(double x, double y, double z) {
		return finalModule.getValue(x, y, z);
	}
}