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

import java.util.Collection;
import java.util.HashSet;

public class AddressSet extends HashSet<Address>
{
	private static final long serialVersionUID = -3137789107020787032L;

	public AddressSet( )
	{
		super();
	}
	
	public AddressSet( AddressSet other )
	{
		super( other );
	}
	
	public AddressSet( Collection<Address> src )
	{
		super();
		addAll( src );
	}
}
