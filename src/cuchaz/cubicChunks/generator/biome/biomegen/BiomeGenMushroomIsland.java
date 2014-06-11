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

import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.init.Blocks;

public class BiomeGenMushroomIsland extends CubeBiomeGenBase
{
	public BiomeGenMushroomIsland( int par1 )
	{
		super( par1 );

		CubeBiomeDecorator.DecoratorConfig cfg = this.decorator().decoratorConfig();

		cfg.treesPerColumn( -100 );
		cfg.flowersPerColumn( -100 );
		cfg.grassPerColumn( -100 );
		cfg.mushroomsPerColumn( 1 );
		cfg.bigMushroomsPerColumn( 1 );
		
		this.topBlock = Blocks.mycelium;
		this.spawnableMonsterList.clear();
		this.spawnableCreatureList.clear();
		this.spawnableWaterCreatureList.clear();
		this.spawnableCreatureList.add( new CubeBiomeGenBase.SpawnListEntry( EntityMooshroom.class, 8, 4, 8 ) );
	}
}
