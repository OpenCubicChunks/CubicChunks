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

import net.minecraft.init.Blocks;

public class BiomeGenBeach extends CubeBiomeGenBase
{
	public BiomeGenBeach( int id )
	{
		super( id );
		this.spawnableCreatureList.clear();
		this.topBlock = Blocks.sand;
		this.fillerBlock = Blocks.sand;
		this.decorator().treesPerChunk = -999;
		this.decorator().deadBushPerChunk = 0;
		this.decorator().reedsPerChunk = 0;
		this.decorator().cactiPerChunk = 0;
	}
}
