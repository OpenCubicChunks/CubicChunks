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
package cubicchunks.client;

import cubicchunks.CubeProvider;
import cubicchunks.CubeProviderTools;
import cubicchunks.CubeWorld;
import cubicchunks.accessors.WorldClientAccessor;
import cubicchunks.lighting.LightingManager;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.IChunkProvider;

public class CubeWorldClient extends WorldClient implements CubeWorld
{
	private LightingManager m_lightingManager;
	
	public CubeWorldClient( NetHandlerPlayClient client, WorldSettings settings, int dimension, EnumDifficulty difficulty, Profiler profiler )
	{
		super( client, settings, dimension, difficulty, profiler );
	}
	
	@Override
	protected IChunkProvider createChunkProvider()
    {
		CubeProviderClient chunkProvider = new CubeProviderClient( this );
		WorldClientAccessor.setChunkProvider( this, chunkProvider );

		// init the lighting manager
		m_lightingManager = new LightingManager( this, chunkProvider );
		
		return chunkProvider;
    }
	
	@Override
	public CubeProvider getCubeProvider( )
	{
		return (CubeProvider)chunkProvider;
	}
	
	@Override
	public LightingManager getLightingManager( )
	{
		return m_lightingManager;
	}
	
	@Override
	public void tick( )
	{
		super.tick();
		
		m_lightingManager.tick();
	}
	
	@Override
	public boolean updateLightByType( EnumSkyBlock lightType, int blockX, int blockY, int blockZ )
    {
		// forward to the new lighting system
		return m_lightingManager.computeDiffuseLighting( blockX, blockY, blockZ, lightType );
    }
	
	@Override
	public boolean checkChunksExist( int minBlockX, int minBlockY, int minBlockZ, int maxBlockX, int maxBlockY, int maxBlockZ )
	{
		return CubeProviderTools.blocksExist( (CubeProvider)chunkProvider, minBlockX, minBlockY, minBlockZ, maxBlockX, maxBlockY, maxBlockZ );
	}
}
