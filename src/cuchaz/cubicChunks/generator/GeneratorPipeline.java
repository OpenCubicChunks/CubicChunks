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
package cuchaz.cubicChunks.generator;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;

import cuchaz.cubicChunks.CubeProvider;
import cuchaz.cubicChunks.util.AddressTools;
import cuchaz.cubicChunks.util.CubeProcessor;
import cuchaz.cubicChunks.util.QueueProcessor;
import cuchaz.cubicChunks.world.Cube;

public class GeneratorPipeline
{
	private static final Logger log = LogManager.getLogger();
	
	private static final int TickBudget = 40; // ms. There are only 50 ms per tick
	
	private CubeProvider m_provider;
	private List<QueueProcessor> m_processors;
	
	public GeneratorPipeline( CubeProvider provider )
	{
		m_provider = provider;
		m_processors = Lists.newArrayList();
		
		// allocate space for the stages
		for( GeneratorStage stage : GeneratorStage.values() )
		{
			if( !stage.isLastStage() )
			{
				m_processors.add( null );
			}
		}
	}
	
	public void addStage( GeneratorStage stage, CubeProcessor processor )
	{
		m_processors.set( stage.ordinal(), processor );
	}
	
	public void checkStages( )
	{
		for( GeneratorStage stage : GeneratorStage.values() )
		{
			if( !stage.isLastStage() )
			{
				if( m_processors.get( stage.ordinal() ) == null )
				{
					throw new Error( "Generator pipline configured incorrectly! Stage " + stage.name() + " is null! Fix your CubeWorldProvider.createGeneratorPipeline()!" );
				}
			}
		}
	}
	
	public void generate( Cube cube )
	{
		GeneratorStage stage = cube.getGeneratorStage();
		if( !stage.isLastStage() )
		{
			m_processors.get( stage.ordinal() ).add( cube.getAddress() );
		}
	}
	
	public int getNumCubes( )
	{
		int num = 0;
		for( GeneratorStage stage : GeneratorStage.values() )
		{
			if( !stage.isLastStage() )
			{
				num += m_processors.get( stage.ordinal() ).getNumInQueue();
			}
		}
		return num;
	}
	
	public int tick( )
	{
		long timeStart = System.currentTimeMillis();
		long timeStop = timeStart + TickBudget;
		
		// process the queues
		int numProcessed = 0;
		for( int stage=0; stage<m_processors.size(); stage++ )
		{
			QueueProcessor processor = m_processors.get( stage );
			numProcessed += processor.processQueue( timeStop );
			
			// move the processed entries into the next stage of the pipeline
			int nextStage = stage + 1;
			for( long address : processor.getProcessedAddresses() )
			{
				// set the generator stage flag on the cube
				int cubeX = AddressTools.getX( address );
				int cubeY = AddressTools.getY( address );
				int cubeZ = AddressTools.getZ( address );
				m_provider.provideCube( cubeX, cubeY, cubeZ ).setGeneratorStage( GeneratorStage.values()[nextStage] );
				
				// advance the address to the next stage
				if( nextStage < m_processors.size() )
				{
					m_processors.get( nextStage ).add( address );
				}
			}
		}
		
		// reporting
		long timeDiff = System.currentTimeMillis() - timeStart;
		if( numProcessed > 0 )
		{
			log.info( String.format( "Generation pipeline processed %d cubes in %d ms.",
				numProcessed, timeDiff
			) );
			for( QueueProcessor processor : m_processors )
			{
				log.info( processor.getProcessingReport() );
			}
		}
		
		return numProcessed;
	}
	
	public void generateAll( )
	{
		while( tick() > 0 );
	}
}
