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
package cubicchunks;

import cubicchunks.generator.GeneratorPipeline;
import cubicchunks.generator.biome.WorldColumnManager;
import cubicchunks.generator.biome.WorldColumnManagerFlat;
import cubicchunks.generator.biome.biomegen.CubeBiomeGenBase;
import cubicchunks.server.CubeWorldServer;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.FlatGeneratorInfo;
<<<<<<< HEAD:src/cuchaz/cubicChunks/CubeWorldProvider.java
import cuchaz.cubicChunks.generator.GeneratorPipeline;
import cuchaz.cubicChunks.generator.biome.alternateGen.AlternateWorldColumnManager;
import cuchaz.cubicChunks.generator.biome.alternateGen.NewAlternateWorldColumnManager;
import cuchaz.cubicChunks.generator.biome.WorldColumnManager;
import cuchaz.cubicChunks.generator.biome.WorldColumnManagerFlat;
import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;
import cuchaz.cubicChunks.generator.terrain.GlobalGeneratorConfig;
import cuchaz.cubicChunks.generator.terrain.NewAlternateTerrainProcessor;
import cuchaz.cubicChunks.generator.terrain.NewTerrainProcessor;
import cuchaz.cubicChunks.server.CubeWorldServer;
=======
>>>>>>> 0c6cf2e... Refactored the package structure:src/main/java/cubicchunks/CubeWorldProvider.java

public abstract class CubeWorldProvider extends WorldProvider
{
	@Override
	protected void registerWorldChunkManager( )
	{
		// NOTE: this is the place we plug in different WorldColumnManagers for
		// different dimensions or world types
		
		if( worldObj.getWorldInfo().getTerrainType() == WorldType.FLAT )
		{
			FlatGeneratorInfo info = FlatGeneratorInfo.createFlatGeneratorFromString( worldObj.getWorldInfo().getGeneratorOptions() );
			worldChunkMgr = new WorldColumnManagerFlat( CubeBiomeGenBase.getBiome( info.getBiome() ), 0.5F );
		}
		else if( worldObj instanceof CubeWorldServer )
		{
			worldChunkMgr = new WorldColumnManager( (CubeWorldServer)worldObj );
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
	
	public abstract int getSeaLevel( );
}
