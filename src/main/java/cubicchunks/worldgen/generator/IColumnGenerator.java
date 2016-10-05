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
package cubicchunks.worldgen.generator;

import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;

public interface IColumnGenerator {
	
	/**
	 * Adds biome's and optionally other stuff to a Column
	 * (can pre-add Cubes but this is not recommended)
	 * 
	 * @param column the column that needs new biomes and other data
	 */
	void generateColumn(Column column);
	
	/**
	 * Called to reload structures that apply to {@code cube}.
	 * Mostly used to get ready for calls to {@link IColumnGenerator#getPossibleCreatures(EnumCreatureType, BlockPos))}
	 * <br>
	 * Note: if your generator works better on a 3D system you can do your 3D
	 * loading in {@link ICubeGenerator#recreateStructures(Cube)}
	 * 
	 * @param cube The cube that is being loaded
	 */
	void recreateStructures(Column column);
}
