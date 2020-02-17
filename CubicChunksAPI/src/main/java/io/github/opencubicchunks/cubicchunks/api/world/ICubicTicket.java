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
package io.github.opencubicchunks.cubicchunks.api.world;

import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.util.math.ChunkPos;

import java.util.Map;

/**
 * A CubicChunks chunkloading ticket. It's a cubic chunks ticket when the ticket's world is a cubic chunks world.
 * This interface is implemented by {@link net.minecraftforge.common.ForgeChunkManager.Ticket}.
 *
 * Use {@link ICubicWorldServer} methods to force load/unload cubes.
 */
public interface ICubicTicket {
    /**
     * Returns an unmodifiable view of all forced cubes, in the form of map from column position,
     * to set of cube Y positions in that column. An implementation is allowed to return a copy
     * instead of a live view.
     *
     * @return unmodifiable view of forced cubes grouped by column position
     */
    Map<ChunkPos, IntSet> getAllForcedChunkCubes();
}
