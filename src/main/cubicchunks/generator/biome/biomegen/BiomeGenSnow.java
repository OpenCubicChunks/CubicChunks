/*
 *  This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2014 Tall Worlds
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.generator.biome.biomegen;

import cubicchunks.generator.populator.DecoratorHelper;
import cubicchunks.generator.populator.WorldGenAbstractTreeCube;
import cubicchunks.generator.populator.generators.WorldGenIcePathCube;
import cubicchunks.generator.populator.generators.WorldGenIceSpikeCube;
import cubicchunks.generator.populator.generators.WorldGenTaiga2Cube;
import java.util.Random;
import net.minecraft.block.Blocks;
import net.minecraft.world.World;

public class BiomeGenSnow extends CCBiome
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
	protected CCBiome createAndReturnMutated()
	{
		CCBiome biome = (new BiomeGenSnow( this.biomeID + 128, true )).func_150557_a( 13828095, true ).setBiomeName( this.biomeName + " Spikes" ).setEnableSnow().setTemperatureAndRainfall( 0.0F, 0.5F ).setHeightRange( new CCBiome.Height( this.biomeHeight + 0.1F, this.biomeVolatility + 0.1F ) );
		biome.biomeHeight = this.biomeHeight + 0.3F;
		biome.biomeVolatility = this.biomeVolatility + 0.4F;
		return biome;
	}
}
