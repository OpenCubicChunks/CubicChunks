/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
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
