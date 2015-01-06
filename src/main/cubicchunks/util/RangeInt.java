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
package cubicchunks.util;

public class RangeInt
{
	private int m_start;
	private int m_stop;
	
	public RangeInt( int start, int stop )
	{
		m_start = start;
		m_stop = stop;
	}
	
	public int getStart( )
	{
		return m_start;
	}
	
	public int getStop( )
	{
		return m_stop;
	}
	
	@Override
	public String toString( )
	{
		return String.format( "[%d,%d]", m_start, m_stop );
	}
}
