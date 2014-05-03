package cuchaz.cubicChunks.image;
import java.util.Random;

import cuchaz.cubicChunks.gen.procedural.OctaveNoise;

public class ImageWriterMain
{
	public static void main(String[] args)
	{
		Random rand = new Random();
		int rnd = rand.nextInt();
	    OctaveNoise octaveNoise = new OctaveNoise(16, 0.8, rnd);

	    double xStart = 0;
	    double xEnd = 960;
	    double yStart = 0;
	    double yEnd = 540;

	    int xResolution = 1920;
	    int yResolution = 1080;

	    double[][] result = new double[xResolution][yResolution];

	    for(int i = 0; i < xResolution; i++)
	    {
	        for(int j = 0; j < yResolution; j++)
	        {
	            int x = (int) (xStart + i * ((xEnd - xStart) / xResolution));
	            int y = (int) (yStart + j * ((yEnd - yStart) / yResolution));
	            result[i][j] = 0.5 * (1 + octaveNoise.getNoise(x, y));
	        }
	    }
	    
	    ImageWriter.greyWriteImage(result);
	}
}
