/*******************************************************************************
 * Copyright (c) 2014 jeff.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     jeff - initial API and implementation
 ******************************************************************************/
package cuchaz.cubicChunks;

import java.util.Collection;
import java.util.HashSet;

public class ChunkSet extends HashSet<Coords>
{
	private static final long serialVersionUID = -1018340715197554750L;
	
	public ChunkSet( )
	{
		super();
	}
	
	public ChunkSet( ChunkSet other )
	{
		super( other );
	}
	
	public ChunkSet( Collection<Coords> blocks )
	{
		super();
		addAll( blocks );
	}
}
