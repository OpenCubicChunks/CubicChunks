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

import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.FlatGeneratorInfo;
import cuchaz.cubicChunks.generator.GeneratorPipeline;
import cuchaz.cubicChunks.generator.biome.WorldColumnManager;
import cuchaz.cubicChunks.generator.biome.WorldColumnManagerFlat;
import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;
import cuchaz.cubicChunks.server.CubeWorldServer;

public abstract class CubeWorldProvider extends WorldProvider
{
	@Override
	protected void registerWorldChunkManager()
    {
		if( worldObj.getWorldInfo().getTerrainType() == WorldType.FLAT )
		{
			FlatGeneratorInfo info = FlatGeneratorInfo.createFlatGeneratorFromString( worldObj.getWorldInfo().getGeneratorOptions() );
			worldChunkMgr = new WorldColumnManagerFlat( CubeBiomeGenBase.getBiome( info.getBiome() ), 0.5F );
		}
		else
		{
			worldChunkMgr = new WorldColumnManager( worldObj );
		}
    }
	
	public WorldColumnManager getWorldColumnMananger( )
	{
		return (WorldColumnManager)worldChunkMgr;
	}
	
	public abstract GeneratorPipeline createGeneratorPipeline( CubeWorldServer worldServer );
}
