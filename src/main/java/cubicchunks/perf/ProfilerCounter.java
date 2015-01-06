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

public class ProfilerCounter implements Comparable<ProfilerCounter>
{
	private String m_name;
	private long m_elapsedMilliseconds;
	private Timer m_timer;
	private double m_percentTime;
	
	public ProfilerCounter( String name )
	{
		// save parameters
		m_name = name;
		
		// init defaults
		m_elapsedMilliseconds = 0;
		m_timer = null;
		m_percentTime = 0.0;
	}
	
	public String getName( )
	{
		return m_name;
	}
	
	public long getElapsedMilliseconds( )
	{
		return m_elapsedMilliseconds;
	}
	
	public double getPercentTime( )
	{
		return m_percentTime;
	}
	
	public void setPercentTime( double val )
	{
		m_percentTime = val;
	}
	
	public void start( )
	{
		m_timer = new Timer();
		m_timer.start();
	}
	
	public void stop( )
	{
		m_timer.stop();
		m_elapsedMilliseconds += m_timer.getElapsedMilliseconds();
		m_timer = null;
	}
	
	@Override
	public int compareTo( ProfilerCounter other )
	{
		if( m_percentTime > other.m_percentTime )
		{
			return -1;
		}
		else
		{
			return 1;
		}
	}	
}
