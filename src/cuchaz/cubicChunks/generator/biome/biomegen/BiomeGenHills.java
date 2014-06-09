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
	private final WorldGenerator theWorldGenerator;
	private final WorldGenTaiga2Cube genTaiga;
	private final int typeDefault;
	private final int typeForest;
	private final int typeMutated;
	private int type;

	public BiomeGenHills( int biomeID, boolean isForest )
	{
		super( biomeID );
		this.theWorldGenerator = new WorldGenMinable( Blocks.monster_egg, 8 );
		this.genTaiga = new WorldGenTaiga2Cube( false );
		this.typeDefault = 0;
		this.typeForest = 1;
		this.typeMutated = 2;
		this.type = this.typeDefault;

		if( isForest )
		{
			this.decorator().treesPerChunk = 3;
			this.type = this.typeForest;
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
	public void replaceBlocks( World world, Random rand, Cube cube, Cube above, int xAbs, int zAbs, int top, int bottom, int alterationTop, int seaLevel, double depthNoiseValue )
	{
		this.topBlock = Blocks.grass;
		this.field_150604_aj = 0;
		this.fillerBlock = Blocks.dirt;

		if( (depthNoiseValue < -1.0D || depthNoiseValue > 2.0D) && this.type == this.typeMutated )
		{
			this.topBlock = Blocks.gravel;
			this.fillerBlock = Blocks.gravel;
		}
		else if( depthNoiseValue > 1.0D && this.type != this.typeForest )
		{
			this.topBlock = Blocks.stone;
			this.fillerBlock = Blocks.stone;
		}

		this.replaceBlocks_do( world, rand, cube, above, xAbs, zAbs, top, bottom, alterationTop, seaLevel, depthNoiseValue );
	}

	@Override
	protected CubeBiomeGenBase createAndReturnMutated()
	{
		return (new BiomeGenHills( this.biomeID + 128, false )).func_150633_b( this );
	}

	private BiomeGenHills func_150633_b( CubeBiomeGenBase biome )
	{
		this.type = this.typeMutated;
		this.func_150557_a( biome.color, true );
		this.setBiomeName( biome.biomeName + " M" );
		this.setHeightRange( new CubeBiomeGenBase.Height( biome.biomeHeight, biome.biomeVolatility ) );
		this.setTemperatureAndRainfall( biome.temperature, biome.rainfall );
		return this;
	}
}
