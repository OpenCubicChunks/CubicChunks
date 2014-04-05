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

import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;

public class CubicChunkProviderServer extends ChunkProviderServer
{
	public CubicChunkProviderServer( WorldServer world )
	{
		super(
			world,
			new CubicChunkLoader( world.getSaveHandler() ),
			new CubicChunkGenerator( world )
		);
	}
}
