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

public class ValueCache<T>
{
	private T m_value;
	private boolean m_hasValue;
	
	public ValueCache( )
	{
		m_value = null;
		m_hasValue = false;
	}
	
	public T get( )
	{
		if( !m_hasValue )
		{
			throw new UnsupportedOperationException( "Cache has no value!" );
		}
		return m_value;
	}
	
	public void set( T val )
	{
		m_value = val;
		m_hasValue = true;
	}
	
	public boolean hasValue( )
	{
		return m_hasValue;
	}
	
	public void clear( )
	{
		m_value = null;
		m_hasValue = false;
	}
}
