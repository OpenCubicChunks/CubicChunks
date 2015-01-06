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
package main.java.cubicchunks.perf;

public class Timer
{
	private String m_name;
	private long m_startTime;
	private long m_stopTime;
	private boolean m_isRunning;
	
	public Timer( )
	{
		this( "Timer" );
	}
	
	public Timer( String name )
	{
		m_name = name;
		m_startTime = 0;
		m_stopTime = 0;
		m_isRunning = false;
	}
	
	public String getName( )
	{
		return m_name;
	}
	
	public long getStartTime( )
	{
		return m_startTime;
	}
	
	public long getStopTime( )
	{
		return m_stopTime;
	}
	
	public boolean isRunning( )
	{
		return m_isRunning;
	}
	
	public String toString( )
	{
		return m_name + " : " + getElapsedTime();
	}
	
	public void start( )
	{
		m_isRunning = true;
		m_startTime = getTime();
		m_stopTime = -1;
	}
	
	public void stop( )
	{
		m_isRunning = false;
		m_stopTime = getTime();
	}
	
	public long getElapsedMilliseconds( )
	{
		if( m_isRunning )
		{
			return getTime() - m_startTime;
		}
		else
		{
			return m_stopTime - m_startTime;
		}
	}
	
	public float getElapsedSeconds( )
	{
		return getElapsedMilliseconds() / 1000.0f;
	}
	
	public float getElapsedMinutes( )
	{
		return getElapsedMilliseconds() / 1000.0f / 60.0f;
	}
	
	public float getElapsedHours( )
	{
		return getElapsedMilliseconds() / 1000.0f / 60.0f / 60.0f;
	}

	public String getElapsedTime( )
	{
		float seconds = getElapsedSeconds();
		if( seconds < 60.0 )
		{
			return String.format( "%.2fs", seconds );
		}
		
		float minutes = getElapsedMinutes();
		if( minutes < 60 )
		{
			return String.format( "%.2fm", minutes );
		}
		
		float hours = getElapsedHours();
		return String.format( "%.2fh", hours );
	}
	
	private long getTime( )
	{
		return System.currentTimeMillis();
	}
}
