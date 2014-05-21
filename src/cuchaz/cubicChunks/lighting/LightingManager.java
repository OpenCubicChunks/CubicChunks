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

import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cuchaz.cubicChunks.CubeProvider;
import cuchaz.cubicChunks.util.Bits;
import cuchaz.cubicChunks.world.Column;

public class LightingManager
{
	private static final Logger log = LogManager.getLogger();
	
	private static final int TickBudget = 100; // ms
	
	private World m_world;
	private SkyLightProcessor m_skyLightProcessor;
	private SkyLightOcclusionProcessor m_skyLightOcclusionProcessor;
	private DiffuseLightingCalculator m_diffuseLightingCalculator;
	private SkyLightUpdateCalculator m_skyLightUpdateCalculator;
	
	public LightingManager( World world, CubeProvider provider )
	{
		m_world = world;
		
		m_skyLightProcessor = new SkyLightProcessor( "Sky Light", provider, 100 );
		m_skyLightOcclusionProcessor = new SkyLightOcclusionProcessor( "Sky Light Occlusion", provider, 50 );
		
		m_diffuseLightingCalculator = new DiffuseLightingCalculator();
		m_skyLightUpdateCalculator = new SkyLightUpdateCalculator();
	}
	
	public void queueSkyLightCalculation( long columnAddress )
	{
		m_skyLightProcessor.add( columnAddress );
	}
	
	public void queueSkyLightOcclusionCalculation( int blockX, int blockZ )
	{
		long blockColumnAddress =
			Bits.packSignedToLong( blockX, 26, 0 )
			| Bits.packSignedToLong( blockZ, 26, 26 );
		m_skyLightOcclusionProcessor.add( blockColumnAddress );
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
		int numSkyLightsProcessed = m_skyLightProcessor.processQueue( timeStop );
		int numSkyLightOcclusionsProcessed = m_skyLightOcclusionProcessor.processQueue( timeStop );
		
		// reporting
		long timeDiff = System.currentTimeMillis() - timeStart;
		int totalProcessed = numSkyLightsProcessed + numSkyLightOcclusionsProcessed;
		if( totalProcessed > 0 )
		{
			log.info( String.format( "%s Lighting manager processed %d calculations in %d ms.",
				m_world.isClient ? "CLIENT" : "SERVER",
				totalProcessed, timeDiff
			) );
			log.info( m_skyLightProcessor.getProcessingReport() );
			log.info( m_skyLightOcclusionProcessor.getProcessingReport() );
		}
	}
}
