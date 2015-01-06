package main.java.cubicchunks.generator.noise;

import java.util.Random;

public class NoiseGeneratorPerlin extends NoiseGenerator
{
    private NoiseGeneratorSimplex[] generators;
    private int numOctaves;

    public NoiseGeneratorPerlin(Random rand, int octaves)
    {
        this.numOctaves = octaves;
        this.generators = new NoiseGeneratorSimplex[octaves];

        for (int var3 = 0; var3 < octaves; ++var3)
        {
            this.generators[var3] = new NoiseGeneratorSimplex(rand);
        }
    }

    public double noise2D(double xOffset, double zOffset)
    {
        double result = 0.0D;
        double frequency = 1.0D;

        for (int octave = 0; octave < this.numOctaves; ++octave)
        {
            result += this.generators[octave].noise2D(xOffset * frequency, zOffset * frequency) / frequency;
            frequency /= 2.0D;
        }

        return result;
    }

    public double[] arrayNoise2D_pre(double[] p_151599_1_, double p_151599_2_, double p_151599_4_, int p_151599_6_, int p_151599_7_, double p_151599_8_, double p_151599_10_, double p_151599_12_)
    {
        return this.arrayNoise2D(p_151599_1_, p_151599_2_, p_151599_4_, p_151599_6_, p_151599_7_, p_151599_8_, p_151599_10_, p_151599_12_, 0.5D);
    }

    public double[] arrayNoise2D(double[] noiseArray, double xOffset, double zOffset, int xSize, int zSize, double xScale, double zScale, double lacunarity, double gain)
    {
        if (noiseArray != null && noiseArray.length >= xSize * zSize)
        {
            for (int i = 0; i < noiseArray.length; ++i) // clear the array
            {
                noiseArray[i] = 0.0D;
            }
        }
        else
        {
            noiseArray = new double[xSize * zSize];
        }

        double amplitude = 1.0D;
        double frequency = 1.0D;

        for (int octave = 0; octave < this.numOctaves; ++octave)
        {
            this.generators[octave].arrayNoise2D(noiseArray, xOffset, zOffset, xSize, zSize, xScale * frequency * amplitude, zScale * frequency * amplitude, 0.55D / amplitude);
            frequency *= lacunarity;
            amplitude *= gain;
        }

        return noiseArray;
    }
}
