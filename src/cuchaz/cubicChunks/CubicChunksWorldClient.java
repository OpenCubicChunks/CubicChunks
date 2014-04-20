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

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.IChunkProvider;
import cuchaz.cubicChunks.accessors.WorldClientAccessor;

public class CubicChunksWorldClient extends WorldClient
{
	public CubicChunksWorldClient( NetHandlerPlayClient client, WorldSettings settings, int dimension, EnumDifficulty difficulty, Profiler profiler )
	{
		super( client, settings, dimension, difficulty, profiler );
	}
	
	@Override
	protected IChunkProvider createChunkProvider()
    {
		CubicChunkProviderClient chunkProvider = new CubicChunkProviderClient( this );
		WorldClientAccessor.setChunkProvider( this, chunkProvider );
		return chunkProvider;
    }
}
