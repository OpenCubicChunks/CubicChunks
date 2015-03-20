/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 * 
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package cubicchunks.util;

import java.util.List;

import com.google.common.collect.Lists;

import cubicchunks.CubeProvider;

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
