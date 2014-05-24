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

import java.util.Random;

import net.minecraft.world.World;
import cuchaz.cubicChunks.world.Cube;

public class BiomeGenOcean extends CubeBiomeGenBase
{
    public BiomeGenOcean(int par1)
    {
        super(par1);
        this.spawnableCreatureList.clear();
    }

    public CubeBiomeGenBase.TempCategory func_150561_m()
    {
        return CubeBiomeGenBase.TempCategory.OCEAN;
    }

    public void modifyBlocks_pre(World world, Random rand, Cube cube, int xAbs, int yAbs, int zAbs, double var)
    {
        super.modifyBlocks_pre(world, rand, cube, xAbs, yAbs, zAbs, var);
    }
}
