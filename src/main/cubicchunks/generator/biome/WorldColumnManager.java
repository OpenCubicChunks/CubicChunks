/*******************************************************************************
 * This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 *
 * Copyright (c) Tall Worlds
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *******************************************************************************/
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
