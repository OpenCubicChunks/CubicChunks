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

import java.util.HashMap;
import java.util.PriorityQueue;

public class Profiler
{
	private static HashMap<String,ProfilerCounter> m_counters;
	
	static
	{
		reset();
	}
	
	public static void start( String name )
	{
		ProfilerCounter counter = m_counters.get( name );
		
		if( counter == null )
		{
			counter = new ProfilerCounter( name );
			m_counters.put( name, counter );
		}
		
		counter.start();
	}
	
	public static void stop( String name )
	{
		ProfilerCounter counter = m_counters.get( name );
		counter.stop();
	}
	
	public static void stopStart( String stopName, String startName )
	{
		stop( stopName );
		start( startName );
	}
	
	public static void reset( )
	{
		m_counters = new HashMap<String,ProfilerCounter>();
	}
	
	public static String getReport( )
	{
		// calculate total time
		long totalTime = 0;
		for( ProfilerCounter counter : m_counters.values() )
		{
			totalTime += counter.getElapsedMilliseconds();
		}
		
		// update percentages
		for( ProfilerCounter counter : m_counters.values() )
		{
			counter.setPercentTime( 100.0 * (double)counter.getElapsedMilliseconds() / (double)totalTime );
		}
		
		// sort the counters
		PriorityQueue<ProfilerCounter> order = new PriorityQueue<ProfilerCounter>();
		for( ProfilerCounter counter : m_counters.values() )
		{
			order.add( counter );
		}
		
		// build the report
		StringBuilder buf = new StringBuilder();
		buf.append( "Profiling Report:\n" );
		ProfilerCounter counter = null;
		while( ( counter = order.poll() ) != null )
		{
			buf.append( String.format( "%8.2f", (double)counter.getElapsedMilliseconds() / 1000.0 ) );
			buf.append( "s (" );
			buf.append( String.format( "%6.2f", counter.getPercentTime() ) );
			buf.append( "%): " );
			buf.append( counter.getName() );
			buf.append( "\n" );
		}
		
		return buf.toString();
	}
	
	public static String getMemoryUsed( )
	{
		long usedBytes = Runtime.getRuntime().totalMemory();
		
		double usedKibibytes = (double)usedBytes / 1024.0;
		if( usedKibibytes < 1000.0 )
		{
			return String.format( "%.2f", usedKibibytes ) + "KiB";
		}
		
		double usedMebibytes = usedKibibytes / 1024.0;
		if( usedMebibytes < 1000.0 )
		{
			return String.format( "%.2f", usedMebibytes ) + "MiB";
		}
		
		double usedGibibytes = usedMebibytes / 1024.0;
		return String.format( "%.2f", usedGibibytes ) + "GiB";
	}
}
