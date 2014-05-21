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
package cuchaz.cubicChunks.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cuchaz.cubicChunks.CubeProvider;
import cuchaz.cubicChunks.world.Cube;

public abstract class CubeProcessor extends QueueProcessor
{
	private static final Logger log = LogManager.getLogger();
	
	public CubeProcessor( String name, CubeProvider provider, int batchSize )
	{
		super( name, provider, batchSize );
	}
	
	@Override
	public void processBatch( )
	{
		// start processing
		for( long address : m_incomingAddresses )
		{
			// get the cube
			int cubeX = AddressTools.getX( address );
			int cubeY = AddressTools.getY( address );
			int cubeZ = AddressTools.getZ( address );
			Cube cube = m_provider.provideCube( cubeX, cubeY, cubeZ );
			if( cube == null )
			{
				log.warn( String.format( "Unloaded cube (%d,%d,%d) dropped from %s processor queue.",
					cubeX, cubeY, cubeZ, m_name
				) );
				continue;
			}
			
			// add unsuccessful calculations back onto the queue
			boolean success = calculate( cube );
			if( success )
			{
				m_processedAddresses.add( address );
			}
			else
			{
				m_deferredAddresses.add( address );
			}
		}
	}
	
	public abstract boolean calculate( Cube cube );
}
