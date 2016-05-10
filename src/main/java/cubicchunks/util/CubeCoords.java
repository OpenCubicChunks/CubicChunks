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
package cubicchunks.util;

import static cubicchunks.util.Coords.CUBE_MAX_X;
import static cubicchunks.util.Coords.CUBE_MAX_Y;
import static cubicchunks.util.Coords.CUBE_MAX_Z;
import static cubicchunks.util.Coords.HALF_CUBE_MAX_X;
import static cubicchunks.util.Coords.HALF_CUBE_MAX_Y;
import static cubicchunks.util.Coords.HALF_CUBE_MAX_Z;

/**
 * Position of a cube.
 * <p>
 * Tall Worlds uses a column coordinate system (which is really just a cube coordinate system without the y-coordinate), a cube coordinate system, and two block coordinate systems, a cube-relative system, and a world absolute system.
 * <p>
 * It is important that the systems are kept separate. This class should be used whenever a cube coordinate is passed along, so that it is clear that cube coordinates are being used, and not block coordinates.
 * <p>
 * Additionally, I (Nick) like to use xRel, yRel, and zRel for the relative position of a block inside of a cube. In world space, I (Nick) refer to the coordinates as xAbs, yAbs, and zAbs.
 * <p>
 * See {@link AddressTools} for details of hashing the cube coordinates for keys and storage.
 * <p>
 * This class also contains some helper methods to switch from/to block coordinates.
 */
public class CubeCoords {

	private final int cubeX;
	private final int cubeY;
	private final int cubeZ;

	protected CubeCoords(int cubeX, int cubeY, int cubeZ) {
		this.cubeX = cubeX;
		this.cubeY = cubeY;
		this.cubeZ = cubeZ;
	}

	/**
	 * Gets the x position of the cube in the world.
	 *
	 * @return The x position.
	 */
	public int getCubeX() {
		return this.cubeX;
	}

	/**
	 * Gets the y position of the cube in the world.
	 *
	 * @return The y position.
	 */
	public int getCubeY() {
		return this.cubeY;
	}

	/**
	 * Gets the z position of the cube in the world.
	 *
	 * @return The z position.
	 */
	public int getCubeZ() {
		return this.cubeZ;
	}

	/**
	 * Gets the coordinates of the cube as a string.
	 *
	 * @return The coordinates, formatted as a string.
	 */
	@Override
	public String toString() {
		return this.cubeX + "," + this.cubeY + "," + this.cubeZ;
	}

	/**
	 * Compares the CubeCoordinate against the given object.
	 *
	 * @return True if the cube matches the given object, but false if it doesn't match, or is null, or not a CubeCoordinate object.
	 */
	@Override
	public boolean equals(Object otherObject) {
		if (otherObject == this) {
			return true;
		}

		if (otherObject == null) {
			return false;
		}

		if (!(otherObject instanceof Coords)) {
			return false;
		}

		CubeCoords otherCubeCoordinate = (CubeCoords) otherObject;

		if (otherCubeCoordinate.cubeX != cubeX) {
			return false;
		}

		if (otherCubeCoordinate.cubeY != cubeY) {
			return false;
		}

		if (otherCubeCoordinate.cubeZ != cubeZ) {
			return false;
		}

		return true;
	}

	/**
	 * Gets the absolute position of the cube's center on the x axis.
	 *
	 * @return The x center of the cube.
	 */
	public int getXCenter() {
		return cubeX*CUBE_MAX_X + HALF_CUBE_MAX_X;
	}

	/**
	 * Gets the absolute position of the cube's center on the y axis.
	 *
	 * @return The y center of the cube.
	 */
	public int getYCenter() {
		return cubeY*CUBE_MAX_Y + HALF_CUBE_MAX_Y;
	}

	/**
	 * Gets the absolute position of the cube's center on the z axis.
	 *
	 * @return The z center of the cube.
	 */
	public int getZCenter() {
		return cubeZ*CUBE_MAX_Z + HALF_CUBE_MAX_Z;
	}

	public int getMinBlockX() {
		return Coords.cubeToMinBlock(cubeX);
	}

	public int getMinBlockY() {
		return Coords.cubeToMinBlock(cubeX);
	}

	public int getMinBlockZ() {
		return Coords.cubeToMinBlock(cubeX);
	}
}