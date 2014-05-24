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

import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;
import cuchaz.cubicChunks.server.CubeWorldServer;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.world.Column;

public class ColumnGenerator
{
	private CubeWorldServer m_worldServer;
	private CubeBiomeGenBase[] m_biomes;
	
	public ColumnGenerator( CubeWorldServer worldServer )
	{
		m_worldServer = worldServer;
		m_biomes = null;
	}
	
	public Column generateColumn( int cubeX, int cubeZ )
	{
		// generate biome info. This is a hackjob.
		m_biomes = (CubeBiomeGenBase[])m_worldServer.getCubeWorldProvider().getWorldColumnMananger().loadBlockGeneratorData(
			m_biomes,
			Coords.cubeToMinBlock( cubeX ), Coords.cubeToMinBlock( cubeZ ),
			16, 16
		);
		
		// UNDONE: generate temperature map
		// UNDONE: generate rainfall map
		
		return new Column( m_worldServer, cubeX, cubeZ, m_biomes );
	}
}
