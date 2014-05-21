package cuchaz.cubicChunks.generator;

import java.util.List;

import net.minecraft.world.World;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;

import cuchaz.cubicChunks.CubeProvider;
import cuchaz.cubicChunks.util.QueueProcessor;

public class GeneratorPipeline
{
	private static final Logger log = LogManager.getLogger();
	
	private static final int TickBudget = 100; // ms
	
	private World m_world;
	private List<QueueProcessor> m_processors;
	
	public GeneratorPipeline( World world, CubeProvider provider )
	{
		m_world = world;
		m_processors = Lists.newArrayList();
		
		// build the stages of the pipeline
		for( GeneratorStage stage : GeneratorStage.values() )
		{
			m_processors.add( stage.getProcessor( provider ) );
		}
	}
	
	public void tick( )
	{
		long timeStart = System.currentTimeMillis();
		long timeStop = timeStart + TickBudget;
		
		// process the queues
		int numProcessed = 0;
		for( QueueProcessor processor : m_processors )
		{
			numProcessed += processor.processQueue( timeStop );
		}
		
		// reporting
		long timeDiff = System.currentTimeMillis() - timeStart;
		if( numProcessed > 0 )
		{
			log.info( String.format( "%s Generation pipeline processed %d cubes in %d ms.",
				m_world.isClient ? "CLIENT" : "SERVER",
				numProcessed, timeDiff
			) );
			for( QueueProcessor processor : m_processors )
			{
				log.info( processor.getProcessingReport() );
			}
		}
	}
}
