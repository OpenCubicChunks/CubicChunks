/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 * 
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package cubicchunks.image;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageWriter {
	
	// just convenience methods for debug
	
	public static void greyWriteImage(double[][] data) {
		// this takes and array of doubles between 0 and 1 and generates a grey scale image from them
		
		BufferedImage image = new BufferedImage(data.length, data[0].length, BufferedImage.TYPE_INT_RGB);
		
		for (int y = 0; y < data[0].length; y++) {
			for (int x = 0; x < data.length; x++) {
				if (data[x][y] > 1) {
					data[x][y] = 1;
				}
				if (data[x][y] < 0) {
					data[x][y] = 0;
				}
				
				Color col = new Color((float)data[x][y], (float)data[x][y], (float)data[x][y]);
				image.setRGB(x, y, col.getRGB());
			}
		}
		
		try {
			// retrieve image
			File outputfile = new File("saved.png");
			outputfile.createNewFile();
			
			ImageIO.write(image, "png", outputfile);
		} catch (IOException e) {
			// o no! Blank catches are bad
			throw new RuntimeException("I didn't handle this very well");
		}
	}
}
