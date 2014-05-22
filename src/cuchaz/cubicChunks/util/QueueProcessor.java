/*******************************************************************************
 * Copyright (c) 2014 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.cubicChunks.util;

import java.util.List;

import com.google.common.collect.Lists;

import cuchaz.cubicChunks.CubeProvider;

public abstract class QueueProcessor
{
	protected String m_name;
	protected CubeProvider m_provider;
	private int m_batchSize;
	private BatchedSetQueue<Long> m_queue;
	protected List<Long> m_incomingAddresses;
	protected List<Long> m_processedAddresses;
	protected List<Long> m_deferredAddresses;
	
	public QueueProcessor( String name, CubeProvider provider, int batchSize )
	{
		m_name = name;
		m_provider = provider;
		m_batchSize = batchSize;
		
		m_queue = new BatchedSetQueue<Long>();
		m_incomingAddresses = Lists.newArrayList();
		m_processedAddresses = Lists.newArrayList();
		m_deferredAddresses = Lists.newArrayList();
	}
	
	public void add( long address )
	{
		m_queue.add( address );
	}
	
	public void addAll( List<Long> addresses )
	{
		m_queue.addAll( addresses );
	}
	
	public int getNumInQueue( )
	{
		return m_queue.size();
	}
	
	public int processQueue( long timeStop )
	{
		m_processedAddresses.clear();
		m_deferredAddresses.clear();
		
		// is there time left?
		while( System.currentTimeMillis() < timeStop )
		{
			// get a batch of addresses
			m_incomingAddresses.clear();
			m_queue.getBatch( m_incomingAddresses, m_batchSize );
			
			// nothing left to do?
			if( m_incomingAddresses.isEmpty() )
			{
				break;
			}
			
			// process it
			processBatch();
		}
		
		// put the deferred addresses back on the queue
		for( long address : m_deferredAddresses )
		{
			m_queue.add( address );
		}
		
		return m_processedAddresses.size();
	}
	
	public List<Long> getProcessedAddresses( )
	{
		return m_processedAddresses;
	}
	
	public String getProcessingReport( )
	{
		return String.format( "\t%22s: %3d processed, %d remaining", m_name, m_processedAddresses.size(), m_queue.size() );
	}
	
	public abstract void processBatch( );
}
