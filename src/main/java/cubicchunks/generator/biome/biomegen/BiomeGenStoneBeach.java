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
package main.java.cubicchunks.generator.biome.biomegen;

import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeDecorator.DecoratorConfig.DISABLE;
import net.minecraft.init.Blocks;

public class BiomeGenStoneBeach extends CubeBiomeGenBase
{
	public BiomeGenStoneBeach( int id )
	{
		super( id );
		this.spawnableCreatureList.clear();
		this.topBlock = Blocks.stone;
		this.fillerBlock = Blocks.stone;
		
		CubeBiomeDecorator.DecoratorConfig cfg = this.decorator().decoratorConfig();
		
		cfg.treesPerColumn( DISABLE);
		cfg.deadBushPerColumn( 0);
		cfg.reedsPerColumn( 0);
		cfg.cactiPerColumn( 0);
	}
}
