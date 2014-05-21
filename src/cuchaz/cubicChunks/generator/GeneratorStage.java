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

import net.minecraft.world.WorldServer;
import cuchaz.cubicChunks.CubeProvider;
import cuchaz.cubicChunks.lighting.FirstLightProcessor;
import cuchaz.cubicChunks.util.QueueProcessor;

public enum GeneratorStage
{
	Terrain
	{
		@Override
		public QueueProcessor getProcessor( WorldServer worldServer, CubeProvider provider )
		{
			return new TerrainProcessor( "Terrain", provider, 10, worldServer );
		}
	},
	Features
	{
		@Override
		public QueueProcessor getProcessor( WorldServer worldServer, CubeProvider provider )
		{
			return new FeatureProcessor( "Features", provider, 10 );
		}
	},
	Biomes
	{
		@Override
		public QueueProcessor getProcessor( WorldServer worldServer, CubeProvider provider )
		{
			return new BiomeProcessor( "Biomes", provider, 10, worldServer );
		}
	},
	Lighting
	{
		@Override
		public QueueProcessor getProcessor( WorldServer worldServer, CubeProvider provider )
		{
			return new FirstLightProcessor( "Lighting", provider, 10 );
		}
	},
	Population
	{
		@Override
		public QueueProcessor getProcessor( WorldServer worldServer, CubeProvider provider )
		{
			return new PopulationProcessor( "Population", provider, 10 );
		}
	},
	Live;
	
	public QueueProcessor getProcessor( WorldServer worldServer, CubeProvider provider )
	{
		return null;
	}

	public static GeneratorStage getFirstStage( )
	{
		return values()[0];
	}
	
	public static GeneratorStage getLastStage( )
	{
		return values()[values().length - 1];
	}
	
	public boolean isLastStage( )
	{
		return ordinal() == values().length - 1;
	}
}
