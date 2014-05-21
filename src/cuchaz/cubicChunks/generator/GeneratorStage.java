package cuchaz.cubicChunks.generator;

import cuchaz.cubicChunks.CubeProvider;
import cuchaz.cubicChunks.util.QueueProcessor;

public enum GeneratorStage
{
	Terrain
	{
		@Override
		public QueueProcessor getProcessor( CubeProvider provider )
		{
			// UNDONE
			return null;
		}
	},
	Structures
	{
		@Override
		public QueueProcessor getProcessor( CubeProvider provider )
		{
			// UNDONE
			return null;
		}
	},
	Lighting
	{
		@Override
		public QueueProcessor getProcessor( CubeProvider provider )
		{
			return new LightingProcessor( "First Lighting", provider, 10 );
		}
	};
	
	public abstract QueueProcessor getProcessor( CubeProvider provider );
}
