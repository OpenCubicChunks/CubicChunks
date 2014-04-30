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
package cuchaz.cubicChunks.visibility;

import java.util.Collection;
import java.util.TreeSet;

import com.google.common.collect.Sets;

import cuchaz.cubicChunks.util.AddressTools;

public abstract class CubeSelector
{
	private TreeSet<Long> m_visible;
	private TreeSet<Long> m_newlyVisible;
	private TreeSet<Long> m_newlyHidden;
	private TreeSet<Long> m_nextVisible;
	
	public CubeSelector( )
	{
		m_visible = Sets.newTreeSet();
		m_newlyVisible = Sets.newTreeSet();
		m_newlyHidden = Sets.newTreeSet();
		m_nextVisible = Sets.newTreeSet();
	}
	
	public void setPlayerPosition( long address, int viewDistance )
	{
		int chunkX = AddressTools.getX( address );
		int chunkY = AddressTools.getY( address );
		int chunkZ = AddressTools.getZ( address );
		
		// compute the cubic chunk visibility
		m_nextVisible.clear();
		computeVisible( m_nextVisible, chunkX, chunkY, chunkZ, viewDistance );
		
		m_newlyVisible.clear();
		m_newlyVisible.addAll( m_nextVisible );
		m_newlyVisible.removeAll( m_visible );
		
		m_newlyHidden.clear();
		m_newlyHidden.addAll( m_visible );
		m_newlyHidden.removeAll( m_nextVisible );
		
		// swap the buffers
		TreeSet<Long> swap = m_visible;
		m_visible = m_nextVisible;
		m_nextVisible = swap;
	}
	
	public Iterable<Long> getVisibleCubes( )
	{
		return m_visible;
	}
	
	public Iterable<Long> getNewlyVisibleCubes( )
	{
		return m_newlyVisible;
	}
	
	public Iterable<Long> getNewlyHiddenCubes( )
	{
		return m_newlyHidden;
	}
	
	public boolean isVisible( long address )
	{
		return m_visible.contains( address );
	}
	
	protected abstract void computeVisible( Collection<Long> out, int chunkX, int chunkY, int chunkZ, int viewDistance );
}
