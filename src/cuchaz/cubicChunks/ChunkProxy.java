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

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class ChunkProxy extends Chunk
{
	public ChunkProxy( World world, int x, int z )
	{
		// called for chunk loading
		super( world, x, z );
	}
	
	public ChunkProxy( World world, Block[] blocks, byte[] meta, int x, int z )
    {
		// called for chunk generation
		super( world, x, z );
		
		// UNDONE: save block ids and meta
    }
}
