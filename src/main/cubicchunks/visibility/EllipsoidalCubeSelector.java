/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
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
package cubicchunks.visibility;

import java.util.Collection;

import cubicchunks.util.AddressTools;

public class EllipsoidalCubeSelector extends CubeSelector {
	
	private static final int SemiAxisY = 3;
	private static final int SemiAxisY2 = SemiAxisY * SemiAxisY;
	
	@Override
	protected void computeVisible(Collection<Long> out, int cubeX, int cubeY, int cubeZ, int viewDistance) {
		// equation for an axis-aligned ellipsoid:
		// x^2/a^2 + y^2/b^2 + z^2/c^2 = 1
		// where a,b,c are the semi-principal axes
		int SemiAxisXZ = viewDistance;
		int SemiAxisXZ2 = SemiAxisXZ * SemiAxisXZ;
		
		for (int x = -SemiAxisXZ; x <= SemiAxisXZ; x++) {
			int x2 = x * x;
			for (int z = -SemiAxisXZ; z <= SemiAxisXZ; z++) {
				int z2 = z * z;
				int test = (x2 + z2 - SemiAxisXZ2) * SemiAxisY2;
				for (int y = -SemiAxisY; y <= SemiAxisY; y++) {
					int y2 = y * y;
					if (test <= -y2 * SemiAxisXZ2) // test for point in ellipsoid, but using only integer arithmetic
					{
						out.add(AddressTools.getAddress(x + cubeX, y + cubeY, z + cubeZ));
					}
				}
			}
		}
	}
}