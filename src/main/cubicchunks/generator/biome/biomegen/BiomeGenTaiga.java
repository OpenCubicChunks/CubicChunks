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
package cubicchunks.generator.biome.biomegen;

import cuchaz.cubicChunks.generator.populator.DecoratorHelper;
import cuchaz.cubicChunks.generator.populator.WorldGenAbstractTreeCube;
import cuchaz.cubicChunks.generator.populator.WorldGeneratorCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenMegaPineTreeCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenTaiga1Cube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenTaiga2Cube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenTallGrassCube;
import cuchaz.cubicChunks.world.Cube;

import java.util.Random;

import cubicchunks.world.Cube;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenBlockBlob;
<<<<<<< HEAD:src/cuchaz/cubicChunks/generator/biome/biomegen/BiomeGenTaiga.java
=======
import net.minecraft.world.gen.feature.WorldGenMegaPineTree;
import net.minecraft.world.gen.feature.WorldGenTaiga1;
import net.minecraft.world.gen.feature.WorldGenTaiga2;
import net.minecraft.world.gen.feature.WorldGenTallGrass;
import net.minecraft.world.gen.feature.WorldGenerator;
>>>>>>> 0c6cf2e... Refactored the package structure:src/main/java/cubicchunks/generator/biome/biomegen/BiomeGenTaiga.java

public class BiomeGenTaiga extends CubeBiomeGenBase
{
	private static final WorldGenTaiga1Cube wGenTree1 = new WorldGenTaiga1Cube();
	private static final WorldGenTaiga2Cube wGenTree2 = new WorldGenTaiga2Cube( false );
	private static final WorldGenMegaPineTreeCube wGenMegaPineTree1 = new WorldGenMegaPineTreeCube( false, false );
	private static final WorldGenMegaPineTreeCube wGenMegaPineTree2 = new WorldGenMegaPineTreeCube( false, true );
	private static final WorldGenBlockBlob wGenBlockBlob = new WorldGenBlockBlob( Blocks.mossy_cobblestone, 0 );
	private final int type;

	@SuppressWarnings("unchecked")
	public BiomeGenTaiga( int id, int type )
	{
		super( id );
		this.type = type;
		this.spawnableCreatureList.add( new CubeBiomeGenBase.SpawnListEntry( EntityWolf.class, 8, 4, 4 ) );

		CubeBiomeDecorator.DecoratorConfig cfg = this.decorator().decoratorConfig();

		cfg.treesPerColumn( 10 );

		if( type != 1 && type != 2 )
		{
			cfg.grassPerColumn( 1 );
			cfg.mushroomsPerColumn( 1 );
		}
		else
		{
			cfg.grassPerColumn( 7 );
			cfg.deadBushPerColumn( 1 );
			cfg.mushroomsPerColumn( 3 );
		}
	}

	@Override
	public WorldGenAbstractTreeCube checkSpawnTree( Random p_150567_1_ )
	{
		return ((this.type == 1 || this.type == 2) && p_150567_1_.nextInt( 3 ) == 0 ? (this.type != 2 && p_150567_1_.nextInt( 13 ) != 0 ? wGenMegaPineTree1 : wGenMegaPineTree2) : (p_150567_1_.nextInt( 3 ) == 0 ? wGenTree1 : wGenTree2));
	}

	/**
	 * Gets a WorldGen appropriate for this biome.
	 */
	@Override
	public WorldGeneratorCube getRandomWorldGenForGrass( Random par1Random )
	{
		return par1Random.nextInt( 5 ) > 0 ? new WorldGenTallGrassCube( Blocks.tallgrass, 2 ) : new WorldGenTallGrassCube( Blocks.tallgrass, 1 );
	}

	@Override
	public void decorate( World world, Random rand, int x, int y, int z )
	{
		DecoratorHelper gen = new DecoratorHelper( world, rand, x, y, z );

		if( this.type == 1 || this.type == 2 )
		{
			gen.generateAtSurface( wGenBlockBlob, rand.nextInt( 3 ), 1 );
		}

		worldGenDoublePlant.setType( 3 );
		gen.generateAtRandSurfacePlus32( worldGenDoublePlant, 7, 1 );

		super.decorate( world, rand, x, y, z );
	}

	@Override
	public void replaceBlocks( World world, Random rand, Cube cube, Cube above, int xAbs, int zAbs, int top, int bottom, int alterationTop, int seaLevel, double depthNoiseValue )
	{
		if( this.type == 1 || this.type == 2 )
		{
			this.topBlock = Blocks.grass;
			this.field_150604_aj = 0;
			this.fillerBlock = Blocks.dirt;

			if( depthNoiseValue > 1.75D )
			{
				this.topBlock = Blocks.dirt;
				this.field_150604_aj = 1;
			}
			else if( depthNoiseValue > -0.95D )
			{
				this.topBlock = Blocks.dirt;
				this.field_150604_aj = 2;
			}
		}

		this.replaceBlocks_do( world, rand, cube, above, xAbs, zAbs, top, bottom, alterationTop, seaLevel, depthNoiseValue );
	}

	@Override
	protected CubeBiomeGenBase createAndReturnMutated()
	{
		return this.biomeID == CubeBiomeGenBase.megaTaiga.biomeID ? (new BiomeGenTaiga( this.biomeID + 128, 2 )).func_150557_a( 5858897, true ).setBiomeName( "Mega Spruce Taiga" ).func_76733_a( 5159473 ).setTemperatureAndRainfall( 0.25F, 0.8F ).setHeightRange( new CubeBiomeGenBase.Height( this.biomeHeight, this.biomeVolatility ) ) : super.createAndReturnMutated();
	}
}
