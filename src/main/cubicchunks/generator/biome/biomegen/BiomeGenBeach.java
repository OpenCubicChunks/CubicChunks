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

import net.minecraft.init.Blocks;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeDecorator.DecoratorConfig.DISABLE;

public class BiomeGenBeach extends CubeBiomeGenBase
{
	public BiomeGenBeach( int id )
	{
		super( id );
		this.spawnableCreatureList.clear();
		this.topBlock = Blocks.sand;
		this.fillerBlock = Blocks.sand;
		CubeBiomeDecorator.DecoratorConfig cfg = this.decorator().decoratorConfig();
		cfg.treesPerColumn( DISABLE );
		cfg.deadBushPerColumn( 0 );
		cfg.reedsPerColumn( 0 );
		cfg.cactiPerColumn( 0 );
	}
}
