/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.cubicChunks;

import net.minecraft.world.chunk.IChunkProvider;

public interface CubicChunkProvider extends IChunkProvider
{
	boolean cubicChunkExists( int chunkX, int chunkY, int chunkZ );
	CubicChunk loadCubicChunk( int chunkX, int chunkY, int chunkZ );
}
