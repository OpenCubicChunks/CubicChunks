/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
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
package cubicchunks.worldgen.generator.custom.builder.testing;

import net.minecraft.util.math.Vec3i;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import cubicchunks.worldgen.generator.custom.builder.NoiseSource;

/**
 * Writes noise generator output to images to confirm that it works correctly
 */
public class ImageOutput {

	public static void main(String... args) throws IOException {
		new ImageOutput().writeOutput();
	}

	private void writeColor(BufferedImage valImg, int x, int y, double value) {
		Color col;
		int i = (int) ((value/2 + 0.5)*255);
		if (i < 0) {
			col = Color.RED;
		} else if (i > 255) {
			col = Color.BLUE;
		} else {
			col = new Color(i, i, i);
		}
		valImg.setRGB(x, y, col.getRGB());
	}

	public void writeOutput() throws IOException {
		int w = 512, h = 512;
		BufferedImage valImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		BufferedImage dxImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		BufferedImage dyImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		BufferedImage dzImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

		BufferedImage dxCorrectApprox = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		BufferedImage dyCorrectApprox = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		BufferedImage dzCorrectApprox = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		double[][][] values = new double[w][2][h];
		double[][][] dxArr = new double[w][2][h];
		double[][][] dyArr = new double[w][2][h];
		double[][][] dzArr = new double[w][2][h];
		NoiseSource.perlin()
			.seed(42)
			.frequency(0.003, 0.003, 0.003)
			.normalizeTo(-1, 1)
			.octaves(4)
			.create()
			.forEachScaled(
				new Vec3i(0, 0, 0), new Vec3i(w/64, 1, h/64), new Vec3i(64, 64, 64),
				(x, y, z, dx, dy, dz, v) -> {
					if (y != 0 && y != 1) {
						return;
					}
					values[x][y][z] = v;
					dxArr[x][y][z] = dx;
					dyArr[x][y][z] = dy;
					dzArr[x][y][z] = dz;

					if (y != 0) {
						return;
					}
					writeColor(valImg, x, z, v);
					writeColor(dxImg, x, z, dx*300);
					writeColor(dyImg, x, z, dy*100);
					writeColor(dzImg, x, z, dz*300);
				});


		int y = 0;
		for (int x = 0; x < values.length - 1; x++) {
			for (int z = 0; z < values[x][0].length - 1; z++) {
				double v = values[x][y][z];
				double vx = values[x + 1][y][z];
				double vy = values[x][y + 1][z];
				double vz = values[x][y][z + 1];

				writeColor(dxCorrectApprox, x, z, (vx - v)*300);
				writeColor(dyCorrectApprox, x, z, (vy - v)*100);
				writeColor(dzCorrectApprox, x, z, (vz - v)*300);

			}
		}

		ImageIO.write(valImg, "PNG", new File("values.png"));
		ImageIO.write(dxImg, "PNG", new File("dx_to_test.png"));
		ImageIO.write(dyImg, "PNG", new File("dy_to_test.png"));
		ImageIO.write(dzImg, "PNG", new File("dz_to_test.png"));
		ImageIO.write(dxCorrectApprox, "PNG", new File("dx_correct_approx.png"));
		ImageIO.write(dyCorrectApprox, "PNG", new File("dy_correct_approx.png"));
		ImageIO.write(dzCorrectApprox, "PNG", new File("dz_correct_approx.png"));
	}
}
