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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class ConcurrentBatchedMappedQueue<k,T>
{
	private LinkedHashMap<k,T> m_queue;
	
	public ConcurrentBatchedMappedQueue( )
	{
		m_queue = new LinkedHashMap<k,T>();
	}
	
	public synchronized void add(k key, T val )
	{
		m_queue.put(key,val);
	}
	
	public synchronized void addAll(Map<k, T> vals )
	{
		m_queue.putAll(vals);
	}
	
	public synchronized boolean contains(k key)
	{
		return m_queue.containsKey(key);
	}
	
	public synchronized T get( )
	{
		T val = m_queue.entrySet().iterator().next().getValue();
		return m_queue.remove(val);
	}
	
	public synchronized T get(k key)
	{
		return m_queue.get(key);
	}
	
	public synchronized boolean getBatch( LinkedHashMap<k,T> out, int size )
	{
		// copy the batch to the out list
		Iterator<Entry<k, T>> iter = m_queue.entrySet().iterator();
		for( int i=0; i<size; i++ )
		{
			if (!iter.hasNext())
			{
				break;
			}
			Map.Entry <k,T> entry = iter.next();
			k key = entry.getKey();
			T val = entry.getValue();
			iter.remove();
			if( val == null )
			{
				break;
			}
			out.put(key,val);
		}
		
		// are there more entries?
		return !m_queue.isEmpty();
	}
	
	public synchronized int size()
	{
		return m_queue.size();
	}
}
