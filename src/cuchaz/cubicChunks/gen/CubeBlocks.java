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
package cuchaz.cubicChunks.gen;

import cuchaz.cubicChunks.util.Bits;
import net.minecraft.block.Block;

public class CubeBlocks
{
	private Block[] m_blocks;
	private byte[] m_meta;
	
	public CubeBlocks( )
	{
		m_blocks = new Block[16*16*16];
		m_meta = new byte[16*16*16];
	}
	
	public Block getBlock( int x, int y, int z )
	{
		return m_blocks[getCoord( x, y, z )];
	}
	public void setBlock( int x, int y, int z, Block val )
	{
		m_blocks[getCoord( x, y, z )] = val;
	}
	
	public int getMeta( int x, int y, int z )
	{
		return m_meta[getCoord( x, y, z )];
	}
	public void setMeta( int x, int y, int z, byte val )
	{
		m_meta[getCoord( x, y, z )] = val;
	}
	
	private int getCoord( int x, int y, int z )
	{
		return Bits.packUnsignedToInt( x, 4, 0 ) | Bits.packUnsignedToInt( y, 4, 4 ) | Bits.packUnsignedToInt( z, 4, 8 );
	}
}
