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
	public void forAllVisibleFrom(long cubeAddress, int viewDistance, Consumer<Long> consumer) {
		int cubeX = AddressTools.getX(cubeAddress);
		int cubeY = AddressTools.getY(cubeAddress);
		int cubeZ = AddressTools.getZ(cubeAddress);
		for (int x = cubeX - viewDistance; x <= cubeX + viewDistance; x++) {
			for (int y = cubeY - viewDistance; y <= cubeY + viewDistance; y++) {
				for (int z = cubeZ - viewDistance; z <= cubeZ + viewDistance; z++) {
					consumer.accept(getAddress(x, y, z));
				}
			}
		}
	}

	@Override
	public void findChanged(long oldAddress, long newAddress, int viewDistance, TLongSet cubesToRemove, TLongSet cubesToLoad, TLongSet columnsToRemove, TLongSet columnsToLoad) {
		int oldX = AddressTools.getX(oldAddress);
		int oldY = AddressTools.getY(oldAddress);
		int oldZ = AddressTools.getZ(oldAddress);
		int radius = viewDistance;
		int newX = AddressTools.getX(newAddress);
		int newY = AddressTools.getY(newAddress);
		int newZ = AddressTools.getZ(newAddress);
		int dx = newX - oldX;
		int dy = newY - oldY;
		int dz = newZ - oldZ;

		if (dx != 0 || dy != 0 || dz != 0) {
			for (int currentX = newX - radius; currentX <= newX + radius; ++currentX) {
				for (int currentZ = newZ - radius; currentZ <= newZ + radius; ++currentZ) {
					//first handle columns
					//is current position outside of the old render distance square?
					if (!this.isPointWithinCubeVolume(oldX, 0, oldZ, currentX, 0, currentZ, radius)) {
						columnsToLoad.add(getAddress(currentX, currentZ));
					}

					//if we moved the current point to where it would be previously,
					//would it be outside of current render distance square?
					if (!this.isPointWithinCubeVolume(newX, 0, newZ, currentX - dx, 0, currentZ - dz, radius)) {
						columnsToRemove.add(getAddress(currentX - dx, currentZ - dz));
					}
					for (int currentY = newY - radius; currentY <= newY + radius; ++currentY) {
						//now handle cubes, the same way
						if (!this.isPointWithinCubeVolume(oldX, oldY, oldZ, currentX, currentY, currentZ, radius)) {
							cubesToLoad.add(getAddress(currentX, currentY, currentZ));
							if (cubesToRemove.contains(getAddress(currentX, currentY, currentZ))) {
								int debugLine = 0;
							}
						}
						if (!this.isPointWithinCubeVolume(newX, newY, newZ,
								currentX - dx, currentY - dy, currentZ - dz, radius)) {
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
	public void findAllUnloadedOnViewDistanceDecrease(long playerAddress, int oldViewDistance, int newViewDistance, TLongSet cubesToemove, TLongSet columnsToRemove) {
		int playerCubeX = AddressTools.getX(playerAddress);
		int playerCubeY = AddressTools.getY(playerAddress);
		int playerCubeZ = AddressTools.getZ(playerAddress);

		for (int cubeX = playerCubeX - oldViewDistance; cubeX <= playerCubeX + oldViewDistance; cubeX++) {
			for (int cubeZ = playerCubeZ - oldViewDistance; cubeZ <= playerCubeZ + oldViewDistance; cubeZ++) {
				if (!isPointWithinCubeVolume(playerCubeX, 0, playerCubeZ, cubeX, 0, cubeZ, newViewDistance)) {
					columnsToRemove.add(getAddress(cubeX, cubeZ));
				}
				for (int cubeY = playerCubeY - oldViewDistance; cubeY <= playerCubeY + oldViewDistance; cubeY++) {
					if (!isPointWithinCubeVolume(playerCubeX, playerCubeY, playerCubeZ, cubeX, cubeY, cubeZ, newViewDistance)) {
						cubesToemove.add(getAddress(cubeX, cubeY, cubeZ));
					}
				}
			}
		}
	}

	private boolean isPointWithinCubeVolume(int cubeX, int cubeY, int cubeZ, int pointX, int pointY, int pointZ, int radius) {
		int dx = cubeX - pointX;
		int dy = cubeY - pointY;
		int dz = cubeZ - pointZ;
		return dx >= -radius && dx <= radius
				&& dy >= -radius && dy <= radius
				&& dz >= -radius && dz <= radius;
	}
}