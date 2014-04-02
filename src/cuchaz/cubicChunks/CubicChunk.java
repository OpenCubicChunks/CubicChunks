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
package cuchaz.cubicChunks;

import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class CubicChunk
{
	private World m_world;
	private int m_x;
	private int m_y;
	private int m_z;
	private ExtendedBlockStorage m_storage;
	
	public CubicChunk( World world, int x, int y, int z, boolean hasSky )
	{
		m_world = world;
		m_x = x;
		m_y = y;
		m_z = z;
		m_storage = new ExtendedBlockStorage( y << 4, hasSky );
	}
	
	public long getAddress( )
	{
		return AddressTools.getAddress( m_world.provider.dimensionId, m_x, m_y, m_z );
	}
	
	public World getWorld( )
	{
		return m_world;
	}

	public int getX( )
	{
		return m_x;
	}

	public int getY( )
	{
		return m_y;
	}

	public int getZ( )
	{
		return m_z;
	}
	
	public ExtendedBlockStorage getStorage( )
	{
		return m_storage;
	}
}
