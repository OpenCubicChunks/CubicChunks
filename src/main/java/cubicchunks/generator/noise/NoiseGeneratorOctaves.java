package main.java.cubicchunks.generator.noise;

import java.util.Random;
import net.minecraft.util.MathHelper;

public class NoiseGeneratorOctaves extends NoiseGenerator
{
    /**
     * Collection of noise generation functions.  Output is combined to produce different octaves of noise.
     */
    private NoiseGeneratorImproved[] generatorCollection;
    private int octaves;

    public NoiseGeneratorOctaves(Random rand, int numOctaves)
    {
        this.octaves = numOctaves;
        this.generatorCollection = new NoiseGeneratorImproved[numOctaves];

        for (int var3 = 0; var3 < numOctaves; ++var3)
        {
            this.generatorCollection[var3] = new NoiseGeneratorImproved(rand);
        }
    }

    public double[] generateNoiseOctaves(double[] noiseArray, int noiseX, int noiseY, int noiseZ, int xSize, int ySize, int zSize, double xScale, double yScale, double zScale)
    {
        if (noiseArray == null) // if noise array doesn't exist, create it
        {
            noiseArray = new double[xSize * ySize * zSize];
        }
        else // clear the array if it already exists
        {
            for (int i = 0; i < noiseArray.length; ++i)
            {
                noiseArray[i] = 0.0D;
            }
        }

        double frequency = 1.0D;

        for (int curOctave = 0; curOctave < this.octaves; ++curOctave)
        {
            double xValue = (double)noiseX * frequency * xScale;
            double yValue = (double)noiseY * frequency * yScale;
            double zValue = (double)noiseZ * frequency * zScale;
            
            long xLong = MathHelper.floor_double_long(xValue); // convert the double to a long int and floor it
            long zLong = MathHelper.floor_double_long(zValue);
            
            xValue -= (double)xLong; // subtract the double-cast long from the double??? This should get the decimal portion of xValue
            zValue -= (double)zLong;
            xLong %= 16777216L; // binary select the long
            zLong %= 16777216L;
            xValue += (double)xLong; // add the double-cast long after the binary select to the double
            zValue += (double)zLong;
            
            this.generatorCollection[curOctave].populateNoiseArray(noiseArray, xValue, yValue, zValue, xSize, ySize, zSize, xScale * frequency, yScale * frequency, zScale * frequency, frequency);
            frequency /= 2.0D;
        }

        return noiseArray;
    }

    /**
     * Bouncer function to the main one with some default arguments.
     */
    public double[] generateNoiseOctaves(double[] par1ArrayOfDouble, int par2, int par3, int par4, int par5, double par6, double par8, double par10)
    {
        return this.generateNoiseOctaves(par1ArrayOfDouble, par2, 10, par3, par4, 1, par5, par6, 1.0D, par8);
    }
}
