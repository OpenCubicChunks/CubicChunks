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
package cubicchunks.visibility;

import cubicchunks.util.AddressTools;
import gnu.trove.set.TLongSet;

import java.util.function.Consumer;

import static cubicchunks.util.AddressTools.getAddress;

public class CuboidalCubeSelector extends CubeSelector {

	@Override
	public void forAllVisibleFrom(long cubeAddress, int horizontalViewDistance, int verticalViewDistance, Consumer<Long> consumer) {
		int cubeX = AddressTools.getX(cubeAddress);
		int cubeY = AddressTools.getY(cubeAddress);
		int cubeZ = AddressTools.getZ(cubeAddress);
		for (int x = cubeX - horizontalViewDistance; x <= cubeX + horizontalViewDistance; x++) {
			for (int y = cubeY - verticalViewDistance; y <= cubeY + verticalViewDistance; y++) {
				for (int z = cubeZ - horizontalViewDistance; z <= cubeZ + horizontalViewDistance; z++) {
					consumer.accept(getAddress(x, y, z));
				}
			}
		}
	}

	@Override
	public void findChanged(long oldAddress, long newAddress, int horizontalViewDistance, int verticalViewDistance, TLongSet cubesToRemove, TLongSet cubesToLoad, TLongSet columnsToRemove, TLongSet columnsToLoad) {
		int oldX = AddressTools.getX(oldAddress);
		int oldY = AddressTools.getY(oldAddress);
		int oldZ = AddressTools.getZ(oldAddress);
		int newX = AddressTools.getX(newAddress);
		int newY = AddressTools.getY(newAddress);
		int newZ = AddressTools.getZ(newAddress);
		int dx = newX - oldX;
		int dy = newY - oldY;
		int dz = newZ - oldZ;

		if (dx != 0 || dy != 0 || dz != 0) {
			for (int currentX = newX - horizontalViewDistance; currentX <= newX + horizontalViewDistance; ++currentX) {
				for (int currentZ = newZ - horizontalViewDistance; currentZ <= newZ + horizontalViewDistance; ++currentZ) {
					//first handle columns
					//is current position outside of the old render distance square?
					if (!this.isPointWithinCubeVolume(oldX, 0, oldZ, currentX, 0, currentZ, horizontalViewDistance, verticalViewDistance)) {
						columnsToLoad.add(getAddress(currentX, currentZ));
					}

					//if we moved the current point to where it would be previously,
					//would it be outside of current render distance square?
					if (!this.isPointWithinCubeVolume(newX, 0, newZ, currentX - dx, 0, currentZ - dz, horizontalViewDistance, verticalViewDistance)) {
						columnsToRemove.add(getAddress(currentX - dx, currentZ - dz));
					}
					for (int currentY = newY - verticalViewDistance; currentY <= newY + verticalViewDistance; ++currentY) {
						//now handle cubes, the same way
						if (!this.isPointWithinCubeVolume(oldX, oldY, oldZ, currentX, currentY, currentZ, horizontalViewDistance, verticalViewDistance)) {
							cubesToLoad.add(getAddress(currentX, currentY, currentZ));
							if (cubesToRemove.contains(getAddress(currentX, currentY, currentZ))) {
								int debugLine = 0;
							}
						}
						if (!this.isPointWithinCubeVolume(newX, newY, newZ,
								currentX - dx, currentY - dy, currentZ - dz, horizontalViewDistance, verticalViewDistance)) {
							cubesToRemove.add(getAddress(currentX - dx, currentY - dy, currentZ - dz));
							if (cubesToLoad.contains(getAddress(currentX - dx, currentY - dy, currentZ - dz))) {
								int debugLine = 0;
							}
						}
					}
				}
			}
		}
		assert cubesToLoad.forEach(addr -> !cubesToRemove.contains(addr)) : "cubesToRemove contains element from cubesToLoad!";
		assert columnsToLoad.forEach(addr -> !columnsToRemove.contains(addr)) : "columnsToRemove contains element from columnsToLoad!";
	}

	@Override
	public void findAllUnloadedOnViewDistanceDecrease(long playerAddress, int oldHorizontalViewDistance, int newHorizontalViewDistance, int oldVerticalViewDistance, int newVerticalViewDistance, TLongSet cubesToUnload, TLongSet columnsToUnload) {
		int playerCubeX = AddressTools.getX(playerAddress);
		int playerCubeY = AddressTools.getY(playerAddress);
		int playerCubeZ = AddressTools.getZ(playerAddress);

		for (int cubeX = playerCubeX - oldHorizontalViewDistance; cubeX <= playerCubeX + oldHorizontalViewDistance; cubeX++) {
			for (int cubeZ = playerCubeZ - oldHorizontalViewDistance; cubeZ <= playerCubeZ + oldHorizontalViewDistance; cubeZ++) {
				if (!isPointWithinCubeVolume(playerCubeX, 0, playerCubeZ, cubeX, 0, cubeZ, newHorizontalViewDistance, newVerticalViewDistance)) {
					columnsToUnload.add(getAddress(cubeX, cubeZ));
				}
				for (int cubeY = playerCubeY - oldVerticalViewDistance; cubeY <= playerCubeY + oldVerticalViewDistance; cubeY++) {
					if (!isPointWithinCubeVolume(playerCubeX, playerCubeY, playerCubeZ, cubeX, cubeY, cubeZ, newHorizontalViewDistance, newVerticalViewDistance)) {
						cubesToUnload.add(getAddress(cubeX, cubeY, cubeZ));
					}
				}
			}
		}
	}

	private boolean isPointWithinCubeVolume(int cubeX, int cubeY, int cubeZ, int pointX, int pointY, int pointZ, int horizontal, int vertical) {
		int dx = cubeX - pointX;
		int dy = cubeY - pointY;
		int dz = cubeZ - pointZ;
		return dx >= -horizontal && dx <= horizontal
				&& dy >= -vertical && dy <= vertical
				&& dz >= -horizontal && dz <= horizontal;
	}
}