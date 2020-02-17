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
package io.github.opencubicchunks.cubicchunks.api.worldgen.structure.feature;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.util.XYZAddressable;
import net.minecraft.world.World;

/**
 * Interface implemented by
 * {@link net.minecraft.world.gen.structure.StructureStart} class when cubic chunks is installed.
 * Allows to access CubicChunks-specific behavior of StructureStart
 */
public interface ICubicFeatureStart extends XYZAddressable {
    int getChunkPosY();

    /**
     * Called to mark this StructureStart as a part of cubic chunks structure,
     * and provide necessary cubic chunks specific data.
     * Must be called immediately after constructing the StructureStart.
     *
     * @param world world instance for initialization
     * @param cubeY cube Y coordinate of this structure start
     */
    void initCubic(World world, int cubeY);

    CubePos getCubePos();
    
    /**
     * @return {@code true} when instance has been initialized by initCubic(..) method called
     *         by one of cubic chunks structure generators.
     */
    boolean isCubic();
}