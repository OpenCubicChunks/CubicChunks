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

import cuchaz.cubicChunks.generator.populator.DecoratorHelper;
import cuchaz.cubicChunks.generator.populator.WorldGenAbstractTreeCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenIcePathCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenIceSpikeCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenTaiga2Cube;
import cuchaz.cubicChunks.util.Coords;
import java.util.Random;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class BiomeGenSnow extends CubeBiomeGenBase
{
	private final boolean genIce;
	private final WorldGenIceSpikeCube wgenIceSpike = new WorldGenIceSpikeCube();
	private final WorldGenIcePathCube wGenIcePath = new WorldGenIcePathCube( 4 );

	public BiomeGenSnow( int id, boolean genIce )
	{
		super( id );
		this.genIce = genIce;

		if( genIce )
		{
			this.topBlock = Blocks.snow;
		}

		this.spawnableCreatureList.clear();
	}

	@Override
	public void decorate( World world, Random rand, int x, int y, int z )
	{
		
		if( this.genIce )
		{
			DecoratorHelper gen = new DecoratorHelper(world, rand, x, y, z );
			gen.generateAtSurface( this.wgenIceSpike, 3, 1);
			gen.generateAtSurface( this.wGenIcePath, 2, 1);
		}

		super.decorate( world, rand, x, y, z );
	}

	@Override
	public WorldGenAbstractTreeCube checkSpawnTree( Random p_150567_1_ )
	{
		return new WorldGenTaiga2Cube( false );
	}

	@Override
	protected CubeBiomeGenBase createAndReturnMutated()
	{
		CubeBiomeGenBase biome = (new BiomeGenSnow( this.biomeID + 128, true )).func_150557_a( 13828095, true ).setBiomeName( this.biomeName + " Spikes" ).setEnableSnow().setTemperatureAndRainfall( 0.0F, 0.5F ).setHeightRange( new CubeBiomeGenBase.Height( this.biomeHeight + 0.1F, this.biomeVolatility + 0.1F ) );
		biome.biomeHeight = this.biomeHeight + 0.3F;
		biome.biomeVolatility = this.biomeVolatility + 0.4F;
		return biome;
	}
}
