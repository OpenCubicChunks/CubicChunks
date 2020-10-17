/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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
package io.github.opencubicchunks.cubicchunks.api.util;

import static io.github.opencubicchunks.cubicchunks.api.util.Coords.blockToCube;
import static io.github.opencubicchunks.cubicchunks.api.util.Coords.getCubeXForEntity;
import static io.github.opencubicchunks.cubicchunks.api.util.Coords.getCubeYForEntity;
import static io.github.opencubicchunks.cubicchunks.api.util.Coords.getCubeZForEntity;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.Random;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Position of a cube.
 * <p>
 * Tall Worlds uses a column coordinate system (which is really just a cube coordinate system without the y-coordinate),
 * a cube coordinate system, and two block coordinate systems, a cube-relative system, and a world absolute system.
 * <p>
 * It is important that the systems are kept separate. This class should be used whenever a cube coordinate is passed
 * along, so that it is clear that cube coordinates are being used, and not block coordinates.
 * <p>
 * Additionally, I (Nick) like to use xRel, yRel, and zRel for the relative position of a block inside of a cube. In
 * world space, I (Nick) refer to the coordinates as xAbs, yAbs, and zAbs.
 * <p>
 * This class also contains some helper methods to switch from/to block coordinates.
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CubePos {

    public static final CubePos ZERO = new CubePos(0, 0, 0);

    private static final int Y_BITS = 20;
    private static final int X_BITS = 22;
    private static final int Z_BITS = 22;

    private static final int Z_BIT_OFFSET = 0;
    private static final int X_BIT_OFFSET = Z_BIT_OFFSET + Z_BITS;
    private static final int Y_BIT_OFFSET = X_BIT_OFFSET + X_BITS;

    private final int cubeX;
    private final int cubeY;
    private final int cubeZ;

    public CubePos(int cubeX, int cubeY, int cubeZ) {
        this.cubeX = cubeX;
        this.cubeY = cubeY;
        this.cubeZ = cubeZ;
    }

    public CubePos(XYZAddressable addressable) {
        this.cubeX = addressable.getX();
        this.cubeY = addressable.getY();
        this.cubeZ = addressable.getZ();
    }

    /**
     * Gets the x position of the cube in the world.
     *
     * @return The x position.
     */
    public int getX() {
        return this.cubeX;
    }

    /**
     * Gets the y position of the cube in the world.
     *
     * @return The y position.
     */
    public int getY() {
        return this.cubeY;
    }

    /**
     * Gets the z position of the cube in the world.
     *
     * @return The z position.
     */
    public int getZ() {
        return this.cubeZ;
    }

    /**
     * Gets the coordinates of the cube as a string.
     *
     * @return The coordinates, formatted as a string.
     */
    @Override
    public String toString() {
        return String.format("CubePos(%d, %d, %d)", cubeX, cubeY, cubeZ);
    }

    /**
     * Compares the CubeCoordinate against the given object.
     *
     * @return True if the cube matches the given object, but false if it doesn't match, or is null, or not a
     * CubeCoordinate object.
     */
    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj instanceof CubePos) {
            CubePos otherCoords = (CubePos) obj;
            return otherCoords.cubeX == cubeX && otherCoords.cubeY == cubeY && otherCoords.cubeZ == cubeZ;
        }
        return false;
    }

    /**
     * Returns a specification compliant hashCode for this object.
     *
     * @return A 32bit hashCode for this instance of CubePos.
     */
    @Override
    public int hashCode() {
        return Long.hashCode(Bits.packSignedToLong(cubeX, Y_BITS, Y_BIT_OFFSET)
                | Bits.packSignedToLong(cubeY, X_BITS, X_BIT_OFFSET)
                | Bits.packSignedToLong(cubeZ, Z_BITS, Z_BIT_OFFSET));
    }

    /**
     * Gets the absolute position of the cube's center on the x axis.
     *
     * @return The x center of the cube.
     */
    public int getXCenter() {
        return cubeX * ICube.SIZE + ICube.SIZE / 2;
    }

    /**
     * Gets the absolute position of the cube's center on the y axis.
     *
     * @return The y center of the cube.
     */
    public int getYCenter() {
        return cubeY * ICube.SIZE + ICube.SIZE / 2;
    }

    /**
     * Gets the absolute position of the cube's center on the z axis.
     *
     * @return The z center of the cube.
     */
    public int getZCenter() {
        return cubeZ * ICube.SIZE + ICube.SIZE / 2;
    }

    public int getMinBlockX() {
        return Coords.cubeToMinBlock(cubeX);
    }

    public int getMinBlockY() {
        return Coords.cubeToMinBlock(cubeY);
    }

    public int getMinBlockZ() {
        return Coords.cubeToMinBlock(cubeZ);
    }

    public int getMaxBlockX() {
        return Coords.cubeToMaxBlock(this.cubeX);
    }

    public int getMaxBlockY() {
        return Coords.cubeToMaxBlock(this.cubeY);
    }

    public int getMaxBlockZ() {
        return Coords.cubeToMaxBlock(this.cubeZ);
    }

    public BlockPos getCenterBlockPos() {
        return new BlockPos(getXCenter(), getYCenter(), getZCenter());
    }

    public BlockPos getMinBlockPos() {
        return new BlockPos(getMinBlockX(), getMinBlockY(), getMinBlockZ());
    }

    public BlockPos getMaxBlockPos() {
        return new BlockPos(getMaxBlockX(), getMaxBlockY(), getMaxBlockZ());
    }

    public BlockPos localToBlock(int localX, int localY, int localZ) {
        return new BlockPos(getMinBlockX() + localX, getMinBlockY() + localY, getMinBlockZ() + localZ);
    }

    public CubePos sub(int dx, int dy, int dz) {
        return this.add(-dx, -dy, -dz);
    }

    public CubePos add(int dx, int dy, int dz) {
        return new CubePos(getX() + dx, getY() + dy, getZ() + dz);
    }

    public ChunkPos chunkPos() {
        return new ChunkPos(getX(), getZ());
    }

    public int distSquared(CubePos coords) {
        int dx = coords.cubeX - this.cubeX;
        int dy = coords.cubeY - this.cubeY;
        int dz = coords.cubeZ - this.cubeZ;
        return dx * dx + dy * dy + dz * dz;
    }

    public void forEachWithinRange(int range, Consumer<CubePos> action) {
        for (int x = this.cubeX - range; x < this.cubeX + range; x++) {
            for (int y = this.cubeY - range; y < this.cubeY + range; y++) {
                for (int z = this.cubeZ - range; z < this.cubeZ + range; z++) {
                    action.accept(new CubePos(x, y, z));
                }
            }
        }
    }

    public BlockPos randomPopulationPos(Random rand) {
        return new BlockPos(
                rand.nextInt(ICube.SIZE) + getXCenter(),
                rand.nextInt(ICube.SIZE) + getYCenter(),
                rand.nextInt(ICube.SIZE) + getZCenter());
    }

    public CubePos above() {
        return add(0, 1, 0);
    }

    public CubePos below() {
        return add(0, -1, 0);
    }

    public static CubePos fromBlockCoords(int blockX, int blockY, int blockZ) {
        return new CubePos(blockToCube(blockX), blockToCube(blockY), blockToCube(blockZ));
    }

    public static CubePos fromEntityCoords(double blockX, double blockY, double blockZ) {
        return new CubePos(blockToCube(blockX), blockToCube(blockY), blockToCube(blockZ));
    }

    public static CubePos fromEntity(Entity entity) {
        return new CubePos(getCubeXForEntity(entity), getCubeYForEntity(entity), getCubeZForEntity(entity));
    }

    public static CubePos fromBlockCoords(BlockPos pos) {
        return CubePos.fromBlockCoords(pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean containsBlock(BlockPos pos) {
        return this.cubeX == blockToCube(pos.getX()) && this.cubeY == blockToCube(pos.getY()) && this.cubeZ == blockToCube(pos.getZ());
    }
}
