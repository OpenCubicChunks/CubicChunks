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
package io.github.opencubicchunks.cubicchunks.api.worldgen.structure;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.worldgen.CubePrimer;
import net.minecraft.world.World;

import java.util.Random;

/**
 * Base interface for cubic chunks structure generators (carvers like caves, and structures like strongholds and villages).
 * This corresponds to Minecraft's {@link net.minecraft.world.gen.MapGenBase} class.
 * <p>
 * The basic idea is to loop over all cubes within some radius (max structure size) and figure out which parts of
 * structures starting there intersect currently generated cube.
 */
public interface ICubicStructureGenerator {

    void generate(World world, CubePrimer cube, CubePos cubePos);

    /**
     * Generates structures in given cube, with supplied parameters and handler
     *
     * @param world the world that the structure is generated in
     * @param cube the block buffer to be filled with blocks (Cube)
     * @param cubePos position of the cube to generate structures in
     * @param handler generation handler, to generate blocks for a given structure source point, in the specified cube
     * @param range horizontal search distance for structure sources (in cubes)
     * @param rangeY vertical search distance for structure sources (in cubes)
     * @param spacingBitCount only structure sources on a grid of size 2^spacingBitCount will be considered for generation
     * @param spacingBitCountY only structure sources on a grid of size 2^spacingBitCount will be considered for generation (y coordinate)
     */
    default void generate(World world, CubePrimer cube, CubePos cubePos, Handler handler,
            int range, int rangeY, int spacingBitCount, int spacingBitCountY) {

        //TODO: maybe skip some of this stuff if the cube is empty? (would need to use hints)
        Random rand = new Random(world.getSeed());
        //used to randomize contribution of each coordinate to the cube seed
        //without these swapping x/y/z coordinates would result in the same seed
        //so structures would generate symmetrically
        long randXMul = rand.nextLong();
        long randYMul = rand.nextLong();
        long randZMul = rand.nextLong();

        int spacing = 1 << spacingBitCount;
        int spacingBits = spacing - 1;

        int spacingY = 1 << spacingBitCountY;
        int spacingBitsY = spacingY - 1;

        // as an optimization, this structure looks for structures only in every Nth coordinate on each axis
        // this ensures that all origin points are always a multiple of 2^bits
        // this way positions used as origin position are consistent across chunks
        // With "| spacingBits" also on radius, the "1" bits cancel out to zero with "basePos - radius"
        // because it's an OR, it can never decrease radius
        int radius = range | spacingBits;
        int radiusY = rangeY | spacingBitsY;
        int cubeXOriginBase = cubePos.getX() | spacingBits;
        int cubeYOriginBase = cubePos.getY() | spacingBitsY;
        int cubeZOriginBase = cubePos.getZ() | spacingBits;

        long randSeed = world.getSeed();

        //x/y/zOrigin is location of the structure "center", and cubeX/Y/Z is the currently generated cube
        for (int xOrigin = cubeXOriginBase - radius; xOrigin <= cubeXOriginBase + radius; xOrigin += spacing) {
            long randX = xOrigin * randXMul ^ randSeed;
            for (int yOrigin = cubeYOriginBase - radiusY; yOrigin <= cubeYOriginBase + radiusY; yOrigin += spacingY) {
                long randY = yOrigin * randYMul ^ randX;
                for (int zOrigin = cubeZOriginBase - radius; zOrigin <= cubeZOriginBase + radius; zOrigin += spacing) {
                    long randZ = zOrigin * randZMul ^ randY;
                    rand.setSeed(randZ);
                    handler.generate(world, rand, cube, xOrigin, yOrigin, zOrigin, cubePos);
                }
            }

        }
    }

    @FunctionalInterface
    interface Handler {
        /**
         * Generates blocks in a given cube for a structure that starts at given origin position.
         *
         * @param world the world the structure is generated in
         * @param rand random number generator with seed for the starting position
         * @param cube the block buffer to be filled with blocks (Cube)
         * @param structureX x coordinate of the starting position of currently generated structure
         * @param structureY y coordinate of the starting position of currently generated structure
         * @param structureZ z coordinate of the starting position of currently generated structure
         * @param generatedCubePos position of the cube to fill with blocks
         */
        void generate(World world, Random rand, CubePrimer cube,
                int structureX, int structureY, int structureZ,
                CubePos generatedCubePos);
    }
}
