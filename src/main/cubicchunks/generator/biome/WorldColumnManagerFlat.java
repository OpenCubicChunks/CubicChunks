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

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import cubicchunks.generator.biome.biomegen.CubeBiomeGenBase;
import net.minecraft.world.ChunkPosition;

public class WorldColumnManagerFlat extends WorldColumnManager
{
	/** The biome generator object. */
	private CubeBiomeGenBase biomeGenerator;
	
	/** The rainfall in the world */
	private float rainfall; // this is hell, there IS no rain.
	
	public WorldColumnManagerFlat( CubeBiomeGenBase p_i45374_1_, float p_i45374_2_ )
	{
		this.biomeGenerator = p_i45374_1_;
		this.rainfall = p_i45374_2_;
	}
	
	/**
	 * Returns the BiomeGenBase related to the x, z position on the world.
	 */
	public CubeBiomeGenBase getBiomeGenAt( int par1, int par2 )
	{
		return this.biomeGenerator;
	}
	
	/**
	 * Returns an array of biomes for the location input.
	 * 
	 * It ignores the cubeX, cubeZ input since it doesn't care, it's exactly the
	 * same biome everywhere in the nether.
	 */
	public CubeBiomeGenBase[] getBiomesForGeneration( CubeBiomeGenBase[] aBiomeGenBase, int par2, int par3, int width, int length )
	{
		if( aBiomeGenBase == null || aBiomeGenBase.length < width * length )
		{
			aBiomeGenBase = new CubeBiomeGenBase[width * length];
		}
		
		Arrays.fill( aBiomeGenBase, 0, width * length, this.biomeGenerator ); // fills
																				// the
																				// array
																				// with
																				// biome
																				// 0.
																				// same
																				// biome
																				// everywhere,
																				// then.
		return aBiomeGenBase;
	}
	
	/**
	 * Returns a list of rainfall values for the specified blocks. Args:
	 * listToReuse, x, z, width, length.
	 */
	public float[] getRainfall( float[] aFloat, int par2, int par3, int width, int length )
	{
		if( aFloat == null || aFloat.length < width * length )
		{
			aFloat = new float[width * length];
		}
		
		Arrays.fill( aFloat, 0, width * length, this.rainfall );
		return aFloat;
	}
	
	/**
	 * Returns biomes to use for the blocks and loads the other data like
	 * temperature and humidity onto the WorldChunkManager Args: oldBiomeList,
	 * x, z, width, depth
	 */
	public CubeBiomeGenBase[] loadBlockGeneratorData( CubeBiomeGenBase[] par1ArrayOfBiomeGenBase, int par2, int par3, int par4, int par5 )
	{
		if( par1ArrayOfBiomeGenBase == null || par1ArrayOfBiomeGenBase.length < par4 * par5 )
		{
			par1ArrayOfBiomeGenBase = new CubeBiomeGenBase[par4 * par5];
		}
		
		Arrays.fill( par1ArrayOfBiomeGenBase, 0, par4 * par5, this.biomeGenerator );
		return par1ArrayOfBiomeGenBase;
	}
	
	/**
	 * Return a list of biomes for the specified blocks. Args: listToReuse, x,
	 * y, width, length, cacheFlag (if false, don't check biomeCache to avoid
	 * infinite loop in BiomeCacheBlock)
	 */
	public CubeBiomeGenBase[] getBiomeGenAt( CubeBiomeGenBase[] par1ArrayOfBiomeGenBase, int par2, int par3, int par4, int par5, boolean par6 )
	{
		return this.loadBlockGeneratorData( par1ArrayOfBiomeGenBase, par2, par3, par4, par5 );
	}
	
	public ChunkPosition func_150795_a( int p_150795_1_, int p_150795_2_, int p_150795_3_, List p_150795_4_, Random p_150795_5_ )
	{
		return p_150795_4_.contains( this.biomeGenerator ) ? new ChunkPosition( p_150795_1_ - p_150795_3_ + p_150795_5_.nextInt( p_150795_3_ * 2 + 1 ), 0, p_150795_2_ - p_150795_3_
				+ p_150795_5_.nextInt( p_150795_3_ * 2 + 1 ) ) : null;
	}
	
	/**
	 * checks given Chunk's Biomes against List of allowed ones
	 */
	public boolean areBiomesViable( int par1, int par2, int par3, List biomeList )
	{
		return biomeList.contains( this.biomeGenerator );
	}
}
