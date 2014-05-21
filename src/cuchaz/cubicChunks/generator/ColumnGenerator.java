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

import net.minecraft.world.World;
import cuchaz.cubicChunks.generator.biome.WorldColumnManager;
import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.world.Column;

public class ColumnGenerator
{
	private World m_world;
	private WorldColumnManager m_worldColumnManager;
	private CubeBiomeGenBase[] m_biomes;
	
	public ColumnGenerator( World world )
	{
		m_world = world;
		m_biomes = null;
		
		m_worldColumnManager = new WorldColumnManager(this.m_world);
	}
	
	public Column generateColumn( int cubeX, int cubeZ )
	{
		// generate biome info. This is a hackjob.
		m_biomes = m_worldColumnManager.loadBlockGeneratorData(
			m_biomes,
			Coords.cubeToMinBlock( cubeX ), Coords.cubeToMinBlock( cubeZ ),
			16, 16
		);
		
		// UNDONE: generate temperature map
		// UNDONE: generate rainfall map
		
		return new Column( m_world, cubeX, cubeZ, m_biomes );
	}
}
