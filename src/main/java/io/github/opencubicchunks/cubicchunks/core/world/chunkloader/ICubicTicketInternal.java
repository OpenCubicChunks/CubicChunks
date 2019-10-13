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
package io.github.opencubicchunks.cubicchunks.core.world.chunkloader;

import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicTicket;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.ITicket;
import it.unimi.dsi.fastutil.ints.IntSet;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

// this is internal interface, most of it shouldn't be in API
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ICubicTicketInternal extends ICubicTicket, ITicket {

    void addRequestedCube(CubePos pos);

    void removeRequestedCube(CubePos pos);

    // handling of forge forced chunks

    void setForcedChunkCubes(ChunkPos location, IntSet yCoords);

    void clearForcedChunkCubes(ChunkPos location);

    void setAllForcedChunkCubes(Map<ChunkPos, IntSet> cubePosMap);

    // setters and getters for private data, because no ATs for forge classes
    void setModData(NBTTagCompound modData);

    void setPlayer(String player);

    void setEntityChunkX(int chunkX);

    void setEntityChunkY(int cubeY);

    void setEntityChunkZ(int chunkZ);

    int getEntityChunkX();

    int getEntityChunkY();

    int getEntityChunkZ();

    int getMaxCubeDepth();

    @Override default boolean shouldTick() {
        return true;
    }

    Set<CubePos> requestedCubes();
}
