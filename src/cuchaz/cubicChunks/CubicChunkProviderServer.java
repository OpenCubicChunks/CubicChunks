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

import java.lang.reflect.Field;

import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderGenerate;
import net.minecraft.world.gen.ChunkProviderServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CubicChunkProviderServer extends ChunkProviderServer
{
	private static final Logger log = LogManager.getLogger();
	
	private CubicChunkLoader m_loader;
	
    public CubicChunkProviderServer( WorldServer world )
    {
    	super(
    		world,
    		new CubicChunkLoader( world.getSaveHandler() ),
			new ChunkProviderGenerate( world, world.getSeed(), world.getWorldInfo().isMapFeaturesEnabled() )
    	);
    	
    	// need to get the chunk loader back, but the damn superclass keeps it in a private field
    	// and with how java does constructor chaining, I can't get the damn instance I passed above...
    	try
    	{
	    	Field field = ChunkProviderServer.class.getDeclaredField( "chunkLoader" );
	    	field.setAccessible( true );
	    	m_loader = (CubicChunkLoader)field.get( this );
    	}
    	catch( Exception ex )
    	{
    		// can't be helped
    		throw new Error( ex );
    	}
    }
}
