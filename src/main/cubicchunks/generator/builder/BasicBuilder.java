/*
 *  This file is part of Cubic Chunks, licensed under the MIT License (MIT).
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


public class BasicBuilder implements IBuilder {
	
	ModuleBase finalModule;
	
	// Planet seed. Change this to generate a different planet.
	int SEED = 0;
	
	// Minimum elevation, in meters. This value is approximate.
	double MIN_ELEV = -8192.0;
	
	// Maximum elevation, in meters. This value is approximate.
	double MAX_ELEV = 8192.0;
	
	// Specifies the sea level. This value must be between -1.0
	// (minimum elevation) and +1.0 (maximum planet elevation.)
	double SEA_LEVEL = 0.0;
	
	int NUM_OCTAVES = 10;
	
	@Override
	public void setSeed(int seed) {
		this.SEED = seed;
	}
	
	public void setMinElev(double minElev) {
		this.MIN_ELEV = minElev;
	}
	
	public void setMaxElev(double maxElev) {
		this.MAX_ELEV = maxElev;
	}
	
	public void setSeaLevel(double seaLevel) {
		this.SEA_LEVEL = seaLevel;
	}
	
	public void setOctaves(int numOctaves) {
		this.NUM_OCTAVES = numOctaves;
	}
	
	@Override
	public void build() {
		Perlin perlin = new Perlin();
		perlin.setSeed(SEED);
		perlin.setFrequency(1.0);
		perlin.setPersistence(0.5);
		perlin.setLacunarity(2.2089);
		perlin.setOctaveCount(NUM_OCTAVES);
		perlin.build();
		
		ScaleBias scaleBias = new ScaleBias(perlin);
		scaleBias.setScale(MAX_ELEV);
		scaleBias.setBias(SEA_LEVEL);
		
		finalModule = scaleBias;
		// finalModule = baseContinentDef_pe0;
	}
	
	@Override
	public double getValue(double x, double y, double z) {
		return finalModule.getValue(x, y, z);
	}
	
}
