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
import cuchaz.cubicChunks.generator.populator.generators.WorldGenDesertWellsCube;
import java.util.Random;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeDecorator.DecoratorConfig.DISABLE;

public class BiomeGenDesert extends CubeBiomeGenBase
{
	public BiomeGenDesert( int par1 )
	{
		super( par1 );
		this.spawnableCreatureList.clear();
		this.topBlock = Blocks.sand;
		this.fillerBlock = Blocks.sand;
		CubeBiomeDecorator.DecoratorConfig cfg = this.decorator().decoratorConfig();
		cfg.treesPerColumn( DISABLE );
		cfg.deadBushPerColumn( 2 );
		cfg.reedsPerColumn( 50 );;
		cfg.cactiPerColumn( 10 );
		this.spawnableCreatureList.clear();
	}

	@Override
	public void decorate( World world, Random rand, int x, int y, int z )
	{
		super.decorate( world, rand, x, y, z );
		DecoratorHelper gen = new DecoratorHelper( world, rand, x, y, z );
		gen.generateAtSurface( new WorldGenDesertWellsCube(), 1, 0.001 );
	}
}
