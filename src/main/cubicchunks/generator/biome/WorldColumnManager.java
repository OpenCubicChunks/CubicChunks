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
package cubicchunks.generator.biome;

import java.util.ArrayList;
import java.util.List;

<<<<<<< HEAD:src/main/java/cubicchunks/generator/biome/WorldColumnManager.java
<<<<<<< HEAD:src/cuchaz/cubicChunks/generator/biome/WorldColumnManager.java
=======
import main.java.cubicchunks.generator.biome.biomegen.CubeBiomeGenBase;
=======
import cubicchunks.generator.biome.biomegen.CubeBiomeGenBase;
>>>>>>> 69175bb... - Refactored package structure again to remove /java/:src/main/cubicchunks/generator/biome/WorldColumnManager.java
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.ReportedException;
import net.minecraft.world.ChunkPosition;
>>>>>>> 0c6cf2e... Refactored the package structure:src/main/java/cubicchunks/generator/biome/WorldColumnManager.java
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
<<<<<<< HEAD:src/cuchaz/cubicChunks/generator/biome/WorldColumnManager.java
import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;
=======
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;
>>>>>>> 0c6cf2e... Refactored the package structure:src/main/java/cubicchunks/generator/biome/WorldColumnManager.java

public class WorldColumnManager extends WorldChunkManager
{
	private List<CubeBiomeGenBase> biomesToSpawnIn;
	
	protected WorldColumnManager( )
	{
		this.init();
	}
	
	public WorldColumnManager( long seed, WorldType worldType )
	{
		super( seed, worldType );
		this.init();
	}
	
	private void init( )
	{
		this.biomesToSpawnIn = new ArrayList<CubeBiomeGenBase>();
		this.biomesToSpawnIn.add( CubeBiomeGenBase.forest );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.plains );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.taiga );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.taigaHills );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.forestHills );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.jungle );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.jungleHills );
	}
	
	public WorldColumnManager( World world )
	{
		this( world.getSeed(), world.getWorldInfo().getTerrainType() );
	}
	
	@Override
	public List<CubeBiomeGenBase> getBiomesToSpawnIn( )
	{
		return this.biomesToSpawnIn;
	}
	
	@Override
	public CubeBiomeGenBase getBiomeGenAt( int xAbs, int zAbs )
	{
		return CubeBiomeGenBase.getBiome( super.getBiomeGenAt( xAbs, zAbs ).biomeID );
	}
	
	@Override
	public BiomeGenBase[] getBiomesForGeneration( BiomeGenBase[] biomes, int xGenBlock, int zGenBlock, int width, int length )
	{
		BiomeGenBase vanillaBiomes[] = super.getBiomesForGeneration( biomes, xGenBlock, zGenBlock, width, length );
		return this.convertToCubeBiomes( vanillaBiomes );
	}
	
	@Override
	public BiomeGenBase[] getBiomeGenAt( BiomeGenBase[] biomes, int blockX, int blockZ, int width, int length, boolean flag )
	{
		BiomeGenBase vanillaBiomes[] = super.getBiomeGenAt( biomes, blockX, blockZ, width, length, flag );
		return this.convertToCubeBiomes( vanillaBiomes );
	}
	
	private CubeBiomeGenBase[] convertToCubeBiomes( BiomeGenBase[] array )
	{
		CubeBiomeGenBase[] cubeBiomes = new CubeBiomeGenBase[array.length];
		for( int i = 0; i < array.length; i++ )
			cubeBiomes[i] = CubeBiomeGenBase.getBiome( array[i].biomeID );
		return cubeBiomes;
	}
}
