/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 Tall Worlds
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.visibility;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.Sets;

import cubicchunks.util.AddressTools;

public abstract class CubeSelector {
	
	private TreeSet<Long> m_visibleCubes;
	private TreeSet<Long> m_newlyVisibleCubes;
	private TreeSet<Long> m_newlyHiddenCubes;
	private TreeSet<Long> m_nextVisibleCubes;
	
	private TreeSet<Long> m_visibleColumns;
	private TreeSet<Long> m_newlyVisibleColumns;
	private TreeSet<Long> m_newlyHiddenColumns;
	private TreeSet<Long> m_nextVisibleColumns;
	
	public CubeSelector() {
		m_visibleCubes = Sets.newTreeSet();
		m_newlyVisibleCubes = Sets.newTreeSet();
		m_newlyHiddenCubes = Sets.newTreeSet();
		m_nextVisibleCubes = Sets.newTreeSet();
		
		m_visibleColumns = Sets.newTreeSet();
		m_newlyVisibleColumns = Sets.newTreeSet();
		m_newlyHiddenColumns = Sets.newTreeSet();
		m_nextVisibleColumns = Sets.newTreeSet();
	}
	
	public void setPlayerPosition(long address, int viewDistance) {
		
		int cubeX = AddressTools.getX(address);
		int cubeY = AddressTools.getY(address);
		int cubeZ = AddressTools.getZ(address);
		
		// compute the cube visibility
		m_nextVisibleCubes.clear();
		computeVisibleCubes(m_nextVisibleCubes, cubeX, cubeY, cubeZ, viewDistance);
		
		m_newlyVisibleCubes.clear();
		m_newlyVisibleCubes.addAll(m_nextVisibleCubes);
		m_newlyVisibleCubes.removeAll(m_visibleCubes);
		
		m_newlyHiddenCubes.clear();
		m_newlyHiddenCubes.addAll(m_visibleCubes);
		m_newlyHiddenCubes.removeAll(m_nextVisibleCubes);
		
		// compute the column visibility
		m_nextVisibleColumns.clear();
		for (long cubeAddress : m_nextVisibleCubes) {
			m_nextVisibleColumns.add(AddressTools.cubeToColumn(cubeAddress));
		}
		
		m_newlyVisibleColumns.clear();
		m_newlyVisibleColumns.addAll(m_nextVisibleColumns);
		m_newlyVisibleColumns.removeAll(m_visibleColumns);
		
		m_newlyHiddenColumns.clear();
		m_newlyHiddenColumns.addAll(m_visibleColumns);
		m_newlyHiddenColumns.removeAll(m_nextVisibleColumns);
		
		// swap the buffers
		TreeSet<Long> swap = m_visibleCubes;
		m_visibleCubes = m_nextVisibleCubes;
		m_nextVisibleCubes = swap;
		swap = m_visibleColumns;
		m_visibleColumns = m_nextVisibleColumns;
		m_nextVisibleColumns = swap;
	}
	
	public Set<Long> getVisibleCubes() {
		return m_visibleCubes;
	}
	
	public Set<Long> getNewlyVisibleCubes() {
		return m_newlyVisibleCubes;
	}
	
	public Set<Long> getNewlyHiddenCubes() {
		return m_newlyHiddenCubes;
	}
	
	public boolean isCubeVisible(long address) {
		return m_visibleCubes.contains(address);
	}
	
	protected abstract void computeVisibleCubes(Collection<Long> out, int cubeX, int cubeY, int cubeZ, int viewDistance);
	
	public Set<Long> getVisibleColumns() {
		return m_visibleColumns;
	}
	
	public Set<Long> getNewlyVisibleColumns() {
		return m_newlyVisibleColumns;
	}
	
	public Set<Long> getNewlyHiddenColumns() {
		return m_newlyHiddenColumns;
	}
	
	public boolean isColumnVisible(long address) {
		return m_visibleColumns.contains(address);
	}
}