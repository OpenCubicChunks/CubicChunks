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
package cuchaz.cubicChunks.world;

import java.util.TreeMap;

import net.minecraft.block.BlockAir;

@Deprecated
public class LightIndexBlockProxy extends BlockAir
{
	private static TreeMap<Integer,LightIndexBlockProxy> m_cache;
	
	static
	{
		m_cache = new TreeMap<Integer,LightIndexBlockProxy>();
	}
	
	public static LightIndexBlockProxy get( int opacity )
	{
		LightIndexBlockProxy proxy = m_cache.get( opacity );
		if( proxy == null )
		{
			proxy = new LightIndexBlockProxy( opacity );
			m_cache.put( opacity, proxy );
		}
		return proxy;
	}
	
	private int m_opacity;
	
	protected LightIndexBlockProxy( int opacity )
	{
		m_opacity = opacity;
	}
	
	@Override
	public int getLightOpacity( )
	{
		return m_opacity;
	}
	
	@Override
	public int getLightValue( )
	{
		// assume lights in unloaded cubes are turned off
		return 0;
	}
	
	@Override
	public boolean isOpaqueCube( )
	{
		return m_opacity == 255;
	}
}
