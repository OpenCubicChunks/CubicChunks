/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 *
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *******************************************************************************/
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
