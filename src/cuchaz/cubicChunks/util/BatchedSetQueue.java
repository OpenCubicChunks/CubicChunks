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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;

public class BatchedSetQueue<T>
{
	private LinkedHashSet<T> m_setQueue;
	
	public BatchedSetQueue( )
	{
		m_setQueue = new LinkedHashSet<T>();
	}
	
	public boolean add( T val )
	{
		return m_setQueue.add( val );
	}
	
	public void addAll( Iterable<T> vals )
	{
		for( T val : vals )
		{
			m_setQueue.add( val );
		}
	}
	
	public boolean contains( T val )
	{
		return m_setQueue.contains( val );
	}
	
	public void getBatch( Collection<T> out, int size )
	{
		Iterator<T> iter = m_setQueue.iterator();
		for( int i=0; i<size && iter.hasNext(); i++ )
		{
			out.add( iter.next() );
			iter.remove();
		}
	}
	
	public int size( )
	{
		return m_setQueue.size();
	}
}
