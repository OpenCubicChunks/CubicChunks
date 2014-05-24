/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 *     Nick Whitney - modification and use of the builder
 ******************************************************************************/
package cuchaz.cubicChunks.generator;

import java.util.Random;

import libnoiseforjava.exception.ExceptionInvalidParam;
import net.minecraft.init.Blocks;
import cuchaz.cubicChunks.generator.builder.ComplexWorldBuilder;
import cuchaz.cubicChunks.server.CubeWorldServer;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.world.Cube;

public class ComplexTerrainProcessor extends TerrainProcessor
{
	private ComplexWorldBuilder builder;
	private int m_seaLevel;
	
	public ComplexTerrainProcessor( String name, CubeWorldServer worldServer, int batchSize )
	{
		super( name, worldServer, batchSize );
		
		m_seaLevel = worldServer.getCubeWorldProvider().getSeaLevel();
		
		// UNDONE: switch worldBuilders based on worldType here
		builder = new ComplexWorldBuilder();
		builder.setSeed( new Random( worldServer.getSeed() ).nextInt() );
		builder.setMaxElev( 256 );
		builder.setSeaLevel( worldServer.getCubeWorldProvider().getSeaLevel() );
		builder.setModOctaves( 0 );
		try
		{
			builder.build();
		}
		catch( ExceptionInvalidParam ex )
		{
			// this should really be an illegal argument exception,
			// but apparently the author of libnoiseforjava doesn't follow conventions
			throw new IllegalArgumentException( ex );
		}
	}
	
	@Override
	protected void generateTerrain( Cube cube )
	{
		for( int xRel=0; xRel < 16; xRel++ )
		{
			int xAbs = cube.getX() << 4 | xRel;
			
			for( int zRel=0; zRel < 16; zRel++ )
			{				
				int zAbs = cube.getZ() << 4 | zRel;
				
				double val = builder.getValue(xAbs, 0, zAbs);
				
				for( int yRel=0; yRel < 16; yRel++ )
				{
					int yAbs = Coords.localToBlock( cube.getY(), yRel );
					
					if( val - yAbs > 0.0D )
					{
						cube.setBlockForGeneration( xRel, yRel, zRel, Blocks.stone );
					}
					else if( yAbs < m_seaLevel )
					{
						cube.setBlockForGeneration( xRel, yRel, zRel, Blocks.water );
					}
					else
					{
						cube.setBlockForGeneration( xRel, yRel, zRel, null );
					}
				}		
			}
		}
	}
}
