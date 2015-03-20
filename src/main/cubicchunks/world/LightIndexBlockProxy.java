/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 * 
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package cubicchunks.world;

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
