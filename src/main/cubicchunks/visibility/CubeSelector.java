/*
 *  This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2014 Tall Worlds
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
	
	private TreeSet<Long> m_visible;
	private TreeSet<Long> m_newlyVisible;
	private TreeSet<Long> m_newlyHidden;
	private TreeSet<Long> m_nextVisible;
	
	public CubeSelector() {
		m_visible = Sets.newTreeSet();
		m_newlyVisible = Sets.newTreeSet();
		m_newlyHidden = Sets.newTreeSet();
		m_nextVisible = Sets.newTreeSet();
	}
	
	public void setPlayerPosition(long address, int viewDistance) {
		int cubeX = AddressTools.getX(address);
		int cubeY = AddressTools.getY(address);
		int cubeZ = AddressTools.getZ(address);
		
		// compute the cube visibility
		m_nextVisible.clear();
		computeVisible(m_nextVisible, cubeX, cubeY, cubeZ, viewDistance);
		
		m_newlyVisible.clear();
		m_newlyVisible.addAll(m_nextVisible);
		m_newlyVisible.removeAll(m_visible);
		
		m_newlyHidden.clear();
		m_newlyHidden.addAll(m_visible);
		m_newlyHidden.removeAll(m_nextVisible);
		
		// swap the buffers
		TreeSet<Long> swap = m_visible;
		m_visible = m_nextVisible;
		m_nextVisible = swap;
	}
	
	public Set<Long> getVisibleCubes() {
		return m_visible;
	}
	
	public Set<Long> getNewlyVisibleCubes() {
		return m_newlyVisible;
	}
	
	public Set<Long> getNewlyHiddenCubes() {
		return m_newlyHidden;
	}
	
	public boolean isVisible(long address) {
		return m_visible.contains(address);
	}
	
	protected abstract void computeVisible(Collection<Long> out, int cubeX, int cubeY, int cubeZ, int viewDistance);
}