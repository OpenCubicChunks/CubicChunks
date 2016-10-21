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

import cubicchunks.util.CubeCoords;
import net.minecraft.util.math.ChunkPos;

import java.util.Set;
import java.util.function.Consumer;

public abstract class CubeSelector {
	public abstract void forAllVisibleFrom(CubeCoords cubePos, int horizontalViewDistance, int verticalViewDistance, Consumer<CubeCoords> consumer);

	public abstract void findChanged(CubeCoords oldAddress, CubeCoords newAddress, int horizontalViewDistance, int verticalViewDistance,
	                                 Set<CubeCoords> cubesToRemove, Set<CubeCoords> cubesToLoad, Set<ChunkPos> columnsToRemove, Set<ChunkPos> columnsToLoad);

	public abstract void findAllUnloadedOnViewDistanceDecrease(CubeCoords playerAddress, int oldHorizontalViewDistance, int newHorizontalViewDistance,
	                                                           int oldVerticalViewDistance, int newVerticalViewDistance, Set<CubeCoords> cubesToUnload, Set<ChunkPos> columnsToUnload);
}