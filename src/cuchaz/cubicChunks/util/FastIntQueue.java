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

public class FastIntQueue
{
	private int[] m_queue;
	private int m_start;
	private int m_stop;
	
	public FastIntQueue( )
	{
		m_queue = new int[32768];
		clear();
	}
	
	public boolean hasRoomFor( int n )
	{
		return m_stop + n <= m_queue.length;
	}
	
	public void add( int val )
	{
		m_queue[m_stop++] = val;
	}
	
	public boolean hasNext( )
	{
		return m_start < m_stop;
	}
	
	public int get( )
	{
		return m_queue[m_start++];
	}
	
	public int size( )
	{
		return m_stop;
	}
	
	public void clear( )
	{
		m_start = 0;
		m_stop = 0;
	}
	
	public void reset( )
	{
		m_start = 0;
	}
}
