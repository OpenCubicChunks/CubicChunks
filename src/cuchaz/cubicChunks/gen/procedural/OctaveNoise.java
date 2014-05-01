/*****************************************************************************
 * Copyright (c) Nick Whitney.                      
 *
 * This source is licensed under the GNU LGPL v3
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 * 
 * Based on the code by Richard Tingle.
 *
 ****************************************************************************/
package cuchaz.cubicChunks.gen.procedural;

import java.util.Random;

public class OctaveNoise
{
	private SimplexNoise[] octaves;
	double[] frequencies;
	double[] amplitudes;
	
	double persistence;
	double lacunarity = 2; // could use either 2, 1.8715, or 2.1042
	int seed;
	
	/**
	 * Creates a Noise generator for 2d through 4d noise.
	 * 
	 * @param numberOfOctaves - Number of Octaves. Terrain should be ~16
	 * @param persistence - varies the roughness of the terrain. 0.1 is smooth, 0.9 is rough. Range is 0 to 1.
	 * @param seed - Seed for the generator. Should use WorldSeed.
	 */
	public OctaveNoise(int numberOfOctaves, double persistence, int seed)
	{
		this.persistence = persistence;
		this.seed = seed;
		
		octaves = new SimplexNoise[numberOfOctaves];
		frequencies = new double[numberOfOctaves];
		amplitudes = new double[numberOfOctaves];
		
		Random rnd  = new Random(seed);
		
		for(int i=0;i<numberOfOctaves;i++)
		{
			octaves[i]=new SimplexNoise(rnd.nextInt());

            frequencies[i] = Math.pow(lacunarity,i);
            amplitudes[i] = Math.pow(persistence, octaves.length - i);
		}
	}
	
	public double getNoise(int x, int y)
	{
		double result = 0;
		
		for(int i = 0; i < octaves.length; i++)
		{
			result = result + octaves[i].noise(x/frequencies[i], y/frequencies[i])* amplitudes[i];
		}
		
		return result;
	}
	
	public double getNoise(int x, int y, int z)
	{
		double result = 0;
		
		for(int i = 0; i < octaves.length; i++)
		{
			result = result + octaves[i].noise(x/frequencies[i],  y/frequencies[i], z/frequencies[i]) * amplitudes[i];
		}
		
		return result;
	}
	
	public double getNoise(int x, int y, int z, int w)
	{
		double result = 0;
		
		for (int i = 0; i < octaves.length; i ++)
		{
			result = result + octaves[i].noise(x/frequencies[i], y/frequencies[i], z/frequencies[i], w/frequencies[i]) * amplitudes[i];
		}
		
		return result;
	}
}
