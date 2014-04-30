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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;


public class ConcurrentBatchedQueue<T>
{
	private ArrayDeque<T> m_queue;
	
	public ConcurrentBatchedQueue( )
	{
		m_queue = new ArrayDeque<T>();
	}
	
	public synchronized void add( T val )
	{
		m_queue.add( val );
	}
	
	public synchronized void addAll( Collection<T> vals )
	{
		m_queue.addAll( vals );
	}
	
	public synchronized T get( )
	{
		return m_queue.poll();
	}
	
	public synchronized boolean getBatch( List<T> out, int size )
	{
		// copy the batch to the out list
		for( int i=0; i<size; i++ )
		{
			T val = m_queue.poll();
			if( val == null )
			{
				break;
			}
			out.add( val );
		}
		
		// are there more entries?
		return !m_queue.isEmpty();
	}
}
