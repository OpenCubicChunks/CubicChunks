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
package cubicchunks.generator.biome.biomegen;

import cuchaz.cubicChunks.generator.populator.WorldGenAbstractTreeCube;
import cuchaz.cubicChunks.world.Cube;

import java.util.ArrayList;
import java.util.Random;

import cubicchunks.world.Cube;
import net.minecraft.world.World;
<<<<<<< HEAD:src/cuchaz/cubicChunks/generator/biome/biomegen/BiomeGenMutated.java
import net.minecraft.world.biome.BiomeGenBase;
=======
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
>>>>>>> 0c6cf2e... Refactored the package structure:src/main/java/cubicchunks/generator/biome/biomegen/BiomeGenMutated.java

public class BiomeGenMutated extends CubeBiomeGenBase
{
	protected final CubeBiomeGenBase biome;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public BiomeGenMutated( int biomeID, CubeBiomeGenBase biome )
	{
		super( biomeID );
		this.biome = biome;
		this.func_150557_a( biome.color, true );
		this.biomeName = biome.biomeName + " M";
		this.topBlock = biome.topBlock;
		this.fillerBlock = biome.fillerBlock;
		this.field_76754_C = biome.field_76754_C;
		this.biomeHeight = biome.biomeHeight;
		this.biomeVolatility = biome.biomeVolatility;
		this.temperature = biome.temperature;
		this.rainfall = biome.rainfall;
		this.waterColorMultiplier = biome.waterColorMultiplier;
		this.enableSnow = biome.getEnableSnow();
		this.enableRain = biome.getEnableRain();
		this.spawnableCreatureList = new ArrayList( biome.getSpawnableCreatureList() );
		this.spawnableMonsterList = new ArrayList( biome.getSpawnableMonsterList() );
		this.spawnableCaveCreatureList = new ArrayList( biome.getSpawnableCaveCreatureList() );
		this.spawnableWaterCreatureList = new ArrayList( biome.getSpawnableWaterCreatureList() );
		this.temperature = biome.temperature;
		this.rainfall = biome.rainfall;
		this.biomeHeight = biome.biomeHeight + 0.1F;
		this.biomeVolatility = biome.biomeVolatility + 0.2F;
	}

	/**
	 * returns the chance a creature has to spawn.
	 */
	@Override
	public float getSpawningChance()
	{
		return this.biome.getSpawningChance();
	}

	@Override
	public WorldGenAbstractTreeCube checkSpawnTree( Random p_150567_1_ )
	{
		return this.biome.checkSpawnTree( p_150567_1_ );
	}

	/**
	 * Provides the basic foliage color based on the biome temperature and rainfall
	 */
	@Override
	public int getBiomeFoliageColor( int p_150571_1_, int p_150571_2_, int p_150571_3_ )
	{
		return this.biome.getBiomeFoliageColor( p_150571_1_, p_150571_2_, p_150571_2_ );
	}

	/**
	 * Provides the basic grass color based on the biome temperature and rainfall
	 */
	@Override
	public int getBiomeGrassColor( int p_150558_1_, int p_150558_2_, int p_150558_3_ )
	{
		return this.biome.getBiomeGrassColor( p_150558_1_, p_150558_2_, p_150558_2_ );
	}

	@Override
	public Class<? extends CubeBiomeGenBase> func_150562_l()
	{
		return this.biome.func_150562_l();
	}

	@Override
	public boolean func_150569_a( BiomeGenBase biome )
	{
		return this.biome.func_150569_a( biome );
	}

	@Override
	public CubeBiomeGenBase.TempCategory func_150561_m()
	{
		return this.biome.func_150561_m();
	}
	
	@Override
	public void replaceBlocks( World world, Random rand, Cube cube, Cube above, int xAbs, int zAbs, int top, int bottom, int alterationTop, int seaLevel, double depthNoiseValue )
	{
		biome.replaceBlocks_do( world, rand, cube, above, xAbs, zAbs, top, bottom, alterationTop, seaLevel, depthNoiseValue );
	}
}
