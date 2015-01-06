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
package cubicchunks.lighting;

import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cubicchunks.CubeProvider;
import cubicchunks.util.Bits;
import cubicchunks.world.Column;

public class LightingManager
{
	private static final Logger log = LogManager.getLogger();
	
	private static final int TickBudget = 40; // ms. Only 50 ms in a tick
	
	private World m_world;
	private SkyLightOcclusionProcessor m_skyLightOcclusionProcessor;
	private FirstLightProcessor m_firstLightProcessor;
	private DiffuseLightingCalculator m_diffuseLightingCalculator;
	private SkyLightUpdateCalculator m_skyLightUpdateCalculator;
	
	public LightingManager( World world, CubeProvider provider )
	{
		m_world = world;
		
		m_skyLightOcclusionProcessor = new SkyLightOcclusionProcessor( "Sky Light Occlusion", provider, 50 );
		m_firstLightProcessor = new FirstLightProcessor( "First Light", provider, 10 );
		m_diffuseLightingCalculator = new DiffuseLightingCalculator();
		m_skyLightUpdateCalculator = new SkyLightUpdateCalculator();
	}
	
	public void queueSkyLightOcclusionCalculation( int blockX, int blockZ )
	{
		long blockColumnAddress =
			Bits.packSignedToLong( blockX, 26, 0 )
			| Bits.packSignedToLong( blockZ, 26, 26 );
		m_skyLightOcclusionProcessor.add( blockColumnAddress );
	}
	
	public void queueFirstLightCalculation( long cubeAddress )
	{
		m_firstLightProcessor.add( cubeAddress );
	}
	
	public boolean computeDiffuseLighting( int blockX, int blockY, int blockZ, EnumSkyBlock lightType )
	{
		return m_diffuseLightingCalculator.calculate( m_world, blockX, blockY, blockZ, lightType );
	}
	
	public void computeSkyLightUpdate( Column column, int localX, int localZ, int oldMaxBlockY, int newMaxBlockY )
	{
		m_skyLightUpdateCalculator.calculate( column, localX, localZ, oldMaxBlockY, newMaxBlockY );
	}
	
	public void tick( )
	{
		long timeStart = System.currentTimeMillis();
		long timeStop = timeStart + TickBudget;
		
		// process the queues
		int numProcessed = 0;
		numProcessed += m_skyLightOcclusionProcessor.processQueue( timeStop );
		numProcessed += m_firstLightProcessor.processQueue( timeStop );
		
		// reporting
		long timeDiff = System.currentTimeMillis() - timeStart;
		if( numProcessed > 0 )
		{
			log.info( String.format( "%s Lighting manager processed %d calculations in %d ms.",
				m_world.isClient ? "CLIENT" : "SERVER",
				numProcessed, timeDiff
			) );
			log.info( m_skyLightOcclusionProcessor.getProcessingReport() );
			log.info( m_firstLightProcessor.getProcessingReport() );
		}
	}
}
