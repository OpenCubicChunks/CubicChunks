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
package cubicchunks.image;

import java.util.Random;

import libnoiseforjava.module.Simplex;

public class ImageWriterMain {
	
	public static void main(String[] args) {
		Random rand = new Random();
		int rnd = rand.nextInt();
		Simplex baseContinentDef_pe0 = new Simplex();
		baseContinentDef_pe0.setSeed(0);
		baseContinentDef_pe0.setFrequency(1.0);
		baseContinentDef_pe0.setPersistence(0.5);
		baseContinentDef_pe0.setLacunarity(2.2089);
		baseContinentDef_pe0.setOctaveCount(14);
		baseContinentDef_pe0.build();
		
		double xStart = 0;
		double xEnd = 960;
		double yStart = 0;
		double yEnd = 540;
		
		int xResolution = 1920;
		int yResolution = 1080;
		
		double[][] result = new double[xResolution][yResolution];
		
		for (int i = 0; i < xResolution; i++) {
			for (int j = 0; j < yResolution; j++) {
				int x = (int) (xStart + i * ( (xEnd - xStart) / xResolution));
				int y = (int) (yStart + j * ( (yEnd - yStart) / yResolution));
				result[i][j] = 0.5 * (1 + baseContinentDef_pe0.getValue(x, y, 0));
			}
		}
		
		ImageWriter.greyWriteImage(result);
	}
}