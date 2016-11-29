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
package cubicchunks.worldgen.generator.custom.builder;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static cubicchunks.util.MathUtil.lerp;
import static java.lang.Math.max;
import static java.lang.Math.min;

class ScalingLerpIBuilderIterator implements Iterator<IBuilder.IExtendedEntry> {
	// TODO: explain how it works
	private final int minX;
	private final int minY;
	private final int minZ;
	private final int maxX;
	private final int maxY;
	private final int maxZ;

	private final double xStep;
	private final double yStep;
	private final double zStep;

	private final int minGridX;
	private final int minGridY;
	private final int minGridZ;
	private final int maxGridX;
	private final int maxGridY;
	private final int maxGridZ;

	private final int scaleX;
	private final int scaleY;
	private final int scaleZ;

	private final IBuilder builder;

	private int nextGridX;
	private int nextGridY;
	private int nextGridZ;

	private int nextRelX;
	private int nextRelY;
	private int nextRelZ;

	private double v000, v001, v010, v011, v100, v101, v110, v111;
	private double vx00, vx01, vx10, vx11;
	private double vxy0, vxy1;
	private double vxyz;

	private double dx00, dx01, dx10, dx11;
	private double dxy0, dxy1;
	private double dxyz;

	ScalingLerpIBuilderIterator(IBuilder builder, Vec3i start, Vec3i end, Vec3i scale) {
		this.builder = builder;
		this.scaleX = scale.getX();
		this.scaleY = scale.getY();
		this.scaleZ = scale.getZ();

		minX = min(start.getX(), end.getX());
		minY = min(start.getY(), end.getY());
		minZ = min(start.getZ(), end.getZ());
		maxX = max(start.getX(), end.getX());
		maxY = max(start.getY(), end.getY());
		maxZ = max(start.getZ(), end.getZ());

		minGridX = MathHelper.intFloorDiv(minX, scaleX);
		minGridY = MathHelper.intFloorDiv(minY, scaleY);
		minGridZ = MathHelper.intFloorDiv(minZ, scaleZ);
		maxGridX = MathHelper.intFloorDiv(maxX, scaleX);
		maxGridY = MathHelper.intFloorDiv(maxY, scaleY);
		maxGridZ = MathHelper.intFloorDiv(maxZ, scaleZ);

		nextGridX = minGridX;
		nextGridY = minGridY;
		nextGridZ = minGridZ;

		xStep = 1.0/scaleX;
		yStep = 1.0/scaleY;
		zStep = 1.0/scaleZ;

		nextRelX = 0;
		nextRelY = 0;
		nextRelZ = 0;
	}

	@Override public boolean hasNext() {
		return nextGridX <= maxGridX;
	}

	@Override public IBuilder.IExtendedEntry next() throws NoSuchElementException {
		checkHasNext();
		if (nextRelZ == 0) {
			if (nextRelY == 0) {
				if (nextRelX == 0) {
					onResetX();
				}
				onResetY();
			}
			onResetZ();
		}
		vxyz += dxyz;

		//values needed to calculate gradient vector
		double v00z = lerp(nextRelZ*zStep, v000, v001);
		double v01z = lerp(nextRelZ*zStep, v010, v011);
		double v10z = lerp(nextRelZ*zStep, v100, v101);
		double v11z = lerp(nextRelZ*zStep, v110, v111);

		double v0yz = lerp(nextRelY*yStep, v00z, v01z);
		double v1yz = lerp(nextRelY*yStep, v10z, v11z);

		double vx0z = lerp(nextRelX*xStep, v00z, v10z);
		double vx1z = lerp(nextRelX*xStep, v01z, v11z);

		//calculate gradient vector
		double xGrad = (v1yz - v0yz)*xStep;
		double yGrad = (vx1z - vx0z)*yStep;
		double zGrad = (vxy1 - vxy0)*zStep;

		IBuilder.IExtendedEntry entry = new IBuilder.ImmutbleExtendedEntry(
			global(nextGridX, scaleX, nextRelX),
			global(nextGridY, scaleY, nextRelY),
			global(nextGridZ, scaleZ, nextRelZ),
			vxyz, xGrad, yGrad, zGrad
		);

		incrementPos();
		return entry;
	}

	private void checkHasNext() {
		if (!hasNext()) {
			throw new NoSuchElementException();
		}
	}

	private void onResetX() {
		nextRelX = boundClampRelPosMin(nextGridX, scaleX, minX);

		// get corners
		v000 = builder.get(nextGridX, nextGridY, nextGridZ);
		v001 = builder.get(nextGridX, nextGridY, nextGridZ + 1);
		v010 = builder.get(nextGridX, nextGridY + 1, nextGridZ);
		v011 = builder.get(nextGridX, nextGridY + 1, nextGridZ + 1);
		v100 = builder.get(nextGridX + 1, nextGridY, nextGridZ);
		v101 = builder.get(nextGridX + 1, nextGridY, nextGridZ + 1);
		v110 = builder.get(nextGridX + 1, nextGridY + 1, nextGridZ);
		v111 = builder.get(nextGridX + 1, nextGridY + 1, nextGridZ + 1);

		// step 1
		dx00 = (v100 - v000)*xStep;
		dx01 = (v101 - v001)*xStep;
		dx10 = (v110 - v010)*xStep;
		dx11 = (v111 - v011)*xStep;

		vx00 = v000 + dx00*nextRelX;
		vx01 = v001 + dx01*nextRelX;
		vx10 = v010 + dx10*nextRelX;
		vx11 = v011 + dx11*nextRelX;
	}

	private void onResetY() {
		nextRelY = boundClampRelPosMin(nextGridY, scaleY, minY);
		// step 2
		dxy0 = (vx10 - vx00)*yStep;
		dxy1 = (vx11 - vx01)*yStep;

		vxy0 = vx00 + dxy0*nextRelY;
		vxy1 = vx01 + dxy1*nextRelY;

		vx00 += dx00;
		vx01 += dx01;
		vx10 += dx10;
		vx11 += dx11;
	}

	private void onResetZ() {
		nextRelZ = boundClampRelPosMin(nextGridZ, scaleZ, minZ);

		dxyz = (vxy1 - vxy0)*zStep;

		vxyz = vxy0 + dxyz*nextRelZ - dxyz;

		vxy0 += dxy0;
		vxy1 += dxy1;
	}

	private void incrementPos() {
		nextRelZ++;
		if (nextRelZ >= scaleZ || global(nextGridZ, scaleZ, nextRelZ) > maxZ) {
			nextRelZ = 0;
			nextRelY++;
			if (nextRelY >= scaleY || global(nextGridY, scaleY, nextRelY) > maxY) {
				nextRelY = 0;
				nextRelX++;
				if (nextRelX >= scaleX || global(nextGridX, scaleX, nextRelX) > maxX) {
					nextRelX = 0;
					incrementGridPos();
				}
			}
		}
	}

	private void incrementGridPos() {
		nextGridZ++;
		if (nextGridZ > maxGridZ) {
			nextGridZ = minGridZ;
			nextGridY++;
			if (nextGridY > maxGridY) {
				nextGridY = minGridY;
				nextGridX++;
			}
		}
	}

	private static int boundClampRelPosMin(int gridPos, int scale, int minPos) {
		int globPos = gridPos*scale;//+localPos
		if (globPos < minPos) {
			return minPos - globPos;
		}
		return 0;
	}

	private static int global(int gridPos, int scale, int relPos) {
		return gridPos*scale + relPos;
	}
}
