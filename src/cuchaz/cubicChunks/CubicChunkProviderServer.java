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

import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;

public class CubicChunkProviderServer extends ChunkProviderServer
{
    public CubicChunkProviderServer( WorldServer world )
    {
    	super(
    		world,
    		new CubicChunkLoader( world.getSaveHandler() ),
			new CubicChunkGenerator( world )
    	);
    }
    
    @Override
    public boolean unloadQueuedChunks( )
    {
    	// NOTE: this is called every tick and behaves like a tick function -- it might even be misnamed...
    	// the return value is always ignored
    	
    	// for each loaded chunk...
    	
        // UNDONE: need something to check for entities that migrated off their cubic chunk
        // move the entity to its new cubic chunk if that new cubic chunk is loaded
        // or report the entity lost
    	
    	return false;
    }
}
