/*******************************************************************************
 * Copyright (c) 2014 Nick Whitney.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Nick Whitney - initial implementation and adaptation from libnoise.
 ******************************************************************************/
package main.java.cubicchunks.generator.builder;

import libnoiseforjava.module.ModuleBase;
import libnoiseforjava.module.Perlin;
import libnoiseforjava.module.ScaleBias;

public class BasicBuilder implements IBuilder
{	
	ModuleBase finalModule;

	// Planet seed.  Change this to generate a different planet.
	int SEED = 0;

	// Minimum elevation, in meters.  This value is approximate.
	double MIN_ELEV = -8192.0;

	// Maximum elevation, in meters.  This value is approximate.
	double MAX_ELEV = 8192.0;

	// Specifies the sea level.  This value must be between -1.0
	// (minimum elevation) and +1.0 (maximum planet elevation.)
	double SEA_LEVEL = 0.0;
	
	int NUM_OCTAVES = 10;
	
	@Override
	public void setSeed(int seed)
	{
		this.SEED = seed;	
	}
	
	public void setMinElev(double minElev)
	{
		this.MIN_ELEV = minElev;	
	}
	
	public void setMaxElev(double maxElev)
	{
		this.MAX_ELEV = maxElev;	
	}

	public void setSeaLevel(double seaLevel)
	{
		this.SEA_LEVEL = seaLevel;	
	}
	
	public void setOctaves(int numOctaves)
	{
		this.NUM_OCTAVES = numOctaves;	
	}

	@Override
	public void build()
	{
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
//		finalModule = baseContinentDef_pe0;
	}

	@Override
	public double getValue(double x, double y, double z)
	{
		return finalModule.getValue(x, y, z);
	}

}
