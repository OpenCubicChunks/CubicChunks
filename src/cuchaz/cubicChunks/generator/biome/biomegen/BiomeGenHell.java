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

import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.entity.monster.EntityPigZombie;

public class BiomeGenHell extends CubeBiomeGenBase
{
	@SuppressWarnings("unchecked")
	public BiomeGenHell( int id )
	{
		super( id );
		this.spawnableMonsterList.clear();
		this.spawnableCreatureList.clear();
		this.spawnableWaterCreatureList.clear();
		this.spawnableCaveCreatureList.clear();
		this.spawnableMonsterList.add( new CubeBiomeGenBase.SpawnListEntry( EntityGhast.class, 50, 4, 4 ) );
		this.spawnableMonsterList.add( new CubeBiomeGenBase.SpawnListEntry( EntityPigZombie.class, 100, 4, 4 ) );
		this.spawnableMonsterList.add( new CubeBiomeGenBase.SpawnListEntry( EntityMagmaCube.class, 1, 4, 4 ) );
	}
}
