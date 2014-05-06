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
package cuchaz.cubicChunks.lighting;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cuchaz.cubicChunks.CubeProvider;
import cuchaz.cubicChunks.util.AddressTools;
import cuchaz.cubicChunks.util.BatchedSetQueue;
import cuchaz.cubicChunks.world.BlankColumn;
import cuchaz.cubicChunks.world.Column;

public class LightingManager
{
	private static final Logger log = LogManager.getLogger();
	
	private static final int TickBudget = 100; // ms
	private static final int SkyLightBatchSize = 100;
	private static final int FirstLightBatchSize = 10;
	
	private World m_world;
	private CubeProvider m_provider;
	private transient List<Long> m_addresses;
	private transient List<Long> m_deferredAddresses;
	
	private BatchedSetQueue<Long> m_skyLightQueue;
	private BatchedSetQueue<Long> m_firstLightQueue;
	
	private SkyLightCalculator m_skyLightCalculator;
	private FirstLightCalculator m_firstLightCalculator;
	private DiffuseLightingCalculator m_diffuseLightingCalculator;
	
	public LightingManager( World world, CubeProvider provider )
	{
		m_world = world;
		m_provider = provider;
		m_addresses = new ArrayList<Long>();
		m_deferredAddresses = new ArrayList<Long>();
		
		m_skyLightQueue = new BatchedSetQueue<Long>();
		m_firstLightQueue = new BatchedSetQueue<Long>();
		
		m_skyLightCalculator = new SkyLightCalculator();
		m_firstLightCalculator = new FirstLightCalculator();
		m_diffuseLightingCalculator = new DiffuseLightingCalculator();
	}
	
	public void queueSkyLightCalculation( long columnAddress )
	{
		m_skyLightQueue.add( columnAddress );
	}
	
	public void queueFirstLightCalculation( long columnAddress )
	{
		m_firstLightQueue.add( columnAddress );
	}
	
	public boolean computeDiffuseLighting( int blockX, int blockY, int blockZ, EnumSkyBlock lightType )
	{
		return m_diffuseLightingCalculator.calculate( m_world, blockX, blockY, blockZ, lightType );
	}
	
	public void tick( )
	{
		long timeStart = System.currentTimeMillis();
		long timeStop = timeStart + TickBudget;
		
		// process the queues
		int numSkyLightsProcessed = processColumnQueue( m_skyLightQueue, timeStop, SkyLightBatchSize, m_skyLightCalculator );
		int numFirstLightsProcessed = processColumnQueue( m_firstLightQueue, timeStop, FirstLightBatchSize, m_firstLightCalculator );
		
		// reporting
		long timeDiff = System.currentTimeMillis() - timeStart;
		int totalProcessed = numSkyLightsProcessed + numFirstLightsProcessed;
		if( totalProcessed > 0 )
		{
			log.info( String.format( "%s Lighting manager processed %d calculations in %d ms.",
				m_world.isClient ? "CLIENT" : "SERVER",
				totalProcessed, timeDiff
			) );
			log.info( String.format( "\t%16s: %3d processed, %d remaining", "Sky Lights", numSkyLightsProcessed, m_skyLightQueue.size() ) );
			log.info( String.format( "\t%16s: %3d processed, %d remaining", "First Lights", numFirstLightsProcessed, m_firstLightQueue.size() ) );
		}
	}
	
	private int processColumnQueue( BatchedSetQueue<Long> queue, long timeStop, int batchSize, ColumnCalculator calculator )
	{
		m_deferredAddresses.clear();
		
		int numProcessed = 0;
		
		// is there time left?
		while( System.currentTimeMillis() < timeStop )
		{
			// get a batch of addresses
			m_addresses.clear();
			queue.getBatch( m_addresses, batchSize );
			
			// nothing left to do?
			if( m_addresses.isEmpty() )
			{
				break;
			}
			
			// process it
			numProcessed += processColumnBatch( calculator );
		}
		
		// put the deferred addresses back on the queue
		for( long address : m_deferredAddresses )
		{
			queue.add( address );
		}
		
		return numProcessed;
	}
	
	private int processColumnBatch( ColumnCalculator calculator )
	{
		// start processing
		int numSuccesses = 0;
		for( long columnAddress : m_addresses )
		{
			// get the column
			int cubeX = AddressTools.getX( columnAddress );
			int cubeZ = AddressTools.getZ( columnAddress );
			Column column = (Column)m_provider.provideChunk( cubeX, cubeZ );
			
			// skip blank columns
			if( column == null || column instanceof BlankColumn )
			{
				continue;
			}
			
			// add unsuccessful calculations back onto the queue
			boolean success = calculator.calculate( column );
			if( !success )
			{
				m_deferredAddresses.add( columnAddress );
			}
			else
			{
				numSuccesses++;
			}
		}
		return numSuccesses;
	}
}
