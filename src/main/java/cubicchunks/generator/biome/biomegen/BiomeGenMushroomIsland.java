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

import main.java.cubicchunks.generator.biome.biomegen.CubeBiomeGenBase.SpawnListEntry;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.init.Blocks;

public class BiomeGenMushroomIsland extends CubeBiomeGenBase
{
    private static final String __OBFID = "CL_00000177";

    public BiomeGenMushroomIsland(int par1)
    {
        super(par1);
        this.theBiomeDecorator.treesPerChunk = -100;
        this.theBiomeDecorator.flowersPerChunk = -100;
        this.theBiomeDecorator.grassPerChunk = -100;
        this.theBiomeDecorator.mushroomsPerChunk = 1;
        this.theBiomeDecorator.bigMushroomsPerChunk = 1;
        this.topBlock = Blocks.mycelium;
        this.spawnableMonsterList.clear();
        this.spawnableCreatureList.clear();
        this.spawnableWaterCreatureList.clear();
        this.spawnableCreatureList.add(new CubeBiomeGenBase.SpawnListEntry(EntityMooshroom.class, 8, 4, 8));
    }
}
