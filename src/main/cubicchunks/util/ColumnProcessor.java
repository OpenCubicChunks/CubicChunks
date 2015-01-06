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
package cubicchunks.util;

import cubicchunks.CubeProvider;
import cubicchunks.world.BlankColumn;
import cubicchunks.world.Column;

public abstract class ColumnProcessor extends QueueProcessor
{
	public ColumnProcessor( String name, CubeProvider provider, int batchSize )
	{
		super( name, provider, batchSize );
	}
	
	@Override
	public void processBatch( )
	{
		// start processing
		for( long address : m_incomingAddresses )
		{
			// get the column
			int cubeX = AddressTools.getX( address );
			int cubeZ = AddressTools.getZ( address );
			Column column = (Column)m_provider.provideChunk( cubeX, cubeZ );
			
			// skip blank columns
			if( column == null || column instanceof BlankColumn )
			{
				continue;
			}
			
			// add unsuccessful calculations back onto the queue
			boolean success = calculate( column );
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
	
	public abstract boolean calculate( Column column );
}
