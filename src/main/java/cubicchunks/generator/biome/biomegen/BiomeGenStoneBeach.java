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

import net.minecraft.init.Blocks;

public class BiomeGenStoneBeach extends CubeBiomeGenBase
{
    private static final String __OBFID = "CL_00000184";

    public BiomeGenStoneBeach(int p_i45384_1_)
    {
        super(p_i45384_1_);
        this.spawnableCreatureList.clear();
        this.topBlock = Blocks.stone;
        this.fillerBlock = Blocks.stone;
        this.theBiomeDecorator.treesPerChunk = -999;
        this.theBiomeDecorator.deadBushPerChunk = 0;
        this.theBiomeDecorator.reedsPerChunk = 0;
        this.theBiomeDecorator.cactiPerChunk = 0;
    }
}
