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
package cubicchunks.worldgen.builder;

import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import mcp.MethodsReturnNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ScalingForEachVanillaNoGradient {
	private final double[] heightMap;
	private final OldScalingIterator.MutableExtendedEntry entry;

	public ScalingForEachVanillaNoGradient() {
		this.heightMap = new double[75];
		this.entry = new OldScalingIterator.MutableExtendedEntry();
	}

	public void forEach(Consumer<OldScalingIterator.IExtendedEntry> consumer) {
		for (int sectionX = 0; sectionX < 4; ++sectionX) {
			int xSectionPart = sectionX*4;
			int i0__ = sectionX*5;
			int i1__ = (sectionX + 1)*5;

			for (int sectionZ = 0; sectionZ < 4; ++sectionZ) {
				int zSectionPart = sectionZ*4;
				int i0_0 = (i0__ + sectionZ)*3;
				int i0_1 = (i0__ + sectionZ + 1)*3;
				int i1_0 = (i1__ + sectionZ)*3;
				int i1_1 = (i1__ + sectionZ + 1)*3;

				for (int sectionY = 0; sectionY < 2; ++sectionY) {
					int ySectionPart = sectionY*8;
					double v0y0 = this.heightMap[i0_0 + sectionY];
					double v0y1 = this.heightMap[i0_1 + sectionY];
					double v1y0 = this.heightMap[i1_0 + sectionY];
					double v1y1 = this.heightMap[i1_1 + sectionY];
					double d0y0 = (this.heightMap[i0_0 + sectionY + 1] - v0y0)*0.125D;
					double d0y1 = (this.heightMap[i0_1 + sectionY + 1] - v0y1)*0.125D;
					double d1y0 = (this.heightMap[i1_0 + sectionY + 1] - v1y0)*0.125D;
					double d1y1 = (this.heightMap[i1_1 + sectionY + 1] - v1y1)*0.125D;

					for (int yRel = 0; yRel < 8; ++yRel) {
						int yCoord = ySectionPart + yRel;
						double vxy0 = v0y0;
						double vxy1 = v0y1;
						double dxy0 = (v1y0 - v0y0)*0.25D;
						double dxy1 = (v1y1 - v0y1)*0.25D;

						for (int xRel = 0; xRel < 4; ++xRel) {
							int xCoord = xSectionPart + xRel;
							double dxyz = (vxy1 - vxy0)*0.25D;
							double vxyz = vxy0 - dxyz;

							for (int zRel = 0; zRel < 4; ++zRel) {
								int zCoord = zSectionPart + zRel;
								entry.setX(xCoord);
								entry.setY(yCoord);
								entry.setZ(zCoord);
								entry.setValue(vxyz);
								consumer.accept(entry);
							}

							vxy0 += dxy0;
							vxy1 += dxy1;
						}

						v0y0 += d0y0;
						v0y1 += d0y1;
						v1y0 += d1y0;
						v1y1 += d1y1;
					}
				}
			}
		}
	}
}
