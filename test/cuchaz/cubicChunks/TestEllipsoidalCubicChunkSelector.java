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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.TreeSet;

import org.junit.Test;

public class TestEllipsoidalCubicChunkSelector
{
	@Test
	public void small( )
	{
		EllipsoidalCubicChunkSelector selector = new EllipsoidalCubicChunkSelector();
		selector.setPlayerPosition( AddressTools.getAddress( 0, 5, 5, 5 ), 1 );
		
		TreeSet<Long> addresses = (TreeSet<Long>)selector.getVisibleCubicChunks();
		assertTrue( addresses.contains( AddressTools.getAddress( 0, 4, 5, 5 ) ) );
		assertTrue( addresses.contains( AddressTools.getAddress( 0, 5, 5, 4 ) ) );
		assertTrue( addresses.contains( AddressTools.getAddress( 0, 5, 3, 5 ) ) );
		assertTrue( addresses.contains( AddressTools.getAddress( 0, 5, 4, 5 ) ) );
		assertTrue( addresses.contains( AddressTools.getAddress( 0, 5, 5, 5 ) ) );
		assertTrue( addresses.contains( AddressTools.getAddress( 0, 5, 6, 5 ) ) );
		assertTrue( addresses.contains( AddressTools.getAddress( 0, 5, 7, 5 ) ) );
		assertTrue( addresses.contains( AddressTools.getAddress( 0, 5, 5, 6 ) ) );
		assertTrue( addresses.contains( AddressTools.getAddress( 0, 6, 5, 5 ) ) );
		assertEquals( 9, addresses.size() );
	}
}
