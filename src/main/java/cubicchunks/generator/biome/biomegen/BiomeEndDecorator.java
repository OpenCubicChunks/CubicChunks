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

import cuchaz.cubicChunks.generator.populator.WorldGeneratorCube;
import cuchaz.cubicChunks.generator.populator.generators.WorldGenSpikesCube;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.init.Blocks;

public class BiomeEndDecorator extends CubeBiomeDecorator
{
	protected WorldGeneratorCube spikeGen;

	public BiomeEndDecorator()
	{
		this.spikeGen = new WorldGenSpikesCube( Blocks.end_stone );
	}

	@Override
	protected void decorate_do( CubeBiomeGenBase biome )
	{
		this.generateOres();

		gen.generateAtSurface( this.spikeGen, 1, 0.2);

		if( gen.chunk_X == 0 && gen.chunk_Z == 0 )
		{
			EntityDragon dragon = new EntityDragon( this.currentWorld );
			dragon.setLocationAndAngles( 0.0D, 128.0D, 0.0D, this.randomGenerator.nextFloat() * 360.0F, 0.0F );
			gen.world.spawnEntityInWorld( dragon );
		}
	}
}
