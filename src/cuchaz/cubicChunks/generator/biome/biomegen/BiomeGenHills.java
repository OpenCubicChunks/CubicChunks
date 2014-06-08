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
package cuchaz.cubicChunks.generator.biome.biomegen;

import cuchaz.cubicChunks.CubeWorldProvider;
import cuchaz.cubicChunks.generator.populator.DecoratorHelper;
import cuchaz.cubicChunks.generator.populator.WorldGenAbstractTreeCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenTaiga2Cube;
import cuchaz.cubicChunks.generator.terrain.NewTerrainProcessor;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.world.Cube;
import java.util.Random;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraft.world.gen.feature.WorldGenerator;

public class BiomeGenHills extends CubeBiomeGenBase
{
	private WorldGenerator theWorldGenerator;
	private WorldGenTaiga2Cube genTaiga;
	private int value1;
	private int value2;
	private int value3;
	private int value4;

	public BiomeGenHills( int biomeID, boolean flag )
	{
		super( biomeID );
		this.theWorldGenerator = new WorldGenMinable( Blocks.monster_egg, 8 );
		this.genTaiga = new WorldGenTaiga2Cube( false );
		this.value1 = 0;
		this.value2 = 1;
		this.value3 = 2;
		this.value4 = this.value1;

		if( flag )
		{
			this.decorator().treesPerChunk = 3;
			this.value4 = this.value2;
		}
	}

	@Override
	public WorldGenAbstractTreeCube checkSpawnTree( Random rand )
	{
		return (rand.nextInt( 3 ) > 0 ? this.genTaiga : super.checkSpawnTree( rand ));
	}

	@Override
	public void decorate( World world, Random rand, int x, int y, int z )
	{
		super.decorate( world, rand, x, y, z );

		DecoratorHelper gen = new DecoratorHelper( world, rand, x, y, z );
		int numGen = 3 + rand.nextInt( 6 );

		gen.generateSingleBlocks( Blocks.emerald_ore, numGen, 1, -0.75 );
		
		//generate silverfish stone (monsteregg)
		gen.generateAtRandomHeight( 7, 1, theWorldGenerator, 0 );
	}

	@Override
	public void modifyBlocks_pre( World world, Random rand, Cube cube, int xAbs, int yAbs, int zAbs, double var )
	{
		this.topBlock = Blocks.grass;
		this.field_150604_aj = 0;
		this.fillerBlock = Blocks.dirt;

		if( (var < -1.0D || var > 2.0D) && this.value4 == this.value3 )
		{
			this.topBlock = Blocks.gravel;
			this.fillerBlock = Blocks.gravel;
		}
		else if( var > 1.0D && this.value4 != this.value2 )
		{
			this.topBlock = Blocks.stone;
			this.fillerBlock = Blocks.stone;
		}

		this.modifyBlocks( world, rand, cube, xAbs, yAbs, zAbs, var );
	}

	@Override
	protected CubeBiomeGenBase createAndReturnMutated()
	{
		return (new BiomeGenHills( this.biomeID + 128, false )).func_150633_b( this );
	}

	private BiomeGenHills func_150633_b( CubeBiomeGenBase biome )
	{
		this.value4 = this.value3;
		this.func_150557_a( biome.color, true );
		this.setBiomeName( biome.biomeName + " M" );
		this.setHeightRange( new CubeBiomeGenBase.Height( biome.biomeHeight, biome.biomeVolatility ) );
		this.setTemperatureAndRainfall( biome.temperature, biome.rainfall );
		return this;
	}
}
